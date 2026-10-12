package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.HistorialReservaDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.SolicitudCambioReservaDAO;
import com.sigrid.sigrid.dao.TurnoDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dto.CambioPedido;
import com.sigrid.sigrid.dto.CreditoFila;
import com.sigrid.sigrid.dto.ReprogramadaFila;
import com.sigrid.sigrid.repositorio.HistorialReserva;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.SolicitudCambioReserva;
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
 * Listados del administrador sobre cancelaciones y reprogramaciones: pedidos por resolver (con la anticipación con que
 * se pidieron), créditos con los días que les quedan y reprogramaciones ya hechas.
 */
@ExtendWith(MockitoExtension.class)
class CambiosReservaServicioListadosTest {

    @Mock
    private SolicitudCambioReservaDAO solicitudDAO;
    @Mock
    private ReservaDAO reservaDAO;
    @Mock
    private HistorialReservaDAO historialDAO;
    @Mock
    private TurnoDAO turnoDAO;
    @Mock
    private UsuarioDAO usuarioDAO;

    @InjectMocks
    private CambiosReservaServicio servicio;

    private final Socio socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO,
            DatosDePrueba.categoria(1, "Alumno UNSE", true));
    private final Instalacion cancha = DatosDePrueba.instalacion(1, "Cancha de Fútbol 11",
            Instalacion.TipoAcceso.ARANCELADO, Instalacion.Estado.HABILITADA);
    private final Turno turno = DatosDePrueba.turno(5, cancha, LocalTime.of(10, 0), LocalTime.of(11, 0));

    private Reserva reserva(int id, LocalDate fecha, Reserva.Estado estado) {
        Reserva r = DatosDePrueba.reserva(id, socio, turno, fecha, estado);
        r.setIdPago(DatosDePrueba.pago(id, "20000", Pago.Estado.CONFIRMADO));
        return r;
    }

    // ---------- pedidos por resolver ----------

    private SolicitudCambioReserva pedido(Reserva r, LocalDateTime solicitado) {
        SolicitudCambioReserva c = new SolicitudCambioReserva();
        c.setIdSolicitud(50);
        c.setIdReserva(r);
        c.setTipo(SolicitudCambioReserva.Tipo.CANCELACION);
        c.setMotivo("Viaje de trabajo");
        c.setFechaSolicitud(solicitado);
        return c;
    }

    private CambioPedido unPedido(SolicitudCambioReserva c) {
        when(solicitudDAO.listarPendientes()).thenReturn(List.of(c));
        return servicio.listarPedidos(AHORA).get(0);
    }

    @Test
    void elPedidoDiceConCuantasHorasDeAnticipacionSeHizo() {
        // turno el 14/10 a las 10:00; pedido el 13/10 a las 04:00 = 30 horas antes
        Reserva r = reserva(10, LocalDate.of(2026, 10, 14), Reserva.Estado.CONFIRMADA);

        CambioPedido p = unPedido(pedido(r, LocalDateTime.of(2026, 10, 13, 4, 0)));

        assertEquals("Pedido con 30 h de anticipación", p.getAnticipacion());
        assertFalse(p.isConCredito());
        assertFalse(p.isReprogramacion());
    }

    @Test
    void conMasDe72HorasLaAnticipacionSeDiceEnDias() {
        Reserva r = reserva(10, LocalDate.of(2026, 10, 15), Reserva.Estado.CONFIRMADA);

        assertEquals("Pedido con 71 h de anticipación",
                unPedido(pedido(r, LocalDateTime.of(2026, 10, 12, 11, 0))).getAnticipacion());
        assertEquals("Pedido con 3 días de anticipación",
                unPedido(pedido(r, LocalDateTime.of(2026, 10, 12, 10, 0))).getAnticipacion());
    }

    @Test
    void elPedidoDeUnaReservaConCreditoDiceHastaCuandoVale() {
        Reserva r = reserva(10, LocalDate.of(2026, 10, 14), Reserva.Estado.CANCELADA);
        r.setFechaLimiteReprogramacion(LocalDate.of(2026, 11, 11));
        SolicitudCambioReserva c = pedido(r, AHORA.minusHours(3));
        c.setTipo(SolicitudCambioReserva.Tipo.REPROGRAMACION);

        CambioPedido p = unPedido(c);

        assertTrue(p.isConCredito());
        assertTrue(p.isReprogramacion());
        assertEquals("Usa el crédito de una reserva cancelada (vence el 11/11/2026)", p.getAnticipacion());
    }

    @Test
    void elPedidoMuestraLaEsperaYElMonto() {
        Reserva r = reserva(10, LocalDate.of(2026, 10, 14), Reserva.Estado.CONFIRMADA);

        CambioPedido p = unPedido(pedido(r, AHORA.minusHours(3)));

        assertEquals(180, p.getMinutosEspera());
        assertEquals("hace 3 h", p.getHace());
        assertEquals("$ 20.000", p.getMonto());
        assertEquals("Viaje de trabajo", p.getMotivo());
    }

    @Test
    void elPedidoMuestraElTurnoActualYElNuevoSiEsReprogramacion() {
        Reserva r = reserva(10, LocalDate.of(2026, 10, 14), Reserva.Estado.CANCELADA);
        r.setFechaLimiteReprogramacion(HOY.plusDays(10));
        SolicitudCambioReserva c = pedido(r, AHORA.minusHours(1));
        c.setTipo(SolicitudCambioReserva.Tipo.REPROGRAMACION);
        c.setIdTurnoNuevo(DatosDePrueba.turno(6, cancha, LocalTime.of(18, 0), LocalTime.of(19, 0)));
        c.setFechaNueva(LocalDate.of(2026, 10, 16));

        CambioPedido p = unPedido(c);

        assertEquals("mié 14/10/2026 · 10:00 – 11:00", p.getActual());
        assertEquals("vie 16/10/2026 · 18:00 – 19:00", p.getNuevo());
    }

    @Test
    void unaCancelacionNoTieneTurnoNuevo() {
        Reserva r = reserva(10, LocalDate.of(2026, 10, 14), Reserva.Estado.CONFIRMADA);

        assertNull(unPedido(pedido(r, AHORA.minusHours(1))).getNuevo());
    }

    // ---------- créditos ----------

    private CreditoFila credito(LocalDate limite) {
        Reserva r = reserva(10, LocalDate.of(2026, 10, 14), Reserva.Estado.CANCELADA);
        r.setFechaLimiteReprogramacion(limite);
        when(reservaDAO.listarConCredito()).thenReturn(List.of(r));
        return servicio.listarCreditos(HOY).get(0);
    }

    @Test
    void unCreditoQueVenceHoyEstaVigenteYPorVencer() {
        CreditoFila c = credito(HOY);

        assertTrue(c.isVigente());
        assertTrue(c.isPorVencer());
        assertEquals("vence hoy", c.getPlazo());
    }

    @Test
    void unCreditoQueVenceEnSieteDiasEstaPorVencerYEnOchoNo() {
        assertTrue(credito(HOY.plusDays(7)).isPorVencer());
        assertFalse(credito(HOY.plusDays(8)).isPorVencer());
    }

    @Test
    void elPlazoDiceCuantosDiasQuedan() {
        assertEquals("queda 1 día", credito(HOY.plusDays(1)).getPlazo());
        assertEquals("quedan 5 días", credito(HOY.plusDays(5)).getPlazo());
    }

    @Test
    void unCreditoVencidoNoEstaVigenteNiPorVencerYDiceHaceCuanto() {
        CreditoFila c = credito(HOY.minusDays(3));

        assertFalse(c.isVigente());
        assertFalse(c.isPorVencer());
        assertEquals("hace 3 días", c.getPlazo());
        assertEquals(-3, c.getDiasRestantes());
    }

    // ---------- reprogramaciones ya hechas ----------

    @Test
    void laReprogramacionMuestraDeQueTurnoAQueTurnoYCuando() {
        Reserva r = DatosDePrueba.reserva(10, socio, DatosDePrueba.turno(6, cancha, LocalTime.of(18, 0),
                LocalTime.of(19, 0)), LocalDate.of(2026, 10, 16), Reserva.Estado.CONFIRMADA);
        HistorialReserva h = new HistorialReserva(1);
        h.setIdReserva(r);
        h.setIdTurnoAnterior(turno);
        h.setFechaAnterior(LocalDate.of(2026, 10, 14));
        h.setMotivo("Reprogramación: Uso mi crédito");
        h.setFechaCambio(LocalDateTime.of(2026, 10, 11, 17, 5));
        when(historialDAO.listarReprogramaciones("Reprogramación")).thenReturn(List.of(h));

        ReprogramadaFila f = servicio.listarReprogramadas().get(0);

        assertEquals("mié 14/10/2026 · 10:00 – 11:00", f.getDe());
        assertEquals("vie 16/10/2026 · 18:00 – 19:00", f.getA());
        assertEquals("11/10/2026 17:05", f.getCuando());
        assertEquals("Lucía Gómez", f.getSocio());
    }
}
