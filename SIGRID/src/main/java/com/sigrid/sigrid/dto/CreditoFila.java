package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Una reserva cancelada con crédito para reprogramar (vigente o vencido). */
public class CreditoFila implements Serializable {

    private final Integer idReserva;
    private final String socio;
    private final String email;
    private final String instalacion;
    private final String icono;
    private final String turnoOriginal; // "vie 27/09/2026 · 18:00 – 19:00"
    private final String limite; // "dd/MM/yyyy"
    private final long diasRestantes; // negativo si venció
    private final boolean vigente;
    private final boolean porVencer; // vigente y vence en 7 días o menos
    private final String monto;
    private final String busqueda;

    public CreditoFila(Integer idReserva, String socio, String email, String instalacion, String icono, String turnoOriginal, String limite, long diasRestantes, boolean vigente, boolean porVencer, String monto, String busqueda) {
        this.idReserva = idReserva;
        this.socio = socio;
        this.email = email;
        this.instalacion = instalacion;
        this.icono = icono;
        this.turnoOriginal = turnoOriginal;
        this.limite = limite;
        this.diasRestantes = diasRestantes;
        this.vigente = vigente;
        this.porVencer = porVencer;
        this.monto = monto;
        this.busqueda = busqueda;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public String getSocio() {
        return socio;
    }

    public String getEmail() {
        return email;
    }

    public String getInstalacion() {
        return instalacion;
    }

    public String getIcono() {
        return icono;
    }

    public String getTurnoOriginal() {
        return turnoOriginal;
    }

    public String getLimite() {
        return limite;
    }

    public long getDiasRestantes() {
        return diasRestantes;
    }

    public boolean isVigente() {
        return vigente;
    }

    public boolean isPorVencer() {
        return porVencer;
    }

    public String getMonto() {
        return monto;
    }

    public String getBusqueda() {
        return busqueda;
    }

    /** "vence hoy", "quedan 5 días" o, si venció, "hace 3 días". */
    public String getPlazo() {
        long d = Math.abs(diasRestantes);
        String dias = d == 1 ? "1 día" : d + " días";
        return diasRestantes == 0 ? "vence hoy" : diasRestantes > 0 ? (d == 1 ? "queda " : "quedan ") + dias : "hace " + dias;
    }
}
