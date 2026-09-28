package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.CategoriaSocioDAO;
import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.TarifaAlquilerDAO;
import com.sigrid.sigrid.dao.TarifaMembresiaDAO;
import com.sigrid.sigrid.dto.HistorialTarifa;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Tarifa;
import com.sigrid.sigrid.repositorio.TarifaAlquiler;
import com.sigrid.sigrid.repositorio.TarifaMembresia;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * Cambio de precios de las instalaciones (por tipo de socio) y de la cuota de membresía. Se hace desde las mismas vistas
 * que los muestran (instalaciones y membresías), no desde una pantalla propia. Reglas:
 * - Un precio nunca se edita: se agrega uno nuevo con la fecha desde la que rige y el anterior se cierra el día previo.
 *   Por eso el cambio rige desde hoy o desde una fecha futura (queda programado), nunca hacia atrás, y no se puede pisar
 *   uno que ya tenga fecha igual o posterior.
 * - Se cargan los 4 tipos de socio juntos y solo se agrega fila para los que cambiaron.
 * - El alumno paga siempre menos que los demás y el externo siempre más.
 *
 * CDI simple (no EJB) y Serializable, igual que MembresiasServicio. Las transacciones las abre este servicio.
 */
@Dependent
public class TarifasServicio implements Serializable {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal MAXIMO = new BigDecimal("99999999.99"); // DECIMAL(10,2)

    @Inject
    private TarifaAlquilerDAO alquilerDAO;
    @Inject
    private TarifaMembresiaDAO membresiaDAO;
    @Inject
    private CategoriaSocioDAO categoriaDAO;
    @Inject
    private InstalacionDAO instalacionDAO;

    // ---------- lectura ----------

    /** Los tipos de socio, en el orden de las columnas de precios. */
    public List<CategoriaSocio> categorias() {
        List<CategoriaSocio> categorias = new ArrayList<>(categoriaDAO.listarTodos());
        categorias.sort(Comparator.comparing(CategoriaSocio::getIdCategoriaSocio));
        return categorias;
    }

    /** Lo que rige hoy por tipo de socio (id de la categoría -> "25000"; vacío si no hay): sirve para llenar el formulario. */
    public Map<Integer, String> vigentesAlquiler(Integer idInstalacion, LocalDate hoy) {
        return vigentes(alquilerDAO.listarPorInstalacion(idInstalacion), hoy);
    }

    public Map<Integer, String> vigentesMembresia(LocalDate hoy) {
        return vigentes(membresiaDAO.listarHistorial(), hoy);
    }

    /** Cambios de precio de una instalación, el más reciente primero (los programados arriba). */
    public List<HistorialTarifa> historialAlquiler(Integer idInstalacion, LocalDate hoy) {
        return historial(alquilerDAO.listarPorInstalacion(idInstalacion), hoy);
    }

    public List<HistorialTarifa> historialMembresia(LocalDate hoy) {
        return historial(membresiaDAO.listarHistorial(), hoy);
    }

    private Map<Integer, String> vigentes(List<? extends Tarifa> historial, LocalDate hoy) {
        Map<Integer, String> vigentes = new LinkedHashMap<>();
        for (CategoriaSocio c : categorias()) {
            Tarifa t = enVigencia(historial, c.getIdCategoriaSocio(), hoy);
            vigentes.put(c.getIdCategoriaSocio(), t == null ? "" : t.getPrecio().stripTrailingZeros().toPlainString());
        }
        return vigentes;
    }

    private static <T extends Tarifa> List<HistorialTarifa> historial(List<T> filas, LocalDate hoy) {
        Map<LocalDate, List<T>> porFecha = new TreeMap<>(Comparator.reverseOrder());
        for (T t : filas) {
            porFecha.computeIfAbsent(t.getVigenteDesde(), k -> new ArrayList<>()).add(t);
        }
        List<HistorialTarifa> historial = new ArrayList<>();
        for (Map.Entry<LocalDate, List<T>> e : porFecha.entrySet()) {
            e.getValue().sort(Comparator.comparing(t -> t.getIdCategoriaSocio().getIdCategoriaSocio()));
            List<String> cambios = new ArrayList<>();
            for (T t : e.getValue()) {
                cambios.add(t.getIdCategoriaSocio().getNombreCategoria() + " " + DashboardServicio.monto(t.getPrecio()));
            }
            historial.add(new HistorialTarifa("Desde el " + e.getKey().format(FECHA), String.join(" · ", cambios),
                    e.getKey().isAfter(hoy)));
        }
        return historial;
    }

    /** Regla del polideportivo: el alumno paga menos que todos y el externo más que todos. Con un precio sin cargar no hay qué comparar. */
    public static boolean ordenValido(List<CategoriaSocio> categorias, List<BigDecimal> importes) {
        BigDecimal alumno = null;
        BigDecimal externo = null;
        List<BigDecimal> otros = new ArrayList<>();
        for (int i = 0; i < categorias.size(); i++) {
            BigDecimal precio = importes.get(i);
            String nombre = DashboardServicio.normalizar(categorias.get(i).getNombreCategoria());
            if (precio == null) {
                return true;
            } else if (nombre.contains("alumno")) {
                alumno = precio;
            } else if (nombre.contains("externo")) {
                externo = precio;
            } else {
                otros.add(precio);
            }
        }
        if (alumno == null || externo == null) {
            return true;
        }
        if (alumno.compareTo(externo) >= 0) {
            return false;
        }
        for (BigDecimal otro : otros) {
            if (alumno.compareTo(otro) >= 0 || externo.compareTo(otro) <= 0) {
                return false;
            }
        }
        return true;
    }

