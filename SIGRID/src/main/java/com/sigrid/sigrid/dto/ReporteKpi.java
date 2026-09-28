package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Una cifra de un reporte con su variación respecto del período anterior. */
public class ReporteKpi implements Serializable {

    private final String etiqueta;
    private final String icono;
    private final String valor;          // ya formateado: "$ 120.000", "68 %"
    private final double numero;         // el mismo valor sin formato (para exportar)
    private final String sub;            // aclaración debajo de la cifra
    private final String variacion;      // "+12 % vs. período anterior"; "" si no hay con qué comparar
    private final String variacionClase; // ok (mejora), off (empeora) o muted
    private final boolean alerta;        // pinta el ícono como alerta

    public ReporteKpi(String etiqueta, String icono, String valor, double numero, String sub, String variacion,
            String variacionClase, boolean alerta) {
        this.etiqueta = etiqueta;
        this.icono = icono;
        this.valor = valor;
        this.numero = numero;
        this.sub = sub;
        this.variacion = variacion;
        this.variacionClase = variacionClase;
        this.alerta = alerta;
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

    public String getSub() {
        return sub;
    }

    public String getVariacion() {
        return variacion;
    }

    public String getVariacionClase() {
        return variacionClase;
    }

    public boolean isAlerta() {
        return alerta;
    }

    public boolean isConVariacion() {
        return !variacion.isEmpty();
    }
}
