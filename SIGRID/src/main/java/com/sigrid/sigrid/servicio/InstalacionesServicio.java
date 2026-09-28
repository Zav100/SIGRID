package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.TarifaAlquilerDAO;
import com.sigrid.sigrid.dao.TurnoDAO;
import com.sigrid.sigrid.dto.Apertura;
import com.sigrid.sigrid.dto.Conflictos;
import com.sigrid.sigrid.dto.Demanda;
import com.sigrid.sigrid.dto.EspacioPlano;
import com.sigrid.sigrid.dto.FichaInstalacion;
import com.sigrid.sigrid.dto.PrecioFila;
import com.sigrid.sigrid.dto.RankingItem;
import com.sigrid.sigrid.dto.ReservaFila;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.TarifaAlquiler;
import com.sigrid.sigrid.repositorio.Turno;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista de instalaciones del administrador: el plano (por estado de hoy o por ocupación), los precios de reserva por tipo
 * de socio, la demanda de cada instalación y la ficha con las acciones que cambian su vida en el sistema:
 * - poner en mantenimiento (temporal, con fecha estimada de reactivación opcional) y reactivar;
 * - dar de baja (definitiva);
 * - editar descripción y capacidad.
 * Sacar una instalación de servicio no se hace pisando reservas: las confirmadas se cancelan por el predio (quedan con el
 * crédito de reprogramación de 30 días, ver CambiosReservaServicio), las que esperan el pago se cancelan sin más y las que ya
 * tienen comprobante por validar bloquean la operación hasta que el administrador las resuelva.
 *
 * Las instalaciones de acceso LIBRE (la pileta) no se reservan, no tienen turnos ni precios: se manejan con HorarioServicio.
 *
 * CDI simple (no EJB) y Serializable, igual que DashboardServicio. Las transacciones las abre este servicio.
 */
@Dependent
public class InstalacionesServicio implements Serializable {

    /** Ventana de la demanda y la ocupación: los últimos 30 días, hoy incluido. */
    public static final int DIAS_DEMANDA = 30;

    private static final int DIAS_PROXIMAS = 60;
    private static final int MAX_PROXIMAS = 3;
    private static final int OCUPACION_ALTA = 25; // desde acá se pinta como "muy usada"
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    @Inject
    private InstalacionDAO instalacionDAO;
    @Inject
    private TurnoDAO turnoDAO;
    @Inject
    private TarifaAlquilerDAO tarifaDAO;
    @Inject
    private TarifasServicio tarifasServicio;
    @Inject
    private ReservaDAO reservaDAO;
    @Inject
    private HorarioServicio horarioServicio;
    @Inject
    private DashboardServicio dashboardServicio;
    @Inject
    private CambiosReservaServicio cambiosServicio;

    // ---------- lectura ----------

    /** Todas las instalaciones, por nombre. */
    public List<Instalacion> listar() {
        List<Instalacion> lista = new ArrayList<>(instalacionDAO.listarTodos());
        lista.sort(Comparator.comparing(i -> DashboardServicio.normalizar(i.getNombre())));
        return lista;
    }

    /** Reservas activas y lo cobrado por instalación en los últimos DIAS_DEMANDA días (las que no aparecen no tuvieron nada). */
    public Map<Integer, Demanda> demanda(LocalDate hoy) {
        Map<Integer, Demanda> demanda = new HashMap<>();
        for (Object[] fila : reservaDAO.resumenPorInstalacion(hoy.minusDays(DIAS_DEMANDA - 1L), hoy)) {
            Integer id = (Integer) fila[0];
            boolean confirmada = fila[1] == Reserva.Estado.CONFIRMADA;
            long cantidad = (Long) fila[2];
            BigDecimal cobrado = confirmada && fila[3] != null ? (BigDecimal) fila[3] : BigDecimal.ZERO;
            Demanda antes = demanda.getOrDefault(id, Demanda.VACIA);
            demanda.put(id, new Demanda(antes.getReservas() + cantidad,
                    antes.getConfirmadas() + (confirmada ? cantidad : 0), antes.getIngresos().add(cobrado)));
        }
        return demanda;
    }

    /** Porcentaje de las franjas reservables de los últimos días que se reservaron (0 a 100; 0 si no tiene turnos). */
    public int ocupacion(Instalacion instalacion, Demanda demanda, Map<Integer, Long> turnos) {
        long franjas = turnos.getOrDefault(instalacion.getIdInstalacion(), 0L) * DIAS_DEMANDA;
        return franjas == 0 ? 0 : (int) Math.min(100, Math.round(100.0 * demanda.getReservas() / franjas));
    }

