package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Una instalación que el socio puede reservar, con el precio que le corresponde según su tipo de socio. */
public class InstalacionReservable implements Serializable {

    private final Integer idInstalacion;
    private final String nombre;
    private final String icono;
    private final String disciplina;
    private final String descripcion;
    private final String capacidad;
    private final String precio;
    private final boolean habilitada;
    private final String aviso;

    public InstalacionReservable(Integer idInstalacion, String nombre, String icono, String disciplina, String descripcion, String capacidad, String precio, boolean habilitada, String aviso) {
        this.idInstalacion = idInstalacion;
        this.nombre = nombre;
        this.icono = icono;
        this.disciplina = disciplina;
        this.descripcion = descripcion;
        this.capacidad = capacidad;
        this.precio = precio;
        this.habilitada = habilitada;
        this.aviso = aviso;
    }

    public Integer getIdInstalacion() {
        return idInstalacion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIcono() {
        return icono;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getCapacidad() {
        return capacidad;
    }

    public String getPrecio() {
        return precio;
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    public String getAviso() {
        return aviso;
    }
}
