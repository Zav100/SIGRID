package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Un aviso del resumen ejecutivo (membresías por vencer, instalación mal valorada, franjas vacías…). */
public class ReporteAlerta implements Serializable {

    private final String icono;
    private final String texto;
    private final String clase; // warn, off u ok

    public ReporteAlerta(String icono, String texto, String clase) {
        this.icono = icono;
        this.texto = texto;
        this.clase = clase;
    }

    public String getIcono() {
        return icono;
    }

    public String getTexto() {
        return texto;
    }

    public String getClase() {
        return clase;
    }
}
