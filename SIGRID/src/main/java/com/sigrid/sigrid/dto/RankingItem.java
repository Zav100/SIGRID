package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Una barra de un ranking (instalaciones más reservadas, horas más pedidas): cuánto pesa frente al primero. */
public class RankingItem implements Serializable {

    private final String etiqueta;
    private final String icono;
    private final long cantidad;
    private final int porcentaje; // 0 a 100, relativo al primero del ranking (dibuja el largo de la barra)

    public RankingItem(String etiqueta, String icono, long cantidad, int porcentaje) {
        this.etiqueta = etiqueta;
        this.icono = icono;
        this.cantidad = cantidad;
        this.porcentaje = porcentaje;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getIcono() {
        return icono;
    }

    public long getCantidad() {
        return cantidad;
    }

    public int getPorcentaje() {
        return porcentaje;
    }
}
