package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.HistorialReservaDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.SolicitudCambioReservaDAO;
import com.sigrid.sigrid.dao.TurnoDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dto.CambioPedido;
import com.sigrid.sigrid.dto.CreditoFila;
import com.sigrid.sigrid.dto.ReprogramadaFila;
import com.sigrid.sigrid.repositorio.HistorialReserva;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.SolicitudCambioReserva;
import com.sigrid.sigrid.repositorio.Turno;
import com.sigrid.sigrid.repositorio.Usuario;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cancelaciones y reprogramaciones. Reglas del polideportivo:
 * - Una reserva confirmada NO se reprograma: el socio solo puede pedir cancelarla.
 * - No hay devoluciones de dinero: cancelar una reserva pagada la deja CANCELADA con un crédito de 30 días
 *   (fechaLimiteReprogramacion) para reprogramarla. El crédito ES la reserva; al usarlo vuelve a CONFIRMADA.
 * - Cancelar o reprogramar exige 48 h de anticipación (medidas contra el momento del pedido). Pasado ese
 *   plazo el socio pierde lo pagado: el sistema ni siquiera admite el pedido.
 * - No hay diferencia de precio y el crédito solo se usa en la misma instalación.
 * - El administrador NO reprograma por el socio: solo aprueba o rechaza lo que el socio pide. Lo único que
 *   puede hacer por su cuenta es cancelar por el predio (mantenimiento, clima), que también deja crédito.
 *
 * CDI simple (no EJB) y Serializable, igual que DashboardServicio. Las transacciones las abre este servicio.
 */
@Dependent
public class CambiosReservaServicio implements Serializable {

    public static final int HORAS_ANTICIPACION = 48;
    public static final int DIAS_CREDITO = 30;
    public static final int DIAS_POR_VENCER = 7;
    /** El historial guarda el tipo de cambio en el prefijo del motivo. */
    public static final String REPROGRAMACION = "Reprogramación";
    public static final String CANCELACION = "Cancelación";

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Inject
    private SolicitudCambioReservaDAO solicitudDAO;
    @Inject
    private ReservaDAO reservaDAO;
    @Inject
    private HistorialReservaDAO historialDAO;
    @Inject
    private TurnoDAO turnoDAO;
    @Inject
    private UsuarioDAO usuarioDAO;

    // ---------- lectura ----------

    public long contarPedidos() {
        return solicitudDAO.contarPendientes();
    }

    /** Pedidos por resolver, el más antiguo primero. */
    public List<CambioPedido> listarPedidos(LocalDateTime ahora) {
        List<CambioPedido> filas = new ArrayList<>();
        for (SolicitudCambioReserva c : solicitudDAO.listarPendientes()) {
            Reserva r = c.getIdReserva();
            Turno turno = r.getIdTurno();
            Instalacion instalacion = turno.getIdInstalacion();
            String socio = DashboardServicio.nombreSocio(r.getIdSocio());
            String email = r.getIdSocio().getIdUsuario().getEmail();
            boolean conCredito = r.getEstado() == Reserva.Estado.CANCELADA;
            long minutos = Math.max(0, Duration.between(c.getFechaSolicitud(), ahora).toMinutes());

            String anticipacion;
            if (conCredito) {
                anticipacion = "Usa el crédito de una reserva cancelada"
                        + (r.getFechaLimiteReprogramacion() == null ? "" : " (vence el "
                        + r.getFechaLimiteReprogramacion().format(FECHA) + ")");
            } else {
                long horas = Duration.between(c.getFechaSolicitud(), inicio(r)).toHours();
                anticipacion = "Pedido con " + (horas >= 72 ? horas / 24 + " días" : horas + " h") + " de anticipación";
            }

            filas.add(new CambioPedido(c.getIdSolicitud(), c.getTipo() == SolicitudCambioReserva.Tipo.REPROGRAMACION,
                    conCredito, socio, email, r.getIdSocio().getDni(), instalacion.getNombre(),
                    DashboardServicio.icono(instalacion.getTipoDisciplina()), turnoTexto(r.getFechaTurno(), turno),
                    c.getIdTurnoNuevo() == null ? null : turnoTexto(c.getFechaNueva(), c.getIdTurnoNuevo()),
                    c.getMotivo(), montoDe(r), textoEspera(minutos), anticipacion, minutos,
                    ReservaServicio.normalizar(socio + " " + email + " " + r.getIdSocio().getDni() + " "
                            + instalacion.getNombre() + " " + instalacion.getTipoDisciplina())));
        }
        return filas;
    }

