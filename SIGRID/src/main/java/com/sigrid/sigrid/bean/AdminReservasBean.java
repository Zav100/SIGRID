package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.FiltroReservas;
import com.sigrid.sigrid.dto.InformeReservas;
import com.sigrid.sigrid.dto.PaginaReservas;
import com.sigrid.sigrid.dto.RankingItem;
import com.sigrid.sigrid.dto.RecaudacionDiaria;
import com.sigrid.sigrid.dto.ReservaListado;
import com.sigrid.sigrid.dto.ResumenReservas;
import com.sigrid.sigrid.servicio.ReservaServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Bean de admin/reservas-dashboard-admin.xhtml. La base aplica los filtros (período, estado, instalación y buscador) y
 * entrega solo la tanda que se muestra y su total; los indicadores y los rankings salen de consultas de agregación.
 * Nunca se cargan todas las reservas (CC-06). El PDF se arma con los mismos filtros, con el período obligatorio.
 */
@Named("adminReservasBean")
@ViewScoped
public class AdminReservasBean implements Serializable {

    private static final int PAGINA = 25;

    @Inject
    private ReservaServicio servicio;
    @Inject
    private LoginBean loginBean;
    @Inject
    private AdminNavBean adminNavBean;

    private LocalDate hoy;
    private List<String> tipos;
    private List<String> instalaciones;
    private ResumenReservas resumen;
    private PaginaReservas pagina;
    private FiltroReservas filtro = new FiltroReservas();
    private InformeReservas informe;
    private int limite = PAGINA;

    // lo que el administrador eligió en la barra de filtros
    private String busqueda = "";
    private String estado = "";  // "", POR_CONFIRMAR, ESPERANDO_PAGO, CONFIRMADA, CANCELADA o RECHAZADA
    private String tiempo = "";  // "", HOY, PROXIMAS, PASADAS, SEMANA o MES
    private String tipo = "";    // "" o una disciplina (tipo de instalación)
    private String instalacion = ""; // "" o el nombre de una instalación
    private String desde = "";   // "" o aaaa-mm-dd (un día elegido a mano pisa al de "tiempo")
    private String hasta = "";

    @PostConstruct
    public void init() {
        hoy = LocalDate.now();
        tipos = servicio.disciplinas();
        instalaciones = servicio.instalaciones();
        recargar();
    }

    private void recargar() {
        hoy = LocalDate.now();
        resumen = servicio.resumen(LocalDateTime.now());
        filtrar();
    }

    /** Arma el filtro con lo elegido en la barra y vuelve a pedir la primera tanda a la base. */
    public void filtrar() {
        limite = PAGINA;
        filtro = armarFiltro();
        informe = null;
        consultar();
    }

    private void consultar() {
        pagina = servicio.listarPagina(filtro, limite, hoy, LocalTime.now());
    }

    private FiltroReservas armarFiltro() {
        FiltroReservas f = new FiltroReservas();
        f.setTexto(busqueda);
        f.setEstado(estado);
        f.setDisciplina(tipo);
        f.setInstalacion(instalacion);
        switch (tiempo) {
            case "HOY":
                f.setDesde(hoy);
                f.setHasta(hoy);
                break;
            case "SEMANA":
                f.setDesde(hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
                f.setHasta(hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
                break;
            case "MES":
                f.setDesde(hoy.withDayOfMonth(1));
                f.setHasta(hoy.with(TemporalAdjusters.lastDayOfMonth()));
                break;
            case "PROXIMAS":
                f.setFinalizadas(false);
                break;
            case "PASADAS":
                f.setFinalizadas(true);
                break;
            default:
                break;
        }
        LocalDate d = dia(desde);
        LocalDate h = dia(hasta);
        if (d != null) {
            f.setDesde(d);
        }
        if (h != null) {
            f.setHasta(h);
        }
        return f;
    }

    private static LocalDate dia(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public void verMas() {
        limite += PAGINA;
        consultar();
    }

    public void limpiar() {
        busqueda = "";
        estado = "";
        tiempo = "";
        tipo = "";
        instalacion = "";
        desde = "";
        hasta = "";
        filtrar();
    }

    // ---------- PDF ----------

    /**
     * Arma el informe con los filtros de la barra (el período es obligatorio). Si no se puede (sin período, sin
     * reservas o demasiadas), deja el aviso en pantalla y la vista no abre la impresión.
     */
    public void prepararInforme() {
        filtro = armarFiltro();
        informe = servicio.informe(filtro, LocalDateTime.now());
        if (informe.getAviso() != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, informe.getAviso(), null));
        }
    }

    public InformeReservas getInforme() {
        return informe;
    }

    // ---------- acciones sobre las que esperan confirmación (AJAX) ----------

    public void confirmar(Integer idReserva) {
        resolver(idReserva, true);
    }

    public void rechazar(Integer idReserva) {
        resolver(idReserva, false);
    }

    private void resolver(Integer idReserva, boolean aceptar) {
        ReservaListado fila = pagina.getFilas().stream().filter(r -> r.getIdReserva().equals(idReserva)).findFirst()
                .orElse(null);
        Integer admin = loginBean.getIdUsuarioLogueado();
        boolean resuelta = aceptar ? servicio.confirmar(idReserva, admin) : servicio.rechazar(idReserva, admin);

        FacesMessage mensaje;
        if (!resuelta) {
            mensaje = new FacesMessage(FacesMessage.SEVERITY_WARN, "Esa solicitud ya no estaba por confirmar.", null);
        } else {
            String reserva = fila == null ? "la solicitud" : fila.getSocio() + " · " + fila.getInstalacion() + " · "
                    + fila.getCuando().toLowerCase() + " " + fila.getHorario();
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
        return resumen.getPorConfirmar();
    }

    public long getDeHoy() {
        return resumen.getDeHoy();
    }

    public long getProximasConfirmadas() {
        return resumen.getProximasConfirmadas();
    }

    public long getCanceladas() {
        return resumen.getCanceladas();
    }

    public int getPorcentajeCanceladas() {
        return resumen.getPorcentajeCanceladas();
    }

    public long getTotal() {
        return resumen.getTotal();
    }

    public RecaudacionDiaria getRecaudacion() {
        return resumen.getRecaudacion();
    }

    public List<RankingItem> getMasReservadas() {
        return resumen.getMasReservadas();
    }

    public List<RankingItem> getHorasPico() {
        return resumen.getHorasPico();
    }

    // ---------- lista y filtros ----------

    public List<ReservaListado> getVisibles() {
        return pagina.getFilas();
    }

    public long getTotalFiltradas() {
        return pagina.getTotal();
    }

    public boolean isHayMas() {
        return pagina.isHayMas();
    }

    public boolean isFiltrando() {
        return filtro.isFiltrando();
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

    public List<String> getInstalaciones() {
        return instalaciones;
    }

    public String getInstalacion() {
        return instalacion;
    }

    public void setInstalacion(String instalacion) {
        this.instalacion = instalacion == null ? "" : instalacion;
    }

    public String getDesde() {
        return desde;
    }

    public void setDesde(String desde) {
        this.desde = desde == null ? "" : desde;
    }

    public String getHasta() {
        return hasta;
    }

    public void setHasta(String hasta) {
        this.hasta = hasta == null ? "" : hasta;
    }
}