    // ---------- cambios (devuelven null si salió bien, o el motivo por el que no se pudo) ----------

    /** Cambia lo que paga cada tipo de socio por reservar una instalación, desde la fecha dada (id de la categoría -> precio). */
    @Transactional
    public String cambiarAlquiler(Integer idInstalacion, Map<Integer, String> nuevos, LocalDate desde, LocalDate hoy) {
        Instalacion instalacion = idInstalacion == null ? null : instalacionDAO.buscarPorId(idInstalacion);
        if (instalacion == null) {
            return "La instalación no existe.";
        }
        if (instalacion.getTipoAcceso() != Instalacion.TipoAcceso.ARANCELADO) {
            return "Esta instalación no se reserva: no tiene precios.";
        }
        return aplicar(alquilerDAO.listarPorInstalacion(idInstalacion), nuevos, desde, hoy, (categoria, precio) -> {
            TarifaAlquiler t = new TarifaAlquiler();
            t.setIdInstalacion(instalacion);
            t.setIdCategoriaSocio(categoria);
            t.setPrecio(precio);
            t.setVigenteDesde(desde);
            alquilerDAO.crear(t);
        });
    }

    /** Cambia la cuota mensual de cada tipo de socio, desde la fecha dada (id de la categoría -> precio). */
    @Transactional
    public String cambiarMembresia(Map<Integer, String> nuevos, LocalDate desde, LocalDate hoy) {
        return aplicar(membresiaDAO.listarHistorial(), nuevos, desde, hoy, (categoria, precio) -> {
            TarifaMembresia t = new TarifaMembresia();
            t.setIdCategoriaSocio(categoria);
            t.setPrecio(precio);
            t.setVigenteDesde(desde);
            membresiaDAO.crear(t);
        });
    }

    private <T extends Tarifa> String aplicar(List<T> historial, Map<Integer, String> nuevos, LocalDate desde, LocalDate hoy,
            BiConsumer<CategoriaSocio, BigDecimal> crear) {
        if (desde == null) {
            return "Elegí desde qué día rige el precio nuevo.";
        }
        if (desde.isBefore(hoy)) {
            return "La fecha no puede ser anterior a hoy: los precios no se cambian hacia atrás.";
        }
        List<CategoriaSocio> categorias = categorias();
        List<BigDecimal> finales = new ArrayList<>();
        for (CategoriaSocio c : categorias) {
            BigDecimal precio = parsear(nuevos.get(c.getIdCategoriaSocio()));
            if (precio == null) {
                return "Completá el precio de " + c.getNombreCategoria() + " con un número mayor a cero (hasta 2 decimales).";
            }
            finales.add(precio);
        }

        List<CategoriaSocio> cambian = new ArrayList<>();
        for (int i = 0; i < categorias.size(); i++) {
            CategoriaSocio c = categorias.get(i);
            T rige = enVigencia(historial, c.getIdCategoriaSocio(), desde);
            if (rige != null && rige.getPrecio().compareTo(finales.get(i)) == 0) {
                continue;
            }
            for (T t : historial) {
                if (t.getIdCategoriaSocio().getIdCategoriaSocio().equals(c.getIdCategoriaSocio())
                        && !t.getVigenteDesde().isBefore(desde)) {
                    return "Ya hay un precio de " + c.getNombreCategoria() + " desde el " + t.getVigenteDesde().format(FECHA)
                            + ": elegí una fecha posterior.";
                }
            }
            cambian.add(c);
        }
        if (cambian.isEmpty()) {
            return "No cambiaste ningún precio.";
        }
        if (!ordenValido(categorias, finales)) {
            return "El alumno tiene que pagar menos que los demás y el externo más que los demás.";
        }

        for (CategoriaSocio c : cambian) {
            T anterior = null; // la que regía antes de la fecha nueva: se cierra el día previo
            for (T t : historial) {
                if (t.getIdCategoriaSocio().getIdCategoriaSocio().equals(c.getIdCategoriaSocio())
                        && (anterior == null || t.getVigenteDesde().isAfter(anterior.getVigenteDesde()))) {
                    anterior = t;
                }
            }
            if (anterior != null && (anterior.getVigenteHasta() == null || !anterior.getVigenteHasta().isBefore(desde))) {
                anterior.setVigenteHasta(desde.minusDays(1));
            }
            crear.accept(c, finales.get(categorias.indexOf(c)));
        }
        return null;
    }

    /** La tarifa de la categoría que rige ese día, o null. */
    private static <T extends Tarifa> T enVigencia(List<T> historial, Integer idCategoria, LocalDate dia) {
        T elegida = null;
        for (T t : historial) {
            if (t.getIdCategoriaSocio().getIdCategoriaSocio().equals(idCategoria) && !t.getVigenteDesde().isAfter(dia)
                    && (t.getVigenteHasta() == null || !t.getVigenteHasta().isBefore(dia))
                    && (elegida == null || t.getVigenteDesde().isAfter(elegida.getVigenteDesde()))) {
                elegida = t;
            }
        }
        return elegida;
    }

    /** Un precio válido (mayor a cero, hasta 2 decimales, entra en DECIMAL(10,2)) o null. Acepta coma decimal. */
    private static BigDecimal parsear(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            BigDecimal precio = new BigDecimal(texto.trim().replace(',', '.'));
            if (precio.signum() <= 0 || precio.stripTrailingZeros().scale() > 2 || precio.compareTo(MAXIMO) > 0) {
                return null;
            }
            return precio.setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }
}
