package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/** Una barra apilada del gráfico de evolución de ingresos: lo cobrado por reservas y por membresías en un día, semana o mes. */
public class ReporteBarra implements Serializable {

    private final String etiqueta;
    private final boolean mostrarEtiqueta; // no se rotulan todas, para que el eje no se amontone
    private final BigDecimal reservas;
    private final BigDecimal membresias;
    private final int altoReservas;    // porcentaje del alto del gráfico
    private final int altoMembresias;
    private final String titulo;       // texto al pasar el mouse

    public ReporteBarra(String etiqueta, boolean mostrarEtiqueta, BigDecimal reservas, BigDecimal membresias,
            int altoReservas, int altoMembresias, String titulo) {
        this.etiqueta = etiqueta;
        this.mostrarEtiqueta = mostrarEtiqueta;
        this.reservas = reservas;
        this.membresias = membresias;
        this.altoReservas = altoReservas;
        this.altoMembresias = altoMembresias;
        this.titulo = titulo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean isMostrarEtiqueta() {
        return mostrarEtiqueta;
    }

    public BigDecimal getReservas() {
        return reservas;
    }

    public BigDecimal getMembresias() {
        return membresias;
    }

    public int getAltoReservas() {
        return altoReservas;
    }

    public int getAltoMembresias() {
        return altoMembresias;
    }

    public String getTitulo() {
        return titulo;
    }
}
