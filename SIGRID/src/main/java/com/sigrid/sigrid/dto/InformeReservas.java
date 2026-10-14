package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Lo que se va a exportar a PDF (CC-06): las reservas del período y los filtros elegidos, con lo que lleva el
 * encabezado de la hoja (período, filtros y fecha de emisión). Si no se puede armar (falta el período, no hay datos o
 * son demasiadas), no hay filas y {@link #getAviso()} dice por qué: nunca se imprime un documento vacío.
 */
public class InformeReservas implements Serializable {

    private final List<ReservaListado> filas;
    private final long total;
    private final String periodo;   // "14/10/2026" o "01/10/2026 al 31/10/2026"
    private final String filtros;   // "Estado: Confirmadas · Instalación: Fútbol"; vacío si no hay
    private final String emitido;   // "12/10/2026 10:00"
    private final String aviso;     // null si el informe está listo

    public InformeReservas(List<ReservaListado> filas, long total, String periodo, String filtros, String emitido,
            String aviso) {
        this.filas = filas;
        this.total = total;
        this.periodo = periodo;
        this.filtros = filtros;
        this.emitido = emitido;
        this.aviso = aviso;
    }

    public List<ReservaListado> getFilas() {
        return filas;
    }

    public long getTotal() {
        return total;
    }

    public String getPeriodo() {
        return periodo;
    }

    public String getFiltros() {
        return filtros;
    }

    public String getEmitido() {
        return emitido;
    }

    public String getAviso() {
        return aviso;
    }

    /** ¿Se puede imprimir? */
    public boolean isListo() {
        return aviso == null && !filas.isEmpty();
    }
}
