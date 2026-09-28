package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/** DTO de lo que pasó con una instalación en un período: reservas activas (confirmadas y por confirmar) y lo cobrado por ellas. */
public class Demanda implements Serializable {

    public static final Demanda VACIA = new Demanda(0, 0, BigDecimal.ZERO);

    private final long reservas;
    private final long confirmadas;
    private final BigDecimal ingresos;

    public Demanda(long reservas, long confirmadas, BigDecimal ingresos) {
        this.reservas = reservas;
        this.confirmadas = confirmadas;
        this.ingresos = ingresos;
    }

    public long getReservas() {
        return reservas;
    }

    public long getConfirmadas() {
        return confirmadas;
    }

    public BigDecimal getIngresos() {
        return ingresos;
    }
}
