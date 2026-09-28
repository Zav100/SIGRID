package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Una valoración de una reserva finalizada, lista para mostrar en el panel del administrador (solo lectura). */
public class ValoracionFila implements Serializable {

    private final LocalDateTime fecha;
    private final String fechaTexto;
    private final String socio;
    private final String categoria;
    private final String instalacion;
    private final String disciplina;
    private final String icono;
    private final String turno;
    private final int puntaje;
    private final String comentario; // "" si el socio no escribió nada

    public ValoracionFila(LocalDateTime fecha, String fechaTexto, String socio, String categoria, String instalacion,
            String disciplina, String icono, String turno, int puntaje, String comentario) {
        this.fecha = fecha;
        this.fechaTexto = fechaTexto;
        this.socio = socio;
        this.categoria = categoria;
        this.instalacion = instalacion;
        this.disciplina = disciplina;
        this.icono = icono;
        this.turno = turno;
        this.puntaje = puntaje;
        this.comentario = comentario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getFechaTexto() {
        return fechaTexto;
    }

    public String getSocio() {
        return socio;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getInstalacion() {
        return instalacion;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public String getIcono() {
        return icono;
    }

    public String getTurno() {
        return turno;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public boolean isConComentario() {
        return !comentario.isEmpty();
    }

    /** 1 o 2 estrellas: lo que conviene revisar primero. */
    public boolean isBaja() {
        return puntaje <= 2;
    }
}
