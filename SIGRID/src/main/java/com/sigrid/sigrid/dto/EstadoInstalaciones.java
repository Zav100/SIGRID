package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO con la cantidad de instalaciones por estado (card "Estado de las instalaciones").
 */
public class EstadoInstalaciones implements Serializable {

    private final long habilitadas;
    private final long mantenimiento;
    private final long deshabilitadas;

    public EstadoInstalaciones(long habilitadas, long mantenimiento, long deshabilitadas) {
        this.habilitadas = habilitadas;
        this.mantenimiento = mantenimiento;
        this.deshabilitadas = deshabilitadas;
    }

    public long getHabilitadas() {
        return habilitadas;
    }

    public long getMantenimiento() {
        return mantenimiento;
    }

    public long getDeshabilitadas() {
        return deshabilitadas;
    }

    public long getTotal() {
        return habilitadas + mantenimiento + deshabilitadas;
    }
}
