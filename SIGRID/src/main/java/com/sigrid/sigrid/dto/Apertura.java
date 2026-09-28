package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** DTO de si una instalación de acceso libre (la pileta) está abierta y cómo decirlo: "Abierta hasta 19:00", "Cerrada · abre mañana 09:00". */
public class Apertura implements Serializable {

    private final boolean abierta;
    private final String texto;

    public Apertura(boolean abierta, String texto) {
        this.abierta = abierta;
        this.texto = texto;
    }

    public boolean isAbierta() {
        return abierta;
    }

    public String getTexto() {
        return texto;
    }
}
