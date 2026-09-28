package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Datos del socio logueado para su panel: cabecera, carnet y membresía. Los textos vienen listos para mostrar; diasRestantes es negativo si la membresía ya venció (y 0 si nunca tuvo una). */
public class PerfilSocio implements Serializable {

    private final Integer idSocio;
    private final String nombre;
    private final String nombrePila;
    private final String iniciales;
    private final String categoria;
    private final String numero;
    private final String desde;
    private final boolean vigente;
    private final long diasRestantes;
    private final String vence;
    private final int progreso;

    public PerfilSocio(Integer idSocio, String nombre, String nombrePila, String iniciales, String categoria, String numero, String desde, boolean vigente, long diasRestantes, String vence, int progreso) {
        this.idSocio = idSocio;
        this.nombre = nombre;
        this.nombrePila = nombrePila;
        this.iniciales = iniciales;
        this.categoria = categoria;
        this.numero = numero;
        this.desde = desde;
        this.vigente = vigente;
        this.diasRestantes = diasRestantes;
        this.vence = vence;
        this.progreso = progreso;
    }

    public Integer getIdSocio() {
        return idSocio;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNombrePila() {
        return nombrePila;
    }

    public String getIniciales() {
        return iniciales;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getNumero() {
        return numero;
    }

    public String getDesde() {
        return desde;
    }

    public boolean isVigente() {
        return vigente;
    }

    public long getDiasRestantes() {
        return diasRestantes;
    }

    public String getVence() {
        return vence;
    }

    public int getProgreso() {
        return progreso;
    }
}
