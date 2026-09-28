package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dao.ComprobantePagoDAO;
import com.sigrid.sigrid.dto.ReservaListado;
import com.sigrid.sigrid.dto.SolicitudFila;
import com.sigrid.sigrid.repositorio.ComprobantePago;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Turno;
import com.sigrid.sigrid.repositorio.Usuario;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resolución de las solicitudes de reserva por parte del administrador: cuando ya cotejó a mano el comprobante
 * y el ingreso en la cuenta, la acepta (reserva CONFIRMADA y pago CONFIRMADO) o la deniega (reserva RECHAZADA,
 * que libera el turno, y pago RECHAZADO; el comprobante queda registrado y su hash sigue bloqueado).
 *
 * CDI simple (no EJB) y Serializable, igual que DashboardServicio. Las transacciones las abre este servicio.
 */
@Dependent
public class ReservaServicio implements Serializable {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter RESERVADA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Inject
    private ReservaDAO reservaDAO;
    @Inject
    private UsuarioDAO usuarioDAO;
    @Inject
    private ComprobantePagoDAO comprobanteDAO;

    /** Todas las reservas (en cualquier estado) para el listado del administrador; la de fecha más lejana primero. */
    public List<ReservaListado> listarTodas(LocalDate hoy, LocalTime ahora) {
        List<ReservaListado> filas = new ArrayList<>();
        for (Reserva r : reservaDAO.listarTodasParaAdmin()) {
            Turno turno = r.getIdTurno();
            Instalacion instalacion = turno.getIdInstalacion();
            Pago pago = r.getIdPago();
            Usuario usuario = r.getIdSocio().getIdUsuario();
            String socio = DashboardServicio.nombreSocio(r.getIdSocio());
            String horario = turno.getHoraInicio().format(HORA) + " – " + turno.getHoraFin().format(HORA);
            boolean finalizada = r.getFechaTurno().isBefore(hoy)
                    || (r.getFechaTurno().equals(hoy) && !turno.getHoraFin().isAfter(ahora));

            String estado;
            String texto;
            String clase;
            switch (r.getEstado()) {
                case CONFIRMADA:
                    estado = "CONFIRMADA";
                    texto = "Confirmada";
                    clase = "ok";
                    break;
                case CANCELADA:
                    estado = "CANCELADA";
                    boolean conCredito = r.getFechaLimiteReprogramacion() != null
                            && !r.getFechaLimiteReprogramacion().isBefore(hoy);
                    texto = conCredito ? "Cancelada · con crédito" : "Cancelada";
                    clase = "off";
                    break;
                case RECHAZADA:
                    estado = "RECHAZADA";
                    texto = "Rechazada";
                    clase = "off";
                    break;
                default: // PENDIENTE_PAGO: con comprobante espera al administrador; sin él, al socio
                    boolean conComprobante = pago != null && pago.getEstado() == Pago.Estado.PENDIENTE_VALIDACION;
                    estado = conComprobante ? "POR_CONFIRMAR" : "ESPERANDO_PAGO";
                    texto = conComprobante ? "Por confirmar" : "Esperando pago";
                    clase = conComprobante ? "warn" : "muted";
            }

            boolean confirmada = r.getEstado() == Reserva.Estado.CONFIRMADA && pago != null
                    && r.getFechaConfirmacion() != null;
            String monto = pago != null ? DashboardServicio.monto(pago.getMonto())
                    : r.getIdTarifa() != null ? DashboardServicio.monto(r.getIdTarifa().getPrecio()) : null;
            filas.add(new ReservaListado(r.getIdReserva(), r.getFechaTurno(), cuando(r.getFechaTurno(), hoy), horario,
                    turno.getHoraInicio().getHour(), finalizada, instalacion.getNombre(), instalacion.getTipoDisciplina(),
                    DashboardServicio.icono(instalacion.getTipoDisciplina()), socio, usuario.getEmail(), estado, texto,
                    clase, monto, r.getFechaReserva().format(RESERVADA),
                    normalizar(socio + " " + usuario.getEmail() + " " + r.getIdSocio().getDni() + " "
                            + instalacion.getNombre() + " " + instalacion.getTipoDisciplina() + " "
                            + r.getFechaTurno().format(FECHA) + " " + r.getIdReserva()),
                    confirmada ? r.getFechaConfirmacion().toLocalDate() : null, confirmada ? pago.getMonto() : null));
        }
        return filas;
    }