    public Map<Integer, Long> turnosPorInstalacion() {
        return turnoDAO.contarPorInstalacion();
    }

    /**
     * Plano del predio. Por estado: gris si no está disponible, con color si hoy tiene reservas (ámbar si alguna está por
     * confirmar) o si la de acceso libre está abierta, y neutro si está libre o cerrada. Por ocupación: color si se reservó
     * OCUPACION_ALTA % o más de las franjas de los últimos días (el porcentaje se lee en cada espacio).
     */
    public List<EspacioPlano> plano(boolean porOcupacion, Map<Integer, Demanda> demanda, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        List<Instalacion> instalaciones = instalacionDAO.listarTodos();
        Map<Integer, Long> turnos = turnoDAO.contarPorInstalacion();

        Map<Integer, int[]> deHoy = new HashMap<>(); // por instalación: [0] reservas, [1] por confirmar
        for (Reserva r : reservaDAO.listarDelDia(hoy)) {
            if (r.getEstado() != Reserva.Estado.CANCELADA) {
                int[] c = deHoy.computeIfAbsent(r.getIdTurno().getIdInstalacion().getIdInstalacion(), k -> new int[2]);
                c[0]++;
                c[1] += r.getEstado() == Reserva.Estado.PENDIENTE_PAGO ? 1 : 0;
            }
        }

        List<EspacioPlano> plano = new ArrayList<>();
        for (String[] espacio : DashboardServicio.PLANO) {
            Instalacion i = instalaciones.stream()
                    .filter(x -> DashboardServicio.normalizar(x.getNombre()).contains(espacio[1])).findFirst().orElse(null);
            String nombre = i == null ? espacio[2] : i.getNombre();

            String estado;
            String detalle;
            if (i == null || i.getEstado() != Instalacion.Estado.HABILITADA) {
                estado = "off";
                detalle = i == null ? "Sin cargar" : i.getEstado() == Instalacion.Estado.DESHABILITADA_MANTENIMIENTO
                        ? "En mantenimiento" : "Deshabilitada";
            } else if (i.getTipoAcceso() == Instalacion.TipoAcceso.LIBRE) {
                Apertura apertura = horarioServicio.ahora(i.getIdInstalacion(), ahora);
                estado = !porOcupacion && apertura.isAbierta() ? "ok" : "free";
                detalle = porOcupacion ? "Acceso libre" : apertura.getTexto();
            } else if (porOcupacion) {
                int pct = ocupacion(i, demanda.getOrDefault(i.getIdInstalacion(), Demanda.VACIA), turnos);
                estado = pct >= OCUPACION_ALTA ? "ok" : "free";
                detalle = pct + " % ocupada";
            } else {
                int[] c = deHoy.getOrDefault(i.getIdInstalacion(), new int[2]);
                estado = c[0] == 0 ? "free" : c[1] > 0 ? "warn" : "ok";
                detalle = c[0] == 0 ? "Libre" : c[0] + (c[0] == 1 ? " reserva" : " reservas");
            }
            plano.add(new EspacioPlano(i == null ? null : i.getIdInstalacion(), espacio[0], nombre,
                    DashboardServicio.icono(espacio[3]), estado, detalle, List.of()));
        }
        return plano;
    }

    /** Los tipos de socio, en el orden de las columnas de precios. */
    public List<CategoriaSocio> categorias() {
        return tarifasServicio.categorias();
    }

