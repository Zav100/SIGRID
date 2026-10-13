package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO de un precio con su vigencia, para las pantallas de tarifas (CC-03): lo que rige hoy ("$ 20.000"; null si no hay),
 * desde cuándo rige ("Rige desde el 01/10/2026") y el próximo cambio programado ("Desde el 01/11/2026: $ 22.000" o
 * "Sin cambios programados").
 */
public class PrecioVigente implements Serializable {

    private final String precio;
    private final String vigencia;
    private final String proximo;

    public PrecioVigente(String precio, String vigencia, String proximo) {
        this.precio = precio;
        this.vigencia = vigencia;
        this.proximo = proximo;
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