    /** Todas las reservas con crédito, las que vencen antes primero (las vencidas quedan al principio). */
    public List<CreditoFila> listarCreditos(LocalDate hoy) {
        List<CreditoFila> filas = new ArrayList<>();
        for (Reserva r : reservaDAO.listarConCredito()) {
            Turno turno = r.getIdTurno();
            Instalacion instalacion = turno.getIdInstalacion();
            String socio = DashboardServicio.nombreSocio(r.getIdSocio());
            String email = r.getIdSocio().getIdUsuario().getEmail();
            long dias = ChronoUnit.DAYS.between(hoy, r.getFechaLimiteReprogramacion());
            filas.add(new CreditoFila(r.getIdReserva(), socio, email, instalacion.getNombre(),
                    DashboardServicio.icono(instalacion.getTipoDisciplina()), turnoTexto(r.getFechaTurno(), turno),
                    r.getFechaLimiteReprogramacion().format(FECHA), dias, dias >= 0, dias >= 0 && dias <= DIAS_POR_VENCER,
                    montoDe(r), ReservaServicio.normalizar(socio + " " + email + " " + r.getIdSocio().getDni() + " "
                    + instalacion.getNombre() + " " + instalacion.getTipoDisciplina())));
        }
        return filas;
    }

    /** Reprogramaciones ya hechas, la más reciente primero: de qué turno a cuál. */
    public List<ReprogramadaFila> listarReprogramadas() {
        List<ReprogramadaFila> filas = new ArrayList<>();
        for (HistorialReserva h : historialDAO.listarReprogramaciones(REPROGRAMACION)) {
            Reserva r = h.getIdReserva();
            Instalacion instalacion = r.getIdTurno().getIdInstalacion();
            String socio = DashboardServicio.nombreSocio(r.getIdSocio());
            String email = r.getIdSocio().getIdUsuario().getEmail();
            filas.add(new ReprogramadaFila(h.getIdHistorial(), socio, email, instalacion.getNombre(),
                    DashboardServicio.icono(instalacion.getTipoDisciplina()),
                    turnoTexto(h.getFechaAnterior(), h.getIdTurnoAnterior()), turnoTexto(r.getFechaTurno(), r.getIdTurno()),
                    h.getMotivo(), h.getFechaCambio().format(FECHA_HORA),
                    ReservaServicio.normalizar(socio + " " + email + " " + instalacion.getNombre() + " "
                            + instalacion.getTipoDisciplina())));
        }
        return filas;
    }

    // ---------- resolución de pedidos (administrador) ----------

    /** Aprueba un pedido y aplica el cambio. @return null si salió bien, o el motivo por el que no se pudo. */
    @Transactional
    public String aprobar(Integer idSolicitud, Integer idAdmin, LocalDateTime ahora) {
        SolicitudCambioReserva c = solicitudDAO.buscarParaResolver(idSolicitud);
        if (c == null || c.getEstado() != SolicitudCambioReserva.Estado.PENDIENTE) {
            return "Ese pedido ya no estaba por resolver.";
        }
        Reserva r = reservaDAO.buscarParaResolver(c.getIdReserva().getIdReserva());
        LocalDate hoy = ahora.toLocalDate();

        if (c.getTipo() == SolicitudCambioReserva.Tipo.CANCELACION) {
            if (r.getEstado() != Reserva.Estado.CONFIRMADA) {
                return "La reserva ya no está confirmada. Rechazá el pedido.";
            }
            cancelar(r, CANCELACION + ": " + c.getMotivo(), hoy);
        } else {
            boolean admite = r.getEstado() == Reserva.Estado.CANCELADA && r.getFechaLimiteReprogramacion() != null
                    && !c.getFechaSolicitud().toLocalDate().isAfter(r.getFechaLimiteReprogramacion());
            if (!admite) {
                return "La reserva ya no tiene un crédito para reprogramar. Rechazá el pedido.";
            }
            String error = validarDestino(r, c.getIdTurnoNuevo(), c.getFechaNueva(), ahora);
            if (error != null) {
                return error + " Rechazá el pedido para que el socio pida otro turno.";
            }
            mover(r, c.getIdTurnoNuevo(), c.getFechaNueva(), REPROGRAMACION + ": " + c.getMotivo());
        }

        c.setEstado(SolicitudCambioReserva.Estado.APROBADA);
        c.setFechaResolucion(ahora);
        c.setIdAdminResolutor(usuarioDAO.buscarPorId(idAdmin));
        return null;
    }

