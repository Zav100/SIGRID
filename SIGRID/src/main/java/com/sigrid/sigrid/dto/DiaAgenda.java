package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DTO de una celda del calendario del socio: el día y sus reservas. En la celda caben pocas, el resto
 * se resume en "N más" (se ven todas al elegir el día).
 */
public class DiaAgenda implements Serializable {

    private static final int EN_CELDA = 2;

    private final LocalDate fecha;
    private final boolean delMes;       // false = día de relleno del mes anterior o siguiente
    private final boolean hoy;
    private final boolean pasado;
    private final boolean seleccionado;
    private final List<ReservaCalendario> reservas;
    private final String etiqueta;      // texto para el tooltip y los lectores de pantalla

    public DiaAgenda(LocalDate fecha, boolean delMes, boolean hoy, boolean pasado, boolean seleccionado,
            List<ReservaCalendario> reservas, String etiqueta) {
        this.fecha = fecha;
        this.delMes = delMes;
        this.hoy = hoy;
        this.pasado = pasado;
        this.seleccionado = seleccionado;
        this.reservas = reservas;
        this.etiqueta = etiqueta;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public int getNumero() {
        return fecha.getDayOfMonth();
    }

    public boolean isDelMes() {
        return delMes;
    }

    public boolean isHoy() {
        return hoy;
    }

    public boolean isPasado() {
        return pasado;
    }

    public boolean isSeleccionado() {
        return seleccionado;
    }

    public List<ReservaCalendario> getReservas() {
        return reservas;
    }

    public List<ReservaCalendario> getVisibles() {
        return reservas.subList(0, Math.min(EN_CELDA, reservas.size()));
    }

    public int getOcultas() {
        return reservas.size() - getVisibles().size();
    }

    /** Clases de color de los puntitos (en el celular, donde no entran las etiquetas): una por cada tipo de reserva del día. */
    public List<String> getPuntos() {
        Set<String> clases = new LinkedHashSet<>();
        for (ReservaCalendario r : reservas) {
            clases.add(r.getEstadoClase());
        }
        return new ArrayList<>(clases);
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
