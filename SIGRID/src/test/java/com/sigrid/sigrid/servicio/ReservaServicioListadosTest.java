package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.ComprobantePagoDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dto.ReservaListado;
import com.sigrid.sigrid.dto.SolicitudFila;
import com.sigrid.sigrid.repositorio.ComprobantePago;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.Turno;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Listados del administrador: el estado que se muestra de cada reserva (con crédito, por confirmar, esperando pago) y
 * las solicitudes por confirmar con su espera y su urgencia. "Ahora" es lunes 12/10/2026 10:00.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServicioListadosTest {

    @Mock
    private ReservaDAO reservaDAO;
    @Mock
    private UsuarioDAO usuarioDAO;
    @Mock
    private ComprobantePagoDAO comprobanteDAO;

    @InjectMocks
    private ReservaServicio servicio;

    private final Socio socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO,
            DatosDePrueba.categoria(1, "Alumno UNSE", true));
    private final Instalacion cancha = DatosDePrueba.instalacion(1, "Cancha de Fútbol 11",
            Instalacion.TipoAcceso.ARANCELADO, Instalacion.Estado.HABILITADA);
    private final Turno manana = DatosDePrueba.turno(5, cancha, LocalTime.of(10, 0), LocalTime.of(11, 0));

    private Reserva reserva(int id, LocalDate fecha, Reserva.Estado estado) {
        return DatosDePrueba.reserva(id, socio, manana, fecha, estado);
    }

    private ReservaListado unica(Reserva r) {
        when(reservaDAO.listarTodasParaAdmin()).thenReturn(List.of(r));
        return servicio.listarTodas(HOY, AHORA.toLocalTime()).get(0);
    }

    // ---------- listado de todas las reservas ----------

    @Test
    void unaConfirmadaSeMuestraConfirmadaYCobradaElDiaDeLaConfirmacion() {
        Reserva r = reserva(10, HOY.plusDays(3), Reserva.Estado.CONFIRMADA);
        r.setIdPago(DatosDePrueba.pago(1, "20000", Pago.Estado.CONFIRMADO));
        r.setFechaConfirmacion(LocalDateTime.of(2026, 10, 9, 15, 30));

        ReservaListado l = unica(r);

        assertEquals("CONFIRMADA", l.getEstado());
        assertEquals("Confirmada", l.getEstadoTexto());
        assertEquals("ok", l.getEstadoClase());
        assertEquals(LocalDate.of(2026, 10, 9), l.getConfirmadaEl());
        assertEquals(0, new java.math.BigDecimal("20000").compareTo(l.getCobrado()));
        assertEquals("$ 20.000", l.getMonto());
        assertTrue(l.isActiva());
    }

    @Test
    void unaCanceladaConCreditoVigenteLoDiceEnElTexto() {
        Reserva r = reserva(10, HOY.plusDays(3), Reserva.Estado.CANCELADA);
        r.setFechaLimiteReprogramacion(HOY);

        ReservaListado l = unica(r);

        assertEquals("Cancelada · con crédito", l.getEstadoTexto());
        assertEquals("off", l.getEstadoClase());
        assertFalse(l.isActiva());
    }

    @Test
    void unaCanceladaConCreditoVencidoAyerNoDiceConCredito() {
        Reserva r = reserva(10, HOY.plusDays(3), Reserva.Estado.CANCELADA);
        r.setFechaLimiteReprogramacion(HOY.minusDays(1));

        assertEquals("Cancelada", unica(r).getEstadoTexto());
    }

    @Test
    void unaPendienteConComprobanteEstaPorConfirmar() {
        Reserva r = reserva(10, HOY.plusDays(3), Reserva.Estado.PENDIENTE_PAGO);
        r.setIdPago(DatosDePrueba.pago(1, "20000", Pago.Estado.PENDIENTE_VALIDACION));

        ReservaListado l = unica(r);

        assertEquals("POR_CONFIRMAR", l.getEstado());
        assertEquals("Por confirmar", l.getEstadoTexto());
        assertEquals("warn", l.getEstadoClase());
        assertTrue(l.isPorConfirmar());
    }

    @Test
    void unaPendienteSinComprobanteEstaEsperandoElPago() {
        ReservaListado l = unica(reserva(10, HOY.plusDays(3), Reserva.Estado.PENDIENTE_PAGO));

        assertEquals("ESPERANDO_PAGO", l.getEstado());
        assertEquals("muted", l.getEstadoClase());
        assertFalse(l.isPorConfirmar());
    }

    @Test
    void unaRechazadaNoOcupaElTurno() {
        ReservaListado l = unica(reserva(10, HOY.plusDays(3), Reserva.Estado.RECHAZADA));

        assertEquals("Rechazada", l.getEstadoTexto());
        assertFalse(l.isActiva());
    }

    @Test
    void unaReservaTerminadaSeMarcaFinalizada() {
        assertTrue(unica(reserva(10, HOY.minusDays(1), Reserva.Estado.CONFIRMADA)).isFinalizada());
    }

    @Test
    void unaReservaDeHoyQueTerminaJustoAhoraYaEstaFinalizada() {
        Turno termina = DatosDePrueba.turno(6, cancha, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Reserva r = DatosDePrueba.reserva(10, socio, termina, HOY, Reserva.Estado.CONFIRMADA);

        assertTrue(unica(r).isFinalizada());
    }

    @Test
    void unaReservaDeHoyQueTodaviaNoTerminoNoEstaFinalizada() {
        Turno sigue = DatosDePrueba.turno(6, cancha, LocalTime.of(9, 30), LocalTime.of(10, 30));
        Reserva r = DatosDePrueba.reserva(10, socio, sigue, HOY, Reserva.Estado.CONFIRMADA);

        assertFalse(unica(r).isFinalizada());
    }

    @Test
    void sinPagoNiTarifaElMontoEsNulo() {
        Reserva sinTarifa = reserva(10, HOY.plusDays(3), Reserva.Estado.PENDIENTE_PAGO);

        assertNull(unica(sinTarifa).getMonto());
    }

    @Test
    void elBuscadorUsaTextoSinTildesNiMayusculas() {
        ReservaListado l = unica(reserva(10, HOY.plusDays(3), Reserva.Estado.CONFIRMADA));

        assertTrue(l.getBusqueda().contains("lucia gomez"));
        assertTrue(l.getBusqueda().contains("30123456"));
        assertTrue(l.getBusqueda().contains("cancha de futbol 11"));
        assertTrue(l.getBusqueda().contains("15/10/2026"));
    }

    // ---------- solicitudes por confirmar ----------

    private Reserva conComprobante(int id, LocalDate fecha, Turno turno, LocalDateTime pagado) {
        Reserva r = DatosDePrueba.reserva(id, socio, turno, fecha, Reserva.Estado.PENDIENTE_PAGO);
        Pago p = DatosDePrueba.pago(id, "20000", Pago.Estado.PENDIENTE_VALIDACION);
        p.setFechaPago(pagado);
        r.setIdPago(p);
        return r;
    }

    private SolicitudFila solicitud(Reserva r) {
        when(reservaDAO.listarPendientes()).thenReturn(List.of(r));
        return servicio.listarSolicitudes(AHORA).get(0);
    }

    @Test
    void laSolicitudMuestraLaEsperaDesdeQueElSocioPago() {
        SolicitudFila s = solicitud(conComprobante(10, HOY.plusDays(3), manana, AHORA.minusMinutes(90)));

        assertEquals(90, s.getMinutosEspera());
        assertEquals("hace 1 h", s.getEspera());
        assertEquals("$ 20.000", s.getMonto());
        assertEquals("Alumno UNSE", s.getCategoria());
    }

    @Test
    void siElPagoEsPosteriorAAhoraLaEsperaEsCero() {
        SolicitudFila s = solicitud(conComprobante(10, HOY.plusDays(3), manana, AHORA.plusMinutes(5)));

        assertEquals(0, s.getMinutosEspera());
        assertEquals("recién", s.getEspera());
    }

    @Test
    void unaSolicitudParaHoyOMananaEsUrgenteYParaPasadoMananaNo() {
        assertTrue(solicitud(conComprobante(10, HOY, manana, AHORA.minusHours(1))).isUrgente());
        assertTrue(solicitud(conComprobante(11, HOY.plusDays(1), manana, AHORA.minusHours(1))).isUrgente());
        assertFalse(solicitud(conComprobante(12, HOY.plusDays(2), manana, AHORA.minusHours(1))).isUrgente());
    }

    @Test
    void unaSolicitudDeUnTurnoQueYaPasoEstaVencida() {
        assertTrue(solicitud(conComprobante(10, HOY.minusDays(1), manana, AHORA.minusHours(1))).isVencida());
    }

    @Test
    void unaSolicitudDeHoyEstaVencidaSoloSiElTurnoYaTermino() {
        Turno termino = DatosDePrueba.turno(6, cancha, LocalTime.of(8, 0), LocalTime.of(9, 0));

        assertTrue(solicitud(conComprobante(10, HOY, termino, AHORA.minusHours(1))).isVencida());
        assertFalse(solicitud(conComprobante(11, HOY, manana, AHORA.minusHours(1))).isVencida());
    }

    @Test
    void laSolicitudMuestraCuandoDiceElSocioQueHizoLaTransferencia() {
        Reserva r = conComprobante(10, HOY.plusDays(3), manana, AHORA.minusHours(1));
        ComprobantePago c = new ComprobantePago(1);
        c.setFechaOperacionDeclarada(LocalDateTime.of(2026, 10, 12, 8, 45));
        when(comprobanteDAO.buscarPorPago(10)).thenReturn(c);

        assertEquals("12/10 08:45", solicitud(r).getOperacion());
    }

    @Test
    void sinComprobanteGuardadoNoHayFechaDeOperacion() {
        Reserva r = conComprobante(10, HOY.plusDays(3), manana, AHORA.minusHours(1));
        when(comprobanteDAO.buscarPorPago(10)).thenReturn(null);

        assertNull(solicitud(r).getOperacion());
    }
}