    /** Solicitudes por confirmar (con comprobante cargado) con lo necesario para cotejarlas; sin ordenar. */
    public List<SolicitudFila> listarSolicitudes(LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        List<SolicitudFila> filas = new ArrayList<>();
        for (Reserva r : reservaDAO.listarPendientes()) {
            Turno turno = r.getIdTurno();
            Instalacion instalacion = turno.getIdInstalacion();
            Pago pago = r.getIdPago();
            Socio socio = r.getIdSocio();
            String nombre = DashboardServicio.nombreSocio(socio);
            ComprobantePago comprobante = comprobanteDAO.buscarPorPago(pago.getIdPago());
            LocalDateTime inicio = r.getFechaTurno().atTime(turno.getHoraInicio());
            long espera = Math.max(0, Duration.between(pago.getFechaPago(), ahora).toMinutes());
            filas.add(new SolicitudFila(r.getIdReserva(), inicio, cuando(r.getFechaTurno(), hoy),
                    turno.getHoraInicio().format(HORA) + " – " + turno.getHoraFin().format(HORA),
                    instalacion.getNombre(), instalacion.getTipoDisciplina(),
                    DashboardServicio.icono(instalacion.getTipoDisciplina()), nombre,
                    socio.getIdUsuario().getEmail(), socio.getDni(), socio.getIdCategoriaSocio().getNombreCategoria(),
                    DashboardServicio.monto(pago.getMonto()), pago.getMonto(),
                    comprobante == null || comprobante.getFechaOperacionDeclarada() == null ? null
                    : comprobante.getFechaOperacionDeclarada().format(RESERVADA),
                    pago.getFechaPago(), espera, textoEspera(espera),
                    !r.getFechaTurno().isAfter(hoy.plusDays(1)),
                    !turno.getHoraFin().isAfter(ahora.toLocalTime()) && r.getFechaTurno().equals(hoy)
                    || r.getFechaTurno().isBefore(hoy),
                    normalizar(nombre + " " + socio.getIdUsuario().getEmail() + " " + socio.getDni() + " "
                            + instalacion.getNombre() + " " + instalacion.getTipoDisciplina())));
        }
        return filas;
    }

    /** "hace 5 min", "hace 3 h", "hace 2 días". */
    static String textoEspera(long minutos) {
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

    static String cuando(LocalDate fecha, LocalDate hoy) {
        if (fecha.equals(hoy)) {
            return "Hoy";
        }
        if (fecha.equals(hoy.plusDays(1))) {
            return "Mañana";
        }
        return fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES) + " " + fecha.format(FECHA);
    }

    /** Sin tildes y en minúsculas, para buscar sin distinguir. */
    public static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(ES);
    }

    /** @return false si la solicitud ya no estaba por confirmar (otro administrador la resolvió) o no tiene comprobante. */
    @Transactional
    public boolean confirmar(Integer idReserva, Integer idAdmin) {
        return resolver(idReserva, idAdmin, true, null);
    }

    /** @return false en los mismos casos que confirmar. */
    @Transactional
    public boolean rechazar(Integer idReserva, Integer idAdmin) {
        return resolver(idReserva, idAdmin, false, null);
    }

    /** Rechaza dejando el motivo (opcional, hasta 255 caracteres) en el pago. @return false en los mismos casos que confirmar. */
    @Transactional
    public boolean rechazar(Integer idReserva, Integer idAdmin, String motivo) {
        return resolver(idReserva, idAdmin, false, motivo);
    }

    private boolean resolver(Integer idReserva, Integer idAdmin, boolean aceptar, String motivo) {
        Reserva reserva = reservaDAO.buscarParaResolver(idReserva);
        Pago pago = reserva == null ? null : reserva.getIdPago();
        if (pago == null || reserva.getEstado() != Reserva.Estado.PENDIENTE_PAGO
                || pago.getEstado() != Pago.Estado.PENDIENTE_VALIDACION) {
            return false;
        }

        Usuario admin = usuarioDAO.buscarPorId(idAdmin);
        LocalDateTime ahora = LocalDateTime.now();
        pago.setEstado(aceptar ? Pago.Estado.CONFIRMADO : Pago.Estado.RECHAZADO);
        pago.setFechaValidacion(ahora);
        pago.setIdAdminValidador(admin);
        if (!aceptar && motivo != null && !motivo.isBlank()) {
            pago.setMotivoRechazo(motivo.trim().length() > 255 ? motivo.trim().substring(0, 255) : motivo.trim());
        }
        reserva.setEstado(aceptar ? Reserva.Estado.CONFIRMADA : Reserva.Estado.RECHAZADA);
        if (aceptar) {
            reserva.setFechaConfirmacion(ahora);
            reserva.setIdAdminConfirmador(admin);
        }
        return true;
    }
}
