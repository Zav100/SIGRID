package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/** DTO de la tarjeta de ingresos: lo cobrado en el mes, cómo viene contra el anterior y los últimos meses en barras. */
public class IngresosMembresias implements Serializable {

    /** Una barra del gráfico. */
    public static class Mes implements Serializable {

        private final String etiqueta;   // "sep"
        private final String monto;      // "$ 12.000"
        private final int porcentaje;    // alto de la barra (0 a 100)
        private final boolean actual;

        public Mes(String etiqueta, String monto, int porcentaje, boolean actual) {
            this.etiqueta = etiqueta;
            this.monto = monto;
            this.porcentaje = porcentaje;
            this.actual = actual;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public String getMonto() {
            return monto;
        }

        public int getPorcentaje() {
            return porcentaje;
        }

        public boolean isActual() {
            return actual;
        }
    }

    /** Lo cobrado en el mes por un tipo de socio (un tramo de la barra). */
    public static class Tipo implements Serializable {

        private final String nombre;
        private final String monto;
        private final int porcentaje;
        private final int indice;    // 0 a 3: el color del tramo en el CSS

        public Tipo(String nombre, String monto, int porcentaje, int indice) {
            this.nombre = nombre;
            this.monto = monto;
            this.porcentaje = porcentaje;
            this.indice = indice;
        }

        public String getNombre() {
            return nombre;
        }

        public String getMonto() {
            return monto;
        }

        public int getPorcentaje() {
            return porcentaje;
        }

        public int getIndice() {
            return indice;
        }
    }

    private final String totalMes;     // "$ 12.000"
    private final String comparacion;  // "+12 % respecto de agosto"
    private final String tendencia;    // "up", "down" o "flat"
    private final List<Mes> meses;
    private final List<Tipo> porTipo;  // lo cobrado este mes, por tipo de socio

    public IngresosMembresias(String totalMes, String comparacion, String tendencia, List<Mes> meses, List<Tipo> porTipo) {
        this.totalMes = totalMes;
        this.comparacion = comparacion;
        this.tendencia = tendencia;
        this.meses = meses;
        this.porTipo = porTipo;
    }

    public String getTotalMes() {
        return totalMes;
    }

    public String getComparacion() {
        return comparacion;
    }

    public String getTendencia() {
        return tendencia;
    }

    /** ¿Se cobró algo este mes? (si no, no hay nada que repartir) */
    public boolean isConIngresos() {
        return porTipo.stream().anyMatch(t -> t.getPorcentaje() > 0);
    }

    public List<Tipo> getPorTipo() {
        return porTipo;
    }

    public List<Mes> getMeses() {
        return meses;
    }
}
