package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.HistorialTarifa;
import com.sigrid.sigrid.dto.IngresosMembresias;
import com.sigrid.sigrid.dto.SuscripcionFila;
import com.sigrid.sigrid.dto.TarifaFila;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.servicio.MembresiasServicio;
import com.sigrid.sigrid.servicio.TarifasServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bean de admin/membresias-dashboard-admin.xhtml. Al abrirse pone al día los vencimientos y lee todo una vez;
 * el buscador y los filtros (AJAX) recortan la lista en memoria y las acciones (aceptar, denegar, cancelar)
 * vuelven a leer.
 */
@Named("adminMembresiasBean")
@ViewScoped
public class AdminMembresiasBean implements Serializable {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final int DIAS_POR_VENCER = 7;
    private static final int MESES_DEL_GRAFICO = 6;

    @Inject
    private MembresiasServicio servicio;
    @Inject
    private LoginBean loginBean;
    @Inject
    private AdminNavBean adminNavBean;
    @Inject
    private TarifasServicio tarifasServicio;

    private LocalDate hoy;
    private List<SuscripcionFila> membresias;
    private List<SuscripcionFila> filtradas;
    private List<SuscripcionFila> porVencer;
    private IngresosMembresias ingresos;
    private List<TarifaFila> tarifas;
    private List<String> tipos;

    // cambio de la cuota mensual: lo que se está cargando por tipo de socio (id de la categoría -> precio) y desde cuándo rige
    private List<CategoriaSocio> categorias;
    private Map<Integer, String> cuotasNuevas = new HashMap<>();
    private String desdeCuotas = "";
    private List<HistorialTarifa> historialCuotas = List.of();
    private String abrir = ""; // formulario que quedó a medias (data-panel): admin.js lo deja abierto tras el AJAX

    // lo que el administrador eligió: buscador, pestaña de estado ("" = todas) y tipo de socio
    private String busqueda = "";
    private String estado = "";
    private String tipo = "";

    @PostConstruct
    public void init() {
        hoy = LocalDate.now();
        servicio.vencerVencidas(hoy);
        tipos = servicio.tiposDeSocio();
        tarifas = servicio.tarifas(hoy);
        categorias = tarifasServicio.categorias();
        cargarCuotas();
        recargar();
    }

    private void cargarCuotas() {
        cuotasNuevas = new HashMap<>(tarifasServicio.vigentesMembresia(hoy));
        desdeCuotas = hoy.toString();
        historialCuotas = tarifasServicio.historialMembresia(hoy);
    }

    private void recargar() {
        membresias = servicio.listar(hoy);
        porVencer = servicio.porVencer(hoy, DIAS_POR_VENCER);
        ingresos = servicio.ingresos(YearMonth.from(hoy), MESES_DEL_GRAFICO);
        filtrar();
    }

    /** Recorta la lista según el buscador (nombre o DNI), la pestaña de estado y el tipo de socio. */
    public void filtrar() {
        String consulta = normalizar(busqueda);
        filtradas = new ArrayList<>();
        for (SuscripcionFila m : membresias) {
            boolean coincide = consulta.isEmpty() || normalizar(m.getSocio() + " " + m.getDni()).contains(consulta);
            boolean deEstado = estado == null || estado.isEmpty() || m.getEstado().equals(estado);
            boolean deTipo = tipo == null || tipo.isEmpty() || m.getTipo().equals(tipo);
            if (coincide && deEstado && deTipo) {
                filtradas.add(m);
            }
        }
    }

    public void elegirEstado(String nuevo) {
        estado = nuevo;
        filtrar();
    }

    // ---------- cambio de la cuota mensual (AJAX) ----------

