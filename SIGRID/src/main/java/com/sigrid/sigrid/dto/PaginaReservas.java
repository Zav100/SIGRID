package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/** Una tanda de reservas del listado y cuántas hay en total con los mismos filtros. */
public class PaginaReservas implements Serializable {

    private final List<ReservaListado> filas;
    private final long total;

    public PaginaReservas(List<ReservaListado> filas, long total) {
        this.filas = filas;
        this.total = total;
    }

    public List<ReservaListado> getFilas() {
        return filas;
    }

    public long getTotal() {
        return total;
    }

    public boolean isHayMas() {
        return total > filas.size();
    }
}
