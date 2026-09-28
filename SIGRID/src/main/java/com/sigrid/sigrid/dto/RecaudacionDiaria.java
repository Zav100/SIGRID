package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/** DTO de la card de recaudación de reservas: lo cobrado hoy, cómo viene contra ayer y los últimos días en barras. */
public class RecaudacionDiaria implements Serializable {

    private final String totalHoy;      // "$ 36.000"
    private final int cantidadHoy;      // reservas confirmadas hoy
    private final String comparacion;   // "+20 % respecto de ayer"
    private final String tendencia;     // "up", "down" o "flat"
    private final List<IngresosMembresias.Mes> dias; // la barra "actual" es hoy

    public RecaudacionDiaria(String totalHoy, int cantidadHoy, String comparacion, String tendencia,
            List<IngresosMembresias.Mes> dias) {
        this.totalHoy = totalHoy;
        this.cantidadHoy = cantidadHoy;
        this.comparacion = comparacion;
        this.tendencia = tendencia;
        this.dias = dias;
    }

    public String getTotalHoy() {
        return totalHoy;
    }

    public int getCantidadHoy() {
        return cantidadHoy;
    }

    public String getComparacion() {
        return comparacion;
    }

    public String getTendencia() {
        return tendencia;
    }

    public List<IngresosMembresias.Mes> getDias() {
        return dias;
    }
}
