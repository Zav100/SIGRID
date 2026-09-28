package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/**
 * DTO de las reservas que se pisan con sacar una instalación de servicio. Las confirmadas y las que todavía esperan el
 * pago se pueden cancelar desde la ficha (las confirmadas quedan con crédito de reprogramación); las que ya tienen
 * comprobante esperando validación las tiene que resolver el administrador en Solicitudes.
 */
public class Conflictos implements Serializable {

    public static final Conflictos NINGUNO = new Conflictos(List.of(), 0, 0, 0);

    private final List<ReservaFila> reservas;
    private final int confirmadas;
    private final int esperandoPago;
    private final int porConfirmar;

    public Conflictos(List<ReservaFila> reservas, int confirmadas, int esperandoPago, int porConfirmar) {
        this.reservas = reservas;
        this.confirmadas = confirmadas;
        this.esperandoPago = esperandoPago;
        this.porConfirmar = porConfirmar;
    }

    public List<ReservaFila> getReservas() {
        return reservas;
    }

    public int getConfirmadas() {
        return confirmadas;
    }

    public int getEsperandoPago() {
        return esperandoPago;
    }

    public int getPorConfirmar() {
        return porConfirmar;
    }

    public int getTotal() {
        return reservas.size();
    }

    public boolean isVacio() {
        return reservas.isEmpty();
    }
}
