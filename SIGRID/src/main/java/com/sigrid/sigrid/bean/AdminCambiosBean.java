package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.CambioPedido;
import com.sigrid.sigrid.dto.CreditoFila;
import com.sigrid.sigrid.dto.ReprogramadaFila;
import com.sigrid.sigrid.dto.ReservaListado;
import com.sigrid.sigrid.servicio.CambiosReservaServicio;
import com.sigrid.sigrid.servicio.ReservaServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Bean de admin/cancelaciones-dashboard-admin.xhtml. Lee todo una vez al abrir la página, al resolver algo y cada
 * pocos segundos (para que un pedido nuevo aparezca solo). Las pestañas y el buscador (AJAX) solo recortan lo leído.
 * Los indicadores son siempre sobre todo, no sobre lo buscado.
 */
@Named("adminCambiosBean")
@ViewScoped
public class AdminCambiosBean implements Serializable {

    @Inject
    private CambiosReservaServicio servicio;
    @Inject
    private ReservaServicio reservaServicio;
    @Inject
    private LoginBean loginBean;
    @Inject
    private AdminNavBean adminNavBean;

    private List<CambioPedido> pedidos;
    private List<CreditoFila> creditos;
    private List<ReprogramadaFila> reprogramadas;
    private List<ReservaListado> cancelables;

    private String pestana = "PEDIDOS"; // PEDIDOS, CREDITOS, REPROGRAMADAS, VENCIDOS o PREDIO
    private String busqueda = "";
    private String motivo = "";         // motivo de la acción en curso (lo carga un prompt del navegador)

    @PostConstruct
    public void init() {
        recargar();
    }

    /** Vuelve a leer todo y el contador del menú (lo llama la actualización automática). */
    public void actualizar() {
        recargar();
        adminNavBean.refrescar();
    }

