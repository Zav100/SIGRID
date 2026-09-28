package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/** Mapa de calor de la ocupación: una fila por hora de inicio y una celda por día de la semana. */
public class ReporteCalor implements Serializable {

    public static class Celda implements Serializable {

        private final String texto;     // "45 %"; "—" si ese día y hora no hay turnos
        private final int intensidad;   // 0 a 100
        private final boolean sinTurnos;
        private final String titulo;

        public Celda(String texto, int intensidad, boolean sinTurnos, String titulo) {
            this.texto = texto;
            this.intensidad = intensidad;
            this.sinTurnos = sinTurnos;
            this.titulo = titulo;
        }

        public String getTexto() {
            return texto;
        }

        public int getIntensidad() {
            return intensidad;
        }

        public boolean isSinTurnos() {
            return sinTurnos;
        }

        public String getTitulo() {
            return titulo;
        }

        /** Nivel 0 a 5 para elegir el tono en CSS. */
        public int getNivel() {
            return sinTurnos ? 0 : Math.min(5, 1 + intensidad / 20);
        }
    }

    public static class Fila implements Serializable {

        private final String hora;
        private final List<Celda> celdas;

        public Fila(String hora, List<Celda> celdas) {
            this.hora = hora;
            this.celdas = celdas;
        }

        public String getHora() {
            return hora;
        }

        public List<Celda> getCeldas() {
            return celdas;
        }
    }

    private final List<String> dias;
    private final List<Fila> filas;

    public ReporteCalor(List<String> dias, List<Fila> filas) {
        this.dias = dias;
        this.filas = filas;
    }

    public List<String> getDias() {
        return dias;
    }

    public List<Fila> getFilas() {
        return filas;
    }
}
