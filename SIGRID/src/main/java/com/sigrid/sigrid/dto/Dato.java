package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Un dato "etiqueta: valor" de una ficha; con chipClase (ok, warn, off, muted) el valor se muestra como una etiqueta de color. */
public class Dato implements Serializable {

    private final String etiqueta;
    private final String valor;
    private final String chipClase; // "" si es texto normal

    public Dato(String etiqueta, String valor, String chipClase) {
        this.etiqueta = etiqueta;
        this.valor = valor;
        this.chipClase = chipClase;
    }

    public Dato(String etiqueta, String valor) {
        this(etiqueta, valor, "");
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getValor() {
        return valor;
    }

    public String getChipClase() {
        return chipClase;
    }

    public boolean isChip() {
        return !chipClase.isEmpty();
    }
}
