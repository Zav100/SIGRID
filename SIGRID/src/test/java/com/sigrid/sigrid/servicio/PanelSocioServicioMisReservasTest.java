package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dto.MiReserva;
import com.sigrid.sigrid.dto.PasoSeguimiento;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.SolicitudCambioReserva;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * "Mis reservas" del socio: en qué estado se muestra cada reserva y qué acciones se le ofrecen (subir comprobante,
 * liberar, pedir cancelación con 48 h, reprogramar con crédito, valorar).
 */
class PanelSocioServicioMisReservasTest extends PanelSocioServicioPruebaBase {

    private Reserva reserva(int id, LocalDate fecha, Reserva.Estado estado) {
        Reserva r = DatosDePrueba.reserva(id, socio, turno, fecha, estado);
        r.setIdTarifa(tarifa);
        return r;
    }

    private MiReserva unica(Reserva r, LocalDateTime ahora) {
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(r));
        return servicio.misReservas(7, ahora).get(0);
    }

    private SolicitudCambioReserva pedidoPendiente(Reserva r, SolicitudCambioReserva.Tipo tipo) {
        SolicitudCambioReserva c = new SolicitudCambioReserva();
        c.setIdSolicitud(50);
        c.setIdReserva(r);
        c.setTipo(tipo);
        return c;
    }

    // ---------- confirmada: pedir la cancelación exige 48 horas ----------

    @Test
    void unaConfirmadaConExactamente48HorasPermitePedirLaCancelacion() {
        MiReserva m = unica(reserva(10, FECHA_TURNO, Reserva.Estado.CONFIRMADA), AHORA);

        assertEquals("Confirmada", m.getEstadoTexto());
        assertEquals("ok", m.getEstadoClase());
        assertEquals("PROXIMAS", m.getGrupo());
        assertTrue(m.isPuedePedirCancelacion());
        assertNull(m.getNota());
    }

    @Test
    void unaConfirmadaConMenosDe48HorasYaNoPermitePedirLaCancelacion() {
        MiReserva m = unica(reserva(10, FECHA_TURNO, Reserva.Estado.CONFIRMADA), AHORA.plusHours(1));

        assertFalse(m.isPuedePedirCancelacion());
        assertEquals("Ya no se puede cancelar: faltan menos de 48 h.", m.getNota());
    }

    @Test
    void unaConfirmadaConMasDe48HorasPermitePedirLaCancelacion() {
        MiReserva m = unica(reserva(10, FECHA_TURNO, Reserva.Estado.CONFIRMADA), AHORA.minusHours(1));

        assertTrue(m.isPuedePedirCancelacion());
    }

    @Test
    void conUnPedidoPendienteSeMuestraCancelacionPedidaYNoSeOfreceOtro() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.CONFIRMADA);
        when(solicitudDAO.listarDelSocio(7)).thenReturn(List.of(pedidoPendiente(r, SolicitudCambioReserva.Tipo.CANCELACION)));

        MiReserva m = unica(r, AHORA);

        assertEquals("Cancelación pedida", m.getEstadoTexto());
        assertEquals("warn", m.getEstadoClase());
        assertFalse(m.isPuedePedirCancelacion());
        assertEquals("Esperando la respuesta del administrador.", m.getNota());
    }

    @Test
    void elMontoSaleDelPagoCuandoLoHay() {
        Reserva conPago = reserva(10, FECHA_TURNO, Reserva.Estado.CONFIRMADA);
        conPago.setIdPago(DatosDePrueba.pago(1, "18000", Pago.Estado.CONFIRMADO));

        assertEquals("$ 18.000", unica(conPago, AHORA).getMonto());
    }

    @Test
    void sinPagoElMontoSaleDeLaTarifa() {
        assertEquals("$ 20.000", unica(reserva(11, FECHA_TURNO, Reserva.Estado.CONFIRMADA), AHORA).getMonto());
    }

    // ---------- terminada: valoración ----------

    @Test
    void unaConfirmadaQueTerminoSeMuestraRealizadaYPideValoracion() {
        MiReserva m = unica(reserva(10, HOY.minusDays(2), Reserva.Estado.CONFIRMADA), AHORA);

        assertEquals("Realizada", m.getEstadoTexto());
        assertEquals("PASADAS", m.getGrupo());
        assertTrue(m.isValorable());
        assertEquals("Valoración pendiente: contanos cómo estuvo tu turno.", m.getNota());
    }

    @Test
    void unaRealizadaYaValoradaMuestraElPuntaje() {
        when(valoracionDAO.puntajesDelSocio(7)).thenReturn(Map.of(10, 4));

        MiReserva m = unica(reserva(10, HOY.minusDays(2), Reserva.Estado.CONFIRMADA), AHORA);

        assertFalse(m.isValorable());
        assertEquals("Tu valoración: 4 de 5 estrellas.", m.getNota());
    }

    @Test
    void unaRealizadaAnteriorALaFuncionDeValoracionesNoPideValorar() {
        MiReserva m = unica(reserva(10, LocalDate.of(2026, 9, 24), Reserva.Estado.CONFIRMADA), AHORA);

        assertFalse(m.isValorable());
        assertNull(m.getNota());
    }

    // ---------- pendiente de pago ----------

    @Test
    void sinComprobanteSeOfreceSubirloYLiberarConElLimiteDe60Minutos() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.PENDIENTE_PAGO);
        r.setFechaReserva(AHORA.minusMinutes(10));

        MiReserva m = unica(r, AHORA);

        assertEquals("Falta el comprobante", m.getEstadoTexto());
        assertEquals("warn", m.getEstadoClase());
        assertTrue(m.isPuedeSubirComprobante());
        assertTrue(m.isPuedeLiberar());
        assertEquals("10:50", m.getLimitePago());
        assertEquals("Subí el comprobante antes de las 10:50 o el turno se libera.", m.getNota());
    }

    @Test
    void conComprobanteQuedaEnRevisionYNoSePuedeLiberar() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.PENDIENTE_PAGO);
        r.setIdPago(DatosDePrueba.pago(1, "20000", Pago.Estado.PENDIENTE_VALIDACION));

        MiReserva m = unica(r, AHORA);

        assertEquals("Comprobante en revisión", m.getEstadoTexto());
        assertFalse(m.isPuedeSubirComprobante());
        assertFalse(m.isPuedeLiberar());
        assertEquals("El administrador está revisando tu transferencia.", m.getNota());
    }

    // ---------- cancelada con crédito ----------

    private Reserva canceladaConCreditoHasta(LocalDate limite) {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.CANCELADA);
        r.setFechaLimiteReprogramacion(limite);
        return r;
    }

    @Test
    void unaCanceladaConCreditoVigentePermiteReprogramar() {
        MiReserva m = unica(canceladaConCreditoHasta(HOY.plusDays(10)), AHORA);

        assertEquals("Cancelada", m.getEstadoTexto());
        assertTrue(m.isPuedeReprogramar());
        assertEquals("22/10/2026", m.getLimiteCredito());
        assertEquals("Tenés un crédito para reprogramar hasta el 22/10/2026.", m.getNota());
    }

    @Test
    void elUltimoDiaDelCreditoTodaviaPermiteReprogramar() {
        assertTrue(unica(canceladaConCreditoHasta(HOY), AHORA).isPuedeReprogramar());
    }

    @Test
    void conElCreditoVencidoAyerNoPermiteReprogramar() {
        MiReserva m = unica(canceladaConCreditoHasta(HOY.minusDays(1)), AHORA);

        assertFalse(m.isPuedeReprogramar());
        assertNull(m.getLimiteCredito());
    }

    @Test
    void conUnPedidoDeReprogramacionPendienteNoSeOfreceReprogramarOtraVez() {
        Reserva r = canceladaConCreditoHasta(HOY.plusDays(10));
        when(solicitudDAO.listarDelSocio(7))
                .thenReturn(List.of(pedidoPendiente(r, SolicitudCambioReserva.Tipo.REPROGRAMACION)));

        MiReserva m = unica(r, AHORA);

        assertFalse(m.isPuedeReprogramar());
        assertEquals("Pediste reprogramar con tu crédito. Esperando la respuesta del administrador.", m.getNota());
    }

    // ---------- rechazada ----------

    @Test
    void unaRechazadaMuestraElMotivoDelAdministrador() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.RECHAZADA);
        Pago pago = DatosDePrueba.pago(1, "20000", Pago.Estado.RECHAZADO);
        pago.setMotivoRechazo("El monto no coincide");
        r.setIdPago(pago);

        MiReserva m = unica(r, AHORA);

        assertEquals("Rechazada", m.getEstadoTexto());
        assertEquals("Motivo: El monto no coincide", m.getNota());
    }

    @Test
    void unaRechazadaSinMotivoDiceQueElTurnoQuedoLibre() {
        MiReserva m = unica(reserva(10, FECHA_TURNO, Reserva.Estado.RECHAZADA), AHORA);

        assertEquals("No se pudo validar el comprobante y el turno quedó libre.", m.getNota());
    }

    // ---------- seguimiento ----------

    @Test
    void elSeguimientoDeUnaConfirmadaTieneCuatroPasos() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.CONFIRMADA);
        r.setIdPago(DatosDePrueba.pago(1, "20000", Pago.Estado.CONFIRMADO));
        r.setFechaConfirmacion(AHORA.minusDays(1));

        List<PasoSeguimiento> pasos = unica(r, AHORA).getPasos();

        assertEquals(4, pasos.size());
        assertEquals("Reserva solicitada", pasos.get(0).getTitulo());
        assertEquals("Comprobante enviado", pasos.get(1).getTitulo());
        assertEquals("Reserva confirmada", pasos.get(2).getTitulo());
        assertEquals("Tu turno", pasos.get(3).getTitulo());
        assertEquals("todo", pasos.get(3).getEstado());
    }

    @Test
    void elSeguimientoSinComprobanteMarcaElComprobanteComoPasoActual() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.PENDIENTE_PAGO);
        r.setFechaReserva(AHORA.minusMinutes(10));

        List<PasoSeguimiento> pasos = unica(r, AHORA).getPasos();

        assertEquals("done", pasos.get(0).getEstado());
        assertEquals("Comprobante de pago", pasos.get(1).getTitulo());
        assertEquals("current", pasos.get(1).getEstado());
        assertEquals("Falta subirlo antes de las 10:50", pasos.get(1).getDetalle());
    }

    @Test
    void elSeguimientoDeUnaCanceladaTerminaEnReservaCancelada() {
        List<PasoSeguimiento> pasos = unica(reserva(10, FECHA_TURNO, Reserva.Estado.CANCELADA), AHORA).getPasos();

        assertEquals("No se envió", pasos.get(1).getDetalle());
        assertEquals("Reserva cancelada", pasos.get(2).getTitulo());
        assertEquals("off", pasos.get(2).getEstado());
    }
}
