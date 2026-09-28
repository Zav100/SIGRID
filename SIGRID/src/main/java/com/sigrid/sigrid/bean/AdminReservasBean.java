package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.IngresosMembresias;
import com.sigrid.sigrid.dto.RankingItem;
import com.sigrid.sigrid.dto.RecaudacionDiaria;
import com.sigrid.sigrid.dto.ReservaListado;
import com.sigrid.sigrid.servicio.DashboardServicio;
import com.sigrid.sigrid.servicio.ReservaServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Bean de admin/reservas-dashboard-admin.xhtml. Las reservas se leen una vez al abrir la página (y al confirmar o
 * denegar una); el buscador y los filtros (AJAX) solo recortan esa lista en memoria. Los indicadores y los
 * rankings se calculan siempre sobre todas las reservas, no sobre las filtradas.
 */
@Named("adminReservasBean")
@ViewScoped
public class AdminReservasBean implements Serializable {

    private static final int PAGINA = 25;
    private static final int TOP = 5;
    private static final int DIAS_DEL_GRAFICO = 7;
    private static final Locale ES = Locale.forLanguageTag("es-AR");

    @Inject
    private ReservaServicio servicio;
    @Inject
    private LoginBean loginBean;
    @Inject
    private AdminNavBean adminNavBean;

    private LocalDate hoy;
    private List<ReservaListado> reservas;
    private List<ReservaListado> filtradas;
    private List<String> tipos;
    private List<RankingItem> masReservadas;
    private List<RankingItem> horasPico;
    private RecaudacionDiaria recaudacion;
    private int limite = PAGINA;

    // lo que el administrador eligió en la barra de filtros
    private String busqueda = "";
    private String estado = "";  // "", POR_CONFIRMAR, ESPERANDO_PAGO, CONFIRMADA, CANCELADA o RECHAZADA
    private String tiempo = "";  // "", HOY, PROXIMAS, PASADAS, SEMANA o MES
    private String tipo = "";    // "" o una disciplina (tipo de instalación)

    @PostConstruct
    public void init() {
        recargar();
    }

    private void recargar() {
        hoy = LocalDate.now();
        reservas = servicio.listarTodas(hoy, LocalTime.now());
        TreeSet<String> disciplinas = new TreeSet<>();
        for (ReservaListado r : reservas) {
            disciplinas.add(r.getDisciplina());
        }
        tipos = new ArrayList<>(disciplinas);
        masReservadas = ranking(false);
        horasPico = ranking(true);
        recaudacion = recaudacion();
        filtrar();
    }

    /** Recorta la lista según el buscador y los tres filtros, y vuelve a la primera página. */
    public void filtrar() {
        limite = PAGINA;
        String consulta = ReservaServicio.normalizar(busqueda == null ? "" : busqueda);
        filtradas = new ArrayList<>();
        for (ReservaListado r : reservas) {
            if ((consulta.isEmpty() || r.getBusqueda().contains(consulta))
                    && (estado.isEmpty() || r.getEstado().equals(estado))
                    && (tipo.isEmpty() || r.getDisciplina().equals(tipo))
                    && deTiempo(r)) {
                filtradas.add(r);
            }
        }
    }