    /**
     * Lo que cuesta reservar cada instalación para cada tipo de socio (tarifa vigente hoy). Las de acceso libre no se reservan.
     * ponytail: un solo SELECT de todas las tarifas vigentes; si el catálogo creciera a miles de filas, filtrar por instalación.
     */
    public List<PrecioFila> precios(List<Instalacion> instalaciones, List<CategoriaSocio> categorias, LocalDate hoy) {
        Map<String, BigDecimal> vigentes = new HashMap<>(); // "instalación-categoría" -> precio; la más reciente pisa a la anterior
        for (TarifaAlquiler t : tarifaDAO.listarVigentes(hoy)) {
            vigentes.put(t.getIdInstalacion().getIdInstalacion() + "-" + t.getIdCategoriaSocio().getIdCategoriaSocio(),
                    t.getPrecio());
        }
        List<PrecioFila> filas = new ArrayList<>();
        for (Instalacion i : instalaciones) {
            String icono = DashboardServicio.icono(i.getTipoDisciplina());
            if (i.getTipoAcceso() == Instalacion.TipoAcceso.LIBRE) {
                filas.add(new PrecioFila(i.getIdInstalacion(), i.getNombre(), icono, true, List.of(), true));
                continue;
            }
            List<BigDecimal> importes = new ArrayList<>();
            List<String> textos = new ArrayList<>();
            for (CategoriaSocio c : categorias) {
                BigDecimal precio = vigentes.get(i.getIdInstalacion() + "-" + c.getIdCategoriaSocio());
                importes.add(precio);
                textos.add(precio == null ? null : DashboardServicio.monto(precio));
            }
            filas.add(new PrecioFila(i.getIdInstalacion(), i.getNombre(), icono, false, textos,
                    TarifasServicio.ordenValido(categorias, importes)));
        }
        return filas;
    }

    /** Las instalaciones que se reservan, de la más a la menos solicitada en los últimos días (las de acceso libre no entran). */
    public List<RankingItem> ranking(List<Instalacion> instalaciones, Map<Integer, Demanda> demanda) {
        List<Instalacion> reservables = new ArrayList<>();
        for (Instalacion i : instalaciones) {
            if (i.getTipoAcceso() == Instalacion.TipoAcceso.ARANCELADO) {
                reservables.add(i);
            }
        }
        reservables.sort(Comparator.<Instalacion>comparingLong(
                i -> -demanda.getOrDefault(i.getIdInstalacion(), Demanda.VACIA).getReservas()));
        long primero = reservables.isEmpty() ? 0
                : demanda.getOrDefault(reservables.get(0).getIdInstalacion(), Demanda.VACIA).getReservas();
        List<RankingItem> ranking = new ArrayList<>();
        for (Instalacion i : reservables) {
            long cantidad = demanda.getOrDefault(i.getIdInstalacion(), Demanda.VACIA).getReservas();
            ranking.add(new RankingItem(i.getNombre(), DashboardServicio.icono(i.getTipoDisciplina()), cantidad,
                    primero == 0 ? 0 : (int) Math.round(100.0 * cantidad / primero)));
        }
        return ranking;
    }

    /** La ficha de una instalación; null si ya no existe. */
    public FichaInstalacion ficha(Integer idInstalacion, Map<Integer, Demanda> demanda, LocalDateTime ahora) {
        Instalacion i = idInstalacion == null ? null : instalacionDAO.buscarPorId(idInstalacion);
        if (i == null) {
            return null;
        }
        LocalDate hoy = ahora.toLocalDate();
        Demanda d = demanda.getOrDefault(i.getIdInstalacion(), Demanda.VACIA);
        boolean reservable = i.getTipoAcceso() == Instalacion.TipoAcceso.ARANCELADO;

        FichaInstalacion f = new FichaInstalacion();
        f.setIdInstalacion(i.getIdInstalacion());
        f.setNombre(i.getNombre());
        f.setIcono(DashboardServicio.icono(i.getTipoDisciplina()));
        f.setDisciplina(i.getTipoDisciplina());
        f.setDescripcion(i.getDescripcion());
        f.setCapacidad(i.getCapacidad());
        f.setReservable(reservable);
        f.setEstado(i.getEstado().name());
        f.setReservas(d.getReservas());
        f.setIngresos(DashboardServicio.monto(d.getIngresos()));
        switch (i.getEstado()) {
            case HABILITADA:
                f.setEstadoTexto("Habilitada");
                f.setEstadoClase("ok");
                break;
            case DESHABILITADA_MANTENIMIENTO:
                f.setEstadoTexto("En mantenimiento");
                f.setEstadoClase("warn");
                break;
            default:
                f.setEstadoTexto("Dada de baja");
                f.setEstadoClase("off");
        }
        if (i.getEstado() != Instalacion.Estado.HABILITADA) {
            f.setMotivoBaja(i.getMotivoBaja());
            f.setBajaDetalle(detalleDeBaja(i, hoy));
            f.setReactivacionVencida(i.getEstado() == Instalacion.Estado.DESHABILITADA_MANTENIMIENTO
                    && i.getFechaFinBaja() != null && i.getFechaFinBaja().isBefore(hoy));
        }

        if (reservable) {
            List<String> turnos = new ArrayList<>();
            for (Turno t : turnoDAO.listarPorInstalacion(i.getIdInstalacion())) {
                turnos.add(t.getHoraInicio().format(HORA) + " – " + t.getHoraFin().format(HORA));
            }
            f.setTurnos(turnos);
            f.setOcupacion(ocupacion(i, d, turnoDAO.contarPorInstalacion()));

            List<Reserva> proximas = new ArrayList<>();
            for (Reserva r : reservaDAO.listarActivasDeInstalacionEntre(i.getIdInstalacion(), hoy, hoy.plusDays(DIAS_PROXIMAS))) {
                if (proximas.size() < MAX_PROXIMAS && r.getFechaTurno().atTime(r.getIdTurno().getHoraFin()).isAfter(ahora)) {
                    proximas.add(r);
                }
            }
            f.setProximas(dashboardServicio.aFilas(proximas, hoy));
        } else {
            Apertura apertura = horarioServicio.ahora(i.getIdInstalacion(), ahora);
            f.setApertura(apertura.getTexto());
            f.setAbierta(apertura.isAbierta());
            f.setHorarios(horarioServicio.listar(i.getIdInstalacion(), hoy));
        }
        return f;
    }

