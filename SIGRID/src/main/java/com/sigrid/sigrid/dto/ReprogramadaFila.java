package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Una reprogramación ya hecha, leída del historial de la reserva. */
public class ReprogramadaFila implements Serializable {

    private final Integer idHistorial;
    private final String socio;
    private final String email;
    private final String instalacion;
    private final String icono;
    private final String de; // turno anterior
    private final String a; // turno actual
    private final String motivo;
    private final String cuando; // cuándo se hizo
    private final String busqueda;

    public ReprogramadaFila(Integer idHistorial, String socio, String email, String instalacion, String icono, String de, String a, String motivo, String cuando, String busqueda) {
        this.idHistorial = idHistorial;
        this.socio = socio;
        this.email = email;
        this.instalacion = instalacion;
        this.icono = icono;
        this.de = de;
        this.a = a;
        this.motivo = motivo;
        this.cuando = cuando;
        this.busqueda = busqueda;
    }

    public Integer getIdHistorial() {
        return idHistorial;
    }

    public String getSocio() {
        return socio;
    }

    public String getEmail() {
        return email;
    }

    public String getInstalacion() {
        return instalacion;
    }

    public String getIcono() {
        return icono;
    }

    public String getDe() {
        return de;
    }

    public String getA() {
        return a;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getCuando() {
        return cuando;
    }

    public String getBusqueda() {
        return busqueda;
    }
}
