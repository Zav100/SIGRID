package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/** Indicadores y rankings de la página de reservas: salen de consultas de agregación, no de cargar todas las filas. */
public class ResumenReservas implements Serializable {

    private final long porConfirmar;
    private final long deHoy;
    private final long proximasConfirmadas;
    private final long canceladas;     // canceladas y rechazadas
    private final long total;
    private final List<RankingItem> masReservadas;
    private final List<RankingItem> horasPico;
    private final RecaudacionDiaria recaudacion;

    public ResumenReservas(long porConfirmar, long deHoy, long proximasConfirmadas, long canceladas, long total,
            List<RankingItem> masReservadas, List<RankingItem> horasPico, RecaudacionDiaria recaudacion) {
        this.porConfirmar = porConfirmar;
        this.deHoy = deHoy;
        this.proximasConfirmadas = proximasConfirmadas;
        this.canceladas = canceladas;
        this.total = total;
        this.masReservadas = masReservadas;
        this.horasPico = horasPico;
        this.recaudacion = recaudacion;
    }

    public long getPorConfirmar() {
        return porConfirmar;
    }

    public long getDeHoy() {
        return deHoy;
    }

    public long getProximasConfirmadas() {
        return proximasConfirmadas;
    }

    public long getCanceladas() {
        return canceladas;
    }

    public long getTotal() {
        return total;
    }

    /** Qué parte de todas las reservas terminó cancelada o rechazada, en %. */
    public int getPorcentajeCanceladas() {
        return total == 0 ? 0 : (int) Math.round(100.0 * canceladas / total);
    }

    public List<RankingItem> getMasReservadas() {
        return masReservadas;
    }

    public List<RankingItem> getHorasPico() {
        return horasPico;
    }

    public RecaudacionDiaria getRecaudacion() {
        return recaudacion;
    }
}
