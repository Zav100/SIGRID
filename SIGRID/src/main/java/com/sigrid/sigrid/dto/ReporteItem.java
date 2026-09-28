package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * Una fila de un ranking o lista de un reporte: nombre, valor, una barra y, si hace falta, una segunda parte
 * apilada (por ejemplo lo que aporta un tipo de socio por reservas y por membresías).
 */
public class ReporteItem implements Serializable {

    private final String etiqueta;
    private final String icono;
    private final String valor;    // ya formateado
    private final double numero;   // el mismo valor sin formato (para exportar)
    private final String detalle;
    private final int porcentaje;  // largo de la barra, 0 a 100
    private final int porcentaje2; // segunda parte apilada (0 si no hay)
    private final String tono;     // "", ok, warn u off: color de la barra

    public ReporteItem(String etiqueta, String icono, String valor, double numero, String detalle, int porcentaje,
            int porcentaje2, String tono) {
        this.etiqueta = etiqueta;
        this.icono = icono;
        this.valor = valor;
        this.numero = numero;
        this.detalle = detalle;
        this.porcentaje = Math.max(0, Math.min(100, porcentaje));
        this.porcentaje2 = Math.max(0, Math.min(100 - this.porcentaje, porcentaje2));
        this.tono = tono;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getIcono() {
        return icono;
    }

    public String getValor() {
        return valor;
    }

    public double getNumero() {
        return numero;
    }

    public String getDetalle() {
        return detalle;
    }

    public int getPorcentaje() {
        return porcentaje;
    }

    public int getPorcentaje2() {
        return porcentaje2;
    }

    public String getTono() {
        return tono;
    }
}
