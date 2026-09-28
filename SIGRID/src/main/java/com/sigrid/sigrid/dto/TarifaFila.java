package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** DTO de una tarifa vigente de la cuota mensual: tipo de socio y su precio ("$ 4.000"; null si todavía no hay una cargada). */
public class TarifaFila implements Serializable {

    private final String tipo;
    private final String precio;

    public TarifaFila(String tipo, String precio) {
        this.tipo = tipo;
        this.precio = precio;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPrecio() {
        return precio;
    }
}
