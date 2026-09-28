package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/** Una reserva del socio tal como la ve en su panel. "grupo" es PROXIMAS (ocupa un turno futuro) o PASADAS; estadoClase es ok, warn, off o muted. Las banderas dicen qué acciones admite. */
public class MiReserva implements Serializable {

    private final Integer idReserva;
    private final String instalacion;
    private final String icono;
    private final String disciplina;
    private final String cuando;
    private final String fecha;
    private final String horario;
    private final String monto;
    private final String estadoTexto;
    private final String estadoClase;
    private final String grupo;
    private final LocalDateTime inicio;
    private final boolean puedeSubirComprobante;
    private final boolean puedeLiberar;
    private final boolean puedePedirCancelacion;
    private final String nota;
    private final String limitePago;
    private final List<PasoSeguimiento> pasos;
    private final boolean valorable;
    private final boolean puedeReprogramar;
    private final String limiteCredito;

    public MiReserva(Integer idReserva, String instalacion, String icono, String disciplina, String cuando, String fecha, String horario, String monto, String estadoTexto, String estadoClase, String grupo, LocalDateTime inicio, boolean puedeSubirComprobante, boolean puedeLiberar, boolean puedePedirCancelacion, String nota, String limitePago,
            List<PasoSeguimiento> pasos, boolean valorable, boolean puedeReprogramar, String limiteCredito) {
        this.puedeReprogramar = puedeReprogramar;
        this.limiteCredito = limiteCredito;
        this.idReserva = idReserva;
        this.instalacion = instalacion;
        this.icono = icono;
        this.disciplina = disciplina;
        this.cuando = cuando;
        this.fecha = fecha;
        this.horario = horario;
        this.monto = monto;
        this.estadoTexto = estadoTexto;
        this.estadoClase = estadoClase;
        this.grupo = grupo;
        this.inicio = inicio;
        this.puedeSubirComprobante = puedeSubirComprobante;
        this.puedeLiberar = puedeLiberar;
        this.puedePedirCancelacion = puedePedirCancelacion;
        this.nota = nota;
        this.limitePago = limitePago;
        this.pasos = pasos;
        this.valorable = valorable;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public String getInstalacion() {
        return instalacion;
    }

    public String getIcono() {
        return icono;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public String getCuando() {
        return cuando;
    }

    public String getFecha() {
        return fecha;
    }

    public String getHorario() {
        return horario;
    }

    public String getMonto() {
        return monto;
    }

    public String getEstadoTexto() {
        return estadoTexto;
    }

    public String getEstadoClase() {
        return estadoClase;
    }

    public String getGrupo() {
        return grupo;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public boolean isPuedeSubirComprobante() {
        return puedeSubirComprobante;
    }

    public boolean isPuedeLiberar() {
        return puedeLiberar;
    }

    public boolean isPuedePedirCancelacion() {
        return puedePedirCancelacion;
    }

    public String getNota() {
        return nota;
    }

    public String getLimitePago() {
        return limitePago;
    }

    public List<PasoSeguimiento> getPasos() {
        return pasos;
    }

    public boolean isValorable() {
        return valorable;
    }

    /** Cancelada con crédito vigente y sin un pedido de reprogramación esperando respuesta. */
    public boolean isPuedeReprogramar() {
        return puedeReprogramar;
    }

    /** Hasta cuándo vale el crédito de reprogramación (dd/MM/yyyy); null si no tiene. */
    public String getLimiteCredito() {
        return limiteCredito;
    }
}
