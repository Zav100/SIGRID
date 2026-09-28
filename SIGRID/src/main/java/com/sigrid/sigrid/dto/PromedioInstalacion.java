package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Cómo viene valorada una instalación: promedio, cantidad y si sube o baja respecto de los 30 días anteriores. */
public class PromedioInstalacion implements Serializable {

    private final String nombre;
    private final String icono;
    private final String disciplina;
    private final int cantidad;
    private final double promedio;     // 0 si todavía no tiene valoraciones
    private final String promedioTexto; // "4,3"; "—" si no tiene
    private final String tendencia;    // "+0,4 en 30 días"; "" si no hay con qué comparar
    private final String tendenciaClase; // ok, off o muted

    public PromedioInstalacion(String nombre, String icono, String disciplina, int cantidad, double promedio,
            String promedioTexto, String tendencia, String tendenciaClase) {
        this.nombre = nombre;
        this.icono = icono;
        this.disciplina = disciplina;
        this.cantidad = cantidad;
        this.promedio = promedio;
        this.promedioTexto = promedioTexto;
        this.tendencia = tendencia;
        this.tendenciaClase = tendenciaClase;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIcono() {
        return icono;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPromedio() {
        return promedio;
    }

    public String getPromedioTexto() {
        return promedioTexto;
    }

    public String getTendencia() {
        return tendencia;
    }

    public String getTendenciaClase() {
        return tendenciaClase;
    }

    public boolean isConDatos() {
        return cantidad > 0;
    }

    /** Largo de la barra (0 a 100): el promedio sobre 5. */
    public int getPorcentaje() {
        return (int) Math.round(promedio * 20);
    }

    /** Para pintar la barra y el puntaje: alto (4 o más), medio (3,5 a 4), bajo (menos de 3,5) o sin (sin datos). */
    public String getNivel() {
        return cantidad == 0 ? "sin" : promedio >= 4 ? "alto" : promedio >= 3.5 ? "medio" : "bajo";
    }

    /** Promedio por debajo de 3,5: la instalación que hay que mirar primero. */
    public boolean isParaRevisar() {
        return cantidad > 0 && promedio < 3.5;
    }
}
