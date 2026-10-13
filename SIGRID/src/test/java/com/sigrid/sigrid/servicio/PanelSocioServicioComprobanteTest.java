package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.repositorio.ComprobantePago;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/**
 * Carga del comprobante de transferencia: tamaño máximo de 5 MB, tipo real del archivo, fecha de la operación (ni
 * futura ni de más de 7 días) y hash repetido. Los archivos se guardan en una carpeta temporal.
 */
class PanelSocioServicioComprobanteTest extends PanelSocioServicioPruebaBase {

    private static final String PROPIEDAD = "sigrid.comprobantes";
    private static final int CINCO_MB = 5 * 1024 * 1024;

    @TempDir
    Path raiz;

    private String propiedadOriginal;
    private Reserva reserva;

    @BeforeEach
    void prepararLaReservaYLaCarpeta() {
        propiedadOriginal = System.getProperty(PROPIEDAD);
        System.setProperty(PROPIEDAD, raiz.resolve("comprobantes").toString());

        reserva = DatosDePrueba.reserva(10, socio, turno, FECHA_TURNO, Reserva.Estado.PENDIENTE_PAGO);
        reserva.setIdTarifa(tarifa);
        lenient().when(reservaDAO.buscarParaResolver(10)).thenReturn(reserva);
    }

    @AfterEach
    void restaurarLaPropiedad() {
        if (propiedadOriginal == null) {
            System.clearProperty(PROPIEDAD);
        } else {
            System.setProperty(PROPIEDAD, propiedadOriginal);
        }
    }

    private void assertRechazado(String mensajeEsperado, byte[] datos, LocalDateTime operacion) {
        ReglaReservaException e = assertThrows(ReglaReservaException.class,
                () -> servicio.subirComprobante(7, 10, datos, operacion, AHORA));
        assertEquals(mensajeEsperado, e.getMessage());
        verify(pagoDAO, never()).crear(any());
        verify(comprobanteDAO, never()).crear(any());
    }

    // ---------- camino feliz ----------

    @DisplayName("CP-13: guarda un PDF válido y deja el pago PENDIENTE_VALIDACION")
    @Test
    void guardaElComprobanteYCreaElPagoPorElPrecioDeLaTarifa() {
        byte[] datos = DatosDePrueba.pdf(2_000);
        String hash = DatosDePrueba.sha256(datos);
        LocalDateTime operacion = AHORA.minusHours(3);
        ArgumentCaptor<Pago> pago = ArgumentCaptor.forClass(Pago.class);
        ArgumentCaptor<ComprobantePago> comprobante = ArgumentCaptor.forClass(ComprobantePago.class);

        servicio.subirComprobante(7, 10, datos, operacion, AHORA);

        verify(pagoDAO).crear(pago.capture());
        assertEquals(0, new java.math.BigDecimal("20000").compareTo(pago.getValue().getMonto()));
        assertEquals(Pago.Estado.PENDIENTE_VALIDACION, pago.getValue().getEstado());
        assertEquals(AHORA, pago.getValue().getFechaPago());
        assertSame(socio.getIdUsuario(), pago.getValue().getIdUsuario());
        assertSame(pago.getValue(), reserva.getIdPago());

        verify(comprobanteDAO).crear(comprobante.capture());
        assertEquals(hash, comprobante.getValue().getHashArchivo());
        assertEquals("reserva-10-" + hash.substring(0, 12) + ".pdf", comprobante.getValue().getArchivoUrl());
        assertEquals(operacion, comprobante.getValue().getFechaOperacionDeclarada());
        assertTrue(Files.isRegularFile(raiz.resolve("comprobantes").resolve(comprobante.getValue().getArchivoUrl())));
    }

    @DisplayName("CP-13: guarda un PNG con extensión png")
    @Test
    void guardaUnPngConLaExtensionPng() {
        byte[] datos = DatosDePrueba.png(3_000);
        ArgumentCaptor<ComprobantePago> comprobante = ArgumentCaptor.forClass(ComprobantePago.class);

        servicio.subirComprobante(7, 10, datos, AHORA.minusHours(1), AHORA);

        verify(comprobanteDAO).crear(comprobante.capture());
        assertTrue(comprobante.getValue().getArchivoUrl().endsWith(".png"));
    }

    // ---------- tamaño máximo de 5 MB ----------

    @DisplayName("CP-13: acepta un archivo de exactamente 5 MB")
    @Test
    void aceptaUnArchivoDeExactamente5MB() {
        servicio.subirComprobante(7, 10, DatosDePrueba.pdf(CINCO_MB), AHORA.minusHours(1), AHORA);

        verify(pagoDAO).crear(any(Pago.class));
    }

    @DisplayName("CP-14: rechaza un archivo de 5 MB más un byte")
    @Test
    void rechazaUnArchivoDeUnByteMasQue5MB() {
        assertRechazado("El archivo pesa más de 5 MB.", DatosDePrueba.pdf(CINCO_MB + 1), AHORA.minusHours(1));
    }

    @Test
    void rechazaSinArchivo() {
        assertRechazado("Elegí el archivo del comprobante.", null, AHORA.minusHours(1));
    }

    @Test
    void rechazaUnArchivoVacio() {
        assertRechazado("Elegí el archivo del comprobante.", new byte[0], AHORA.minusHours(1));
    }

