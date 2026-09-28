package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Un pedido del socio (cancelar o reprogramar) que el administrador debe aprobar o rechazar. */
public class CambioPedido implements Serializable {

    private final Integer idSolicitud;
    private final boolean reprogramacion; // false = cancelación
    private final boolean conCredito; // la reserva ya estaba cancelada: usa un crédito
    private final String socio;
    private final String email;
    private final String dni;
    private final String instalacion;
    private final String icono;
    private final String actual; // turno actual: "vie 27/09/2026 · 18:00 – 19:00"
    private final String nuevo; // turno pedido; null si es una cancelación
    private final String motivo; // lo que escribió el socio
    private final String monto; // lo que pagó; null si no hay pago
    private final String hace; // "hace 3 h"
    private final String anticipacion; // "Pedido con 5 días de anticipación"
    private final long minutosEspera;
    private final String busqueda;

    public CambioPedido(Integer idSolicitud, boolean reprogramacion, boolean conCredito, String socio, String email, String dni, String instalacion, String icono, String actual, String nuevo, String motivo, String monto, String hace, String anticipacion, long minutosEspera, String busqueda) {
        this.idSolicitud = idSolicitud;
        this.reprogramacion = reprogramacion;
        this.conCredito = conCredito;
        this.socio = socio;
        this.email = email;
        this.dni = dni;
        this.instalacion = instalacion;
        this.icono = icono;
        this.actual = actual;
        this.nuevo = nuevo;
        this.motivo = motivo;
        this.monto = monto;
        this.hace = hace;
        this.anticipacion = anticipacion;
        this.minutosEspera = minutosEspera;
        this.busqueda = busqueda;
    }

    public Integer getIdSolicitud() {
        return idSolicitud;
    }

    public boolean isReprogramacion() {
        return reprogramacion;
    }

    public boolean isConCredito() {
        return conCredito;
    }

    public String getSocio() {
        return socio;
    }

    public String getEmail() {
        return email;
    }

    public String getDni() {
        return dni;
    }

    public String getInstalacion() {
        return instalacion;
    }

    public String getIcono() {
        return icono;
    }

    public String getActual() {
        return actual;
    }

    public String getNuevo() {
        return nuevo;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getMonto() {
        return monto;
    }

    public String getHace() {
        return hace;
    }

    public String getAnticipacion() {
        return anticipacion;
    }

    public long getMinutosEspera() {
        return minutosEspera;
    }

    public String getBusqueda() {
        return busqueda;
    }
}
