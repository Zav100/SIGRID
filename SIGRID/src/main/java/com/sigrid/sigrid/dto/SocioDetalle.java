package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/** La ficha completa de un socio (solo lectura) para el panel del administrador; ya viene con los textos listos. */
public class SocioDetalle implements Serializable {

    private final String iniciales;
    private final String nombre;
    private final String categoria;
    private final String numero;       // "#00010", como en el carnet
    private final String socioDesde;   // "09/2026", para el carnet
    private final boolean vigente;     // membresía vigente; si no, el carnet se ve congelado
    private final List<Dato> personales;
    private final List<Dato> membresia;
    private final List<Dato> historial; // últimas membresías: periodo → estado y monto
    private final List<Dato> actividad;

    public SocioDetalle(String iniciales, String nombre, String categoria, String numero, String socioDesde,
            boolean vigente, List<Dato> personales, List<Dato> membresia, List<Dato> historial, List<Dato> actividad) {
        this.iniciales = iniciales;
        this.nombre = nombre;
        this.categoria = categoria;
        this.numero = numero;
        this.socioDesde = socioDesde;
        this.vigente = vigente;
        this.personales = personales;
        this.membresia = membresia;
        this.historial = historial;
        this.actividad = actividad;
    }

    public String getIniciales() {
        return iniciales;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getNumero() {
        return numero;
    }

    public String getSocioDesde() {
        return socioDesde;
    }

    public boolean isVigente() {
        return vigente;
    }

    public List<Dato> getPersonales() {
        return personales;
    }

    public List<Dato> getMembresia() {
        return membresia;
    }

    public List<Dato> getHistorial() {
        return historial;
    }

    public List<Dato> getActividad() {
        return actividad;
    }
}
