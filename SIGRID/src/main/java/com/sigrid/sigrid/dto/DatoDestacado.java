package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO de un dato destacado de la sección "Información general" (ej. "100+" / "Socios activos").
 */
public class DatoDestacado implements Serializable {

    private String valor;
    private String etiqueta;
    private boolean estrella; // true = se muestra una estrella junto al valor (calificación)

    public DatoDestacado(String valor, String etiqueta, boolean estrella) {
        this.valor = valor;
        this.etiqueta = etiqueta;
        this.estrella = estrella;
    }

    public String getValor() {
        return valor;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean isEstrella() {
        return estrella;
    }
}
