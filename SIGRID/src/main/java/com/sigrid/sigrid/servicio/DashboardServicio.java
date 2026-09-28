package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.SolicitudCambioReservaDAO;
import com.sigrid.sigrid.dao.SuscripcionSocioDAO;
import com.sigrid.sigrid.dao.TurnoDAO;
import com.sigrid.sigrid.dto.Apertura;
import com.sigrid.sigrid.dto.DiaCalendario;
import com.sigrid.sigrid.dto.EspacioPlano;
import com.sigrid.sigrid.dto.EstadoInstalaciones;
import com.sigrid.sigrid.dto.ReservaFila;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.Usuario;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Datos del dashboard del administrador (inicio). Solo lectura: consulta a los DAO y devuelve DTO con los
 * textos ya listos para mostrar (horarios, "Hoy"/"Mañana", etiquetas de estado, íconos por disciplina).
 *
 * CDI simple (no EJB) y Serializable porque lo inyecta un bean @ViewScoped (passivating), igual que UsuarioServicio.
 */
@Dependent
public class DashboardServicio implements Serializable {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DIA_LARGO = DateTimeFormatter.ofPattern("d 'de' MMMM", ES);
    private static final int DIAS_DEL_CALENDARIO = 42; // 6 semanas: la grilla no cambia de alto al pasar de mes

    /**
     * Espacios del plano del polideportivo: {clave (su lugar lo define el CSS, .adm-space-{clave}), palabra que lo
     * identifica dentro del nombre de la instalación (sin tildes), nombre si todavía no está cargada, disciplina del ícono}.
     * Una instalación que no esté en esta lista no aparece en el plano (sí en las tablas).
     */
    static final String[][] PLANO = {
        {"rugby", "rugby", "Cancha de Rugby", "Rugby"},
        {"pileta", "pileta", "Pileta", "Natación"},
        {"sum", "sum", "SUM", "Salón de eventos"},
        {"quincho1", "quincho 1", "Quincho 1", "Quincho con asador"},
        {"quincho2", "quincho 2", "Quincho 2", "Quincho con asador"},
        {"quincho3", "quincho 3", "Quincho 3", "Quincho con asador"},
        {"futbol", "futbol", "Cancha de Fútbol 11", "Fútbol"},
        {"beach", "beach", "Cancha de Beach Vóley", "Vóley"}};

    @Inject
    private ReservaDAO reservaDAO;
    @Inject
    private InstalacionDAO instalacionDAO;
    @Inject
    private TurnoDAO turnoDAO;
    @Inject
    private SolicitudCambioReservaDAO solicitudCambioDAO;
    @Inject
    private SuscripcionSocioDAO suscripcionSocioDAO;
    @Inject
    private HorarioServicio horarioServicio;

    /** Agenda de un día: activas y canceladas por horario (las rechazadas no se muestran). */
    public List<ReservaFila> agendaDelDia(LocalDate fecha, LocalDate hoy) {
        return aFilas(reservaDAO.listarDelDia(fecha), hoy);
    }

    /** Solicitudes por confirmar: solo las que ya tienen comprobante cargado, que el administrador debe cotejar a mano. */
    public List<ReservaFila> solicitudesPendientes(LocalDate hoy) {
        return aFilas(reservaDAO.listarPendientes(), hoy);
    }

    public List<ReservaFila> proximasConfirmadas(LocalDate hoy, LocalTime ahora, int maximo) {
        return aFilas(reservaDAO.listarProximasConfirmadas(hoy, ahora, maximo), hoy);
    }

    public long contarActivasDelDia(LocalDate fecha) {
        return reservaDAO.contarActivasDelDia(fecha);
    }

    /** Solicitudes de membresía con comprobante por validar (contador del menú). */
    public long contarMembresiasPendientes() {
        return suscripcionSocioDAO.contarPendientesConComprobante();
    }

    /** Pedidos de cancelación o reprogramación que esperan la aprobación del administrador (contador del menú). */
    public long contarCambiosPendientes() {
        return solicitudCambioDAO.contarPendientes();
    }

    public long contarSolicitudesPendientes() {
        return reservaDAO.contarPendientes();
    }

    /** Turnos ocupados del día sobre los turnos reservables de las instalaciones habilitadas (0 a 100). */
    public int porcentajeOcupacion(long reservasActivas) {
        long turnos = turnoDAO.contarDeInstalacionesHabilitadas();
        return turnos == 0 ? 0 : (int) Math.min(100, Math.round(100.0 * reservasActivas / turnos));
    }

    /** Membresías vigentes que vencen dentro de los próximos días (alerta de vencimiento, RF-03.5). */
    public int contarMembresiasPorVencer(LocalDate hoy, int dias) {
        return suscripcionSocioDAO.listarVigentesQueVencenEntre(hoy, hoy.plusDays(dias)).size();
    }

