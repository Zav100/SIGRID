package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO de una celda del calendario de reservas: el día y cuántas reservas tiene en cada estado
 * (de ahí salen los puntitos de colores).
 */
public class DiaCalendario implements Serializable {

    private final LocalDate fecha;
    private final boolean delMes;       // false = día de relleno del mes anterior o siguiente
    private final boolean hoy;
    private final boolean seleccionado;
    private final int confirmadas;
    private final int pendientes;
    private final int canceladas;
    private final String etiqueta;      // texto para el tooltip y los lectores de pantalla

    public DiaCalendario(LocalDate fecha, boolean delMes, boolean hoy, boolean seleccionado,
            int confirmadas, int pendientes, int canceladas, String etiqueta) {
        this.fecha = fecha;
        this.delMes = delMes;
        this.hoy = hoy;
        this.seleccionado = seleccionado;
        this.confirmadas = confirmadas;
        this.pendientes = pendientes;
        this.canceladas = canceladas;
        this.etiqueta = etiqueta;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public int getNumero() {
        return fecha.getDayOfMonth();
    }

    public boolean isDelMes() {
        return delMes;
    }

    public boolean isHoy() {
        return hoy;
    }

    public boolean isSeleccionado() {
        return seleccionado;
    }

    public int getConfirmadas() {
        return confirmadas;
    }

    public int getPendientes() {
        return pendientes;
    }

    public int getCanceladas() {
        return canceladas;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