    /** "Desde el 26/09/2026 · reactivación estimada el 01/10/2026 (en 5 días)". */
    private static String detalleDeBaja(Instalacion i, LocalDate hoy) {
        String desde = i.getFechaInicioBaja() == null ? "" : "Desde el " + i.getFechaInicioBaja().format(FECHA);
        if (i.getEstado() == Instalacion.Estado.DESHABILITADA) {
            return desde.isEmpty() ? "Baja definitiva" : desde + " · baja definitiva";
        }
        LocalDate fin = i.getFechaFinBaja();
        String hasta;
        if (fin == null) {
            hasta = "sin fecha de reactivación";
        } else if (fin.isBefore(hoy)) {
            hasta = "la fecha estimada de reactivación (" + fin.format(FECHA) + ") ya pasó";
        } else {
            long dias = ChronoUnit.DAYS.between(hoy, fin);
            hasta = "reactivación estimada el " + fin.format(FECHA)
                    + (dias == 0 ? " (hoy)" : dias == 1 ? " (mañana)" : " (en " + dias + " días)");
        }
        return desde.isEmpty() ? capitalizar(hasta) : desde + " · " + hasta;
    }

    private static String capitalizar(String texto) {
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    /**
     * Las reservas que se pisan con sacar la instalación de servicio desde ahora hasta la fecha dada (null = sin fecha de fin):
     * las que están activas y todavía no empezaron.
     */
    public Conflictos conflictos(Integer idInstalacion, LocalDate hasta, LocalDateTime ahora) {
        List<Reserva> reservas = reservasEnConflicto(idInstalacion, hasta, ahora);
        int confirmadas = 0;
        int esperandoPago = 0;
        int porConfirmar = 0;
        for (Reserva r : reservas) {
            if (r.getEstado() == Reserva.Estado.CONFIRMADA) {
                confirmadas++;
            } else if (r.getIdPago() == null) {
                esperandoPago++;
            } else {
                porConfirmar++;
            }
        }
        List<ReservaFila> filas = dashboardServicio.aFilas(reservas, ahora.toLocalDate());
        return new Conflictos(filas, confirmadas, esperandoPago, porConfirmar);
    }

    private List<Reserva> reservasEnConflicto(Integer idInstalacion, LocalDate hasta, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        List<Reserva> reservas = new ArrayList<>();
        for (Reserva r : reservaDAO.listarActivasDeInstalacionEntre(idInstalacion, hoy, hasta == null ? hoy.plusYears(5) : hasta)) {
            if (r.getFechaTurno().atTime(r.getIdTurno().getHoraInicio()).isAfter(ahora)) {
                reservas.add(r);
            }
        }
        return reservas;
    }

    // ---------- acciones (devuelven null si salió bien, o el motivo por el que no se pudo) ----------

    /** Cambia la descripción y la capacidad (vacía = sin límite definido). */
    @Transactional
    public String editar(Integer idInstalacion, String descripcion, Integer capacidad) {
        Instalacion i = instalacionDAO.buscarPorId(idInstalacion);
        if (i == null) {
            return "La instalación no existe.";
        }
        if (capacidad != null && capacidad <= 0) {
            return "La capacidad tiene que ser un número mayor a cero.";
        }
        if (descripcion != null && descripcion.length() > 1000) {
            return "La descripción es demasiado larga (máximo 1000 caracteres).";
        }
        i.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim());
        i.setCapacidad(capacidad);
        return null;
    }

