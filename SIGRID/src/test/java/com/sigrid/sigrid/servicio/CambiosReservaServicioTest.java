package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.HistorialReservaDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.SolicitudCambioReservaDAO;
import com.sigrid.sigrid.dao.TurnoDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.repositorio.HistorialReserva;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.SolicitudCambioReserva;
import com.sigrid.sigrid.repositorio.Turno;
import com.sigrid.sigrid.repositorio.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cancelaciones y reprogramaciones: 48 h de anticipación (47, 48 y 49), crédito de 30 días en la misma instalación,
 * un solo pedido pendiente por reserva y resolución por el administrador.
 */
@ExtendWith(MockitoExtension.class)
class CambiosReservaServicioTest {

    private static final String MENSAJE_PLAZO = "Ya pasó el plazo: hay que pedirlo con al menos 48 h de anticipación.";
    private static final String MENSAJE_SIN_CREDITO = "Una reserva no se reprograma: pedí cancelarla y usá el crédito que te queda.";

    private final Instalacion cancha = DatosDePrueba.instalacion(1, "Cancha de Fútbol 11",
            Instalacion.TipoAcceso.ARANCELADO, Instalacion.Estado.HABILITADA);
    private final Instalacion rugby = DatosDePrueba.instalacion(2, "Cancha de Rugby",
            Instalacion.TipoAcceso.ARANCELADO, Instalacion.Estado.HABILITADA);
    private final Socio socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO,
            DatosDePrueba.categoria(1, "Alumno UNSE", true));

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

    // ---------- armado de datos ----------

    /** Reserva confirmada y pagada cuyo turno de una hora empieza en el momento pedido. */
    private Reserva confirmadaQueEmpieza(LocalDateTime inicio) {
        Turno turno = DatosDePrueba.turno(5, cancha, inicio.toLocalTime(), inicio.toLocalTime().plusHours(1));
        Reserva r = DatosDePrueba.reserva(10, socio, turno, inicio.toLocalDate(), Reserva.Estado.CONFIRMADA);
        r.setIdPago(DatosDePrueba.pago(1, "20000", Pago.Estado.CONFIRMADO));
        return r;
    }

    /** Reserva cancelada con crédito para reprogramar hasta la fecha indicada. */
    private Reserva canceladaConCreditoHasta(LocalDate limite) {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(3));
        r.setEstado(Reserva.Estado.CANCELADA);
        r.setFechaLimiteReprogramacion(limite);
        return r;
    }

    private Turno turnoNuevo(int id, Instalacion instalacion, LocalTime inicio) {
        return DatosDePrueba.turno(id, instalacion, inicio, inicio.plusHours(1));
    }

    private SolicitudCambioReserva pedido(int id, Reserva reserva, SolicitudCambioReserva.Tipo tipo) {
        SolicitudCambioReserva c = new SolicitudCambioReserva();
        c.setIdSolicitud(id);
        c.setIdReserva(reserva);
        c.setTipo(tipo);
        c.setMotivo("Viaje de trabajo");
        c.setFechaSolicitud(AHORA.minusDays(1));
        return c;
    }

    // ---------- regla de las 48 horas ----------

    @Test
    void unTurnoQueEmpezaEn47HorasEstaFueraDePlazo() {
        assertTrue(CambiosReservaServicio.fueraDePlazo(AHORA.plusHours(47), AHORA));
    }

    @Test
    void unTurnoQueEmpezaEn48HorasExactasEstaEnPlazo() {
        assertFalse(CambiosReservaServicio.fueraDePlazo(AHORA.plusHours(48), AHORA));
    }

    @Test
    void unTurnoQueEmpezaEn49HorasEstaEnPlazo() {
        assertFalse(CambiosReservaServicio.fueraDePlazo(AHORA.plusHours(49), AHORA));
    }

    @Test
    void unTurnoQueEmpezaEn47Horas59MinutosEstaFueraDePlazo() {
        assertTrue(CambiosReservaServicio.fueraDePlazo(AHORA.plusHours(48).minusMinutes(1), AHORA));
    }

    // ---------- pedido de cancelación del socio ----------

    @Test
    void aceptaPedirLaCancelacionConExactamente48Horas() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusHours(48));
        when(reservaDAO.buscarPorId(10)).thenReturn(r);
        ArgumentCaptor<SolicitudCambioReserva> pedido = ArgumentCaptor.forClass(SolicitudCambioReserva.class);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null,
                "Me lesioné la rodilla", AHORA);

        assertNull(error);
        verify(solicitudDAO).crear(pedido.capture());
        assertEquals(SolicitudCambioReserva.Tipo.CANCELACION, pedido.getValue().getTipo());
        assertSame(r, pedido.getValue().getIdReserva());
        assertEquals("Me lesioné la rodilla", pedido.getValue().getMotivo());
        assertEquals(AHORA, pedido.getValue().getFechaSolicitud());
        assertEquals(SolicitudCambioReserva.Estado.PENDIENTE, pedido.getValue().getEstado());
    }

    @Test
    void aceptaPedirLaCancelacionConMasDe48Horas() {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusHours(49)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertNull(error);
        verify(solicitudDAO).crear(any());
    }

    @Test
    void rechazaPedirLaCancelacionConMenosDe48Horas() {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusHours(47)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertEquals(MENSAJE_PLAZO, error);
        verify(solicitudDAO, never()).crear(any());
    }

    @Test
    void rechazaPedirLaCancelacionConUnMinutoMenosDe48Horas() {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusHours(48).minusMinutes(1)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertEquals(MENSAJE_PLAZO, error);
        verify(solicitudDAO, never()).crear(any());
    }

    @Test
    void rechazaElPedidoDeUnaReservaQueEsDeOtroSocio() {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusDays(5)));

        String error = servicio.solicitar(8, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertEquals("La reserva no existe.", error);
        verify(solicitudDAO, never()).crear(any());
    }

    @Test
    void rechazaElPedidoDeUnaReservaInexistente() {
        when(reservaDAO.buscarPorId(99)).thenReturn(null);

        String error = servicio.solicitar(7, 99, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertEquals("La reserva no existe.", error);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaElPedidoSinMotivo(String motivo) {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusDays(5)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, motivo, AHORA);

        assertEquals("Contanos el motivo del pedido.", error);
        verify(solicitudDAO, never()).crear(any());
    }

    @Test
    void rechazaUnSegundoPedidoPendienteParaLaMismaReserva() {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusDays(5)));
        when(solicitudDAO.hayPendienteDe(10)).thenReturn(true);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertEquals("Ya hay un pedido pendiente para esta reserva.", error);
        verify(solicitudDAO, never()).crear(any());
    }

    @Test
    void rechazaPedirLaCancelacionDeUnaReservaQueNoEstaConfirmada() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        r.setEstado(Reserva.Estado.PENDIENTE_PAGO);
        when(reservaDAO.buscarPorId(10)).thenReturn(r);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertEquals("Solo se puede pedir la cancelación de una reserva confirmada.", error);
    }

    @Test
    void recortaElMotivoA255Caracteres() {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusDays(5)));
        ArgumentCaptor<SolicitudCambioReserva> pedido = ArgumentCaptor.forClass(SolicitudCambioReserva.class);

        servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "a".repeat(300), AHORA);

        verify(solicitudDAO).crear(pedido.capture());
        assertEquals(255, pedido.getValue().getMotivo().length());
    }

    // ---------- pedido de reprogramación (usa el crédito de una reserva cancelada) ----------

    @Test
    void aceptaReprogramarConUnCreditoVigenteAUnTurnoLibreDeLaMismaInstalacion() {
        Reserva r = canceladaConCreditoHasta(HOY.plusDays(10));
        Turno nuevo = turnoNuevo(6, cancha, LocalTime.of(18, 0));
        LocalDate fechaNueva = HOY.plusDays(5);
        when(reservaDAO.buscarPorId(10)).thenReturn(r);
        when(turnoDAO.buscarPorId(6)).thenReturn(nuevo);
        ArgumentCaptor<SolicitudCambioReserva> pedido = ArgumentCaptor.forClass(SolicitudCambioReserva.class);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, fechaNueva,
                "Uso mi crédito", AHORA);

        assertNull(error);
        verify(solicitudDAO).crear(pedido.capture());
        assertSame(nuevo, pedido.getValue().getIdTurnoNuevo());
        assertEquals(fechaNueva, pedido.getValue().getFechaNueva());
    }

    @Test
    void aceptaUsarElCreditoElUltimoDiaDeSuVigencia() {
        Reserva r = canceladaConCreditoHasta(HOY);
        when(reservaDAO.buscarPorId(10)).thenReturn(r);
        when(turnoDAO.buscarPorId(6)).thenReturn(turnoNuevo(6, cancha, LocalTime.of(18, 0)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(5),
                "Uso mi crédito", AHORA);

        assertNull(error);
    }

    @Test
    void rechazaReprogramarConElCreditoVencidoAyer() {
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaConCreditoHasta(HOY.minusDays(1)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(5),
                "Uso mi crédito", AHORA);

        assertEquals(MENSAJE_SIN_CREDITO, error);
        verify(solicitudDAO, never()).crear(any());
    }

    @Test
    void rechazaReprogramarUnaReservaConfirmadaQueNoTieneCredito() {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusDays(5)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(5),
                "Quiero otro horario", AHORA);

        assertEquals(MENSAJE_SIN_CREDITO, error);
    }

    @Test
    void rechazaReprogramarAUnTurnoDeOtraInstalacion() {
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaConCreditoHasta(HOY.plusDays(10)));
        when(turnoDAO.buscarPorId(8)).thenReturn(turnoNuevo(8, rugby, LocalTime.of(18, 0)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 8, HOY.plusDays(5),
                "Uso mi crédito", AHORA);

        assertEquals("Solo se puede reprogramar dentro de la misma instalación.", error);
        verify(solicitudDAO, never()).crear(any());
    }

    @Test
    void rechazaReprogramarAUnTurnoQueYaEstaReservado() {
        LocalDate fechaNueva = HOY.plusDays(5);
        Turno nuevo = turnoNuevo(6, cancha, LocalTime.of(18, 0));
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaConCreditoHasta(HOY.plusDays(10)));
        when(turnoDAO.buscarPorId(6)).thenReturn(nuevo);
        when(reservaDAO.buscarActivaPorTurnoYFecha(6, fechaNueva))
                .thenReturn(DatosDePrueba.reserva(77, socio, nuevo, fechaNueva, Reserva.Estado.CONFIRMADA));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, fechaNueva,
                "Uso mi crédito", AHORA);

        assertEquals("El turno pedido ya está reservado.", error);
        verify(solicitudDAO, never()).crear(any());
    }

    @Test
    void aceptaReprogramarAUnTurnoQueEmpezaEnExactamente48Horas() {
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaConCreditoHasta(HOY.plusDays(10)));
        when(turnoDAO.buscarPorId(6)).thenReturn(turnoNuevo(6, cancha, LocalTime.of(10, 0)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(2),
                "Uso mi crédito", AHORA);

        assertNull(error);
    }

    @Test
    void rechazaReprogramarAUnTurnoQueEmpezaEn47Horas59Minutos() {
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaConCreditoHasta(HOY.plusDays(10)));
        when(turnoDAO.buscarPorId(6)).thenReturn(turnoNuevo(6, cancha, LocalTime.of(9, 59)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(2),
                "Uso mi crédito", AHORA);

        assertEquals("El turno pedido empieza en menos de 48 h: elegí uno con más anticipación.", error);
        verify(solicitudDAO, never()).crear(any());
    }

    @Test
    void rechazaReprogramarSinElegirElTurnoNuevo() {
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaConCreditoHasta(HOY.plusDays(10)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, null, null,
                "Uso mi crédito", AHORA);

        assertEquals("Falta elegir el turno.", error);
    }

    // ---------- el administrador aprueba ----------

    @Test
    void alAprobarUnaCancelacionLaReservaQuedaCanceladaConCreditoDe30Dias() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.CANCELACION);
        Usuario admin = DatosDePrueba.usuario(1, "Marcela", "Ibarra", "admin@polideportivo.ejemplo");
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);
        when(usuarioDAO.buscarPorId(1)).thenReturn(admin);

        String error = servicio.aprobar(50, 1, AHORA);

        assertNull(error);
        assertEquals(Reserva.Estado.CANCELADA, r.getEstado());
        assertEquals(LocalDate.of(2026, 11, 11), r.getFechaLimiteReprogramacion(), "hoy 12/10 + 30 días");
        assertEquals(SolicitudCambioReserva.Estado.APROBADA, c.getEstado());
        assertEquals(AHORA, c.getFechaResolucion());
        assertSame(admin, c.getIdAdminResolutor());
    }

    @Test
    void alAprobarUnaCancelacionDejaElTurnoAnteriorEnElHistorial() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        LocalDate fechaOriginal = r.getFechaTurno();
        Turno turnoOriginal = r.getIdTurno();
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(pedido(50, r, SolicitudCambioReserva.Tipo.CANCELACION));
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);
        ArgumentCaptor<HistorialReserva> historial = ArgumentCaptor.forClass(HistorialReserva.class);

        servicio.aprobar(50, 1, AHORA);

        verify(historialDAO).crear(historial.capture());
        assertSame(turnoOriginal, historial.getValue().getIdTurnoAnterior());
        assertEquals(fechaOriginal, historial.getValue().getFechaAnterior());
        assertEquals("Cancelación: Viaje de trabajo", historial.getValue().getMotivo());
    }

    @Test
    void unaReservaCanceladaSinPagoNoDejaCredito() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        r.setIdPago(null);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(pedido(50, r, SolicitudCambioReserva.Tipo.CANCELACION));
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        servicio.aprobar(50, 1, AHORA);

        assertEquals(Reserva.Estado.CANCELADA, r.getEstado());
        assertNull(r.getFechaLimiteReprogramacion());
    }

    @Test
    void noAprobaUnPedidoQueYaEstabaResuelto() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.CANCELACION);
        c.setEstado(SolicitudCambioReserva.Estado.APROBADA);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);

        String error = servicio.aprobar(50, 1, AHORA);

        assertEquals("Ese pedido ya no estaba por resolver.", error);
        assertEquals(Reserva.Estado.CONFIRMADA, r.getEstado());
    }

    @Test
    void noAprobaUnPedidoInexistente() {
        when(solicitudDAO.buscarParaResolver(51)).thenReturn(null);

        String error = servicio.aprobar(51, 1, AHORA);

        assertEquals("Ese pedido ya no estaba por resolver.", error);
    }

    @Test
    void noAprobaLaCancelacionDeUnaReservaQueYaNoEstaConfirmada() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        r.setEstado(Reserva.Estado.CANCELADA);
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.CANCELACION);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        String error = servicio.aprobar(50, 1, AHORA);

        assertEquals("La reserva ya no está confirmada. Rechazá el pedido.", error);
        assertEquals(SolicitudCambioReserva.Estado.PENDIENTE, c.getEstado());
    }

    @Test
    void alAprobarUnaReprogramacionLaReservaVuelveAConfirmadaYConsumeElCredito() {
        Reserva r = canceladaConCreditoHasta(HOY.plusDays(10));
        Turno nuevo = turnoNuevo(6, cancha, LocalTime.of(18, 0));
        LocalDate fechaNueva = HOY.plusDays(5);
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.REPROGRAMACION);
        c.setIdTurnoNuevo(nuevo);
        c.setFechaNueva(fechaNueva);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        String error = servicio.aprobar(50, 1, AHORA);

        assertNull(error);
        assertEquals(Reserva.Estado.CONFIRMADA, r.getEstado());
        assertNull(r.getFechaLimiteReprogramacion(), "el crédito se consume");
        assertSame(nuevo, r.getIdTurno());
        assertEquals(fechaNueva, r.getFechaTurno());
        assertEquals(SolicitudCambioReserva.Estado.APROBADA, c.getEstado());
    }

    @Test
    void noAprobaUnaReprogramacionSiElCreditoVencioAntesDelPedido() {
        Reserva r = canceladaConCreditoHasta(HOY.minusDays(2));
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.REPROGRAMACION);
        c.setFechaSolicitud(AHORA); // pedido hecho después de vencido el crédito
        c.setIdTurnoNuevo(turnoNuevo(6, cancha, LocalTime.of(18, 0)));
        c.setFechaNueva(HOY.plusDays(5));
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        String error = servicio.aprobar(50, 1, AHORA);

        assertEquals("La reserva ya no tiene un crédito para reprogramar. Rechazá el pedido.", error);
        assertEquals(Reserva.Estado.CANCELADA, r.getEstado());
    }

    @Test
    void noAprobaUnaReprogramacionSiElTurnoPedidoSeOcupoMientrasTanto() {
        Reserva r = canceladaConCreditoHasta(HOY.plusDays(10));
        Turno nuevo = turnoNuevo(6, cancha, LocalTime.of(18, 0));
        LocalDate fechaNueva = HOY.plusDays(5);
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.REPROGRAMACION);
        c.setIdTurnoNuevo(nuevo);
        c.setFechaNueva(fechaNueva);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);
        when(reservaDAO.buscarActivaPorTurnoYFecha(6, fechaNueva))
                .thenReturn(DatosDePrueba.reserva(77, socio, nuevo, fechaNueva, Reserva.Estado.CONFIRMADA));

        String error = servicio.aprobar(50, 1, AHORA);

        assertEquals("El turno pedido ya está reservado. Rechazá el pedido para que el socio pida otro turno.", error);
        assertEquals(Reserva.Estado.CANCELADA, r.getEstado());
        assertEquals(SolicitudCambioReserva.Estado.PENDIENTE, c.getEstado());
    }

    // ---------- el administrador rechaza ----------

    @Test
    void alRechazarUnPedidoLaReservaQuedaComoEstabaYSeGuardaElMotivo() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.CANCELACION);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);

        boolean rechazado = servicio.rechazar(50, 1, "El turno ya está pago y no hay causa justificada", AHORA);

        assertTrue(rechazado);
        assertEquals(SolicitudCambioReserva.Estado.RECHAZADA, c.getEstado());
        assertEquals("El turno ya está pago y no hay causa justificada", c.getMotivoRechazo());
        assertEquals(AHORA, c.getFechaResolucion());
        assertEquals(Reserva.Estado.CONFIRMADA, r.getEstado());
    }

    @Test
    void rechazarSinMotivoNoGuardaMotivo() {
        SolicitudCambioReserva c = pedido(50, confirmadaQueEmpieza(AHORA.plusDays(5)),
                SolicitudCambioReserva.Tipo.CANCELACION);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);

        boolean rechazado = servicio.rechazar(50, 1, "   ", AHORA);

        assertTrue(rechazado);
        assertNull(c.getMotivoRechazo());
    }

    @Test
    void noRechazaUnPedidoQueYaEstabaResuelto() {
        SolicitudCambioReserva c = pedido(50, confirmadaQueEmpieza(AHORA.plusDays(5)),
                SolicitudCambioReserva.Tipo.CANCELACION);
        c.setEstado(SolicitudCambioReserva.Estado.RECHAZADA);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);

        boolean rechazado = servicio.rechazar(50, 1, "otro motivo", AHORA);

        assertFalse(rechazado);
        assertNull(c.getFechaResolucion());
    }

    // ---------- cancelación por el predio (la única que hace el administrador por su cuenta) ----------

    @Test
    void laCancelacionPorElPredioNoExige48HorasYDejaCreditoDe30Dias() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusHours(5));
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);
        ArgumentCaptor<HistorialReserva> historial = ArgumentCaptor.forClass(HistorialReserva.class);

        String error = servicio.cancelarPorElPredio(10, "Cancha en mantenimiento por lluvia", AHORA);

        assertNull(error);
        assertEquals(Reserva.Estado.CANCELADA, r.getEstado());
        assertEquals(HOY.plusDays(30), r.getFechaLimiteReprogramacion());
        verify(historialDAO).crear(historial.capture());
        assertEquals("Cancelación por el predio: Cancha en mantenimiento por lluvia", historial.getValue().getMotivo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void laCancelacionPorElPredioExigeUnMotivo(String motivo) {
        String error = servicio.cancelarPorElPredio(10, motivo, AHORA);

        assertEquals("Indicá el motivo de la cancelación.", error);
        verify(reservaDAO, never()).buscarParaResolver(any());
    }

    @Test
    void laCancelacionPorElPredioRechazaUnTurnoQueYaEmpezo() {
        Reserva r = confirmadaQueEmpieza(AHORA.minusHours(1));
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        String error = servicio.cancelarPorElPredio(10, "Cancha en mantenimiento", AHORA);

        assertEquals("Esa reserva ya no está confirmada o el turno ya empezó.", error);
        assertEquals(Reserva.Estado.CONFIRMADA, r.getEstado());
    }

    @Test
    void laCancelacionPorElPredioRechazaUnaReservaNoConfirmada() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusHours(5));
        r.setEstado(Reserva.Estado.CANCELADA);
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        String error = servicio.cancelarPorElPredio(10, "Cancha en mantenimiento", AHORA);

        assertEquals("Esa reserva ya no está confirmada o el turno ya empezó.", error);
    }
}
