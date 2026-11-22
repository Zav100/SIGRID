package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import com.sigrid.sigrid.repositorio.TarifaMembresia;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/**
 * Renovación de la membresía desde el panel del socio (M-02): con la vencida o a 7 días o menos del vencimiento, un
 * pedido por vez, por la cuota vigente de su tipo de socio; deja la suscripción PENDIENTE_PAGO con el pago
 * PENDIENTE_VALIDACION para que el administrador la acepte o la deniegue.
 */
class PanelSocioServicioRenovacionTest extends PanelSocioServicioPruebaBase {

    private static final String PROPIEDAD = "sigrid.comprobantes";

    @TempDir
    Path raiz;

    private String propiedadOriginal;

    @BeforeEach
    void prepararElSocioYLaCuota() {
        propiedadOriginal = System.getProperty(PROPIEDAD);
        System.setProperty(PROPIEDAD, raiz.toString());

        TarifaMembresia cuota = new TarifaMembresia();
        cuota.setIdCategoriaSocio(alumno);
        cuota.setPrecio(new BigDecimal("3000"));
        cuota.setVigenteDesde(LocalDate.of(2026, 1, 1));
        lenient().when(socioDAO.buscarPorId(7)).thenReturn(socio);
        lenient().when(tarifaMembresiaDAO.buscarVigente(alumno.getIdCategoriaSocio(), HOY)).thenReturn(cuota);
        lenient().when(suscripcionDAO.listarPorSocio(7)).thenReturn(Collections.emptyList());
        lenient().when(suscripcionDAO.listarVigentesDelSocio(7, HOY)).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void restaurarLaPropiedad() {
        if (propiedadOriginal == null) {
            System.clearProperty(PROPIEDAD);
        } else {
            System.setProperty(PROPIEDAD, propiedadOriginal);
        }
    }

    private static SuscripcionSocio vigenteHasta(LocalDate vence) {
        SuscripcionSocio s = new SuscripcionSocio(1);
        s.setEstado(SuscripcionSocio.Estado.VIGENTE);
        s.setFechaInicio(vence.minusMonths(1));
        s.setFechaVencimiento(vence);
        return s;
    }

    @DisplayName("M-02: con la membresía vencida crea el pedido por la cuota de su tipo de socio")
    @Test
    void conLaMembresiaVencidaCreaElPedido() {
        ArgumentCaptor<SuscripcionSocio> pedido = ArgumentCaptor.forClass(SuscripcionSocio.class);
        ArgumentCaptor<Pago> pago = ArgumentCaptor.forClass(Pago.class);

        servicio.renovarMembresia(7, DatosDePrueba.png(2_000), AHORA.minusHours(1), AHORA);

        verify(pagoDAO).crear(pago.capture());
        assertEquals(0, new BigDecimal("3000").compareTo(pago.getValue().getMonto()));
        assertEquals(Pago.Estado.PENDIENTE_VALIDACION, pago.getValue().getEstado());
        verify(suscripcionDAO).crear(pedido.capture());
        assertSame(socio, pedido.getValue().getIdSocio());
        assertEquals(SuscripcionSocio.Estado.PENDIENTE_PAGO, pedido.getValue().getEstado());
        assertSame(pago.getValue(), pedido.getValue().getIdPago());
    }

    @DisplayName("M-02: a 7 días del vencimiento ya puede renovar")
    @Test
    void aSieteDiasDelVencimientoPuedeRenovar() {
        lenient().when(suscripcionDAO.listarVigentesDelSocio(7, HOY)).thenReturn(List.of(vigenteHasta(HOY.plusDays(7))));

        assertTrue(servicio.puedeRenovar(7, HOY));
        servicio.renovarMembresia(7, DatosDePrueba.png(2_000), AHORA.minusHours(1), AHORA);
        verify(suscripcionDAO).crear(any());
    }

    @DisplayName("M-02: con más de 7 días de membresía todavía no puede renovar")
    @Test
    void conMasDeSieteDiasNoPuedeRenovar() {
        lenient().when(suscripcionDAO.listarVigentesDelSocio(7, HOY)).thenReturn(List.of(vigenteHasta(HOY.plusDays(8))));

        assertFalse(servicio.puedeRenovar(7, HOY));
        assertThrows(ReglaReservaException.class,
                () -> servicio.renovarMembresia(7, DatosDePrueba.png(2_000), AHORA.minusHours(1), AHORA));
        verify(suscripcionDAO, never()).crear(any());
    }

    @DisplayName("M-02: si ya tiene un pedido en revisión no puede mandar otro")
    @Test
    void conUnPedidoEnRevisionNoMandaOtro() {
        SuscripcionSocio enRevision = new SuscripcionSocio(2);
        enRevision.setIdPago(DatosDePrueba.pago(4, "3000", Pago.Estado.PENDIENTE_VALIDACION));
        lenient().when(suscripcionDAO.listarPorSocio(7)).thenReturn(List.of(enRevision));

        assertFalse(servicio.puedeRenovar(7, HOY));
        ReglaReservaException e = assertThrows(ReglaReservaException.class,
                () -> servicio.renovarMembresia(7, DatosDePrueba.png(2_000), AHORA.minusHours(1), AHORA));
        assertEquals("Ya enviaste el comprobante de tu renovación: el administrador lo está revisando.", e.getMessage());
        verify(pagoDAO, never()).crear(any());
    }

    @DisplayName("M-02: el comprobante de la renovación pasa por las mismas validaciones que el de una reserva")
    @Test
    void elComprobanteSeValidaIgualQueEnLasReservas() {
        ReglaReservaException e = assertThrows(ReglaReservaException.class,
                () -> servicio.renovarMembresia(7, DatosDePrueba.png(2_000), AHORA.minusDays(8), AHORA));
        assertEquals("La transferencia tiene más de 7 días: revisá la fecha y la hora.", e.getMessage());
        verify(suscripcionDAO, never()).crear(any());
    }
}
