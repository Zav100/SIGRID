package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO de una reserva tal como la muestran las cards del dashboard del administrador (agenda del día,
 * solicitudes por confirmar, próximas reservas y tarjetas del plano). Ya viene con los textos listos para mostrar.
 */
public class ReservaFila implements Serializable {

    private final Integer idReserva;
    private final String cuando;        // "Hoy", "Mañana" o "vie 27/09"
    private final String horario;       // "18:00 – 19:00"
    private final String instalacion;
    private final String disciplina;
    private final String icono;         // nombre del ícono (Material Symbols) según la disciplina
    private final String socio;
    private final String estadoTexto;   // "Confirmada", "Pendiente", "Cancelada"
    private final String estadoClase;   // "ok", "warn" u "off": lo usa el CSS para el color de la etiqueta
    private final String monto;         // "$ 18.000": lo transferido, para cotejarlo con la cuenta; null si no hay pago cargado

    public ReservaFila(Integer idReserva, String cuando, String horario, String instalacion, String disciplina,
            String icono, String socio, String estadoTexto, String estadoClase, String monto) {
        this.idReserva = idReserva;
        this.cuando = cuando;
        this.horario = horario;
        this.instalacion = instalacion;
        this.disciplina = disciplina;
        this.icono = icono;
        this.socio = socio;
        this.estadoTexto = estadoTexto;
        this.estadoClase = estadoClase;
        this.monto = monto;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public String getCuando() {
        return cuando;
    }

    public String getHorario() {
        return horario;
    }

    public String getInstalacion() {
        return instalacion;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public String getIcono() {
        return icono;
    }

    public String getSocio() {
        return socio;
    }

    public String getEstadoTexto() {
        return estadoTexto;
    }

    public String getEstadoClase() {
        return estadoClase;
    }

    public String getMonto() {
        return monto;
    }
}