    private boolean deTiempo(ReservaListado r) {
        LocalDate f = r.getFecha();
        switch (tiempo) {
            case "HOY":
                return f.equals(hoy);
            case "PROXIMAS":
                return !r.isFinalizada();
            case "PASADAS":
                return r.isFinalizada();
            case "SEMANA":
                return !f.isBefore(hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                        && !f.isAfter(hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case "MES":
                return YearMonth.from(f).equals(YearMonth.from(hoy));
            default:
                return true;
        }
    }

    public void verMas() {
        limite += PAGINA;
    }

    /** Para exportar a PDF: muestra todas las filas (de las filtradas) y no solo la primera página. */
    public void mostrarTodo() {
        limite = Math.max(PAGINA, filtradas.size());
    }

    public void limpiar() {
        busqueda = "";
        estado = "";
        tiempo = "";
        tipo = "";
        filtrar();
    }

    // ---------- acciones sobre las que esperan confirmación (AJAX) ----------

    public void confirmar(Integer idReserva) {
        resolver(idReserva, true);
    }

    public void rechazar(Integer idReserva) {
        resolver(idReserva, false);
    }

    private void resolver(Integer idReserva, boolean aceptar) {
        ReservaListado fila = reservas.stream().filter(r -> r.getIdReserva().equals(idReserva)).findFirst().orElse(null);
        Integer admin = loginBean.getIdUsuarioLogueado();
        boolean resuelta = aceptar ? servicio.confirmar(idReserva, admin) : servicio.rechazar(idReserva, admin);

        FacesMessage mensaje;
        if (!resuelta || fila == null) {
            mensaje = new FacesMessage(FacesMessage.SEVERITY_WARN, "Esa solicitud ya no estaba por confirmar.", null);
        } else {
            String reserva = fila.getSocio() + " · " + fila.getInstalacion() + " · " + fila.getCuando().toLowerCase()
                    + " " + fila.getHorario();
            mensaje = new FacesMessage(FacesMessage.SEVERITY_INFO,
                    (aceptar ? "Confirmaste la reserva de " : "Rechazaste la solicitud de ") + reserva
                    + (aceptar ? "." : ". El turno quedó libre."), null);
        }
        FacesContext.getCurrentInstance().addMessage(null, mensaje);
        recargar();
        adminNavBean.refrescar(); // el contador de solicitudes del menú ya cambió
    }

    // ---------- indicadores y rankings (sobre todas las reservas) ----------

    public long getPorConfirmar() {
        return reservas.stream().filter(ReservaListado::isPorConfirmar).count();
    }

    public long getDeHoy() {
        return reservas.stream().filter(r -> r.getFecha().equals(hoy) && r.isActiva()).count();
    }

    public long getProximasConfirmadas() {
        return reservas.stream().filter(r -> "CONFIRMADA".equals(r.getEstado()) && !r.isFinalizada()).count();
    }

    public long getCanceladas() {
        return reservas.stream().filter(r -> !r.isActiva()).count();
    }

    /** Qué parte de todas las reservas terminó cancelada o rechazada, en %. */
    public int getPorcentajeCanceladas() {
        return reservas.isEmpty() ? 0 : (int) Math.round(100.0 * getCanceladas() / reservas.size());
    }

    public int getTotal() {
        return reservas.size();
    }

    public RecaudacionDiaria getRecaudacion() {
        return recaudacion;
    }

    /** Lo cobrado por reservas confirmadas hoy (por día de confirmación, como el ingreso en la cuenta) y los últimos días. */
    private RecaudacionDiaria recaudacion() {
        BigDecimal[] porDia = new BigDecimal[DIAS_DEL_GRAFICO]; // el último es hoy
        java.util.Arrays.fill(porDia, BigDecimal.ZERO);
        int cantidadHoy = 0;
        for (ReservaListado r : reservas) {
            if (r.getConfirmadaEl() == null) {
                continue;
            }
            long atras = java.time.temporal.ChronoUnit.DAYS.between(r.getConfirmadaEl(), hoy);
            if (atras >= 0 && atras < DIAS_DEL_GRAFICO) {
                int i = DIAS_DEL_GRAFICO - 1 - (int) atras;
                porDia[i] = porDia[i].add(r.getCobrado());
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

    public List<RankingItem> getMasReservadas() {
        return masReservadas;
    }

    public List<RankingItem> getHorasPico() {
        return horasPico;
    }

    /** Las {@value #TOP} instalaciones (o horas de inicio) con más reservas que ocupan o ocuparon el turno. */
    private List<RankingItem> ranking(boolean porHora) {
        Map<String, Long> cuentas = new LinkedHashMap<>();
        Map<String, String> iconos = new LinkedHashMap<>();
        for (ReservaListado r : reservas) {
            if (r.isActiva()) {
                String clave = porHora ? String.format("%02d:00 h", r.getHoraInicio()) : r.getInstalacion();
                cuentas.merge(clave, 1L, Long::sum);
                iconos.putIfAbsent(clave, porHora ? "schedule" : r.getIcono());
            }
        }
        List<Map.Entry<String, Long>> orden = new ArrayList<>(cuentas.entrySet());
        orden.sort(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));
        List<RankingItem> ranking = new ArrayList<>();
        for (Map.Entry<String, Long> e : orden.subList(0, Math.min(TOP, orden.size()))) {
            ranking.add(new RankingItem(e.getKey(), iconos.get(e.getKey()), e.getValue(),
                    (int) Math.round(100.0 * e.getValue() / orden.get(0).getValue())));
        }
        return ranking;
    }

    // ---------- lista y filtros ----------

    public List<ReservaListado> getVisibles() {
        return filtradas.subList(0, Math.min(limite, filtradas.size()));
    }

    public int getTotalFiltradas() {
        return filtradas.size();
    }

    public boolean isHayMas() {
        return filtradas.size() > limite;
    }

    public boolean isFiltrando() {
        return !busqueda.isBlank() || !estado.isEmpty() || !tiempo.isEmpty() || !tipo.isEmpty();
    }

    public List<String> getTipos() {
        return tipos;
    }

    public String getBusqueda() {
        return busqueda;
    }

    public void setBusqueda(String busqueda) {
        this.busqueda = busqueda == null ? "" : busqueda;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado == null ? "" : estado;
    }

    public String getTiempo() {
        return tiempo;
    }

    public void setTiempo(String tiempo) {
        this.tiempo = tiempo == null ? "" : tiempo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo == null ? "" : tipo;
    }
}