    private void recargar() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        pedidos = servicio.listarPedidos(ahora);
        creditos = servicio.listarCreditos(hoy);
        reprogramadas = servicio.listarReprogramadas();
        cancelables = new ArrayList<>();
        for (ReservaListado r : reservaServicio.listarTodas(hoy, ahora.toLocalTime())) {
            if ("CONFIRMADA".equals(r.getEstado()) && !r.isFinalizada()) {
                cancelables.add(r);
            }
        }
        cancelables.sort(Comparator.comparing(ReservaListado::getFecha).thenComparing(ReservaListado::getHorario));
    }

    // ---------- acciones ----------

    public void aprobar(Integer idSolicitud) {
        CambioPedido pedido = pedidos.stream().filter(p -> p.getIdSolicitud().equals(idSolicitud)).findFirst().orElse(null);
        String error = null;
        try {
            error = servicio.aprobar(idSolicitud, loginBean.getIdUsuarioLogueado(), LocalDateTime.now());
        } catch (RuntimeException e) { // el turno se lo llevó otra reserva justo ahora (lo frena la BD)
            error = "El turno pedido ya fue reservado por otro socio. Rechazá el pedido.";
        }
        if (error != null) {
            avisar(FacesMessage.SEVERITY_WARN, error);
        } else {
            avisar(FacesMessage.SEVERITY_INFO, pedido == null ? "Aprobaste el pedido."
                    : pedido.isReprogramacion()
                    ? "Aprobaste la reprogramación de " + pedido.getSocio() + ": ahora juega " + pedido.getNuevo() + "."
                    : "Aprobaste la cancelación de " + pedido.getSocio() + ". Le queda un crédito de "
                    + CambiosReservaServicio.DIAS_CREDITO + " días para reprogramar.");
        }
        finalizarAccion();
    }

    public void rechazar(Integer idSolicitud) {
        CambioPedido pedido = pedidos.stream().filter(p -> p.getIdSolicitud().equals(idSolicitud)).findFirst().orElse(null);
        boolean rechazado = servicio.rechazar(idSolicitud, loginBean.getIdUsuarioLogueado(), motivo, LocalDateTime.now());
        avisar(rechazado && pedido != null ? FacesMessage.SEVERITY_INFO : FacesMessage.SEVERITY_WARN,
                rechazado && pedido != null ? "Rechazaste el pedido de " + pedido.getSocio() + ". Su reserva sigue como estaba."
                : "Ese pedido ya no estaba por resolver.");
        finalizarAccion();
    }

    public void cancelarPorElPredio(Integer idReserva) {
        ReservaListado reserva = cancelables.stream().filter(r -> r.getIdReserva().equals(idReserva)).findFirst().orElse(null);
        String error = servicio.cancelarPorElPredio(idReserva, motivo, LocalDateTime.now());
        if (error != null) {
            avisar(FacesMessage.SEVERITY_WARN, error);
        } else {
            avisar(FacesMessage.SEVERITY_INFO, "Cancelaste la reserva de " + (reserva == null ? "el socio" : reserva.getSocio())
                    + ". Le queda un crédito de " + CambiosReservaServicio.DIAS_CREDITO + " días para reprogramar.");
        }
        finalizarAccion();
    }

    private void finalizarAccion() {
        motivo = "";
        actualizar();
    }

    private static void avisar(FacesMessage.Severity nivel, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(nivel, texto, null));
    }

    // ---------- pestañas ----------

    public void elegir(String pestana) {
        this.pestana = pestana;
    }

    public String getPestana() {
        return pestana;
    }

    // ---------- indicadores (sobre todo, no sobre lo buscado) ----------

    public int getTotalPedidos() {
        return pedidos.size();
    }

    public long getCreditosVigentes() {
        return creditos.stream().filter(CreditoFila::isVigente).count();
    }

    public long getCreditosPorVencer() {
        return creditos.stream().filter(CreditoFila::isPorVencer).count();
    }

    public long getCreditosVencidos() {
        return creditos.stream().filter(c -> !c.isVigente()).count();
    }

    public int getTotalReprogramadas() {
        return reprogramadas.size();
    }

    public int getTotalCancelables() {
        return cancelables.size();
    }

    public int getDiasPorVencer() {
        return CambiosReservaServicio.DIAS_POR_VENCER;
    }

    public int getHorasAnticipacion() {
        return CambiosReservaServicio.HORAS_ANTICIPACION;
    }

    public int getDiasCredito() {
        return CambiosReservaServicio.DIAS_CREDITO;
    }

    // ---------- listas (recortadas por el buscador) ----------

    public List<CambioPedido> getPedidos() {
        return filtrar(pedidos, CambioPedido::getBusqueda);
    }

    public List<CreditoFila> getVigentes() {
        List<CreditoFila> vigentes = new ArrayList<>();
        for (CreditoFila c : creditos) {
            if (c.isVigente()) {
                vigentes.add(c);
            }
        }
        return filtrar(vigentes, CreditoFila::getBusqueda);
    }

    /** Créditos que no se usaron a tiempo, el que venció hace menos primero. */
    public List<CreditoFila> getVencidos() {
        List<CreditoFila> vencidos = new ArrayList<>();
        for (CreditoFila c : creditos) {
            if (!c.isVigente()) {
                vencidos.add(c);
            }
        }
        vencidos.sort(Comparator.comparingLong(CreditoFila::getDiasRestantes).reversed());
        return filtrar(vencidos, CreditoFila::getBusqueda);
    }

    public List<ReprogramadaFila> getReprogramadas() {
        return filtrar(reprogramadas, ReprogramadaFila::getBusqueda);
    }

    public List<ReservaListado> getCancelables() {
        return filtrar(cancelables, ReservaListado::getBusqueda);
    }

    private <T> List<T> filtrar(List<T> lista, Function<T, String> texto) {
        String consulta = ReservaServicio.normalizar(busqueda);
        if (consulta.isEmpty()) {
            return lista;
        }
        List<T> filtrada = new ArrayList<>();
        for (T x : lista) {
            if (texto.apply(x).contains(consulta)) {
                filtrada.add(x);
            }
        }
        return filtrada;
    }

    public String getBusqueda() {
        return busqueda;
    }

    public void setBusqueda(String busqueda) {
        this.busqueda = busqueda == null ? "" : busqueda;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo == null ? "" : motivo;
    }
}
