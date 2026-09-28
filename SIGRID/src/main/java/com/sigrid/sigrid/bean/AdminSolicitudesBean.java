package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.SolicitudFila;
import com.sigrid.sigrid.servicio.ReservaServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Bean de admin/solicitudes-dashboard-admin.xhtml. Lee las solicitudes al abrir la página, al resolver una y cada
 * pocos segundos (la página se actualiza sola para que las nuevas aparezcan); el buscador, el filtro y el orden
 * (AJAX) solo recortan y ordenan esa lista en memoria.
 */
@Named("adminSolicitudesBean")
@ViewScoped
public class AdminSolicitudesBean implements Serializable {

    @Inject
    private ReservaServicio servicio;
    @Inject
    private LoginBean loginBean;
    @Inject
    private AdminNavBean adminNavBean;

    private List<SolicitudFila> solicitudes;
    private List<SolicitudFila> filtradas;
    private List<String> tipos;

    private String busqueda = "";
    private String tipo = "";       // "" o una disciplina
    private String orden = "TURNO"; // TURNO (el turno más cercano primero) o ANTIGUAS (las que más esperan primero)
    private String motivo = "";     // motivo de la denegación en curso (lo carga un prompt del navegador)

    @PostConstruct
    public void init() {
        recargar();
    }

    /** Vuelve a leer las solicitudes (la lista y los indicadores) y el contador del menú. */
    public void actualizar() {
        recargar();
        adminNavBean.refrescar();
    }

    private void recargar() {
        solicitudes = servicio.listarSolicitudes(LocalDateTime.now());
        TreeSet<String> disciplinas = new TreeSet<>();
        for (SolicitudFila s : solicitudes) {
            disciplinas.add(s.getDisciplina());
        }
        tipos = new ArrayList<>(disciplinas);
        filtrar();
    }

    public void filtrar() {
        String consulta = ReservaServicio.normalizar(busqueda);
        filtradas = new ArrayList<>();
        for (SolicitudFila s : solicitudes) {
            if ((consulta.isEmpty() || s.getBusqueda().contains(consulta))
                    && (tipo.isEmpty() || s.getDisciplina().equals(tipo))) {
                filtradas.add(s);
            }
        }
        filtradas.sort("ANTIGUAS".equals(orden)
                ? Comparator.comparing(SolicitudFila::getEnviada)
                : Comparator.comparing(SolicitudFila::getInicioTurno));
    }

    public void confirmar(Integer idReserva) {
        resolver(idReserva, true);
    }

    public void rechazar(Integer idReserva) {
        resolver(idReserva, false);
    }

    private void resolver(Integer idReserva, boolean aceptar) {
        SolicitudFila fila = solicitudes.stream().filter(s -> s.getIdReserva().equals(idReserva)).findFirst().orElse(null);
        Integer admin = loginBean.getIdUsuarioLogueado();
        boolean resuelta = aceptar ? servicio.confirmar(idReserva, admin) : servicio.rechazar(idReserva, admin, motivo);
        motivo = "";

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
        actualizar();
    }

    // ---------- indicadores (sobre todas las solicitudes, no sobre las filtradas) ----------

    public int getTotal() {
        return solicitudes.size();
    }

    /** Las de turno hoy o mañana: hay que resolverlas primero. */
    public long getUrgentes() {
        return solicitudes.stream().filter(s -> s.isUrgente() && !s.isVencida()).count();
    }

    public long getVencidas() {
        return solicitudes.stream().filter(SolicitudFila::isVencida).count();
    }

    /** Suma de lo transferido: lo que debería haber ingresado en la cuenta si se aceptan todas. */
    public String getMontoTotal() {
        BigDecimal suma = BigDecimal.ZERO;
        for (SolicitudFila s : solicitudes) {
            suma = suma.add(s.getImporte());
        }
        return com.sigrid.sigrid.servicio.DashboardServicio.monto(suma);
    }

    // ---------- lista y filtros ----------

    public List<SolicitudFila> getFiltradas() {
        return filtradas;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo == null ? "" : tipo;
    }

    public String getOrden() {
        return orden;
    }

    public void setOrden(String orden) {
        this.orden = orden == null ? "TURNO" : orden;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo == null ? "" : motivo;
    }
}
