package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una reserva tal como la muestra el listado de reservas del administrador. Los textos vienen listos para mostrar;
 * "estado" es la clave que usan los filtros (POR_CONFIRMAR, ESPERANDO_PAGO, CONFIRMADA, CANCELADA o RECHAZADA).
 */
public class ReservaListado implements Serializable {

    private final Integer idReserva;
    private final LocalDate fecha;
    private final String cuando;        // "Hoy", "Mañana" o "vie 27/09"
    private final String horario;       // "18:00 – 19:00"
    private final int horaInicio;       // 18: agrupa las horas más pedidas
    private final boolean finalizada;   // el turno ya terminó
    private final String instalacion;
    private final String disciplina;
    private final String icono;
    private final String socio;
    private final String email;
    private final String estado;
    private final String estadoTexto;
    private final String estadoClase;   // "ok", "warn", "off" o "muted"
    private final String monto;
    private final String reservadaEl;   // "12/09 14:30"
    private final String busqueda;      // texto ya normalizado sobre el que busca el buscador
    private final LocalDate confirmadaEl; // día en que el administrador la confirmó; null si no está confirmada
    private final BigDecimal cobrado;     // lo cobrado al confirmarla; null si no está confirmada o no tiene pago

    public ReservaListado(Integer idReserva, LocalDate fecha, String cuando, String horario, int horaInicio,
            boolean finalizada, String instalacion, String disciplina, String icono, String socio, String email,
            String estado, String estadoTexto, String estadoClase, String monto, String reservadaEl, String busqueda,
            LocalDate confirmadaEl, BigDecimal cobrado) {
        this.idReserva = idReserva;
        this.fecha = fecha;
        this.cuando = cuando;
        this.horario = horario;
        this.horaInicio = horaInicio;
        this.finalizada = finalizada;
        this.instalacion = instalacion;
        this.disciplina = disciplina;
        this.icono = icono;
        this.socio = socio;
        this.email = email;
        this.estado = estado;
        this.estadoTexto = estadoTexto;
        this.estadoClase = estadoClase;
        this.monto = monto;
        this.reservadaEl = reservadaEl;
        this.busqueda = busqueda;
        this.confirmadaEl = confirmadaEl;
        this.cobrado = cobrado;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getCuando() {
        return cuando;
    }

    public String getHorario() {
        return horario;
    }

    public int getHoraInicio() {
        return horaInicio;
    }

    public boolean isFinalizada() {
        return finalizada;
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

    public String getEstado() {
        return estado;
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

    public String getReservadaEl() {
        return reservadaEl;
    }

    public LocalDate getConfirmadaEl() {
        return confirmadaEl;
    }

    public BigDecimal getCobrado() {
        return cobrado;
    }

    public String getBusqueda() {
        return busqueda;
    }

    /** Las que el administrador puede aceptar o denegar ahora. */
    public boolean isPorConfirmar() {
        return "POR_CONFIRMAR".equals(estado);
    }

    /** Ocupa (u ocupó) el turno: confirmada o a la espera de confirmación / pago. */
    public boolean isActiva() {
        return !"CANCELADA".equals(estado) && !"RECHAZADA".equals(estado);
    }
}
