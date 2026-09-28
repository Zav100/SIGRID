package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Datos del formulario de alta pública (register.xhtml), todo texto tal como llega del formulario. */
public class RegistroSocio implements Serializable {

    // Paso 1
    private String email;
    private String password;
    private String confirmarPassword;

    // Paso 2
    private String nombre;
    private String apellido;
    private String dni;
    private String fechaNacimiento; // yyyy-MM-dd
    private String telefono;
    private String tipoSocio; // "EXTERNO" | "INTERNO"
    private String categoriaInterna; // "ALUMNO" | "DOCENTE" | "NO_DOCENTE" (solo si tipoSocio = INTERNO)
    private String legajo;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmarPassword() {
        return confirmarPassword;
    }

    public void setConfirmarPassword(String confirmarPassword) {
        this.confirmarPassword = confirmarPassword;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getTipoSocio() {
        return tipoSocio;
    }

    public void setTipoSocio(String tipoSocio) {
        this.tipoSocio = tipoSocio;
    }

    public String getCategoriaInterna() {
        return categoriaInterna;
    }

    public void setCategoriaInterna(String categoriaInterna) {
        this.categoriaInterna = categoriaInterna;
    }

    public String getLegajo() {
        return legajo;
    }

    public void setLegajo(String legajo) {
        this.legajo = legajo;
    }
}