    /** Rechaza un pedido (la reserva queda como estaba), con un motivo opcional. @return false si ya estaba resuelto. */
    @Transactional
    public boolean rechazar(Integer idSolicitud, Integer idAdmin, String motivo, LocalDateTime ahora) {
        SolicitudCambioReserva c = solicitudDAO.buscarParaResolver(idSolicitud);
        if (c == null || c.getEstado() != SolicitudCambioReserva.Estado.PENDIENTE) {
            return false;
        }
        c.setEstado(SolicitudCambioReserva.Estado.RECHAZADA);
        c.setFechaResolucion(ahora);
        c.setIdAdminResolutor(usuarioDAO.buscarPorId(idAdmin));
        if (motivo != null && !motivo.isBlank()) {
            c.setMotivoRechazo(recortar(motivo.trim()));
        }
        return true;
    }

    /**
     * Cancelación por el predio (mantenimiento, clima): la única que el administrador hace sin pedido del socio.
     * No exige 48 h porque la causa no es del socio, y le deja el crédito de 30 días.
     * @return null si salió bien, o el motivo por el que no se pudo.
     */
    @Transactional
    public String cancelarPorElPredio(Integer idReserva, String motivo, LocalDateTime ahora) {
        if (motivo == null || motivo.isBlank()) {
            return "Indicá el motivo de la cancelación.";
        }
        Reserva r = reservaDAO.buscarParaResolver(idReserva);
        if (r == null || r.getEstado() != Reserva.Estado.CONFIRMADA || !inicio(r).isAfter(ahora)) {
            return "Esa reserva ya no está confirmada o el turno ya empezó.";
        }
        cancelar(r, CANCELACION + " por el predio: " + motivo.trim(), ahora.toLocalDate());
        return null;
    }

    // ---------- pedidos del socio (los usará la vista del socio, que todavía no existe) ----------

    /**
     * Registra un pedido del socio, con las reglas del polideportivo: reserva propia, un solo pedido a la vez, 48 h de
     * anticipación para cancelar y, para reprogramar, un crédito vigente y un turno libre de la misma instalación.
     * @param idTurnoNuevo y fechaNueva solo para REPROGRAMACION
     * @return null si quedó registrado, o el motivo por el que no se admite.
     */
    @Transactional
    public String solicitar(Integer idSocio, Integer idReserva, SolicitudCambioReserva.Tipo tipo,
            Integer idTurnoNuevo, LocalDate fechaNueva, String motivo, LocalDateTime ahora) {
        Reserva r = reservaDAO.buscarPorId(idReserva);
        if (r == null || !r.getIdSocio().getIdSocio().equals(idSocio)) {
            return "La reserva no existe.";
        }
        if (motivo == null || motivo.isBlank()) {
            return "Contanos el motivo del pedido.";
        }
        if (solicitudDAO.hayPendienteDe(idReserva)) {
            return "Ya hay un pedido pendiente para esta reserva.";
        }

        boolean conCredito = r.getEstado() == Reserva.Estado.CANCELADA && r.getFechaLimiteReprogramacion() != null
                && !ahora.toLocalDate().isAfter(r.getFechaLimiteReprogramacion());
        if (tipo == SolicitudCambioReserva.Tipo.REPROGRAMACION) {
            if (!conCredito) {
                return "Una reserva no se reprograma: pedí cancelarla y usá el crédito que te queda.";
            }
        } else if (r.getEstado() != Reserva.Estado.CONFIRMADA) {
            return "Solo se puede pedir la cancelación de una reserva confirmada.";
        } else if (inicio(r).isBefore(ahora.plusHours(HORAS_ANTICIPACION))) {
            return "Ya pasó el plazo: hay que pedirlo con al menos " + HORAS_ANTICIPACION + " h de anticipación.";
        }

        SolicitudCambioReserva c = new SolicitudCambioReserva();
        c.setIdReserva(r);
        c.setTipo(tipo);
        c.setMotivo(recortar(motivo.trim()));
        c.setFechaSolicitud(ahora);
        if (tipo == SolicitudCambioReserva.Tipo.REPROGRAMACION) {
            Turno nuevo = idTurnoNuevo == null ? null : turnoDAO.buscarPorId(idTurnoNuevo);
            String error = validarDestino(r, nuevo, fechaNueva, ahora);
            if (error != null) {
                return error;
            }
            c.setIdTurnoNuevo(nuevo);
            c.setFechaNueva(fechaNueva);
        }
        solicitudDAO.crear(c);
        return null;
    }

