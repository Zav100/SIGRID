package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.HorarioAperturaDAO;
import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dto.Apertura;
import com.sigrid.sigrid.dto.HorarioFila;
import com.sigrid.sigrid.repositorio.HorarioApertura;
import com.sigrid.sigrid.repositorio.Instalacion;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Horario de apertura de las instalaciones de acceso LIBRE (la pileta). Regla del polideportivo: la pileta no se reserva
 * como una cancha; está abierta por franjas horarias (semanales o de un día específico) y se entra cuando se quiera mientras
 * esté abierta, sin límite de tiempo por persona. Por eso acá solo se guarda cuándo abre, no quién entra ni por cuánto.
 * Las franjas de un día específico se suman a las semanales.
 *
 * CDI simple (no EJB) y Serializable, igual que DashboardServicio. Las transacciones las abre este servicio.
 */
@Dependent
public class HorarioServicio implements Serializable {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final String[] DIAS_LARGOS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};
    private static final int DIAS_A_MIRAR = 14; // hasta dónde busca la próxima apertura

    @Inject
    private HorarioAperturaDAO horarioDAO;
    @Inject
    private InstalacionDAO instalacionDAO;

    // ---------- lectura ----------

    /** Cómo está la instalación en este momento: abierta hasta cuándo, o cerrada y cuándo abre. */
    public Apertura ahora(Integer idInstalacion, LocalDateTime ahora) {
        List<HorarioApertura> franjas = horarioDAO.listarPorInstalacion(idInstalacion);
        LocalDate hoy = ahora.toLocalDate();
        LocalTime hora = ahora.toLocalTime();

        for (HorarioApertura h : franjas) {
            if (h.rigeEl(hoy) && !hora.isBefore(h.getHoraApertura()) && hora.isBefore(h.getHoraCierre())) {
                return new Apertura(true, "Abierta hasta " + h.getHoraCierre().format(HORA));
            }
        }
        for (int d = 0; d <= DIAS_A_MIRAR; d++) {
            LocalDate dia = hoy.plusDays(d);
            final boolean esHoy = d == 0;
            LocalTime abre = franjas.stream()
                    .filter(h -> h.rigeEl(dia) && (!esHoy || h.getHoraApertura().isAfter(hora)))
                    .map(HorarioApertura::getHoraApertura).min(Comparator.naturalOrder()).orElse(null);
            if (abre != null) {
                String cuando = d == 0 ? "hoy a las " + abre.format(HORA)
                        : d == 1 ? "mañana " + abre.format(HORA)
                        : dia.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", "") + " "
                        + dia.format(DIA_MES) + " " + abre.format(HORA);
                return new Apertura(false, "Cerrada · abre " + cuando);
            }
        }
        return new Apertura(false, franjas.isEmpty() ? "Sin horarios cargados" : "Cerrada");
    }

    /** Cómo está el día elegido en el calendario: el ahora si es hoy; si no, sus franjas. */
    public Apertura enDia(Integer idInstalacion, LocalDate dia, LocalDateTime ahora) {
        if (dia.equals(ahora.toLocalDate())) {
            return ahora(idInstalacion, ahora);
        }
        List<String> franjas = new ArrayList<>();
        for (HorarioApertura h : horarioDAO.listarPorInstalacion(idInstalacion)) {
            if (h.rigeEl(dia)) {
                franjas.add(h.getHoraApertura().format(HORA) + " – " + h.getHoraCierre().format(HORA));
            }
        }
        return franjas.isEmpty() ? new Apertura(false, "Cerrada ese día")
                : new Apertura(true, "Abre " + String.join(" · ", franjas));
    }

    /** Las franjas para mostrar: semanales agrupadas por horario ("Lun a Vie · 09:00 – 19:00") y después los días específicos que vienen. */
    public List<HorarioFila> listar(Integer idInstalacion, LocalDate hoy) {
        Map<String, List<HorarioApertura>> semanales = new LinkedHashMap<>();
        List<HorarioFila> filas = new ArrayList<>();
        List<HorarioFila> deFecha = new ArrayList<>();
        for (HorarioApertura h : horarioDAO.listarPorInstalacion(idInstalacion)) {
            String horario = h.getHoraApertura().format(HORA) + " – " + h.getHoraCierre().format(HORA);
            if (h.getFecha() == null) {
                semanales.computeIfAbsent(horario, k -> new ArrayList<>()).add(h);
            } else if (!h.getFecha().isBefore(hoy)) {
                deFecha.add(new HorarioFila(h.getFecha().getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", "")
                        + " " + h.getFecha().format(DIA_MES), horario, String.valueOf(h.getIdHorario()), true));
            }
        }
        for (Map.Entry<String, List<HorarioApertura>> e : semanales.entrySet()) {
            List<HorarioApertura> grupo = e.getValue();
            grupo.sort(Comparator.comparing(HorarioApertura::getDiaSemana));
            List<Integer> dias = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (HorarioApertura h : grupo) {
                dias.add(h.getDiaSemana());
                ids.add(String.valueOf(h.getIdHorario()));
            }
            filas.add(new HorarioFila(textoDias(dias), e.getKey(), String.join(",", ids), false));
        }
        filas.addAll(deFecha); // los grupos ya salen por su primer día: el DAO ordena por día de la semana
        return filas;
    }

    /** "Lun a Vie", "Sáb y Dom", "Lun, Mié y Vie": rachas de 3 o más días seguidos se abrevian con "a". */
    static String textoDias(List<Integer> dias) {
        List<String> partes = new ArrayList<>();
        int i = 0;
        while (i < dias.size()) {
            int j = i;
            while (j + 1 < dias.size() && dias.get(j + 1) == dias.get(j) + 1) {
                j++;
            }
            if (j - i + 1 >= 3) {
                partes.add(DIAS[dias.get(i) - 1] + " a " + DIAS[dias.get(j) - 1]);
            } else {
                for (int k = i; k <= j; k++) {
                    partes.add(DIAS[dias.get(k) - 1]);
                }
            }
            i = j + 1;
        }
        int n = partes.size();
        return n == 1 ? partes.get(0) : String.join(", ", partes.subList(0, n - 1)) + " y " + partes.get(n - 1);
    }

    // ---------- escritura (devuelven null si salió bien, o el motivo por el que no se pudo) ----------

    /** Abre la instalación esos días de la semana (1 = lunes ... 7 = domingo) en ese horario, todas las semanas. */
    @Transactional
    public String agregarSemanal(Integer idInstalacion, List<Integer> dias, LocalTime apertura, LocalTime cierre) {
        String error = validar(idInstalacion, apertura, cierre);
        if (error != null) {
            return error;
        }
        if (dias == null || dias.isEmpty()) {
            return "Elegí al menos un día de la semana.";
        }
        Instalacion instalacion = instalacionDAO.buscarPorId(idInstalacion);
        List<HorarioApertura> existentes = horarioDAO.listarPorInstalacion(idInstalacion);
        for (Integer dia : dias) {
            for (HorarioApertura h : existentes) {
                if (dia.equals(h.getDiaSemana()) && seSuperpone(h, apertura, cierre)) {
                    return "Ya hay una franja abierta el " + DIAS_LARGOS[dia - 1] + " que se superpone con ese horario.";
                }
            }
        }
        for (Integer dia : dias) {
            HorarioApertura h = nueva(instalacion, apertura, cierre);
            h.setDiaSemana(dia);
            horarioDAO.crear(h);
        }
        return null;
    }

    /** Abre la instalación un día específico (se suma al horario semanal). */
    @Transactional
    public String agregarDeFecha(Integer idInstalacion, LocalDate fecha, LocalTime apertura, LocalTime cierre, LocalDate hoy) {
        String error = validar(idInstalacion, apertura, cierre);
        if (error != null) {
            return error;
        }
        if (fecha == null) {
            return "Elegí el día.";
        }
        if (fecha.isBefore(hoy)) {
            return "Ese día ya pasó.";
        }
        for (HorarioApertura h : horarioDAO.listarPorInstalacion(idInstalacion)) {
            if (h.rigeEl(fecha) && seSuperpone(h, apertura, cierre)) {
                return "Ese día ya está abierto en un horario que se superpone con ese.";
            }
        }
        HorarioApertura h = nueva(instalacionDAO.buscarPorId(idInstalacion), apertura, cierre);
        h.setFecha(fecha);
        horarioDAO.crear(h);
        return null;
    }

    /** Quita una franja (todas las filas de su grupo). Solo borra las que son de esa instalación. */
    @Transactional
    public void quitar(Integer idInstalacion, List<Integer> ids) {
        for (Integer id : ids) {
            HorarioApertura h = horarioDAO.buscarPorId(id);
            if (h != null && h.getIdInstalacion().getIdInstalacion().equals(idInstalacion)) {
                horarioDAO.quitar(id);
            }
        }
    }

    // ---------- internos ----------

    private String validar(Integer idInstalacion, LocalTime apertura, LocalTime cierre) {
        Instalacion instalacion = idInstalacion == null ? null : instalacionDAO.buscarPorId(idInstalacion);
        if (instalacion == null) {
            return "La instalación no existe.";
        }
        if (instalacion.getTipoAcceso() != Instalacion.TipoAcceso.LIBRE) {
            return "Esta instalación se reserva por turnos: no tiene horario de apertura.";
        }
        if (apertura == null || cierre == null) {
            return "Completá la hora de apertura y la de cierre.";
        }
        if (!cierre.isAfter(apertura)) {
            return "El cierre tiene que ser posterior a la apertura.";
        }
        return null;
    }

    private static boolean seSuperpone(HorarioApertura h, LocalTime apertura, LocalTime cierre) {
        return apertura.isBefore(h.getHoraCierre()) && cierre.isAfter(h.getHoraApertura());
    }

    private static HorarioApertura nueva(Instalacion instalacion, LocalTime apertura, LocalTime cierre) {
        HorarioApertura h = new HorarioApertura();
        h.setIdInstalacion(instalacion);
        h.setHoraApertura(apertura);
        h.setHoraCierre(cierre);
        return h;
    }
}