    public EstadoInstalaciones estadoInstalaciones() {
        Map<Instalacion.Estado, Long> cantidades = instalacionDAO.contarPorEstado();
        return new EstadoInstalaciones(
                cantidades.getOrDefault(Instalacion.Estado.HABILITADA, 0L),
                cantidades.getOrDefault(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, 0L),
                cantidades.getOrDefault(Instalacion.Estado.DESHABILITADA, 0L));
    }

    /** Grilla del mes (lunes a domingo, 6 semanas) con los puntitos de cada día. */
    public List<DiaCalendario> calendario(YearMonth mes, LocalDate seleccionado, LocalDate hoy) {
        LocalDate inicio = mes.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // por día: [0] confirmadas, [1] pendientes, [2] canceladas
        Map<LocalDate, int[]> porDia = new HashMap<>();
        for (Object[] fila : reservaDAO.contarPorFechaYEstado(inicio, inicio.plusDays(DIAS_DEL_CALENDARIO - 1))) {
            int[] cantidades = porDia.computeIfAbsent((LocalDate) fila[0], d -> new int[3]);
            int cantidad = ((Long) fila[2]).intValue();
            switch ((Reserva.Estado) fila[1]) {
                case CONFIRMADA:
                    cantidades[0] = cantidad;
                    break;
                case PENDIENTE_PAGO:
                    cantidades[1] = cantidad;
                    break;
                default:
                    cantidades[2] = cantidad;
            }
        }

        List<DiaCalendario> dias = new ArrayList<>(DIAS_DEL_CALENDARIO);
        for (int i = 0; i < DIAS_DEL_CALENDARIO; i++) {
            LocalDate dia = inicio.plusDays(i);
            int[] c = porDia.getOrDefault(dia, new int[3]);
            dias.add(new DiaCalendario(dia, YearMonth.from(dia).equals(mes), dia.equals(hoy),
                    dia.equals(seleccionado), c[0], c[1], c[2], etiquetaDia(dia, c)));
        }
        return dias;
    }

    /**
     * Plano de las instalaciones para el día de la agenda: cada espacio queda gris si no está disponible
     * (deshabilitado, en mantenimiento o sin cargar), con color si tiene reservas ese día y neutro si está libre.
     * Las de acceso libre (la pileta) no se reservan: se ven con color si están abiertas ese día.
     */
    public List<EspacioPlano> plano(List<ReservaFila> agenda, LocalDate dia) {
        List<Instalacion> instalaciones = instalacionDAO.listarTodos();
        List<EspacioPlano> plano = new ArrayList<>();
        for (String[] espacio : PLANO) {
            Instalacion instalacion = instalaciones.stream()
                    .filter(i -> normalizar(i.getNombre()).contains(espacio[1])).findFirst().orElse(null);
            String nombre = instalacion == null ? espacio[2] : instalacion.getNombre();

            List<ReservaFila> reservas = new ArrayList<>();
            boolean conPendientes = false;
            for (ReservaFila fila : agenda) {
                if (fila.getInstalacion().equals(nombre) && !"off".equals(fila.getEstadoClase())) {
                    reservas.add(fila);
                    conPendientes |= "warn".equals(fila.getEstadoClase());
                }
            }

            String estado;
            String detalle;
            if (instalacion == null || instalacion.getEstado() != Instalacion.Estado.HABILITADA) {
                estado = "off";
                detalle = instalacion == null ? "Sin cargar"
                        : instalacion.getEstado() == Instalacion.Estado.DESHABILITADA_MANTENIMIENTO
                        ? "En mantenimiento" : "Deshabilitada";
            } else if (instalacion.getTipoAcceso() == Instalacion.TipoAcceso.LIBRE) {
                Apertura apertura = horarioServicio.enDia(instalacion.getIdInstalacion(), dia, LocalDateTime.now());
                estado = apertura.isAbierta() ? "ok" : "free";
                detalle = apertura.getTexto();
            } else if (reservas.isEmpty()) {
                estado = "free";
                detalle = "Libre";
            } else {
                estado = conPendientes ? "warn" : "ok";
                detalle = reservas.size() + (reservas.size() == 1 ? " reserva" : " reservas");
            }
            plano.add(new EspacioPlano(instalacion == null ? null : instalacion.getIdInstalacion(), espacio[0], nombre,
                    icono(espacio[3]), estado, detalle, reservas));
        }
        return plano;
    }

