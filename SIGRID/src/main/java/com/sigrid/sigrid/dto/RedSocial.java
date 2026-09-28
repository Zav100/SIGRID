package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO de una red social del footer. El ícono es un SVG (atributo "d" del path)
 * o, si {@code imagen} no es null, un archivo de webapp/assets.
 */
public class RedSocial implements Serializable {

    private String nombre;    // Para aria-label (accesibilidad)
    private String url;       // Link externo al perfil
    private String iconoSvg;  // Atributo "d" del <path> del ícono (viewBox 24x24, relleno currentColor)
    private String imagen;    // Archivo dentro de webapp/assets; tiene prioridad sobre iconoSvg

    /** Red con ícono SVG. */
    public RedSocial(String nombre, String url, String iconoSvg) {
        this.nombre = nombre;
        this.url = url;
        this.iconoSvg = iconoSvg;
    }

    /** Red con ícono en imagen (archivo de webapp/assets). */
    public static RedSocial conImagen(String nombre, String url, String imagen) {
        RedSocial red = new RedSocial(nombre, url, null);
        red.imagen = imagen;
        return red;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUrl() {
        return url;
    }

    public String getIconoSvg() {
        return iconoSvg;
    }

    public String getImagen() {
        return imagen;
    }
}
