package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.ComprobantePagoDAO;
import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.PagoDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.SocioDAO;
import com.sigrid.sigrid.dao.SolicitudCambioReservaDAO;
import com.sigrid.sigrid.dao.SuscripcionSocioDAO;
import com.sigrid.sigrid.dao.TarifaAlquilerDAO;
import com.sigrid.sigrid.dao.ValoracionReservaDAO;
import com.sigrid.sigrid.repositorio.ValoracionReserva;
import com.sigrid.sigrid.dao.TarifaMembresiaDAO;
import com.sigrid.sigrid.repositorio.TarifaMembresia;
import com.sigrid.sigrid.dao.TurnoDAO;
import com.sigrid.sigrid.dto.Aviso;
import com.sigrid.sigrid.dto.DiaSocio;
import com.sigrid.sigrid.dto.InstalacionReservable;
import com.sigrid.sigrid.dto.MiReserva;
import com.sigrid.sigrid.dto.PasoSeguimiento;
import com.sigrid.sigrid.dto.PerfilSocio;
import com.sigrid.sigrid.dto.TurnoDisponible;
import com.sigrid.sigrid.repositorio.ComprobantePago;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.SolicitudCambioReserva;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import com.sigrid.sigrid.repositorio.TarifaAlquiler;
import com.sigrid.sigrid.repositorio.Turno;
import com.sigrid.sigrid.repositorio.Usuario;
import com.sigrid.sigrid.util.ComprobanteArchivos;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Todo lo que el socio ve y hace en su panel (/socio/*): su perfil y membresía, sus reservas, los avisos, y el alta de
 * una reserva nueva con su comprobante de pago. Siempre trabaja sobre el socio que le pasa el bean (el de la sesión):
 * nunca confía en un id que venga de la página sin comprobar que la reserva sea suya.
 *
 * Flujo de una reserva: el socio elige turno y la reserva nace PENDIENTE_PAGO (ocupa el turno); tiene
 * MINUTOS_PARA_PAGAR para subir el comprobante de la transferencia, que crea el pago PENDIENTE_VALIDACION y hace que
 * aparezca en "Solicitudes pendientes" del administrador (ReservaServicio.confirmar / rechazar la resuelven). Si no
 * lo sube a tiempo, liberarVencidas (tarea programada) la cancela y el turno vuelve a estar libre (RF-04.8).
 *
 * CDI simple (no EJB) y Serializable, igual que DashboardServicio. Las transacciones las abre este servicio.
 */
@Dependent
public class PanelSocioServicio implements Serializable {

    /** Cuántos días hacia adelante se puede reservar. */
    public static final int DIAS_ADELANTE = 30;
    public static final int MINUTOS_PARA_PAGAR = 60;
    /** Turnos que un socio puede tener a la vez esperando su comprobante: evita acaparar turnos sin pagar. */
    static final int MAX_SIN_COMPROBANTE = 3;
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int DIAS_RECIENTE = 14;
    /**
     * La valoración es obligatoria para las reservas que terminaron desde este día: las anteriores son historia previa a
     * la función y no bloquean (si no, un socio antiguo tendría que valorar todo su pasado antes de reservar).
     */
    static final LocalDate VALORACION_DESDE = LocalDate.of(2026, 9, 25);
    private static final int MAX_COMENTARIO = 500;

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES_ANIO = DateTimeFormatter.ofPattern("MM/yyyy");

    @Inject
    private SocioDAO socioDAO;
    @Inject
    private SuscripcionSocioDAO suscripcionDAO;
    @Inject
    private ReservaDAO reservaDAO;
    @Inject
    private TurnoDAO turnoDAO;
    @Inject
    private InstalacionDAO instalacionDAO;
    @Inject
    private TarifaAlquilerDAO tarifaDAO;
    @Inject
    private PagoDAO pagoDAO;
    @Inject
    private ValoracionReservaDAO valoracionDAO;
    @Inject
    private TarifaMembresiaDAO tarifaMembresiaDAO;
    @Inject
    private ComprobantePagoDAO comprobanteDAO;
    @Inject
    private SolicitudCambioReservaDAO solicitudDAO;

    // ---------- lectura ----------

    /** El socio de una cuenta de usuario; null si esa cuenta no es de un socio o está dado de baja. */
    public Socio buscarSocio(Integer idUsuario) {
        Socio socio = idUsuario == null ? null : socioDAO.buscarPorIdUsuario(idUsuario);
        return socio == null || socio.isBajaLogica() ? null : socio;
    }

    public PerfilSocio perfil(Socio socio, LocalDate hoy) {
        Usuario u = socio.getIdUsuario();
        boolean vigente = socio.getEstado() == Socio.Estado.ACTIVO;
        LocalDate vence = null;
        int progreso = 0;
        if (vigente) {
            List<SuscripcionSocio> vigentes = suscripcionDAO.listarVigentesDelSocio(socio.getIdSocio(), hoy);
            if (!vigentes.isEmpty()) { // la que vence más tarde
                SuscripcionSocio s = vigentes.get(0);
                vence = s.getFechaVencimiento();
                long total = s.getFechaInicio() == null ? 0 : ChronoUnit.DAYS.between(s.getFechaInicio(), vence);
                long faltan = ChronoUnit.DAYS.between(hoy, vence);
                progreso = total <= 0 ? 0 : (int) Math.max(0, Math.min(100, 100 - faltan * 100 / total));
            }
        } else {
            for (SuscripcionSocio s : suscripcionDAO.listarPorSocio(socio.getIdSocio())) {
                if (s.getEstado() == SuscripcionSocio.Estado.VENCIDA && s.getFechaVencimiento() != null
                        && (vence == null || s.getFechaVencimiento().isAfter(vence))) {
                    vence = s.getFechaVencimiento();
                }
            }
        }
        String nombre = u.getNombre();
        String pila = nombre == null || nombre.isBlank() ? u.getEmail().split("@")[0] : nombre.trim().split("\\s+")[0];
        return new PerfilSocio(socio.getIdSocio(), DashboardServicio.nombreSocio(socio), pila, SociosServicio.iniciales(u),
                socio.getIdCategoriaSocio().getNombreCategoria(), SociosServicio.numero(socio.getIdSocio()),
                u.getFechaAlta().format(MES_ANIO), vigente, vence == null ? 0 : ChronoUnit.DAYS.between(hoy, vence),
                vence == null ? null : vence.format(FECHA), progreso);
    }

    /** Todas sus reservas: las de turno más lejano primero. */
    public List<MiReserva> misReservas(Integer idSocio, LocalDateTime ahora) {
        Set<Integer> conPedido = new HashSet<>();
        for (SolicitudCambioReserva c : solicitudDAO.listarDelSocio(idSocio)) {
            if (c.getEstado() == SolicitudCambioReserva.Estado.PENDIENTE) {
                conPedido.add(c.getIdReserva().getIdReserva());
            }
        }
        Map<Integer, Integer> valoradas = valoracionDAO.puntajesDelSocio(idSocio);
        List<MiReserva> filas = new ArrayList<>();
        for (Reserva r : reservaDAO.listarPorSocio(idSocio)) {
            filas.add(armar(r, conPedido, valoradas, ahora));
        }
        return filas;
    }

    private MiReserva armar(Reserva r, Set<Integer> conPedido, Map<Integer, Integer> valoradas, LocalDateTime ahora) {
        Turno t = r.getIdTurno();
        Instalacion i = t.getIdInstalacion();
        Pago pago = r.getIdPago();
        LocalDate hoy = ahora.toLocalDate();
        LocalDateTime inicio = r.getFechaTurno().atTime(t.getHoraInicio());
        boolean finalizada = !r.getFechaTurno().atTime(t.getHoraFin()).isAfter(ahora);

        String texto;
        String clase;
        String nota = null;
        String limite = null;
        String grupo = "PASADAS";
        boolean subir = false;
        boolean liberar = false;
        boolean pedir = false;
        boolean reprogramar = false;
        String limiteCredito = null;
        switch (r.getEstado()) {
            case CONFIRMADA:
                if (finalizada) {
                    texto = "Realizada";
                    clase = "muted";
                } else if (conPedido.contains(r.getIdReserva())) {
                    grupo = "PROXIMAS";
                    texto = "Cancelación pedida";
                    clase = "warn";
                    nota = "Esperando la respuesta del administrador.";
                } else {
                    grupo = "PROXIMAS";
                    texto = "Confirmada";
                    clase = "ok";
                    if (inicio.isBefore(ahora.plusHours(CambiosReservaServicio.HORAS_ANTICIPACION))) {
                        nota = "Ya no se puede cancelar: faltan menos de " + CambiosReservaServicio.HORAS_ANTICIPACION + " h.";
                    } else {
                        pedir = true;
                    }
                }
                break;
            case PENDIENTE_PAGO:
                grupo = "PROXIMAS";
                clase = "warn";
                if (pago == null) {
                    texto = "Falta el comprobante";
                    limite = r.getFechaReserva().plusMinutes(MINUTOS_PARA_PAGAR).format(HORA);
                    nota = "Subí el comprobante antes de las " + limite + " o el turno se libera.";
                    subir = true;
                    liberar = true;
                } else {
                    texto = "Comprobante en revisión";
                    nota = "El administrador está revisando tu transferencia.";
                }
                break;
            case CANCELADA:
                texto = "Cancelada";
                clase = "off";
                if (r.getFechaLimiteReprogramacion() != null && !r.getFechaLimiteReprogramacion().isBefore(hoy)) {
                    limiteCredito = r.getFechaLimiteReprogramacion().format(FECHA);
                    if (conPedido.contains(r.getIdReserva())) {
                        nota = "Pediste reprogramar con tu crédito. Esperando la respuesta del administrador.";
                    } else {
                        nota = "Tenés un crédito para reprogramar hasta el " + limiteCredito + ".";
                        reprogramar = true;
                    }
                }
                break;
            default: // RECHAZADA
                texto = "Rechazada";
                clase = "off";
                nota = pago != null && pago.getMotivoRechazo() != null ? "Motivo: " + pago.getMotivoRechazo()
                        : "No se pudo validar el comprobante y el turno quedó libre.";
        }

        boolean valorable = false;
        if (r.getEstado() == Reserva.Estado.CONFIRMADA && finalizada) {
            Integer puntaje = valoradas.get(r.getIdReserva());
            if (puntaje != null) {
                nota = "Tu valoración: " + puntaje + " de 5 estrellas.";
            } else if (debeValorarse(r, ahora)) {
                valorable = true;
                nota = "Valoración pendiente: contanos cómo estuvo tu turno.";
            }
        }

        BigDecimal monto = pago != null ? pago.getMonto() : r.getIdTarifa() != null ? r.getIdTarifa().getPrecio() : null;
        return new MiReserva(r.getIdReserva(), i.getNombre(), DashboardServicio.icono(i.getTipoDisciplina()),
                i.getTipoDisciplina(), ReservaServicio.cuando(r.getFechaTurno(), hoy), r.getFechaTurno().format(FECHA),
                t.getHoraInicio().format(HORA) + " – " + t.getHoraFin().format(HORA),
                monto == null ? null : DashboardServicio.monto(monto), texto, clase, grupo, inicio, subir, liberar, pedir,
                nota, limite, pasos(r, pago, finalizada, limite), valorable, reprogramar, limiteCredito);
    }

    /** ¿Terminó y cae en las reservas cuya valoración es obligatoria? (no mira si ya se valoró) */
    private static boolean debeValorarse(Reserva r, LocalDateTime ahora) {
        return r.getEstado() == Reserva.Estado.CONFIRMADA && !r.getFechaTurno().isBefore(VALORACION_DESDE)
                && !r.getFechaTurno().atTime(r.getIdTurno().getHoraFin()).isAfter(ahora);
    }

    /** Las reservas terminadas que el socio todavía no valoró y que le impiden reservar. */
    private List<Reserva> sinValorar(Integer idSocio, LocalDateTime ahora) {
        Map<Integer, Integer> valoradas = valoracionDAO.puntajesDelSocio(idSocio);
        List<Reserva> lista = new ArrayList<>();
        for (Reserva r : reservaDAO.listarPorSocio(idSocio)) {
            if (debeValorarse(r, ahora) && !valoradas.containsKey(r.getIdReserva())) {
                lista.add(r);
            }
        }
        return lista;
    }

    /** El seguimiento de la reserva, como el de un paquete: solicitada, comprobante y confirmación (y el turno si se confirmó). */
    private static List<PasoSeguimiento> pasos(Reserva r, Pago pago, boolean finalizada, String limite) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        boolean pendiente = r.getEstado() == Reserva.Estado.PENDIENTE_PAGO;
        List<PasoSeguimiento> l = new ArrayList<>();
        l.add(new PasoSeguimiento("Reserva solicitada", r.getFechaReserva().format(f) + " · el turno quedó apartado para vos", "done"));
        if (pago != null) {
            l.add(new PasoSeguimiento("Comprobante enviado", pago.getFechaPago().format(f), "done"));
        } else if (pendiente) {
            l.add(new PasoSeguimiento("Comprobante de pago", "Falta subirlo antes de las " + limite, "current"));
        } else {
            l.add(new PasoSeguimiento("Comprobante de pago", "No se envió", "off"));
        }
        switch (r.getEstado()) {
            case CONFIRMADA:
                l.add(new PasoSeguimiento("Reserva confirmada", r.getFechaConfirmacion() == null ? "" : r.getFechaConfirmacion().format(f), "done"));
                l.add(new PasoSeguimiento("Tu turno", finalizada ? "Realizado" : r.getFechaTurno().format(FECHA), finalizada ? "done" : "todo"));
                break;
            case PENDIENTE_PAGO:
                l.add(new PasoSeguimiento("Pendiente de confirmación", pago == null ? "Se revisa después de que subas el comprobante"
                        : "El administrador la revisa: puede tardar hasta 24 h", pago == null ? "todo" : "current"));
                break;
            case RECHAZADA:
                l.add(new PasoSeguimiento("Reserva rechazada", pago != null && pago.getMotivoRechazo() != null ? pago.getMotivoRechazo()
                        : "No se pudo validar el pago; el turno quedó libre", "off"));
                break;
            default:
                l.add(new PasoSeguimiento("Reserva cancelada", "El turno quedó libre", "off"));
        }
        return l;
    }

    /** Los avisos del socio, los más importantes primero. */
    public List<Aviso> avisos(Socio socio, PerfilSocio perfil, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        List<Aviso> avisos = new ArrayList<>();

        if (!perfil.isVigente()) {
            avisos.add(new Aviso("lock", "Tu membresía está vencida",
                    (perfil.getVence() == null ? "Todavía no tenés una membresía vigente. "
                    : "Venció el " + perfil.getVence() + ". ")
                    + "Mientras no la renueves no podés hacer reservas nuevas y tu carnet queda congelado.", "off", null, 0));
        } else if (perfil.getVence() != null && perfil.getDiasRestantes() <= CambiosReservaServicio.DIAS_POR_VENCER) {
            long d = perfil.getDiasRestantes();
            avisos.add(new Aviso("hourglass_top", d == 0 ? "Tu membresía vence hoy" : d == 1 ? "Tu membresía vence mañana"
                    : "Tu membresía vence en " + d + " días", "Vence el " + perfil.getVence()
                    + ". Renovala a tiempo para seguir reservando y no perder tu carnet.", "warn", null, 1));
        }

        for (Reserva r : sinValorar(socio.getIdSocio(), ahora)) {
            avisos.add(new Aviso("star", "Valoración pendiente", "¿Cómo estuvo tu turno? " + resumen(r, hoy)
                    + ". Valorala para poder hacer nuevas reservas.", "warn", null, 1));
        }

        List<SolicitudCambioReserva> pedidos = solicitudDAO.listarDelSocio(socio.getIdSocio());
        Set<Integer> conPedido = new HashSet<>(); // reservas con un pedido de cancelación sin resolver
        for (SolicitudCambioReserva c : pedidos) {
            if (c.getEstado() == SolicitudCambioReserva.Estado.PENDIENTE) {
                conPedido.add(c.getIdReserva().getIdReserva());
            }
        }

        for (Reserva r : reservaDAO.listarPorSocio(socio.getIdSocio())) {
            String detalle = resumen(r, hoy);
            LocalDateTime inicio = r.getFechaTurno().atTime(r.getIdTurno().getHoraInicio());
            boolean futura = r.getFechaTurno().atTime(r.getIdTurno().getHoraFin()).isAfter(ahora);
            Pago pago = r.getIdPago();
            switch (r.getEstado()) {
                case PENDIENTE_PAGO:
                    if (pago == null) {
                        avisos.add(new Aviso("receipt_long", "Subí el comprobante de tu reserva", detalle + ". Tenés hasta las "
                                + r.getFechaReserva().plusMinutes(MINUTOS_PARA_PAGAR).format(HORA)
                                + " o el turno se libera.", "warn", hace(r.getFechaReserva(), ahora), 2));
                    } else {
                        avisos.add(new Aviso("pending_actions", "Estamos revisando tu comprobante", detalle
                                + ". Te avisamos acá apenas el administrador lo valide.", "muted",
                                hace(pago.getFechaPago(), ahora), 8));
                    }
                    break;
                case CONFIRMADA:
                    if (conPedido.contains(r.getIdReserva())) {
                        break; // ya tiene su aviso: el pedido de cancelación
                    }
                    if (futura && inicio.isBefore(ahora.plusHours(48))) {
                        avisos.add(new Aviso("event_available", "Tu turno es " + ReservaServicio.cuando(r.getFechaTurno(), hoy).toLowerCase(),
                                detalle + ". Llevá tu carnet digital.", "ok", null, 6));
                    } else if (futura && r.getFechaConfirmacion() != null
                            && r.getFechaConfirmacion().isAfter(ahora.minusDays(7))) {
                        avisos.add(new Aviso("check_circle", "Reserva confirmada", detalle
                                + ". El administrador validó tu pago.", "ok", hace(r.getFechaConfirmacion(), ahora), 7));
                    }
                    break;
                case RECHAZADA:
                    if (r.getFechaTurno().isAfter(hoy.minusDays(DIAS_RECIENTE))) {
                        avisos.add(new Aviso("event_busy", "Reserva rechazada", detalle + ". "
                                + (pago != null && pago.getMotivoRechazo() != null ? "Motivo: " + pago.getMotivoRechazo() + ". " : "")
                                + "El turno quedó libre.", "off", pago == null || pago.getFechaValidacion() == null ? null
                                : hace(pago.getFechaValidacion(), ahora), 3));
                    }
                    break;
                case CANCELADA:
                    if (r.getFechaLimiteReprogramacion() != null && !r.getFechaLimiteReprogramacion().isBefore(hoy)
                            && !conPedido.contains(r.getIdReserva())) {
                        avisos.add(new Aviso("event_repeat", "Tenés un crédito para reprogramar", detalle
                                + ". Podés usarlo hasta el " + r.getFechaLimiteReprogramacion().format(FECHA) + ".", "warn", null, 4));
                    }
                    break;
                default:
                    break;
            }
        }

        for (SolicitudCambioReserva c : pedidos) {
            String detalle = resumen(c.getIdReserva(), hoy);
            boolean reprogramacion = c.getTipo() == SolicitudCambioReserva.Tipo.REPROGRAMACION;
            if (c.getEstado() == SolicitudCambioReserva.Estado.PENDIENTE) {
                avisos.add(new Aviso("pending_actions", reprogramacion ? "Pedido de reprogramación en revisión"
                        : "Pedido de cancelación en revisión", (reprogramacion ? detalle + " → " + destino(c, hoy) : detalle)
                        + ". El administrador todavía no respondió.", "muted", hace(c.getFechaSolicitud(), ahora), 8));
            } else if (c.getFechaResolucion() != null && c.getFechaResolucion().isAfter(ahora.minusDays(DIAS_RECIENTE))) {
                boolean aprobada = c.getEstado() == SolicitudCambioReserva.Estado.APROBADA;
                String titulo = (reprogramacion ? "Reprogramación " : "Cancelación ") + (aprobada ? "aprobada" : "rechazada");
                String texto = reprogramacion
                        ? (aprobada ? "Tu reserva quedó confirmada en el turno nuevo. " + detalle + "."
                        : "Tu crédito sigue vigente: podés pedir otro turno. "
                        + (c.getMotivoRechazo() != null ? "Motivo: " + c.getMotivoRechazo() : ""))
                        : detalle + ". " + (aprobada ? "Quedó un crédito de " + CambiosReservaServicio.DIAS_CREDITO + " días para reprogramar."
                        : c.getMotivoRechazo() != null ? "Motivo: " + c.getMotivoRechazo() : "La reserva sigue en pie.");
                avisos.add(new Aviso(aprobada ? "check_circle" : "event_busy", titulo, texto, aprobada ? "ok" : "off",
                        hace(c.getFechaResolucion(), ahora), 5));
            }
        }

        avisos.sort(Comparator.comparingInt(Aviso::getOrden));
        return avisos;
    }

    /** "Cancha de Fútbol 11 · Mañana 18:00 – 19:00". */
    private static String resumen(Reserva r, LocalDate hoy) {
        Turno t = r.getIdTurno();
        return t.getIdInstalacion().getNombre() + " · " + ReservaServicio.cuando(r.getFechaTurno(), hoy) + " "
                + t.getHoraInicio().format(HORA) + " – " + t.getHoraFin().format(HORA);
    }

    /** El turno que pide una reprogramación: "Mañana 18:00 – 19:00". */
    private static String destino(SolicitudCambioReserva c, LocalDate hoy) {
        Turno t = c.getIdTurnoNuevo();
        return ReservaServicio.cuando(c.getFechaNueva(), hoy) + " " + t.getHoraInicio().format(HORA) + " – " + t.getHoraFin().format(HORA);
    }

    private static String hace(LocalDateTime momento, LocalDateTime ahora) {
        return ReservaServicio.textoEspera(Math.max(0, ChronoUnit.MINUTES.between(momento, ahora)));
    }

    /** Los próximos días desde "desde", con cuántas reservas activas (futuras) tiene el socio cada uno. */
    public List<DiaSocio> dias(List<MiReserva> reservas, LocalDate desde, int cantidad) {
        Map<LocalDate, Integer> porDia = new HashMap<>();
        for (MiReserva r : reservas) {
            if ("PROXIMAS".equals(r.getGrupo())) {
                porDia.merge(r.getInicio().toLocalDate(), 1, Integer::sum);
            }
        }
        List<DiaSocio> dias = new ArrayList<>();
        for (int k = 0; k < cantidad; k++) {
            LocalDate d = desde.plusDays(k);
            dias.add(new DiaSocio(d, d.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", ""),
                    String.valueOf(d.getDayOfMonth()), d.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", ""),
                    porDia.getOrDefault(d, 0), k == 0));
        }
        return dias;
    }

    /** Instalaciones que se reservan, con el precio de su tipo de socio; las no disponibles llevan un aviso. */
    public List<InstalacionReservable> instalaciones(Socio socio, LocalDate hoy) {
        List<InstalacionReservable> lista = new ArrayList<>();
        for (Instalacion i : instalacionDAO.listarReservables()) {
            TarifaAlquiler tarifa = tarifaDAO.buscarVigente(i.getIdInstalacion(),
                    socio.getIdCategoriaSocio().getIdCategoriaSocio(), hoy);
            boolean habilitada = i.getEstado() == Instalacion.Estado.HABILITADA;
            String aviso = null;
            if (i.getEstado() == Instalacion.Estado.DESHABILITADA_MANTENIMIENTO) {
                aviso = "En mantenimiento" + (i.getFechaFinBaja() == null ? "" : " hasta el " + i.getFechaFinBaja().format(FECHA));
            } else if (!habilitada) {
                aviso = "No disponible";
            } else if (tarifa == null) {
                aviso = "Sin precio cargado";
            }
            lista.add(new InstalacionReservable(i.getIdInstalacion(), i.getNombre(), DashboardServicio.icono(i.getTipoDisciplina()),
                    i.getTipoDisciplina(), i.getDescripcion(), i.getCapacidad() == null ? null : "Hasta " + i.getCapacidad() + " personas",
                    tarifa == null ? null : DashboardServicio.monto(tarifa.getPrecio()), habilitada && tarifa != null, aviso));
        }
        return lista;
    }

    /** Las franjas de una instalación en un día, cada una libre o con el motivo por el que no se puede elegir. */
    public List<TurnoDisponible> turnos(Integer idSocio, Integer idInstalacion, LocalDate fecha, LocalDateTime ahora) {
        Map<Integer, Reserva> ocupados = new HashMap<>();
        for (Reserva r : reservaDAO.listarActivasDeInstalacionEntre(idInstalacion, fecha, fecha)) {
            ocupados.put(r.getIdTurno().getIdTurno(), r);
        }
        List<TurnoDisponible> lista = new ArrayList<>();
        for (Turno t : turnoDAO.listarPorInstalacion(idInstalacion)) {
            Reserva r = ocupados.get(t.getIdTurno());
            String motivo = r != null ? (r.getIdSocio().getIdSocio().equals(idSocio) ? "Tu reserva" : "Reservado")
                    : !fecha.atTime(t.getHoraInicio()).isAfter(ahora) ? "Ya pasó" : null;
            lista.add(new TurnoDisponible(t.getIdTurno(), t.getHoraInicio().format(HORA) + " – " + t.getHoraFin().format(HORA),
                    motivo == null, motivo));
        }
        return lista;
    }

    /** Las franjas de la instalación de una reserva propia con crédito, para elegir a cuál reprogramarla; vacío si no es suya. */
    public List<TurnoDisponible> turnosParaReprogramar(Integer idSocio, Integer idReserva, LocalDate fecha, LocalDateTime ahora) {
        Reserva r = idReserva == null ? null : reservaDAO.buscarPorId(idReserva);
        if (r == null || !r.getIdSocio().getIdSocio().equals(idSocio)) {
            return new ArrayList<>();
        }
        return turnos(idSocio, r.getIdTurno().getIdInstalacion().getIdInstalacion(), fecha, ahora);
    }

    /** La cuota mensual vigente de su tipo de socio; null si no hay. */
    public String cuota(Socio socio, LocalDate hoy) {
        TarifaMembresia t = tarifaMembresiaDAO.buscarVigente(socio.getIdCategoriaSocio().getIdCategoriaSocio(), hoy);
        return t == null ? null : DashboardServicio.monto(t.getPrecio());
    }

    /** El precio del turno para el socio, con texto; null si no hay tarifa vigente. */
    public String precio(Socio socio, Integer idInstalacion, LocalDate fecha) {
        TarifaAlquiler t = tarifaDAO.buscarVigente(idInstalacion, socio.getIdCategoriaSocio().getIdCategoriaSocio(), fecha);
        return t == null ? null : DashboardServicio.monto(t.getPrecio());
    }

    // ---------- escritura ----------

    /**
     * Reserva un turno: queda PENDIENTE_PAGO (ocupa el turno) con la tarifa del socio. Si dos socios piden el mismo turno
     * a la vez, el segundo choca con el UNIQUE de la base y recibe el aviso de que ya lo tomaron.
     * @return el id de la reserva creada.
     * @throws ReglaReservaException si alguna regla lo impide.
     */
    @Transactional
    public Integer reservar(Integer idSocio, Integer idTurno, LocalDate fecha, LocalDateTime ahora) {
        Socio socio = socioDAO.buscarPorId(idSocio);
        if (socio == null || socio.isBajaLogica() || socio.getEstado() != Socio.Estado.ACTIVO) {
            throw new ReglaReservaException("Tu membresía no está vigente: renovala para volver a reservar.");
        }
        int pendientes = sinValorar(idSocio, ahora).size();
        if (pendientes > 0) {
            throw new ReglaReservaException("Tenés " + pendientes + (pendientes == 1 ? " valoración pendiente" : " valoraciones pendientes")
                    + ": valorá tu turno anterior para poder reservar de nuevo.");
        }
        Turno turno = idTurno == null ? null : turnoDAO.buscarPorId(idTurno);
        if (turno == null || fecha == null) {
            throw new ReglaReservaException("Elegí el turno que querés reservar.");
        }
        Instalacion instalacion = turno.getIdInstalacion();
        if (instalacion.getTipoAcceso() != Instalacion.TipoAcceso.ARANCELADO) {
            throw new ReglaReservaException("Esa instalación no se reserva: es de acceso libre.");
        }
        if (instalacion.getEstado() != Instalacion.Estado.HABILITADA) {
            throw new ReglaReservaException("La instalación no está disponible.");
        }
        LocalDate hoy = ahora.toLocalDate();
        if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(DIAS_ADELANTE))) {
            throw new ReglaReservaException("Elegí un día entre hoy y los próximos " + DIAS_ADELANTE + " días.");
        }
        if (!fecha.atTime(turno.getHoraInicio()).isAfter(ahora)) {
            throw new ReglaReservaException("Ese turno ya pasó.");
        }
        TarifaAlquiler tarifa = tarifaDAO.buscarVigente(instalacion.getIdInstalacion(),
                socio.getIdCategoriaSocio().getIdCategoriaSocio(), fecha);
        if (tarifa == null) {
            throw new ReglaReservaException("Esa instalación todavía no tiene un precio cargado para tu tipo de socio.");
        }
        if (reservaDAO.contarSinComprobanteDe(idSocio) >= MAX_SIN_COMPROBANTE) {
            throw new ReglaReservaException("Ya tenés " + MAX_SIN_COMPROBANTE
                    + " reservas esperando el comprobante: subilo o liberalas antes de pedir otra.");
        }
        if (reservaDAO.buscarActivaPorTurnoYFecha(idTurno, fecha) != null) {
            throw new ReglaReservaException("Ese turno acaba de ser reservado por otra persona. Elegí otro.");
        }

        Reserva reserva = new Reserva();
        reserva.setIdSocio(socio);
        reserva.setIdTurno(turno);
        reserva.setFechaTurno(fecha);
        reserva.setIdTarifa(tarifa);
        reserva.setEstado(Reserva.Estado.PENDIENTE_PAGO);
        reserva.setFechaReserva(ahora);
        try {
            reservaDAO.insertar(reserva); // el INSERT sale ya: si otro se llevó el turno, el UNIQUE de la BD falla acá
        } catch (PersistenceException e) {
            throw new ReglaReservaException("Ese turno acaba de ser reservado por otra persona. Elegí otro.");
        }
        return reserva.getIdReserva();
    }

    /**
     * Carga el comprobante de la transferencia de una reserva propia que espera pago: guarda el archivo, crea el pago
     * PENDIENTE_VALIDACION por el precio de la tarifa y lo enlaza a la reserva; desde ese momento le llega al administrador.
     * @throws ReglaReservaException si la reserva no admite comprobante o el archivo no sirve.
     */
    @Transactional
    public void subirComprobante(Integer idSocio, Integer idReserva, byte[] datos, LocalDateTime operacion, LocalDateTime ahora) {
        Reserva r = propiaPendiente(idSocio, idReserva);
        if (r.getIdPago() != null) {
            throw new ReglaReservaException("Esa reserva ya tiene un comprobante cargado.");
        }
        if (datos == null || datos.length == 0) {
            throw new ReglaReservaException("Elegí el archivo del comprobante.");
        }
        if (datos.length > MAX_BYTES) {
            throw new ReglaReservaException("El archivo pesa más de 5 MB.");
        }
        String extension = ComprobanteArchivos.extension(datos);
        if (extension == null) {
            throw new ReglaReservaException("El comprobante tiene que ser una imagen (JPG o PNG) o un PDF.");
        }
        if (operacion == null) {
            throw new ReglaReservaException("Indicá cuándo hiciste la transferencia.");
        }
        if (operacion.isAfter(ahora.plusMinutes(5))) {
            throw new ReglaReservaException("La fecha de la transferencia no puede ser futura.");
        }
        if (operacion.isBefore(ahora.minusDays(7))) {
            throw new ReglaReservaException("La transferencia tiene más de 7 días: revisá la fecha y la hora.");
        }
        String hash = sha256(datos);
        if (comprobanteDAO.existePorHash(hash)) {
            throw new ReglaReservaException("Ese comprobante ya fue usado en otra operación.");
        }

        String archivo = "reserva-" + idReserva + "-" + hash.substring(0, 12) + "." + extension;
        Pago pago = new Pago();
        pago.setIdUsuario(r.getIdSocio().getIdUsuario());
        pago.setMonto(r.getIdTarifa().getPrecio());
        pago.setFechaPago(ahora);
        pagoDAO.crear(pago);
        ComprobantePago comprobante = new ComprobantePago();
        comprobante.setIdPago(pago);
        comprobante.setArchivoUrl(archivo);
        comprobante.setHashArchivo(hash);
        comprobante.setFechaOperacionDeclarada(operacion);
        comprobanteDAO.crear(comprobante);
        r.setIdPago(pago);
        try {
            // ponytail: si el commit falla después queda el archivo suelto (el nombre lleva el hash, reintentar lo pisa)
            ComprobanteArchivos.guardar(archivo, datos);
        } catch (IOException e) {
            throw new ReglaReservaException("No pudimos guardar el archivo. Probá de nuevo en un momento.");
        }
    }

    /**
     * Guarda la valoración (1 a 5 estrellas, comentario opcional) de una reserva propia, confirmada y ya terminada.
     * @throws ReglaReservaException si no se puede valorar.
     */
    @Transactional
    public void valorar(Integer idSocio, Integer idReserva, Integer puntaje, String comentario, LocalDateTime ahora) {
        Reserva r = idReserva == null ? null : reservaDAO.buscarPorId(idReserva);
        if (r == null || !r.getIdSocio().getIdSocio().equals(idSocio)) {
            throw new ReglaReservaException("La reserva no existe.");
        }
        if (r.getEstado() != Reserva.Estado.CONFIRMADA || r.getFechaTurno().atTime(r.getIdTurno().getHoraFin()).isAfter(ahora)) {
            throw new ReglaReservaException("Solo se puede valorar una reserva confirmada cuyo turno ya terminó.");
        }
        if (puntaje == null || puntaje < 1 || puntaje > 5) {
            throw new ReglaReservaException("Elegí de 1 a 5 estrellas para valorar.");
        }
        String texto = comentario == null ? "" : comentario.trim();
        ValoracionReserva v = new ValoracionReserva();
        v.setIdReserva(r);
        v.setPuntaje(puntaje);
        v.setComentario(texto.isEmpty() ? null : texto.length() > MAX_COMENTARIO ? texto.substring(0, MAX_COMENTARIO) : texto);
        v.setFechaValoracion(ahora);
        if (valoracionDAO.buscarPorReserva(idReserva) != null || !valoracionDAO.insertar(v)) {
            throw new ReglaReservaException("Ya valoraste esta reserva.");
        }
    }

    /** El socio suelta una reserva que todavía no pagó: pasa a CANCELADA y el turno queda libre. */
    @Transactional
    public void liberar(Integer idSocio, Integer idReserva) {
        Reserva r = propiaPendiente(idSocio, idReserva);
        if (r.getIdPago() != null) {
            throw new ReglaReservaException("Esa reserva ya tiene un comprobante: no se puede liberar.");
        }
        r.setEstado(Reserva.Estado.CANCELADA);
    }

    /**
     * Tarea programada: cancela las reservas que pasaron MINUTOS_PARA_PAGAR sin comprobante y así libera sus turnos.
     * @return cuántas liberó.
     */
    @Transactional
    public int liberarVencidas(LocalDateTime ahora) {
        int liberadas = 0;
        for (Reserva vieja : reservaDAO.listarPendientesSinComprobanteAntesDe(ahora.minusMinutes(MINUTOS_PARA_PAGAR))) {
            Reserva r = reservaDAO.buscarParaResolver(vieja.getIdReserva()); // con bloqueo: el socio puede estar subiéndolo
            if (r.getEstado() == Reserva.Estado.PENDIENTE_PAGO && r.getIdPago() == null) {
                r.setEstado(Reserva.Estado.CANCELADA);
                liberadas++;
            }
        }
        return liberadas;
    }

    /** La reserva (con bloqueo de escritura) si es del socio y sigue esperando el pago; si no, la regla que falló. */
    private Reserva propiaPendiente(Integer idSocio, Integer idReserva) {
        Reserva r = idReserva == null ? null : reservaDAO.buscarParaResolver(idReserva);
        if (r == null || !r.getIdSocio().getIdSocio().equals(idSocio)) {
            throw new ReglaReservaException("La reserva no existe.");
        }
        if (r.getEstado() != Reserva.Estado.PENDIENTE_PAGO) {
            throw new ReglaReservaException("Esa reserva ya no espera el pago (si pasó el plazo, el turno se liberó).");
        }
        return r;
    }

    private static String sha256(byte[] datos) {
        try {
            return String.format("%064x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(datos)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 lo trae toda JVM
        }
    }
}
