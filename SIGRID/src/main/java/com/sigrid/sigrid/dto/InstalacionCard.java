package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO de una tarjeta de instalación de la home pública (contenido fijo del sitio).
 */
public class InstalacionCard implements Serializable {

    private String imagen;     // Archivo dentro de webapp/assets (h:graphicImage value="/assets/...")
    private String alt;
    private String nombre;
    private String subtitulo;
    private String ubicacion;
    private String capacidad;

    public InstalacionCard(String imagen, String alt, String nombre,
            String subtitulo, String ubicacion, String capacidad) {
        this.imagen = imagen;
        this.alt = alt;
        this.nombre = nombre;
        this.subtitulo = subtitulo;
        this.ubicacion = ubicacion;
        this.capacidad = capacidad;
    }

    public String getImagen() {
        return imagen;
    }

    public String getAlt() {
        return alt;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSubtitulo() {
        return subtitulo;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public String getCapacidad() {
        return capacidad;
    }
}