    // ---------- reglas compartidas ----------

    /** El turno pedido debe ser de la misma instalación, estar habilitada, ser futuro, distinto del actual y estar libre. */
    private String validarDestino(Reserva r, Turno nuevo, LocalDate fecha, LocalDateTime ahora) {
        if (nuevo == null || fecha == null) {
            return "Falta elegir el turno.";
        }
        Instalacion instalacion = r.getIdTurno().getIdInstalacion();
        if (!nuevo.getIdInstalacion().getIdInstalacion().equals(instalacion.getIdInstalacion())) {
            return "Solo se puede reprogramar dentro de la misma instalación.";
        }
        if (nuevo.getIdInstalacion().getEstado() != Instalacion.Estado.HABILITADA) {
            return "La instalación no está habilitada.";
        }
        if (!fecha.atTime(nuevo.getHoraInicio()).isAfter(ahora)) {
            return "El turno pedido ya pasó.";
        }
        if (reservaDAO.buscarActivaPorTurnoYFecha(nuevo.getIdTurno(), fecha) != null) {
            return "El turno pedido ya está reservado.";
        }
        return null;
    }

    /** CANCELADA + crédito de 30 días (solo si hubo un pago: sin dinero de por medio no hay nada que reprogramar). */
    private void cancelar(Reserva r, String motivo, LocalDate hoy) {
        registrar(r, motivo);
        r.setEstado(Reserva.Estado.CANCELADA);
        r.setFechaLimiteReprogramacion(r.getIdPago() == null ? null : hoy.plusDays(DIAS_CREDITO));
    }

    /** Pasa la reserva al turno nuevo; si estaba cancelada con crédito, vuelve a CONFIRMADA y el crédito se consume. */
    private void mover(Reserva r, Turno nuevo, LocalDate fecha, String motivo) {
        registrar(r, motivo);
        r.setIdTurno(nuevo);
        r.setFechaTurno(fecha);
        if (r.getEstado() == Reserva.Estado.CANCELADA) {
            r.setEstado(Reserva.Estado.CONFIRMADA);
            r.setFechaLimiteReprogramacion(null);
        }
    }

    /** Deja en el historial el turno y el día anteriores al cambio. */
    private void registrar(Reserva r, String motivo) {
        HistorialReserva h = new HistorialReserva();
        h.setIdReserva(r);
        h.setIdTurnoAnterior(r.getIdTurno());
        h.setFechaAnterior(r.getFechaTurno());
        h.setMotivo(recortar(motivo));
        historialDAO.crear(h);
    }

    private static LocalDateTime inicio(Reserva r) {
        return r.getFechaTurno().atTime(r.getIdTurno().getHoraInicio());
    }

    private static String recortar(String texto) {
        return texto.length() > 255 ? texto.substring(0, 255) : texto;
    }

    /** "vie 27/09/2026 · 18:00 – 19:00". */
    private static String turnoTexto(LocalDate fecha, Turno turno) {
        return fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", "") + " " + fecha.format(FECHA)
                + " · " + turno.getHoraInicio().format(HORA) + " – " + turno.getHoraFin().format(HORA);
    }

    /** Lo pagado por la reserva; si no hay pago, el precio de su tarifa; si tampoco, null (instalación libre). */
    private static String montoDe(Reserva r) {
        BigDecimal monto = r.getIdPago() != null ? r.getIdPago().getMonto()
                : r.getIdTarifa() != null ? r.getIdTarifa().getPrecio() : null;
        return monto == null ? null : DashboardServicio.monto(monto);
    }

    private static String textoEspera(long minutos) {
        if (minutos < 1) {
            return "recién";
        }
        if (minutos < 60) {
            return "hace " + minutos + " min";
        }
        if (minutos < 60 * 24) {
            return "hace " + minutos / 60 + " h";
        }
        long dias = minutos / (60 * 24);
        return "hace " + dias + (dias == 1 ? " día" : " días");
    }
}
