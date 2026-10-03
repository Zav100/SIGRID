package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO de una tarjeta de evento de la home pública (contenido fijo del sitio).
 */
public class EventoCard implements Serializable {

    private String imagen;    // Archivo dentro de webapp/assets
    private String alt;
    private String etiqueta;  // Deporte / rubro (ej. "Voley")
    private String titulo;
    private String fecha;     // Ya formateada para mostrar (ej. "28 sep 2026")

    public EventoCard(String imagen, String alt, String etiqueta, String titulo, String fecha) {
        this.imagen = imagen;
        this.alt = alt;
        this.etiqueta = etiqueta;
        this.titulo = titulo;
        this.fecha = fecha;
    }

    public String getImagen() {
        return imagen;
    }

    public String getAlt() {
        return alt;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getFecha() {
        return fecha;
    }
}
