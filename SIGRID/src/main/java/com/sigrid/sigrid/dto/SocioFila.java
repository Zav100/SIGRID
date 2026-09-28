package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** DTO de un socio en la tabla del panel del administrador; ya viene con los textos listos para mostrar. */
public class SocioFila implements Serializable {

    private final Integer idSocio;
    private final String nombre;
    private final String iniciales;
    private final String email;
    private final String dni;
    private final String categoria;   // "Alumno UNSE", "Docente UNSE", "No Docente UNSE" o "Externo"
    private final String legajo;      // null si no corresponde (externos)
    private final boolean vigente;    // membresía vigente (true) o expirada (false)
    private final boolean porVencer;  // vigente y vence dentro de los próximos días
    private final String vence;       // "27/09/2026"; null si nunca tuvo una membresía
    private final String alta;        // fecha en que se registró

    public SocioFila(Integer idSocio, String nombre, String iniciales, String email, String dni, String categoria,
            String legajo, boolean vigente, boolean porVencer, String vence, String alta) {
        this.idSocio = idSocio;
        this.nombre = nombre;
        this.iniciales = iniciales;
        this.email = email;
        this.dni = dni;
        this.categoria = categoria;
        this.legajo = legajo;
        this.vigente = vigente;
        this.porVencer = porVencer;
        this.vence = vence;
        this.alta = alta;
    }

    public Integer getIdSocio() {
        return idSocio;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIniciales() {
        return iniciales;
    }

    public String getEmail() {
        return email;
    }

    public String getDni() {
        return dni;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getLegajo() {
        return legajo;
    }

    public boolean isVigente() {
        return vigente;
    }

    public boolean isPorVencer() {
        return porVencer;
    }

    public String getVence() {
        return vence;
    }

    public String getAlta() {
        return alta;
    }
}
