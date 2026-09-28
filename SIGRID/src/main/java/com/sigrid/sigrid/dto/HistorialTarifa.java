package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** DTO de un cambio de precios en el historial: desde cuándo rige y qué tipos de socio cambiaron ("Alumno UNSE $ 12.000 · Externo $ 30.000"). */
public class HistorialTarifa implements Serializable {

    private final String cuando;      // "Desde el 01/10/2026"
    private final String cambios;
    private final boolean programado; // todavía no rige

    public HistorialTarifa(String cuando, String cambios, boolean programado) {
        this.cuando = cuando;
        this.cambios = cambios;
        this.programado = programado;
    }

    public String getCuando() {
        return cuando;
    }

    public String getCambios() {
        return cambios;
    }

    public boolean isProgramado() {
        return programado;
    }
}
