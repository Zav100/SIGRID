package com.sigrid.sigrid.dto;

import com.sigrid.sigrid.repositorio.Reserva;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de una reserva tal como la muestra el calendario del administrador (celdas, detalle del día e historial).
 * Ya viene con los textos listos para mostrar; "pasada" es la que ya terminó (o el día ya quedó atrás).
 */
public class ReservaCalendario implements Serializable {

    private final Integer idReserva;
    private final LocalDate fecha;
    private final LocalTime horaInicio;   // para ordenar
    private final String cuando;          // "Hoy", "Mañana" o "vie 27/09"
    private final String desde;           // "18:00": lo que se ve en la celda del calendario
    private final String horario;         // "18:00 – 19:00"
    private final String instalacion;
    private final String instalacionCorta; // sin "Cancha de ": entra en la celda del calendario
    private final String disciplina;
    private final String icono;           // ícono (Material Symbols) según la disciplina
    private final Reserva.Estado estado;
    private final String estadoTexto;     // "Confirmada", "Completada", "Pendiente", "Cancelada", "Rechazada"
    private final String estadoClase;     // "ok", "warn", "off", o "muted" si ya pasó (gris)
    private final boolean pasada;
    private final String monto;           // "$ 18.000"; null si no hay pago cargado
    private final String socio;
    private final String nota;            // aclaración: "Comprobante en revisión", etc.

    public ReservaCalendario(Integer idReserva, LocalDate fecha, LocalTime horaInicio, String cuando, String desde,
            String horario, String instalacion, String instalacionCorta, String disciplina, String icono, Reserva.Estado estado,
            String estadoTexto, String estadoClase, boolean pasada, String monto, String socio, String nota) {
        this.idReserva = idReserva;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.cuando = cuando;
        this.desde = desde;
        this.horario = horario;
        this.instalacion = instalacion;
        this.instalacionCorta = instalacionCorta;
        this.disciplina = disciplina;
        this.icono = icono;
        this.estado = estado;
        this.estadoTexto = estadoTexto;
        this.estadoClase = estadoClase;
        this.pasada = pasada;
        this.monto = monto;
        this.socio = socio;
        this.nota = nota;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public String getCuando() {
        return cuando;
    }

    public String getDesde() {
        return desde;
    }

    public String getHorario() {
        return horario;
    }

    public String getInstalacion() {
        return instalacion;
    }

    public String getInstalacionCorta() {
        return instalacionCorta;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public String getIcono() {
        return icono;
    }

    public Reserva.Estado getEstado() {
        return estado;
    }

    public String getEstadoTexto() {
        return estadoTexto;
    }

    public String getEstadoClase() {
        return estadoClase;
    }

    public boolean isPasada() {
        return pasada;
    }

    public String getMonto() {
        return monto;
    }

    public String getSocio() {
        return socio;
    }

    public String getNota() {
        return nota;
    }
}
