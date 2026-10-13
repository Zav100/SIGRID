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
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.junit.jupiter.api.DisplayName;
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

    // ---------- cancelación automática del socio (CC-01) ----------
    // El socio cancela solo: sin pedido ni administrador de por medio, la reserva queda CANCELADA con crédito de 30 días.

    @DisplayName("CP-22: cancelar con más de 48 h cancela en el acto y deja un crédito de 30 días")
    @Test
    void cp22_cancelaAutomaticamenteConCredito() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusHours(49));
        when(reservaDAO.buscarPorId(10)).thenReturn(r);
        ArgumentCaptor<HistorialReserva> historial = ArgumentCaptor.forClass(HistorialReserva.class);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null,
                "Me lesioné la rodilla", AHORA);

        assertNull(error);
        assertEquals(Reserva.Estado.CANCELADA, r.getEstado());
        assertEquals(HOY.plusDays(30), r.getFechaLimiteReprogramacion());
        verify(historialDAO).crear(historial.capture());
        assertSame(r, historial.getValue().getIdReserva());
        assertEquals("Cancelación: Me lesioné la rodilla", historial.getValue().getMotivo());
    }

    @DisplayName("CP-22: la cancelación automática no deja ningún pedido para el administrador")
    @Test
    void cp22_noCreaPedidoNiInvolucraAlAdministrador() {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusDays(5)));

        servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        verify(solicitudDAO, never()).crear(any());
        verifyNoInteractions(usuarioDAO);
    }

    @DisplayName("CP-22: una reserva ya cancelada no se vuelve a cancelar ni suma otro crédito")
    @Test
    void cp22_noCancelaDosVeces() {
        Reserva r = canceladaConCreditoHasta(HOY.plusDays(10));
        when(reservaDAO.buscarPorId(10)).thenReturn(r);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertEquals("Solo se puede pedir la cancelación de una reserva confirmada.", error);
        assertEquals(HOY.plusDays(10), r.getFechaLimiteReprogramacion());
        verify(historialDAO, never()).crear(any());
    }

    @DisplayName("CP-23: a 47 h 59 min no se cancela: la reserva sigue CONFIRMADA y sin crédito")
    @Test
    void cp23_noCancelaA47Horas59Minutos() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusHours(48).minusMinutes(1));
        when(reservaDAO.buscarPorId(10)).thenReturn(r);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertEquals(MENSAJE_PLAZO, error);
        assertEquals(Reserva.Estado.CONFIRMADA, r.getEstado());
        assertNull(r.getFechaLimiteReprogramacion());
        verify(historialDAO, never()).crear(any());
    }

    @DisplayName("CP-24: a exactamente 48 h se cancela con crédito")
    @Test
    void cp24_cancelaExactamenteA48Horas() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusHours(48));
        when(reservaDAO.buscarPorId(10)).thenReturn(r);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertNull(error);
        assertEquals(Reserva.Estado.CANCELADA, r.getEstado());
        assertEquals(HOY.plusDays(30), r.getFechaLimiteReprogramacion());
    }

    @DisplayName("CP-21: la reserva de otro socio no se cancela")
    @Test
    void cp21_noCancelaLaReservaDeOtroSocio() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        when(reservaDAO.buscarPorId(10)).thenReturn(r);

        String error = servicio.solicitar(8, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertEquals("La reserva no existe.", error);
        assertEquals(Reserva.Estado.CONFIRMADA, r.getEstado());
        assertNull(r.getFechaLimiteReprogramacion());
    }

    @DisplayName("CP-25: cancelada automáticamente, el socio reprograma con el crédito y el administrador lo aprueba")
    @Test
    void cp25_conElCreditoDeLaCancelacionAutomaticaSeReprogramaConAprobacion() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        Turno nuevo = turnoNuevo(6, cancha, LocalTime.of(18, 0));
        LocalDate fechaNueva = HOY.plusDays(6);
        when(reservaDAO.buscarPorId(10)).thenReturn(r);
        when(turnoDAO.buscarPorId(6)).thenReturn(nuevo);
        ArgumentCaptor<SolicitudCambioReserva> pedido = ArgumentCaptor.forClass(SolicitudCambioReserva.class);

        // 1) cancela solo
        assertNull(servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA));
        assertEquals(Reserva.Estado.CANCELADA, r.getEstado());
        // 2) pide usar el crédito: queda PENDIENTE, la reserva todavía no se mueve
        assertNull(servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, fechaNueva,
                "Uso mi crédito", AHORA));
        verify(solicitudDAO).crear(pedido.capture());
        SolicitudCambioReserva c = pedido.getValue();
        assertEquals(SolicitudCambioReserva.Estado.PENDIENTE, c.getEstado());
        assertEquals(Reserva.Estado.CANCELADA, r.getEstado());
        // 3) el administrador aprueba: vuelve a CONFIRMADA en el turno nuevo y el crédito se consume
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        assertNull(servicio.aprobar(50, 1, AHORA));

        assertEquals(Reserva.Estado.CONFIRMADA, r.getEstado());
        assertSame(nuevo, r.getIdTurno());
        assertEquals(fechaNueva, r.getFechaTurno());
        assertNull(r.getFechaLimiteReprogramacion());
        assertEquals(SolicitudCambioReserva.Estado.APROBADA, c.getEstado());
    }

    // ---------- regla de las 48 horas ----------

    @Test
    void unTurnoQueEmpiezaEn47HorasEstaFueraDePlazo() {
        assertTrue(CambiosReservaServicio.fueraDePlazo(AHORA.plusHours(47), AHORA));
    }

    @Test
    void unTurnoQueEmpiezaEn48HorasExactasEstaEnPlazo() {
        assertFalse(CambiosReservaServicio.fueraDePlazo(AHORA.plusHours(48), AHORA));
    }

    @Test
    void unTurnoQueEmpiezaEn49HorasEstaEnPlazo() {
        assertFalse(CambiosReservaServicio.fueraDePlazo(AHORA.plusHours(49), AHORA));
    }

    @Test
    void unTurnoQueEmpiezaEn47Horas59MinutosEstaFueraDePlazo() {
        assertTrue(CambiosReservaServicio.fueraDePlazo(AHORA.plusHours(48).minusMinutes(1), AHORA));
    }

    // ---------- pedido de cancelación del socio ----------

    @DisplayName("CP-23: rechaza cancelar con menos de 48 h")
    @Test
    void rechazaPedirLaCancelacionConMenosDe48Horas() {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusHours(47)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertEquals(MENSAJE_PLAZO, error);
        verify(solicitudDAO, never()).crear(any());
    }

    @DisplayName("CP-23: rechaza cancelar a 47 h 59 min del turno")
    @Test
    void rechazaPedirLaCancelacionConUnMinutoMenosDe48Horas() {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusHours(48).minusMinutes(1)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertEquals(MENSAJE_PLAZO, error);
        verify(solicitudDAO, never()).crear(any());
    }

    @DisplayName("CP-21: no pide cambios sobre la reserva de otro socio")
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

    @DisplayName("CP-27: rechaza un segundo pedido para la misma reserva")
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
    void elMotivoDeLaCancelacionSeRecortaA255CaracteresEnElHistorial() {
        when(reservaDAO.buscarPorId(10)).thenReturn(confirmadaQueEmpieza(AHORA.plusDays(5)));
        ArgumentCaptor<HistorialReserva> historial = ArgumentCaptor.forClass(HistorialReserva.class);

        servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "a".repeat(300), AHORA);

        verify(historialDAO).crear(historial.capture());
        assertEquals(255, historial.getValue().getMotivo().length());
        assertTrue(historial.getValue().getMotivo().startsWith("Cancelación: aaa"));
    }

    @Test
    void elMotivoDeLaReprogramacionSeRecortaA255Caracteres() {
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaConCreditoHasta(HOY.plusDays(10)));
        when(turnoDAO.buscarPorId(6)).thenReturn(turnoNuevo(6, cancha, LocalTime.of(18, 0)));
        ArgumentCaptor<SolicitudCambioReserva> pedido = ArgumentCaptor.forClass(SolicitudCambioReserva.class);

        servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(5), "a".repeat(300),
                AHORA);

        verify(solicitudDAO).crear(pedido.capture());
        assertEquals(255, pedido.getValue().getMotivo().length());
    }

    // ---------- pedido de reprogramación (usa el crédito de una reserva cancelada) ----------

    @DisplayName("CP-25: pide reprogramar con crédito vigente: queda PENDIENTE")
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

    @DisplayName("CP-28: el crédito todavía se usa el último día")
    @Test
    void aceptaUsarElCreditoElUltimoDiaDeSuVigencia() {
        Reserva r = canceladaConCreditoHasta(HOY);
        when(reservaDAO.buscarPorId(10)).thenReturn(r);
        when(turnoDAO.buscarPorId(6)).thenReturn(turnoNuevo(6, cancha, LocalTime.of(18, 0)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(5),
                "Uso mi crédito", AHORA);

        assertNull(error);
    }

    @DisplayName("CP-28: un crédito vencido ayer ya no se puede usar")
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

    @DisplayName("CP-27: rechaza reprogramar a otra instalación")
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
    void aceptaReprogramarAUnTurnoQueEmpiezaEnExactamente48Horas() {
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaConCreditoHasta(HOY.plusDays(10)));
        when(turnoDAO.buscarPorId(6)).thenReturn(turnoNuevo(6, cancha, LocalTime.of(10, 0)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(2),
                "Uso mi crédito", AHORA);

        assertNull(error);
    }

    @Test
    void rechazaReprogramarAUnTurnoQueEmpiezaEn47Horas59Minutos() {
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
    void noApruebaUnPedidoQueYaEstabaResuelto() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.CANCELACION);
        c.setEstado(SolicitudCambioReserva.Estado.APROBADA);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);

        String error = servicio.aprobar(50, 1, AHORA);

        assertEquals("Ese pedido ya no estaba por resolver.", error);
        assertEquals(Reserva.Estado.CONFIRMADA, r.getEstado());
    }

    @Test
    void noApruebaUnPedidoInexistente() {
        when(solicitudDAO.buscarParaResolver(51)).thenReturn(null);

        String error = servicio.aprobar(51, 1, AHORA);

        assertEquals("Ese pedido ya no estaba por resolver.", error);
    }

    @Test
    void noApruebaLaCancelacionDeUnaReservaQueYaNoEstaConfirmada() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        r.setEstado(Reserva.Estado.CANCELADA);
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.CANCELACION);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        String error = servicio.aprobar(50, 1, AHORA);

        assertEquals("La reserva ya no está confirmada. Rechazá el pedido.", error);
        assertEquals(SolicitudCambioReserva.Estado.PENDIENTE, c.getEstado());
    }

    @DisplayName("CP-25: al aprobar, la reserva vuelve a CONFIRMADA en el turno nuevo")
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
    void noApruebaUnaReprogramacionSiElCreditoVencioAntesDelPedido() {
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
    void noApruebaUnaReprogramacionSiElTurnoPedidoSeOcupoMientrasTanto() {
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

    @DisplayName("CP-26: al rechazar una reprogramación el crédito sigue vigente y se puede volver a pedir")
    @Test
    void rechazarUnaReprogramacionDejaElCreditoVigenteHastaSuVencimiento() {
        Reserva r = canceladaConCreditoHasta(HOY.plusDays(10));
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.REPROGRAMACION);
        c.setIdTurnoNuevo(turnoNuevo(6, cancha, LocalTime.of(18, 0)));
        c.setFechaNueva(HOY.plusDays(5));
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);

        boolean rechazado = servicio.rechazar(50, 1, "Ese turno se reservó para un torneo", AHORA);

        assertTrue(rechazado);
        assertEquals(SolicitudCambioReserva.Estado.RECHAZADA, c.getEstado());
        assertEquals(Reserva.Estado.CANCELADA, r.getEstado());
        assertEquals(HOY.plusDays(10), r.getFechaLimiteReprogramacion());

        // con el mismo crédito el socio puede pedir otro turno
        when(reservaDAO.buscarPorId(10)).thenReturn(r);
        when(turnoDAO.buscarPorId(7)).thenReturn(turnoNuevo(7, cancha, LocalTime.of(19, 0)));
        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 7, HOY.plusDays(6),
                "Pido otro horario", AHORA);
        assertNull(error);
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

    @DisplayName("CP-31: cancelación por el predio: CANCELADA con crédito de 30 días")
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

    // ---------- crédito y mantenimiento de la instalación (CC-02) ----------

    private void enMantenimientoDesde(Instalacion instalacion, LocalDate inicio) {
        instalacion.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        instalacion.setFechaInicioBaja(inicio);
    }

    private Instalacion quincho(int id, String nombre) {
        Instalacion i = DatosDePrueba.instalacion(id, nombre, Instalacion.TipoAcceso.ARANCELADO,
                Instalacion.Estado.HABILITADA);
        i.setTipoDisciplina("Quincho con asador");
        return i;
    }

    /** Reserva cancelada con crédito hasta la fecha, de un turno de la instalación pedida. */
    private Reserva canceladaDe(Instalacion instalacion, LocalDate limite) {
        Turno turno = DatosDePrueba.turno(5, instalacion, LocalTime.of(18, 0), LocalTime.of(19, 0));
        Reserva r = DatosDePrueba.reserva(10, socio, turno, AHORA.plusDays(3).toLocalDate(), Reserva.Estado.CANCELADA);
        r.setIdPago(DatosDePrueba.pago(1, "20000", Pago.Estado.CONFIRMADO));
        r.setFechaLimiteReprogramacion(limite);
        return r;
    }

    @DisplayName("CP-29: un crédito no vence mientras la instalación está en mantenimiento, aunque pasen más de 30 días")
    @Test
    void cp29_elCreditoNoVenceMientrasDuraElMantenimiento() {
        enMantenimientoDesde(cancha, HOY.minusDays(45));
        Reserva r = canceladaConCreditoHasta(HOY.minusDays(45).plusDays(10)); // al empezar le quedaban 10 días
        when(reservaDAO.buscarPorId(10)).thenReturn(r);
        when(turnoDAO.buscarPorId(6)).thenReturn(turnoNuevo(6, cancha, LocalTime.of(18, 0)));

        assertTrue(CambiosReservaServicio.enPausa(r));
        assertEquals(HOY.plusDays(10), CambiosReservaServicio.limiteDelCredito(r, HOY));
        // sigue siendo un crédito vigente: lo único que impide usarlo es que la instalación está cerrada
        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(5),
                "Uso mi crédito", AHORA);
        assertEquals("La instalación no está habilitada.", error);
    }

    @DisplayName("CP-29: durante el mantenimiento al crédito le quedan siempre los mismos días")
    @Test
    void cp29_losDiasQueLeQuedanNoCambianDuranteElMantenimiento() {
        enMantenimientoDesde(cancha, HOY.minusDays(20));
        Reserva r = canceladaConCreditoHasta(HOY.minusDays(20).plusDays(25)); // al empezar le quedaban 25 días

        assertEquals(HOY.plusDays(25), CambiosReservaServicio.limiteDelCredito(r, HOY));
        assertEquals(HOY.plusDays(65), CambiosReservaServicio.limiteDelCredito(r, HOY.plusDays(40)));
    }

    @DisplayName("CP-29: un crédito que ya había vencido antes del mantenimiento sigue vencido")
    @Test
    void cp29_unCreditoVencidoAntesDelMantenimientoSigueVencido() {
        enMantenimientoDesde(cancha, HOY.minusDays(5));
        Reserva r = canceladaConCreditoHasta(HOY.minusDays(8));

        assertFalse(CambiosReservaServicio.enPausa(r));
        assertEquals(HOY.minusDays(8), CambiosReservaServicio.limiteDelCredito(r, HOY));
    }

    @DisplayName("CP-28: sin mantenimiento el crédito sigue venciendo en la fecha guardada")
    @Test
    void cp28_sinMantenimientoElPlazoNoSeToca() {
        Reserva r = canceladaConCreditoHasta(HOY.plusDays(3));

        assertFalse(CambiosReservaServicio.enPausa(r));
        assertEquals(HOY.plusDays(3), CambiosReservaServicio.limiteDelCredito(r, HOY));
    }

    @DisplayName("CP-29: cancelar una reserva durante el mantenimiento deja el crédito en pausa desde el primer día")
    @Test
    void cp29_unaCancelacionDuranteElMantenimientoNoEmpiezaACorrerHastaReactivar() {
        enMantenimientoDesde(cancha, HOY.minusDays(5));
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(10)); // un turno posterior a la fecha estimada de reactivación
        when(reservaDAO.buscarPorId(10)).thenReturn(r);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.CANCELACION, null, null, "Viaje", AHORA);

        assertNull(error);
        assertEquals(HOY.plusDays(30), CambiosReservaServicio.limiteDelCredito(r, HOY), "30 días completos");
        assertEquals(HOY.plusDays(50), CambiosReservaServicio.limiteDelCredito(r, HOY.plusDays(20)), "y no corren");
    }

    @DisplayName("CC-02: con la instalación dada de baja, el crédito se usa en una instalación equivalente")
    @Test
    void conLaBajaDefinitivaElCreditoSeUsaEnUnaInstalacionEquivalente() {
        Instalacion quincho1 = quincho(3, "Quincho 1");
        quincho1.setEstado(Instalacion.Estado.DESHABILITADA);
        Instalacion quincho2 = quincho(4, "Quincho 2");
        Turno nuevo = turnoNuevo(6, quincho2, LocalTime.of(18, 0));
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaDe(quincho1, HOY.plusDays(10)));
        when(turnoDAO.buscarPorId(6)).thenReturn(nuevo);
        ArgumentCaptor<SolicitudCambioReserva> pedido = ArgumentCaptor.forClass(SolicitudCambioReserva.class);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(5),
                "Uso mi crédito", AHORA);

        assertNull(error);
        verify(solicitudDAO).crear(pedido.capture());
        assertSame(nuevo, pedido.getValue().getIdTurnoNuevo());
    }

    @DisplayName("CC-02: con la baja definitiva, otra disciplina no es equivalente")
    @Test
    void conLaBajaDefinitivaUnaInstalacionDeOtraDisciplinaNoServe() {
        Instalacion quincho1 = quincho(3, "Quincho 1");
        quincho1.setEstado(Instalacion.Estado.DESHABILITADA);
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaDe(quincho1, HOY.plusDays(10)));
        when(turnoDAO.buscarPorId(6)).thenReturn(turnoNuevo(6, cancha, LocalTime.of(18, 0)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(5),
                "Uso mi crédito", AHORA);

        assertEquals("Solo se puede reprogramar dentro de la misma instalación.", error);
        verify(solicitudDAO, never()).crear(any());
    }

    @DisplayName("CC-02: en mantenimiento (temporal) el crédito no se pasa a otra instalación, ni equivalente")
    @Test
    void enMantenimientoElCreditoNoSePasaAUnaEquivalente() {
        Instalacion quincho1 = quincho(3, "Quincho 1");
        enMantenimientoDesde(quincho1, HOY.minusDays(2));
        Instalacion quincho2 = quincho(4, "Quincho 2");
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaDe(quincho1, HOY.plusDays(10)));
        when(turnoDAO.buscarPorId(6)).thenReturn(turnoNuevo(6, quincho2, LocalTime.of(18, 0)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(5),
                "Uso mi crédito", AHORA);

        assertEquals("Solo se puede reprogramar dentro de la misma instalación.", error);
        verify(solicitudDAO, never()).crear(any());
    }

    // ---------- huecos de la reprogramación y de la resolución ----------

    @Test
    void rechazaReprogramarAUnTurnoDeUnaInstalacionNoHabilitada() {
        Reserva r = canceladaConCreditoHasta(HOY.plusDays(10));
        Turno nuevo = turnoNuevo(6, cancha, LocalTime.of(18, 0));
        cancha.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        when(reservaDAO.buscarPorId(10)).thenReturn(r);
        when(turnoDAO.buscarPorId(6)).thenReturn(nuevo);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(5),
                "Uso mi crédito", AHORA);

        assertEquals("La instalación no está habilitada.", error);
        verify(solicitudDAO, never()).crear(any());
    }

    @Test
    void rechazaReprogramarAUnTurnoQueYaPaso() {
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaConCreditoHasta(HOY.plusDays(10)));
        when(turnoDAO.buscarPorId(6)).thenReturn(turnoNuevo(6, cancha, LocalTime.of(18, 0)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.minusDays(1),
                "Uso mi crédito", AHORA);

        assertEquals("El turno pedido ya pasó.", error);
    }

    @Test
    void rechazaReprogramarSinElegirLaFecha() {
        when(reservaDAO.buscarPorId(10)).thenReturn(canceladaConCreditoHasta(HOY.plusDays(10)));
        when(turnoDAO.buscarPorId(6)).thenReturn(turnoNuevo(6, cancha, LocalTime.of(18, 0)));

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, null,
                "Uso mi crédito", AHORA);

        assertEquals("Falta elegir el turno.", error);
    }

    @Test
    void rechazaReprogramarUnaReservaCanceladaSinFechaLimiteDeCredito() {
        Reserva r = canceladaConCreditoHasta(null);
        when(reservaDAO.buscarPorId(10)).thenReturn(r);

        String error = servicio.solicitar(7, 10, SolicitudCambioReserva.Tipo.REPROGRAMACION, 6, HOY.plusDays(5),
                "Uso mi crédito", AHORA);

        assertEquals(MENSAJE_SIN_CREDITO, error);
    }

    @Test
    void noApruebaUnaReprogramacionDeUnaReservaCanceladaSinCredito() {
        Reserva r = canceladaConCreditoHasta(null);
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.REPROGRAMACION);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        String error = servicio.aprobar(50, 1, AHORA);

        assertEquals("La reserva ya no tiene un crédito para reprogramar. Rechazá el pedido.", error);
    }

    @Test
    void noApruebaUnaReprogramacionDeUnaReservaQueNoEstaCancelada() {
        Reserva r = confirmadaQueEmpieza(AHORA.plusDays(5));
        SolicitudCambioReserva c = pedido(50, r, SolicitudCambioReserva.Tipo.REPROGRAMACION);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        String error = servicio.aprobar(50, 1, AHORA);

        assertEquals("La reserva ya no tiene un crédito para reprogramar. Rechazá el pedido.", error);
        assertEquals(Reserva.Estado.CONFIRMADA, r.getEstado());
    }

    @Test
    void noRechazaUnPedidoInexistente() {
        when(solicitudDAO.buscarParaResolver(51)).thenReturn(null);

        assertFalse(servicio.rechazar(51, 1, "motivo", AHORA));
    }

    @Test
    void rechazarConMotivoNuloNoGuardaMotivo() {
        SolicitudCambioReserva c = pedido(50, confirmadaQueEmpieza(AHORA.plusDays(5)),
                SolicitudCambioReserva.Tipo.CANCELACION);
        when(solicitudDAO.buscarParaResolver(50)).thenReturn(c);

        assertTrue(servicio.rechazar(50, 1, null, AHORA));
        assertNull(c.getMotivoRechazo());
    }

    @Test
    void laCancelacionPorElPredioRechazaUnaReservaInexistente() {
        when(reservaDAO.buscarParaResolver(99)).thenReturn(null);

        String error = servicio.cancelarPorElPredio(99, "Cancha en mantenimiento", AHORA);

        assertEquals("Esa reserva ya no está confirmada o el turno ya empezó.", error);
    }
}
