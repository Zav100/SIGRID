package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.time.LocalDate;

/** Un día para elegir en el calendario del socio, con cuántas reservas propias tiene ese día. */
public class DiaSocio implements Serializable {

    private final LocalDate fecha;
    private final String dia;
    private final String numero;
    private final String mes;
    private final int reservas;
    private final boolean hoy;

    public DiaSocio(LocalDate fecha, String dia, String numero, String mes, int reservas, boolean hoy) {
        this.fecha = fecha;
        this.dia = dia;
        this.numero = numero;
        this.mes = mes;
        this.reservas = reservas;
        this.hoy = hoy;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    /** "2026-09-26": es lo que viaja en los enlaces de selección. */
    public String getIso() {
        return fecha.toString();
    }

    public String getDia() {
        return dia;
    }

    public String getNumero() {
        return numero;
    }

    public String getMes() {
        return mes;
    }

    public int getReservas() {
        return reservas;
    }

    public boolean isHoy() {
        return hoy;
    }
}
