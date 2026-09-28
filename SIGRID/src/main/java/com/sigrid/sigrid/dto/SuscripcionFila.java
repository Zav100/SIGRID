package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/** DTO de una membresía (una renovación) en el panel de membresías del administrador, con los textos ya listos. */
public class SuscripcionFila implements Serializable {

    private final Integer idSuscripcion;
    private final String socio;
    private final String iniciales;
    private final String dni;
    private final String tipo;           // categoría del socio
    private final String estado;         // PENDIENTE_PAGO, VIGENTE, VENCIDA o CANCELADA
    private final String estadoTexto;    // "Pendiente de pago", "Vigente", "Vencida", "Cancelada"
    private final String estadoClase;    // "warn", "ok", "off" o "muted": color de la etiqueta
    private final String nota;           // "Comprobante para validar", "Sin comprobante", "Vence en 3 días"...
    private final String inicio;         // "28/08/2026"; null si todavía no empezó
    private final String vence;          // null si todavía no empezó
    private final BigDecimal importe;    // null si el socio todavía no cargó el pago
    private final String monto;          // "$ 4.000"; null si no hay pago
    private final boolean conComprobante; // pendiente y con el comprobante cargado: se puede aceptar o denegar
    private final boolean porVencer;      // vigente y vence dentro de los próximos días

    public SuscripcionFila(Integer idSuscripcion, String socio, String iniciales, String dni, String tipo,
            String estado, String estadoTexto, String estadoClase, String nota, String inicio, String vence,
            BigDecimal importe, String monto, boolean conComprobante, boolean porVencer) {
        this.idSuscripcion = idSuscripcion;
        this.socio = socio;
        this.iniciales = iniciales;
        this.dni = dni;
        this.tipo = tipo;
        this.estado = estado;
        this.estadoTexto = estadoTexto;
        this.estadoClase = estadoClase;
        this.nota = nota;
        this.inicio = inicio;
        this.vence = vence;
        this.importe = importe;
        this.monto = monto;
        this.conComprobante = conComprobante;
        this.porVencer = porVencer;
    }

    public Integer getIdSuscripcion() {
        return idSuscripcion;
    }

    public String getSocio() {
        return socio;
    }

    public String getIniciales() {
        return iniciales;
    }

    public String getDni() {
        return dni;
    }

    public String getTipo() {
        return tipo;
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

    public String getNota() {
        return nota;
    }

    public String getInicio() {
        return inicio;
    }

    public String getVence() {
        return vence;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public String getMonto() {
        return monto;
    }

    public boolean isConComprobante() {
        return conComprobante;
    }

    public boolean isPorVencer() {
        return porVencer;
    }
}
