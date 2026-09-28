package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO de un ítem del menú lateral del panel de administración.
 * vista es la ruta sin extensión (ej. "/admin/inicio-dashboard-admin"); contador es un número
 * opcional que se muestra como globito (ej. solicitudes pendientes), 0 = sin globito.
 */
public class ItemMenu implements Serializable {

    private final String texto;
    private final String icono;
    private final String vista;
    private final long contador;

    public ItemMenu(String texto, String icono, String vista, long contador) {
        this.texto = texto;
        this.icono = icono;
        this.vista = vista;
        this.contador = contador;
    }

    public String getTexto() {
        return texto;
    }

    public String getIcono() {
        return icono;
    }

    public String getVista() {
        return vista;
    }

    /** Archivo de la vista, para saber si existe y si es la página actual (viewId). */
    public String getArchivo() {
        return vista + ".xhtml";
    }

    public long getContador() {
        return contador;
    }
}
