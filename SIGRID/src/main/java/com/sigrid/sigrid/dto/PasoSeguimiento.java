package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Un paso del seguimiento de una reserva. estado: done (hecho), current (en curso), todo (falta) u off (no se dio). */
public class PasoSeguimiento implements Serializable {

    private final String titulo;
    private final String detalle;
    private final String estado;

    public PasoSeguimiento(String titulo, String detalle, String estado) {
        this.titulo = titulo;
        this.detalle = detalle;
        this.estado = estado;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getEstado() {
        return estado;
    }
}
