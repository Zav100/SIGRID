package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/**
 * DTO de un grupo del menú lateral del panel de administración (título + sus ítems).
 */
public class GrupoMenu implements Serializable {

    private final String titulo;
    private final List<ItemMenu> items;

    public GrupoMenu(String titulo, List<ItemMenu> items) {
        this.titulo = titulo;
        this.items = items;
    }

    public String getTitulo() {
        return titulo;
    }

    public List<ItemMenu> getItems() {
        return items;
    }
}
