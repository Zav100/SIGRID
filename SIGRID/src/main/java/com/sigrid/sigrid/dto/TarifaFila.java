package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO de una tarifa vigente de la cuota mensual: tipo de socio, su precio ("$ 4.000"; null si todavía no hay una cargada),
 * desde cuándo rige y el próximo cambio programado (CC-03).
 */
public class TarifaFila implements Serializable {

    private final String tipo;
    private final String precio;
    private final String vigencia;
    private final String proximo;

    public TarifaFila(String tipo, String precio, String vigencia, String proximo) {
        this.tipo = tipo;
        this.precio = precio;
        this.vigencia = vigencia;
        this.proximo = proximo;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPrecio() {
        return precio;
    }

    public String getVigencia() {
        return vigencia;
    }

    public String getProximo() {
        return proximo;
    }
}