    @DisplayName("CP-14: rechaza un archivo cuyo contenido no es imagen ni PDF")
    @Test
    void rechazaUnArchivoQueNoEsImagenNiPdf() {
        byte[] texto = "transferencia realizada por $20.000".getBytes(StandardCharsets.UTF_8);

        assertRechazado("El comprobante tiene que ser una imagen (JPG o PNG) o un PDF.", texto, AHORA.minusHours(1));
    }

    // ---------- fecha de la operación: ni futura ni de más de 7 días ----------

    @Test
    void rechazaSinLaFechaDeLaTransferencia() {
        assertRechazado("Indicá cuándo hiciste la transferencia.", DatosDePrueba.pdf(500), null);
    }

    @Test
    void aceptaUnaTransferenciaHechaJustoAhora() {
        servicio.subirComprobante(7, 10, DatosDePrueba.pdf(500), AHORA, AHORA);

        verify(pagoDAO).crear(any(Pago.class));
    }

    @DisplayName("CP-15: acepta una transferencia de exactamente 7 días")
    @Test
    void aceptaUnaTransferenciaDeExactamente7DiasAtras() {
        servicio.subirComprobante(7, 10, DatosDePrueba.pdf(500), AHORA.minusDays(7), AHORA);

        verify(pagoDAO).crear(any(Pago.class));
    }

    @DisplayName("CP-15: rechaza una transferencia de más de 7 días")
    @Test
    void rechazaUnaTransferenciaDeUnMinutoMasDe7Dias() {
        assertRechazado("La transferencia tiene más de 7 días: revisá la fecha y la hora.", DatosDePrueba.pdf(500),
                AHORA.minusDays(7).minusMinutes(1));
    }

    @DisplayName("CP-15: rechaza una transferencia con fecha futura")
    @Test
    void rechazaUnaTransferenciaConFechaDeUnaHoraEnElFuturo() {
        assertRechazado("La fecha de la transferencia no puede ser futura.", DatosDePrueba.pdf(500),
                AHORA.plusHours(1));
    }

    @DisplayName("CP-15: rechaza una transferencia adelantada más de 25 minutos")
    @Test
    void rechazaUnaTransferenciaAdelantadaMasDe25Minutos() {
        assertRechazado("La fecha de la transferencia no puede ser futura.", DatosDePrueba.pdf(500),
                AHORA.plusMinutes(26));
    }

    /**
     * D-2 (corregido; regla confirmada por el responsable del proyecto el 12/10/2026): una transferencia se tolera
     * hasta 25 minutos en el futuro (diferencia de reloj entre el banco y el servidor). Antes el código toleraba 5.
     */
    @DisplayName("CP-15: acepta una transferencia adelantada hasta 25 minutos (D-2)")
    @Test
    void aceptaUnaTransferenciaAdelantadaHasta25Minutos() {
        servicio.subirComprobante(7, 10, DatosDePrueba.pdf(500), AHORA.plusMinutes(25), AHORA);

        verify(pagoDAO).crear(any(Pago.class));
    }

    // ---------- hash repetido ----------

    @DisplayName("CP-16: rechaza un archivo repetido (regla del servicio)")
    @Test
    void rechazaUnComprobanteCuyoHashYaSeUsoEnOtraOperacion() {
        byte[] datos = DatosDePrueba.pdf(2_000);
        when(comprobanteDAO.existePorHash(DatosDePrueba.sha256(datos))).thenReturn(true);

        assertRechazado("Ese comprobante ya fue usado en otra operación.", datos, AHORA.minusHours(1));

        assertFalse(Files.exists(raiz.resolve("comprobantes")), "no se guarda ningún archivo");
        assertNull(reserva.getIdPago());
    }

    // ---------- de quién es la reserva y en qué estado está ----------

    @Test
    void rechazaUnaReservaQueYaTieneComprobante() {
        reserva.setIdPago(DatosDePrueba.pago(1, "20000", Pago.Estado.PENDIENTE_VALIDACION));

        assertRechazado("Esa reserva ya tiene un comprobante cargado.", DatosDePrueba.pdf(500), AHORA.minusHours(1));
    }

    @DisplayName("CP-21: no sube un comprobante a la reserva de otro socio")
    @Test
    void rechazaUnaReservaDeOtroSocio() {
        ReglaReservaException e = assertThrows(ReglaReservaException.class,
                () -> servicio.subirComprobante(8, 10, DatosDePrueba.pdf(500), AHORA.minusHours(1), AHORA));

        assertEquals("La reserva no existe.", e.getMessage());
    }

    @Test
    void rechazaUnaReservaQueYaNoEsperaElPago() {
        reserva.setEstado(Reserva.Estado.CANCELADA);

        assertRechazado("Esa reserva ya no espera el pago (si pasó el plazo, el turno se liberó).",
                DatosDePrueba.pdf(500), AHORA.minusHours(1));
    }

    @Test
    void siNoSePuedeGuardarElArchivoAvisaAlSocioQueReintente() throws java.io.IOException {
        // la "carpeta" de comprobantes cuelga de un archivo común, así que no se puede crear
        Path archivoComun = Files.createFile(raiz.resolve("no-es-una-carpeta"));
        System.setProperty(PROPIEDAD, archivoComun.resolve("comprobantes").toString());

        ReglaReservaException e = assertThrows(ReglaReservaException.class,
                () -> servicio.subirComprobante(7, 10, DatosDePrueba.pdf(500), AHORA.minusHours(1), AHORA));

        assertEquals("No pudimos guardar el archivo. Probá de nuevo en un momento.", e.getMessage());
    }
}