    /** Agrega una cuota nueva por tipo de socio (la anterior queda en el historial): rige desde la fecha elegida. */
    public void cambiarCuotas() {
        abrir = "cuotas";
        LocalDate desde;
        try {
            desde = desdeCuotas == null || desdeCuotas.isBlank() ? null : LocalDate.parse(desdeCuotas.trim());
        } catch (DateTimeParseException e) {
            avisarCuotas(FacesMessage.SEVERITY_WARN, "La fecha no es válida.");
            return;
        }
        String error = tarifasServicio.cambiarMembresia(cuotasNuevas, desde, hoy);
        if (error != null) {
            avisarCuotas(FacesMessage.SEVERITY_WARN, error);
            return;
        }
        abrir = "";
        tarifas = servicio.tarifas(hoy);
        cargarCuotas();
        avisarCuotas(FacesMessage.SEVERITY_INFO, "Cambiaste la cuota mensual: rige desde el "
                + desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
    }

    /** El resultado se muestra dentro de la card de la cuota (formulario cuotasForm), no entre los de las membresías. */
    private void avisarCuotas(FacesMessage.Severity gravedad, String texto) {
        FacesContext.getCurrentInstance().addMessage("cuotasForm", new FacesMessage(gravedad, texto, null));
    }

    // ---------- acciones sobre las membresías (AJAX) ----------

    public void aceptar(Integer idSuscripcion) {
        SuscripcionFila fila = buscar(idSuscripcion);
        LocalDate vence = servicio.aceptar(idSuscripcion, loginBean.getIdUsuarioLogueado(), hoy);
        avisar(vence == null || fila == null
                ? new FacesMessage(FacesMessage.SEVERITY_WARN, "Esa solicitud ya no estaba por validar.", null)
                : new FacesMessage(FacesMessage.SEVERITY_INFO, "Aceptaste la membresía de " + fila.getSocio()
                        + ": vigente hasta el " + vence.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " y su carnet queda activo.", null));
    }

    public void denegar(Integer idSuscripcion) {
        SuscripcionFila fila = buscar(idSuscripcion);
        boolean denegada = servicio.denegar(idSuscripcion, loginBean.getIdUsuarioLogueado());
        avisar(!denegada || fila == null
                ? new FacesMessage(FacesMessage.SEVERITY_WARN, "Esa solicitud ya no estaba por validar.", null)
                : new FacesMessage(FacesMessage.SEVERITY_INFO, "Denegaste el comprobante de " + fila.getSocio() + ".", null));
    }

    public void cancelar(Integer idSuscripcion) {
        SuscripcionFila fila = buscar(idSuscripcion);
        Boolean congelado = servicio.cancelar(idSuscripcion, hoy);
        avisar(congelado == null || fila == null
                ? new FacesMessage(FacesMessage.SEVERITY_WARN, "Esa membresía ya no estaba vigente.", null)
                : new FacesMessage(FacesMessage.SEVERITY_INFO, "Cancelaste la membresía de " + fila.getSocio()
                        + (congelado ? ". Su carnet quedó congelado." : "."), null));
    }

    private void avisar(FacesMessage mensaje) {
        FacesContext.getCurrentInstance().addMessage(null, mensaje);
        recargar();
        adminNavBean.refrescar(); // el contador de Membresías del menú ya cambió
    }

    private SuscripcionFila buscar(Integer idSuscripcion) {
        return membresias.stream().filter(m -> m.getIdSuscripcion().equals(idSuscripcion)).findFirst().orElse(null);
    }

    // ---------- indicadores (siempre sobre todas las membresías, no sobre las filtradas) ----------

    public IngresosMembresias getIngresos() {
        return ingresos;
    }

    public long getPorValidar() {
        return membresias.stream().filter(SuscripcionFila::isConComprobante).count();
    }

    /** Plata que está en revisión: la suma de los comprobantes por validar. */
    public String getMontoPorValidar() {
        BigDecimal total = membresias.stream().filter(SuscripcionFila::isConComprobante)
                .map(SuscripcionFila::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
        return servicio.monto(total);
    }

    public long getVigentes() {
        return contar("VIGENTE");
    }

    public long getVencidas() {
        return contar("VENCIDA");
    }

    public long getCanceladas() {
        return contar("CANCELADA");
    }

    public long getPendientes() {
        return contar("PENDIENTE_PAGO");
    }

    public int getTotal() {
        return membresias.size();
    }

    private long contar(String deEstado) {
        return membresias.stream().filter(m -> m.getEstado().equals(deEstado)).count();
    }

    public int getDiasPorVencer() {
        return DIAS_POR_VENCER;
    }

    // ---------- listas ----------

    public List<SuscripcionFila> getFiltradas() {
        return filtradas;
    }

    public List<SuscripcionFila> getPorVencer() {
        return porVencer;
    }

    public List<TarifaFila> getTarifas() {
        return tarifas;
    }

    public List<String> getTipos() {
        return tipos;
    }

    public List<CategoriaSocio> getCategorias() {
        return categorias;
    }

    public Map<Integer, String> getCuotasNuevas() {
        return cuotasNuevas;
    }

    public String getDesdeCuotas() {
        return desdeCuotas;
    }

    public void setDesdeCuotas(String desdeCuotas) {
        this.desdeCuotas = desdeCuotas;
    }

    public List<HistorialTarifa> getHistorialCuotas() {
        return historialCuotas;
    }

    public String getAbrir() {
        return abrir;
    }

    /** Hoy en formato ISO: el mínimo del selector de fecha. */
    public String getHoyIso() {
        return hoy.toString();
    }

    private static String normalizar(String texto) {
        return texto == null || texto.isBlank() ? "" : Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(ES);
    }

    public String getBusqueda() {
        return busqueda;
    }

    public void setBusqueda(String busqueda) {
        this.busqueda = busqueda;
    }

    public String getEstado() {
        return estado;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
