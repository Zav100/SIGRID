package com.sigrid.sigrid.servicio;

/**
 * Una regla del polideportivo que impide la operación pedida por el socio (turno ocupado, membresía vencida,
 * comprobante repetido...). El mensaje se le muestra tal cual. Al ser una excepción sin verificar, la
 * transacción del servicio se deshace sola.
 */
public class ReglaReservaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaReservaException(String mensaje) {
        super(mensaje);
    }
}