    /**
     * Saca la instalación de servicio: en mantenimiento (temporal; hasta = fecha estimada de reactivación, opcional) o de baja
     * definitiva (hasta = null). Si hay reservas en el período: con solicitudes por confirmar no se hace nada; las
     * confirmadas y las que esperan pago solo se cancelan si el administrador lo pidió.
     */
    @Transactional
    public String retirar(Integer idInstalacion, Instalacion.Estado nuevo, String motivo, LocalDate hasta,
            boolean cancelarReservas, LocalDateTime ahora) {
        Instalacion i = instalacionDAO.buscarPorId(idInstalacion);
        LocalDate hoy = ahora.toLocalDate();
        if (i == null) {
            return "La instalación no existe.";
        }
        if (i.getEstado() == Instalacion.Estado.DESHABILITADA) {
            return "Esta instalación ya está dada de baja.";
        }
        if (nuevo == Instalacion.Estado.DESHABILITADA_MANTENIMIENTO && i.getEstado() != Instalacion.Estado.HABILITADA) {
            return "Esta instalación ya está en mantenimiento.";
        }
        if (motivo == null || motivo.isBlank()) {
            return "Indicá el motivo.";
        }
        if (hasta != null && hasta.isBefore(hoy)) {
            return "La fecha de reactivación no puede ser anterior a hoy.";
        }

        Conflictos conflictos = conflictos(idInstalacion, hasta, ahora);
        if (conflictos.getPorConfirmar() > 0) {
            return "Hay " + conflictos.getPorConfirmar() + (conflictos.getPorConfirmar() == 1 ? " solicitud" : " solicitudes")
                    + " con comprobante por confirmar en ese período: resolvelas en Solicitudes pendientes y volvé a intentar.";
        }
        if (!conflictos.isVacio()) {
            if (!cancelarReservas) {
                return "Hay " + conflictos.getTotal() + (conflictos.getTotal() == 1 ? " reserva" : " reservas")
                        + " en ese período. Marcá la opción de cancelarlas para continuar.";
            }
            String causa = (nuevo == Instalacion.Estado.DESHABILITADA ? "Baja de " : "Mantenimiento de ") + i.getNombre()
                    + ": " + motivo.trim();
            for (Reserva r : reservasEnConflicto(idInstalacion, hasta, ahora)) {
                if (r.getEstado() == Reserva.Estado.CONFIRMADA) {
                    String error = cambiosServicio.cancelarPorElPredio(r.getIdReserva(), causa, ahora);
                    if (error != null) {
                        return error;
                    }
                } else {
                    Reserva bloqueada = reservaDAO.buscarParaResolver(r.getIdReserva()); // sin pago cargado: se libera el turno
                    if (bloqueada.getEstado() == Reserva.Estado.PENDIENTE_PAGO && bloqueada.getIdPago() == null) {
                        bloqueada.setEstado(Reserva.Estado.CANCELADA);
                    }
                }
            }
        }

        i.setEstado(nuevo);
        i.setMotivoBaja(motivo.trim().length() > 255 ? motivo.trim().substring(0, 255) : motivo.trim());
        i.setFechaInicioBaja(hoy);
        i.setFechaFinBaja(nuevo == Instalacion.Estado.DESHABILITADA ? null : hasta);
        return null;
    }

    /** Vuelve a habilitar una instalación en mantenimiento. La baja definitiva no se revierte desde acá. */
    @Transactional
    public String reactivar(Integer idInstalacion) {
        Instalacion i = instalacionDAO.buscarPorId(idInstalacion);
        if (i == null) {
            return "La instalación no existe.";
        }
        if (i.getEstado() != Instalacion.Estado.DESHABILITADA_MANTENIMIENTO) {
            return i.getEstado() == Instalacion.Estado.DESHABILITADA ? "La baja definitiva no se revierte."
                    : "Esta instalación ya está habilitada.";
        }
        i.setEstado(Instalacion.Estado.HABILITADA);
        i.setMotivoBaja(null);
        i.setFechaInicioBaja(null);
        i.setFechaFinBaja(null);
        return null;
    }
}
