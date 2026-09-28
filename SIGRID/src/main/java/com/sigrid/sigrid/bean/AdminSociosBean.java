package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.SocioDetalle;
import com.sigrid.sigrid.dto.SocioEdicion;
import com.sigrid.sigrid.dto.SocioFila;
import com.sigrid.sigrid.servicio.SociosServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bean de admin/socios-dashboard-admin.xhtml. Los socios se leen una vez al abrir la página (y al dar de baja);
 * el buscador y los filtros (AJAX) solo recortan esa lista en memoria.
 */
@Named("adminSociosBean")
@ViewScoped
public class AdminSociosBean implements Serializable {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final int DIAS_POR_VENCER = 7;

    @Inject
    private SociosServicio servicio;

    private List<SocioFila> socios;
    private List<SocioFila> filtrados;
    private List<String> tipos;
    private SocioDetalle detalle; // la ficha abierta (solo lectura); null si no hay ninguna

    private SocioEdicion edicion;  // el socio que se está editando; null si no hay formulario abierto
    private String errorEdicion;   // por qué no se pudo guardar, se muestra dentro del formulario

    // lo que el administrador eligió en la barra de filtros
    private String busqueda = "";
    private String estado = "";   // "", "VIGENTE" o "EXPIRADA"
    private String tipo = "";     // "" o el nombre de una categoría

    @PostConstruct
    public void init() {
        tipos = servicio.tiposDeSocio();
        recargar();
    }

    private void recargar() {
        socios = servicio.listar(LocalDate.now(), DIAS_POR_VENCER);
        filtrar();
    }

    /** Recorta la lista según el buscador (nombre, correo, DNI o legajo) y los dos filtros. */
    public void filtrar() {
        String consulta = normalizar(busqueda);
        filtrados = new ArrayList<>();
        for (SocioFila s : socios) {
            boolean coincide = consulta.isEmpty()
                    || normalizar(s.getNombre() + " " + s.getEmail() + " " + s.getDni() + " " + s.getLegajo()).contains(consulta);
            boolean deEstado = vacio(estado) || s.isVigente() == "VIGENTE".equals(estado);
            boolean deTipo = vacio(tipo) || s.getCategoria().equals(tipo);
            if (coincide && deEstado && deTipo) {
                filtrados.add(s);
            }
        }
    }

    /** Abre la ficha del socio (ventana emergente, solo lectura). */
    public void verDetalle(Integer idSocio) {
        detalle = servicio.detalle(idSocio, LocalDate.now());
        if (detalle == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Ese socio ya no estaba en el listado.", null));
        }
    }

    public void cerrarDetalle() {
        detalle = null;
    }

    public SocioDetalle getDetalle() {
        return detalle;
    }

    /** Abre el formulario para corregir los datos del socio. */
    public void editar(Integer idSocio) {
        errorEdicion = null;
        edicion = servicio.paraEditar(idSocio);
        if (edicion == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Ese socio ya no estaba en el listado.", null));
            recargar();
        }
    }

    public void guardarEdicion() {
        errorEdicion = servicio.guardar(edicion, LocalDate.now());
        if (errorEdicion == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Guardaste los datos de " + edicion.getNombre() + " " + edicion.getApellido() + ".", null));
            edicion = null;
            recargar();
        }
    }

    public void cancelarEdicion() {
        edicion = null;
        errorEdicion = null;
    }

    public SocioEdicion getEdicion() {
        return edicion;
    }

    public String getErrorEdicion() {
        return errorEdicion;
    }

    public void darDeBaja(Integer idSocio) {
        SocioFila fila = socios.stream().filter(s -> s.getIdSocio().equals(idSocio)).findFirst().orElse(null);
        boolean dadoDeBaja = servicio.darDeBaja(idSocio);
        FacesContext.getCurrentInstance().addMessage(null, dadoDeBaja && fila != null
                ? new FacesMessage(FacesMessage.SEVERITY_INFO, "Diste de baja a " + fila.getNombre() + ".", null)
                : new FacesMessage(FacesMessage.SEVERITY_WARN, "Ese socio ya no estaba en el listado.", null));
        recargar();
    }

    // ---------- indicadores (siempre sobre todos los socios, no sobre los filtrados) ----------

    public int getTotal() {
        return socios.size();
    }

    public long getVigentes() {
        return socios.stream().filter(SocioFila::isVigente).count();
    }

    public long getPorVencer() {
        return socios.stream().filter(SocioFila::isPorVencer).count();
    }

    public long getExpiradas() {
        return socios.stream().filter(s -> !s.isVigente()).count();
    }

    public int getDiasPorVencer() {
        return DIAS_POR_VENCER;
    }

    // ---------- tabla y filtros ----------

    public List<SocioFila> getFiltrados() {
        return filtrados;
    }

    public List<String> getTipos() {
        return tipos;
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isEmpty();
    }

    /** Sin tildes y en minúsculas, para buscar sin distinguir. */
    private static String normalizar(String texto) {
        return vacio(texto) ? "" : Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
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

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