    /** Frase de arriba del dashboard: cuántas reservas hay hoy y cuántas solicitudes esperan confirmación. */
    public String resumenDelDia(long reservasHoy, long solicitudes) {
        String hoy = reservasHoy == 0 ? "No hay reservas para hoy" : "Hay " + reservasHoy + (reservasHoy == 1 ? " reserva" : " reservas") + " para hoy";
        return solicitudes == 0
                ? hoy + " y no quedan solicitudes por confirmar."
                : hoy + " y " + solicitudes + (solicitudes == 1 ? " solicitud" : " solicitudes") + " por confirmar.";
    }

    List<ReservaFila> aFilas(List<Reserva> reservas, LocalDate hoy) {
        List<ReservaFila> filas = new ArrayList<>();
        for (Reserva r : reservas) {
            Instalacion instalacion = r.getIdTurno().getIdInstalacion();
            Pago pago = r.getIdPago();
            filas.add(new ReservaFila(r.getIdReserva(), cuando(r.getFechaTurno(), hoy),
                    r.getIdTurno().getHoraInicio().format(HORA) + " – " + r.getIdTurno().getHoraFin().format(HORA),
                    instalacion.getNombre(), instalacion.getTipoDisciplina(), icono(instalacion.getTipoDisciplina()),
                    nombreSocio(r.getIdSocio()), textoEstado(r.getEstado()), claseEstado(r.getEstado()),
                    pago == null ? null : monto(pago.getMonto())));
        }
        return filas;
    }

    /** "$ 18.000"; los centavos solo si los tiene (para poder cotejarlo con el ingreso en la cuenta). */
    public static String monto(BigDecimal monto) {
        NumberFormat formato = NumberFormat.getNumberInstance(ES);
        formato.setMaximumFractionDigits(2);
        formato.setMinimumFractionDigits(monto.stripTrailingZeros().scale() > 0 ? 2 : 0);
        return "$ " + formato.format(monto);
    }

    /** Nombre y apellido; si el socio no completó el alta (nombre NULL), su correo. */
    static String nombreSocio(Socio socio) {
        Usuario usuario = socio.getIdUsuario();
        String nombre = ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
        return nombre.isEmpty() ? usuario.getEmail() : nombre;
    }

    private static String cuando(LocalDate fecha, LocalDate hoy) {
        if (fecha.equals(hoy)) {
            return "Hoy";
        }
        if (fecha.equals(hoy.plusDays(1))) {
            return "Mañana";
        }
        return fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES) + " " + fecha.format(DIA_MES);
    }

    private static String textoEstado(Reserva.Estado estado) {
        switch (estado) {
            case CONFIRMADA:
                return "Confirmada";
            case PENDIENTE_PAGO:
                return "Pendiente";
            default:
                return "Cancelada";
        }
    }

    private static String claseEstado(Reserva.Estado estado) {
        switch (estado) {
            case CONFIRMADA:
                return "ok";
            case PENDIENTE_PAGO:
                return "warn";
            default:
                return "off";
        }
    }

    /** Ícono (Material Symbols) según la disciplina, que es texto libre; si no se reconoce, un estadio. */
    static String icono(String disciplina) {
        String d = normalizar(disciplina);
        if (d.contains("futbol")) {
            return "sports_soccer";
        }
        if (d.contains("voley")) {
            return "sports_volleyball";
        }
        if (d.contains("basquet") || d.contains("basket")) {
            return "sports_basketball";
        }
        if (d.contains("tenis")) {
            return "sports_tennis";
        }
        if (d.contains("natacion") || d.contains("pileta")) {
            return "pool";
        }
        if (d.contains("rugby")) {
            return "sports_rugby";
        }
        if (d.contains("hockey")) {
            return "sports_hockey";
        }
        if (d.contains("gimnasio") || d.contains("fitness")) {
            return "fitness_center";
        }
        if (d.contains("quincho") || d.contains("asador")) {
            return "outdoor_grill";
        }
        if (d.contains("evento")) {
            return "groups";
        }
        return "stadium";
    }

    /** Sin tildes y en minúsculas, para comparar textos libres (nombres y disciplinas). */
    static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(ES);
    }

    private static String etiquetaDia(LocalDate dia, int[] c) {
        List<String> partes = new ArrayList<>();
        if (c[0] > 0) {
            partes.add(c[0] + (c[0] == 1 ? " confirmada" : " confirmadas"));
        }
        if (c[1] > 0) {
            partes.add(c[1] + (c[1] == 1 ? " pendiente" : " pendientes"));
        }
        if (c[2] > 0) {
            partes.add(c[2] + (c[2] == 1 ? " cancelada" : " canceladas"));
        }
        return dia.format(DIA_LARGO) + (partes.isEmpty() ? ": sin reservas" : ": " + String.join(", ", partes));
    }
}
