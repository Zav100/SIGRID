package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Un aviso para el socio, armado a partir de su membresía y sus reservas (no hay tabla de avisos). clase: ok, warn, off o muted. */
public class Aviso implements Serializable {

    private final String icono;
    private final String titulo;
    private final String texto;
    private final String clase;
    private final String cuando;
    private final int orden;

    public Aviso(String icono, String titulo, String texto, String clase, String cuando, int orden) {
        this.icono = icono;
        this.titulo = titulo;
        this.texto = texto;
        this.clase = clase;
        this.cuando = cuando;
        this.orden = orden;
    }

    public String getIcono() {
        return icono;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getTexto() {
        return texto;
    }

    public String getClase() {
        return clase;
    }

    public String getCuando() {
        return cuando;
    }

    public int getOrden() {
        return orden;
    }
}
