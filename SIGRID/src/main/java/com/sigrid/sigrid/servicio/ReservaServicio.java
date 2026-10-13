package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dao.ComprobantePagoDAO;
import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.TurnoDAO;
import com.sigrid.sigrid.dto.FiltroReservas;
import com.sigrid.sigrid.dto.InformeReservas;
import com.sigrid.sigrid.dto.IngresosMembresias;
import com.sigrid.sigrid.dto.PaginaReservas;
import com.sigrid.sigrid.dto.RankingItem;
import com.sigrid.sigrid.dto.RecaudacionDiaria;
import com.sigrid.sigrid.dto.ReservaListado;
import com.sigrid.sigrid.dto.ResumenReservas;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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

    /** Lo máximo que entra en un PDF: pasado esto se pide acotar el período en vez de armar un documento enorme. */
    public static final int MAXIMO_INFORME = 2000;
    private static final int TOP = 5;
    private static final int DIAS_DEL_GRAFICO = 7;
    private static final DateTimeFormatter EMISION = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Inject
    private InstalacionDAO instalacionDAO;
    @Inject
    private TurnoDAO turnoDAO;

    /**
     * Una tanda del listado del administrador con los filtros aplicados por la base (CC-06). Si la tanda no se llenó,
     * ya se sabe el total y no se vuelve a contar (una búsqueda sin resultados recorre la tabla una sola vez).
     */
    public PaginaReservas listarPagina(FiltroReservas filtro, int cantidad, LocalDate hoy, LocalTime ahora) {
        List<ReservaListado> filas = new ArrayList<>();
        for (Reserva r : reservaDAO.buscarParaAdmin(filtro, hoy, ahora, cantidad)) {
            filas.add(aFila(r, hoy, ahora));
        }
        long total = filas.size() < cantidad ? filas.size() : reservaDAO.contarParaAdmin(filtro, hoy, ahora);
        return new PaginaReservas(filas, total);
    }

    /** Indicadores y rankings de la página de reservas, calculados por la base (no se cargan todas las filas). */
    public ResumenReservas resumen(LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        LocalTime hora = ahora.toLocalTime();

        long canceladas = 0;
        long total = 0;
        for (Object[] fila : reservaDAO.contarPorEstado()) {
            Reserva.Estado estado = (Reserva.Estado) fila[0];
            long cantidad = ((Number) fila[1]).longValue();
            total += cantidad;
            if (estado == Reserva.Estado.CANCELADA || estado == Reserva.Estado.RECHAZADA) {
                canceladas += cantidad;
            }
        }

        // las reservas activas se cuentan por franja y se suman por instalación y por hora de inicio
        Map<Integer, Long> porTurno = new HashMap<>();
        for (Object[] fila : reservaDAO.contarActivasPorTurno()) {
            porTurno.put((Integer) fila[0], ((Number) fila[1]).longValue());
        }
        Map<String, Long> porInstalacion = new HashMap<>();
        Map<String, String> disciplinas = new HashMap<>();
        Map<Integer, Long> porHora = new TreeMap<>(); // varios turnos de la misma hora de inicio se suman
        for (Turno t : turnoDAO.listarConInstalacion()) {
            Long cantidad = porTurno.get(t.getIdTurno());
            if (cantidad == null) {
                continue;
            }
            porInstalacion.merge(t.getIdInstalacion().getNombre(), cantidad, Long::sum);
            disciplinas.putIfAbsent(t.getIdInstalacion().getNombre(), t.getIdInstalacion().getTipoDisciplina());
            porHora.merge(t.getHoraInicio().getHour(), cantidad, Long::sum);
        }
        List<Map.Entry<String, Long>> ordenInstalaciones = new ArrayList<>(porInstalacion.entrySet());
        ordenInstalaciones.sort(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));
        List<RankingItem> masReservadas = new ArrayList<>();
        for (Map.Entry<String, Long> e : ordenInstalaciones.subList(0, Math.min(TOP, ordenInstalaciones.size()))) {
            masReservadas.add(new RankingItem(e.getKey(), DashboardServicio.icono(disciplinas.get(e.getKey())),
                    e.getValue(), (int) Math.round(100.0 * e.getValue() / ordenInstalaciones.get(0).getValue())));
        }

        List<Map.Entry<Integer, Long>> orden = new ArrayList<>(porHora.entrySet());
        orden.sort(Map.Entry.<Integer, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));
        List<RankingItem> horasPico = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : orden.subList(0, Math.min(TOP, orden.size()))) {
            horasPico.add(new RankingItem(String.format("%02d:00 h", e.getKey()), "schedule", e.getValue(),
                    (int) Math.round(100.0 * e.getValue() / orden.get(0).getValue())));
        }

        return new ResumenReservas(reservaDAO.contarPendientes(), reservaDAO.contarActivasDelDia(hoy),
                reservaDAO.contarProximasConfirmadas(hoy, hora), canceladas, total, masReservadas, horasPico,
                recaudacion(hoy, reservaDAO.listarConfirmadasDesde(hoy.minusDays(DIAS_DEL_GRAFICO - 1).atStartOfDay())));
    }

    /** Lo cobrado por reservas confirmadas hoy (por día de confirmación, como el ingreso en la cuenta) y los últimos días. */
    private static RecaudacionDiaria recaudacion(LocalDate hoy, List<Reserva> confirmadas) {
        BigDecimal[] porDia = new BigDecimal[DIAS_DEL_GRAFICO]; // el último es hoy
        java.util.Arrays.fill(porDia, BigDecimal.ZERO);
        int cantidadHoy = 0;
        for (Reserva r : confirmadas) {
            if (r.getIdPago() == null || r.getFechaConfirmacion() == null) {
                continue;
            }
            long atras = java.time.temporal.ChronoUnit.DAYS.between(r.getFechaConfirmacion().toLocalDate(), hoy);
            if (atras >= 0 && atras < DIAS_DEL_GRAFICO) {
                int i = DIAS_DEL_GRAFICO - 1 - (int) atras;
                porDia[i] = porDia[i].add(r.getIdPago().getMonto());
                cantidadHoy += atras == 0 ? 1 : 0;
            }
        }

        BigDecimal maximo = BigDecimal.ZERO;
        for (BigDecimal m : porDia) {
            maximo = maximo.max(m);
        }
        List<IngresosMembresias.Mes> dias = new ArrayList<>();
        for (int i = 0; i < DIAS_DEL_GRAFICO; i++) {
            boolean esHoy = i == DIAS_DEL_GRAFICO - 1;
            int porcentaje = maximo.signum() == 0 ? 0
                    : porDia[i].multiply(BigDecimal.valueOf(100)).divide(maximo, 0, RoundingMode.HALF_UP).intValue();
            dias.add(new IngresosMembresias.Mes(
                    esHoy ? "hoy" : hoy.minusDays(DIAS_DEL_GRAFICO - 1 - i).getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, ES).replace(".", ""),
                    DashboardServicio.monto(porDia[i]), porcentaje, esHoy));
        }

        BigDecimal actual = porDia[DIAS_DEL_GRAFICO - 1];
        BigDecimal ayer = porDia[DIAS_DEL_GRAFICO - 2];
        String comparacion;
        String tendencia;
        if (ayer.signum() == 0) {
            comparacion = actual.signum() == 0 ? "Todavía sin cobros hoy" : "Sin cobros ayer";
            tendencia = "flat";
        } else {
            long variacion = actual.subtract(ayer).multiply(BigDecimal.valueOf(100))
                    .divide(ayer, 0, RoundingMode.HALF_UP).longValue();
            comparacion = (variacion > 0 ? "+" : "") + variacion + " % respecto de ayer";
            tendencia = variacion > 0 ? "up" : variacion < 0 ? "down" : "flat";
        }
        return new RecaudacionDiaria(DashboardServicio.monto(actual), cantidadHoy, comparacion, tendencia, dias);
    }

    /**
     * Lo que se exporta a PDF: el período y los filtros elegidos. Sin período, con el período al revés, sin reservas o
     * con más de {@link #MAXIMO_INFORME} no hay documento: el aviso dice por qué (nunca se arma un PDF vacío ni uno
     * que agote la memoria). La base cuenta primero y solo trae las filas si entran.
     */
    public InformeReservas informe(FiltroReservas filtro, LocalDateTime ahora) {
        String emitido = ahora.format(EMISION);
        LocalDate desde = filtro.getDesde();
        LocalDate hasta = filtro.getHasta();
        if (desde == null || hasta == null) {
            return sinInforme("", filtro, emitido, "Elegí el período (desde y hasta) antes de exportar.");
        }
        if (desde.isAfter(hasta)) {
            return sinInforme("", filtro, emitido,
                    "El período está al revés: la fecha desde es posterior a la fecha hasta.");
        }
        String periodo = desde.equals(hasta) ? desde.format(FECHA) : desde.format(FECHA) + " al " + hasta.format(FECHA);

        LocalDate hoy = ahora.toLocalDate();
        LocalTime hora = ahora.toLocalTime();
        long total = reservaDAO.contarParaAdmin(filtro, hoy, hora);
        if (total == 0) {
            return sinInforme(periodo, filtro, emitido, "No hay reservas en el período elegido (" + periodo + ").");
        }
        if (total > MAXIMO_INFORME) {
            NumberFormat n = NumberFormat.getIntegerInstance(ES);
            return sinInforme(periodo, filtro, emitido, "El período tiene " + n.format(total)
                    + " reservas y un PDF admite hasta " + n.format(MAXIMO_INFORME) + ": acotá el período o los filtros.");
        }
        List<ReservaListado> filas = new ArrayList<>();
        for (Reserva r : reservaDAO.buscarParaInforme(filtro, hoy, hora, MAXIMO_INFORME)) {
            filas.add(aFila(r, hoy, hora));
        }
        return new InformeReservas(filas, total, periodo, textoDeFiltros(filtro), emitido, null);
    }

    private static InformeReservas sinInforme(String periodo, FiltroReservas filtro, String emitido, String aviso) {
        return new InformeReservas(new ArrayList<>(), 0, periodo, textoDeFiltros(filtro), emitido, aviso);
    }

    /** "Estado: Confirmadas · Instalación: Fútbol · Búsqueda: «gomez»"; vacío si no hay filtros además del período. */
    private static String textoDeFiltros(FiltroReservas f) {
        List<String> partes = new ArrayList<>();
        switch (f.getEstado()) {
            case "POR_CONFIRMAR":
                partes.add("Estado: Por confirmar");
                break;
            case "ESPERANDO_PAGO":
                partes.add("Estado: Esperando pago");
                break;
            case "CONFIRMADA":
                partes.add("Estado: Confirmadas");
                break;
            case "CANCELADA":
                partes.add("Estado: Canceladas");
                break;
            case "RECHAZADA":
                partes.add("Estado: Rechazadas");
                break;
            default:
                break;
        }
        if (!f.getDisciplina().isEmpty()) {
            partes.add("Instalación: " + f.getDisciplina());
        }
        if (f.getFinalizadas() != null) {
            partes.add(f.getFinalizadas() ? "Turnos: ya pasaron" : "Turnos: próximos");
        }
        if (!f.getTexto().isEmpty()) {
            partes.add("Búsqueda: «" + f.getTexto() + "»");
        }
        return String.join(" · ", partes);
    }

    /** Los tipos de instalación que se reservan, para el filtro. */
    public List<String> disciplinas() {
        TreeSet<String> tipos = new TreeSet<>();
        for (Instalacion i : instalacionDAO.listarReservables()) {
            tipos.add(i.getTipoDisciplina());
        }
        return new ArrayList<>(tipos);
    }

    /** Las confirmadas que todavía no terminaron, la más próxima primero (las que el predio puede cancelar). */
    public List<ReservaListado> listarProximasConfirmadas(LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        LocalTime hora = ahora.toLocalTime();
        List<ReservaListado> filas = new ArrayList<>();
        for (Reserva r : reservaDAO.listarProximasConfirmadasParaAdmin(hoy, hora)) {
            filas.add(aFila(r, hoy, hora));
        }
        return filas;
    }

    /** Una reserva tal como la ve el administrador en el listado (estado, monto, buscador). */
    private ReservaListado aFila(Reserva r, LocalDate hoy, LocalTime ahora) {
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
                        && !CambiosReservaServicio.limiteDelCredito(r, hoy).isBefore(hoy);
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
        return new ReservaListado(r.getIdReserva(), r.getFechaTurno(), cuando(r.getFechaTurno(), hoy), horario,
                turno.getHoraInicio().getHour(), finalizada, instalacion.getNombre(), instalacion.getTipoDisciplina(),
                DashboardServicio.icono(instalacion.getTipoDisciplina()), socio, usuario.getEmail(), estado, texto,
                clase, monto, r.getFechaReserva().format(RESERVADA),
                normalizar(socio + " " + usuario.getEmail() + " " + r.getIdSocio().getDni() + " "
                        + instalacion.getNombre() + " " + instalacion.getTipoDisciplina() + " "
                        + r.getFechaTurno().format(FECHA) + " " + r.getIdReserva()),
                confirmada ? r.getFechaConfirmacion().toLocalDate() : null, confirmada ? pago.getMonto() : null);
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
