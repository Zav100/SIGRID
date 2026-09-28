package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO de un link interno (fila legal del footer, etc.). Se usa con
 * h:link outcome="#{enlace.outcome}" fragment="#{enlace.fragmento}".
 */
public class Enlace implements Serializable {

    private String texto;      // Texto visible del link
    private String outcome;    // Página interna a la que apunta
    private String fragmento;  // Ancla dentro de la página (sin "#"); null si no hay

    public Enlace(String texto, String outcome) {
        this(texto, outcome, null);
    }

    public Enlace(String texto, String outcome, String fragmento) {
        this.texto = texto;
        this.outcome = outcome;
        this.fragmento = fragmento;
    }

    public String getTexto() {
        return texto;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getFragmento() {
        return fragmento;
    }
}
