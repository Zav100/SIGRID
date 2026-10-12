package com.sigrid.sigrid.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Prueba de humo: confirma que Maven compila, encuentra las pruebas, las ejecuta y que la clase de producción
 * se carga con la API de Jakarta EE en el classpath de pruebas.
 */
class PruebaDeHumoTest {

    @Test
    void cargaUnaClaseDeProduccionYGuardaElMensajeTalCual() {
        // preparar
        String mensaje = "Ese turno acaba de ser reservado por otra persona. Elegí otro.";

        // ejecutar
        ReglaReservaException excepcion = new ReglaReservaException(mensaje);

        // verificar
        assertEquals(mensaje, excepcion.getMessage());
    }
}
