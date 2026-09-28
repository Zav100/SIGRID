package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO de un ítem de la galería de la home: una foto, o un emoji sobre un fondo degradado.
 */
public class GaleriaItem implements Serializable {

    private String imagen;      // Archivo dentro de webapp/assets; null si el ítem es un emoji
    private String emoji;       // Se muestra solo si no hay imagen
    private String claseFondo;  // "photo-real" para fotos; "grad-a".."grad-d" para los degradados del CSS
    private String leyenda;     // Texto del lightbox (data-caption) y alt de la imagen

    public GaleriaItem(String imagen, String emoji, String claseFondo, String leyenda) {
        this.imagen = imagen;
        this.emoji = emoji;
        this.claseFondo = claseFondo;
        this.leyenda = leyenda;
    }

    public String getImagen() {
        return imagen;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getClaseFondo() {
        return claseFondo;
    }

    public String getLeyenda() {
        return leyenda;
    }
}
