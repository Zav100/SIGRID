package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.DiaCalendario;
import com.sigrid.sigrid.dto.EspacioPlano;
import com.sigrid.sigrid.dto.EstadoInstalaciones;
import com.sigrid.sigrid.dto.ReservaFila;
import com.sigrid.sigrid.servicio.DashboardServicio;
import com.sigrid.sigrid.servicio.ReservaServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Bean de admin/inicio-dashboard-admin.xhtml. Los datos de las cards se leen una vez al abrir la página;
 * el calendario (mes anterior/siguiente y elegir un día) se actualiza por AJAX y solo vuelve a leer
 * los puntitos del mes y la agenda del día elegido.
 */
@Named("adminDashboardBean")
@ViewScoped
public class AdminDashboardBean implements Serializable {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter DIA_LARGO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES);
    private static final int MAX_SOLICITUDES = 5;
    private static final int MAX_AGENDA = 8;
    private static final int MAX_PROXIMAS = 5;
    private static final int DIAS_ALERTA_VENCIMIENTO = 7;

    @Inject
    private DashboardServicio servicio;
    @Inject
    private ReservaServicio reservaServicio;
    @Inject
    private LoginBean loginBean;
    @Inject
    private AdminNavBean adminNavBean;

    private LocalDate hoy;
    private YearMonth mes;
    private LocalDate diaSeleccionado;

    private long reservasHoy;
    private int ocupacionHoy;
    private int membresiasPorVencer;
    private List<ReservaFila> solicitudes;
    private List<ReservaFila> proximas;
    private EstadoInstalaciones instalaciones;
    private List<DiaCalendario> dias;
    private List<ReservaFila> agenda;
    private List<EspacioPlano> plano;

    @PostConstruct
    public void init() {
        hoy = LocalDate.now();
        mes = YearMonth.from(hoy);
        diaSeleccionado = hoy;
        recargar();
    }

    /** Vuelve a leer todo, conservando el mes y el día que el administrador tenía elegidos. */
    private void recargar() {
        reservasHoy = servicio.contarActivasDelDia(hoy);
        ocupacionHoy = servicio.porcentajeOcupacion(reservasHoy);
        membresiasPorVencer = servicio.contarMembresiasPorVencer(hoy, DIAS_ALERTA_VENCIMIENTO);
        solicitudes = servicio.solicitudesPendientes(hoy);
        proximas = servicio.proximasConfirmadas(hoy, LocalTime.now(), MAX_PROXIMAS);
        instalaciones = servicio.estadoInstalaciones();
        cargarCalendario();
        cargarAgenda();
    }

    // ---------- acciones del calendario (AJAX) ----------

    public void mesAnterior() {
        mes = mes.minusMonths(1);
        cargarCalendario();
    }

    public void mesSiguiente() {
        mes = mes.plusMonths(1);
        cargarCalendario();
    }

    public void irAHoy() {
        seleccionarDia(hoy);
    }

    /** Elige un día: el calendario salta a su mes (si era un día de relleno) y la agenda pasa a mostrar ese día. */
    public void seleccionarDia(LocalDate fecha) {
        diaSeleccionado = fecha;
        mes = YearMonth.from(fecha);
        cargarCalendario();
        cargarAgenda();
    }

    private void cargarCalendario() {
        dias = servicio.calendario(mes, diaSeleccionado, hoy);
    }

    /** La agenda y el plano son del mismo día, así que se leen juntos. */
    private void cargarAgenda() {
        agenda = servicio.agendaDelDia(diaSeleccionado, hoy);
        plano = servicio.plano(agenda, diaSeleccionado);
    }

    // ---------- acciones sobre las solicitudes (AJAX) ----------

    public void confirmar(Integer idReserva) {
        resolver(idReserva, true);
    }

    public void rechazar(Integer idReserva) {
        resolver(idReserva, false);
    }

    private void resolver(Integer idReserva, boolean aceptar) {
        ReservaFila fila = solicitudes.stream().filter(s -> s.getIdReserva().equals(idReserva)).findFirst().orElse(null);
        Integer admin = loginBean.getIdUsuarioLogueado();
        boolean resuelta = aceptar ? reservaServicio.confirmar(idReserva, admin) : reservaServicio.rechazar(idReserva, admin);

        FacesMessage mensaje;
        if (!resuelta || fila == null) {
            mensaje = new FacesMessage(FacesMessage.SEVERITY_WARN, "Esa solicitud ya no estaba por confirmar.", null);
        } else {
            String reserva = fila.getSocio() + " · " + fila.getInstalacion() + " · " + fila.getCuando().toLowerCase(ES)
                    + " " + fila.getHorario();
            mensaje = new FacesMessage(FacesMessage.SEVERITY_INFO,
                    (aceptar ? "Confirmaste la reserva de " : "Rechazaste la solicitud de ") + reserva
                    + (aceptar ? "." : ". El turno quedó libre."), null);
        }
        FacesContext.getCurrentInstance().addMessage(null, mensaje);
        recargar();
        adminNavBean.refrescar(); // el contador de solicitudes del menú ya cambió
    }

    // ---------- encabezado ----------

    public String getSaludo() {
        int hora = LocalTime.now().getHour();
        String momento = hora < 12 ? "Buenos días" : hora < 20 ? "Buenas tardes" : "Buenas noches";
        return momento + ", " + loginBean.getNombreUsuarioLogueado();
    }

    public String getResumenDelDia() {
        return servicio.resumenDelDia(reservasHoy, solicitudes.size());
    }

    public String getFechaLarga() {
        return capitalizar(hoy.format(FECHA_LARGA));
    }

    // ---------- indicadores ----------

    public long getReservasHoy() {
        return reservasHoy;
    }

    public int getOcupacionHoy() {
        return ocupacionHoy;
    }

    public int getMembresiasPorVencer() {
        return membresiasPorVencer;
    }

    public int getDiasAlertaVencimiento() {
        return DIAS_ALERTA_VENCIMIENTO;
    }

    public int getTotalSolicitudes() {
        return solicitudes.size();
    }

    // ---------- listas ----------

    public List<ReservaFila> getSolicitudesVisibles() {
        return solicitudes.subList(0, Math.min(MAX_SOLICITUDES, solicitudes.size()));
    }

    public int getSolicitudesOcultas() {
        return solicitudes.size() - getSolicitudesVisibles().size();
    }

    public List<ReservaFila> getProximas() {
        return proximas;
    }

    public List<ReservaFila> getAgendaVisible() {
        return agenda.subList(0, Math.min(MAX_AGENDA, agenda.size()));
    }

    public int getAgendaOculta() {
        return agenda.size() - getAgendaVisible().size();
    }

    public int getTotalAgenda() {
        return agenda.size();
    }

    public EstadoInstalaciones getInstalaciones() {
        return instalaciones;
    }

    public List<EspacioPlano> getPlano() {
        return plano;
    }

    /** Qué día muestran el plano y la agenda: "Hoy", "Mañana" o "jueves 1 de octubre". */
    public String getEtiquetaDia() {
        if (diaSeleccionado.equals(hoy)) {
            return "Hoy";
        }
        return diaSeleccionado.equals(hoy.plusDays(1)) ? "Mañana" : diaSeleccionado.format(DIA_LARGO);
    }

    // ---------- calendario ----------

    public List<DiaCalendario> getDias() {
        return dias;
    }

    public String getTituloMes() {
        return capitalizar(mes.getMonth().getDisplayName(TextStyle.FULL, ES)) + " " + mes.getYear();
    }

    /** ¿El calendario ya está en el mes actual con el día de hoy elegido? (entonces no hace falta el botón "Hoy") */
    public boolean isEnHoy() {
        return mes.equals(YearMonth.from(hoy)) && diaSeleccionado.equals(hoy);
    }

    public String getTituloAgenda() {
        if (diaSeleccionado.equals(hoy)) {
            return "Reservas de hoy";
        }
        if (diaSeleccionado.equals(hoy.plusDays(1))) {
            return "Reservas de mañana";
        }
        return "Reservas del " + diaSeleccionado.format(DIA_LARGO);
    }

    private static String capitalizar(String texto) {
        return texto.substring(0, 1).toUpperCase(ES) + texto.substring(1);
    }
}
