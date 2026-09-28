package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.PromedioInstalacion;
import com.sigrid.sigrid.dto.RankingItem;
import com.sigrid.sigrid.dto.ValoracionFila;
import com.sigrid.sigrid.servicio.ValoracionesServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bean de admin/valoraciones-dashboard-admin.xhtml (solo lectura). Las valoraciones se leen una vez al abrir la
 * página; los filtros (AJAX) solo recortan el listado en memoria. Los indicadores, la distribución y los promedios
 * por instalación se calculan siempre sobre todas las valoraciones.
 */
@Named("adminValoracionesBean")
@ViewScoped
public class AdminValoracionesBean implements Serializable {

    private static final int PAGINA = 20;
    private static final List<Integer> ESCALA = List.of(1, 2, 3, 4, 5);

    @Inject
    private ValoracionesServicio servicio;

    private List<ValoracionFila> todas;
    private List<ValoracionFila> filtradas;
    private List<PromedioInstalacion> promedios;
    private List<RankingItem> distribucion;
    private List<String> instalaciones;
    private double promedio;
    private int limite = PAGINA;

    // lo que el administrador eligió en la barra de filtros
    private String instalacion = ""; // "" o el nombre de una instalación
    private String puntaje = "";     // "", 5, 4, 3, 2, 1 o BAJAS (1 y 2 estrellas)
    private String comentario = "";  // "" o CON (solo las que escribieron algo)

    @PostConstruct
    public void init() {
        todas = servicio.listar();
        promedios = servicio.promedios(todas, LocalDateTime.now());
        instalaciones = promedios.stream().map(PromedioInstalacion::getNombre).sorted().toList();
        promedio = todas.stream().mapToInt(ValoracionFila::getPuntaje).average().orElse(0);

        distribucion = new ArrayList<>();
        for (int estrellas = 5; estrellas >= 1; estrellas--) {
            long cantidad = contar(estrellas);
            // el "icono" de cada tramo es el sufijo de su color (val-c5 ... val-c1)
            distribucion.add(new RankingItem(estrellas + (estrellas == 1 ? " estrella" : " estrellas"), "c" + estrellas,
                    cantidad, todas.isEmpty() ? 0 : (int) Math.round(100.0 * cantidad / todas.size())));
        }
        filtrar();
    }

    private long contar(int estrellas) {
        return todas.stream().filter(v -> v.getPuntaje() == estrellas).count();
    }

    /** Recorta el listado según los tres filtros y vuelve a la primera página. */
    public void filtrar() {
        limite = PAGINA;
        filtradas = new ArrayList<>();
        for (ValoracionFila v : todas) {
            if ((instalacion.isEmpty() || v.getInstalacion().equals(instalacion))
                    && cumplePuntaje(v)
                    && (comentario.isEmpty() || v.isConComentario())) {
                filtradas.add(v);
            }
        }
    }

    private boolean cumplePuntaje(ValoracionFila v) {
        if (puntaje.isEmpty()) {
            return true;
        }
        return "BAJAS".equals(puntaje) ? v.isBaja() : String.valueOf(v.getPuntaje()).equals(puntaje);
    }

    public void verMas() {
        limite += PAGINA;
    }

    /** Para exportar a PDF: muestra todas las filas (de las filtradas) y no solo la primera página. */
    public void mostrarTodo() {
        limite = Math.max(PAGINA, filtradas.size());
    }

    public void limpiar() {
        instalacion = "";
        puntaje = "";
        comentario = "";
        filtrar();
    }

    // ---------- indicadores (sobre todas las valoraciones) ----------

    public int getTotal() {
        return todas.size();
    }

    public String getPromedioTexto() {
        return todas.isEmpty() ? "—" : ValoracionesServicio.decimal(promedio);
    }

    /** Qué parte de las valoraciones dio 4 o 5 estrellas, en %. */
    public int getPorcentajeSatisfechos() {
        return todas.isEmpty() ? 0
                : (int) Math.round(100.0 * todas.stream().filter(v -> v.getPuntaje() >= 4).count() / todas.size());
    }

    /** Cuántas dieron 1 o 2 estrellas. */
    public long getParaRevisar() {
        return todas.stream().filter(ValoracionFila::isBaja).count();
    }

    public long getConComentario() {
        return todas.stream().filter(ValoracionFila::isConComentario).count();
    }

    public List<Integer> getEscala() {
        return ESCALA;
    }

    /** Estrellas llenas para el promedio general (redondeado al entero más cercano). */
    public long getEstrellasPromedio() {
        return Math.round(promedio);
    }

    /**
     * Fondo de la dona de la distribución: un tramo por puntaje (5 a 1), con los colores --val-c5 a --val-c1
     * definidos en valoraciones.css. Sin valoraciones no se dibuja.
     */
    public String getGradiente() {
        StringBuilder tramos = new StringBuilder();
        int desde = 0;
        for (int estrellas = 5; estrellas >= 1; estrellas--) {
            int hasta = estrellas == 1 ? 100 : desde + (int) Math.round(100.0 * contar(estrellas) / Math.max(1, todas.size()));
            tramos.append(tramos.length() == 0 ? "" : ", ")
                    .append("var(--val-c").append(estrellas).append(") ").append(desde).append("% ").append(hasta).append('%');
            desde = hasta;
        }
        return "conic-gradient(" + tramos + ")";
    }

    public List<RankingItem> getDistribucion() {
        return distribucion;
    }

    public List<PromedioInstalacion> getPromedios() {
        return promedios;
    }

    // ---------- listado y filtros ----------

    public List<ValoracionFila> getVisibles() {
        return filtradas.subList(0, Math.min(limite, filtradas.size()));
    }

    public int getTotalFiltradas() {
        return filtradas.size();
    }

    public boolean isHayMas() {
        return filtradas.size() > limite;
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

    public String getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(String puntaje) {
        this.puntaje = puntaje == null ? "" : puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario == null ? "" : comentario;
    }
}
