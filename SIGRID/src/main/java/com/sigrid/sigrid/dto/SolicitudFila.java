package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una solicitud de reserva por confirmar, con lo que el administrador necesita para cotejar el comprobante
 * contra el ingreso en la cuenta: quién es, qué turno pidió, cuánto y cuándo transfirió, y hace cuánto espera.
 */
public class SolicitudFila implements Serializable {

    private final Integer idReserva;
    private final LocalDateTime inicioTurno;
    private final String cuando;
    private final String horario;
    private final String instalacion;
    private final String disciplina;
    private final String icono;
    private final String socio;
    private final String email;
    private final String dni;
    private final String categoria;
    private final String monto;
    private final BigDecimal importe;
    private final String operacion;       // cuándo dijo el socio que hizo la transferencia; null si no lo declaró
    private final LocalDateTime enviada;  // cuándo cargó el comprobante
    private final long minutosEspera;
    private final String espera;          // "hace 3 h"
    private final boolean urgente;        // el turno es hoy o mañana
    private final boolean vencida;        // el turno ya pasó
    private final String busqueda;

    public SolicitudFila(Integer idReserva, LocalDateTime inicioTurno, String cuando, String horario, String instalacion,
            String disciplina, String icono, String socio, String email, String dni, String categoria, String monto,
            BigDecimal importe, String operacion, LocalDateTime enviada, long minutosEspera, String espera,
            boolean urgente, boolean vencida, String busqueda) {
        this.idReserva = idReserva;
        this.inicioTurno = inicioTurno;
        this.cuando = cuando;
        this.horario = horario;
        this.instalacion = instalacion;
        this.disciplina = disciplina;
        this.icono = icono;
        this.socio = socio;
        this.email = email;
        this.dni = dni;
        this.categoria = categoria;
        this.monto = monto;
        this.importe = importe;
        this.operacion = operacion;
        this.enviada = enviada;
        this.minutosEspera = minutosEspera;
        this.espera = espera;
        this.urgente = urgente;
        this.vencida = vencida;
        this.busqueda = busqueda;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public LocalDateTime getInicioTurno() {
        return inicioTurno;
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

    public String getEmail() {
        return email;
    }

    public String getDni() {
        return dni;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getMonto() {
        return monto;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public String getOperacion() {
        return operacion;
    }

    public LocalDateTime getEnviada() {
        return enviada;
    }

    public long getMinutosEspera() {
        return minutosEspera;
    }

    public String getEspera() {
        return espera;
    }

    public boolean isUrgente() {
        return urgente;
    }

    public boolean isVencida() {
        return vencida;
    }

    public String getBusqueda() {
        return busqueda;
    }
}
