package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import jakarta.persistence.PersistenceException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Reserva de un turno (48 h de anticipación, hasta 30 días hacia adelante, solo socios activos, máximo 3 reservas
 * sin comprobante) y liberación de turnos que no se pagaron (a los 60 minutos).
 */
class PanelSocioServicioReservaTest extends PanelSocioServicioPruebaBase {

    private static final String MENSAJE_ANTICIPACION = "Las reservas se hacen con al menos 48 h de anticipación al turno.";

    /** El momento en que empieza el turno del 14/10 a las 10:00. */
    private static final LocalDateTime INICIO = FECHA_TURNO.atTime(10, 0);

    @BeforeEach
    void prepararElCaminoFeliz() {
        lenient().when(socioDAO.buscarPorId(7)).thenReturn(socio);
        lenient().when(turnoDAO.buscarPorId(5)).thenReturn(turno);
        lenient().when(tarifaDAO.buscarVigente(anyInt(), anyInt(), any(LocalDate.class))).thenReturn(tarifa);
        lenient().doAnswer(inv -> {
            ((Reserva) inv.getArgument(0)).setIdReserva(99);
            return null;
        }).when(reservaDAO).insertar(any(Reserva.class));
    }

    private void assertRechazada(String mensajeEsperado, LocalDate fecha, LocalDateTime ahora) {
        ReglaReservaException e = assertThrows(ReglaReservaException.class,
                () -> servicio.reservar(7, 5, fecha, ahora));
        assertEquals(mensajeEsperado, e.getMessage());
        verify(reservaDAO, never()).insertar(any());
    }

    // ---------- anticipación de 48 horas ----------

    @Test
    void reservaConExactamente48HorasDeAnticipacion() {
        ArgumentCaptor<Reserva> guardada = ArgumentCaptor.forClass(Reserva.class);

        Integer id = servicio.reservar(7, 5, FECHA_TURNO, INICIO.minusHours(48));

        assertEquals(99, id);
        verify(reservaDAO).insertar(guardada.capture());
        assertEquals(Reserva.Estado.PENDIENTE_PAGO, guardada.getValue().getEstado());
        assertSame(socio, guardada.getValue().getIdSocio());
        assertSame(turno, guardada.getValue().getIdTurno());
        assertEquals(FECHA_TURNO, guardada.getValue().getFechaTurno());
        assertSame(tarifa, guardada.getValue().getIdTarifa());
        assertEquals(INICIO.minusHours(48), guardada.getValue().getFechaReserva());
    }

    @Test
    void reservaConMasDe48HorasDeAnticipacion() {
        Integer id = servicio.reservar(7, 5, FECHA_TURNO, INICIO.minusHours(49));

        assertEquals(99, id);
    }

    @Test
    void rechazaReservarConMenosDe48HorasDeAnticipacion() {
        assertRechazada(MENSAJE_ANTICIPACION, FECHA_TURNO, INICIO.minusHours(47));
    }

    @Test
    void rechazaReservarConUnMinutoMenosDe48Horas() {
        assertRechazada(MENSAJE_ANTICIPACION, FECHA_TURNO, INICIO.minusHours(48).plusMinutes(1));
    }

    @Test
    void rechazaUnTurnoQueYaPaso() {
        assertRechazada("Ese turno ya pasó.", FECHA_TURNO, INICIO.plusMinutes(30));
    }

    @Test
    void buscaLaTarifaDeLaInstalacionYDelTipoDeSocioParaEseDia() {
        servicio.reservar(7, 5, FECHA_TURNO, INICIO.minusHours(48));

        verify(tarifaDAO).buscarVigente(1, 1, FECHA_TURNO);
    }

    // ---------- hasta 30 días hacia adelante ----------

    @Test
    void aceptaReservarUnTurnoDentroDeLosProximos30Dias() {
        LocalDate dia30 = HOY.plusDays(30);

        Integer id = servicio.reservar(7, 5, dia30, AHORA);

        assertEquals(99, id);
    }

    @Test
    void rechazaReservarUnTurnoAMasDe30DiasDeHoy() {
        assertRechazada("Elegí un día entre hoy y los próximos 30 días.", HOY.plusDays(31), AHORA);
    }

    @Test
    void rechazaReservarUnDiaAnteriorAHoy() {
        assertRechazada("Elegí un día entre hoy y los próximos 30 días.", HOY.minusDays(1), AHORA);
    }

    // ---------- membresía vigente (socio ACTIVO) ----------

    @Test
    void rechazaReservarSiLaMembresiaNoEstaVigente() {
        socio.setEstado(Socio.Estado.NO_ACTIVO);

        assertRechazada("Tu membresía no está vigente: renovala para volver a reservar.", FECHA_TURNO, AHORA);
    }

    @Test
    void rechazaReservarSiElSocioFueDadoDeBaja() {
        socio.setBajaLogica(true);

        assertRechazada("Tu membresía no está vigente: renovala para volver a reservar.", FECHA_TURNO, AHORA);
    }

    @Test
    void rechazaReservarSiElSocioNoExiste() {
        when(socioDAO.buscarPorId(7)).thenReturn(null);

        assertRechazada("Tu membresía no está vigente: renovala para volver a reservar.", FECHA_TURNO, AHORA);
    }

    // ---------- máximo 3 reservas sin comprobante ----------

    @Test
    void aceptaReservarConDosReservasEsperandoElComprobante() {
        when(reservaDAO.contarSinComprobanteDe(7)).thenReturn(2L);

        Integer id = servicio.reservar(7, 5, FECHA_TURNO, AHORA);

        assertEquals(99, id);
    }

    @Test
    void rechazaReservarConTresReservasEsperandoElComprobante() {
        when(reservaDAO.contarSinComprobanteDe(7)).thenReturn(3L);

        assertRechazada("Ya tenés 3 reservas esperando el comprobante: subilo o liberalas antes de pedir otra.",
                FECHA_TURNO, AHORA);
    }

    // ---------- otras reglas de la reserva ----------

    @Test
    void rechazaUnaInstalacionDeAccesoLibre() {
        cancha.setTipoAcceso(Instalacion.TipoAcceso.LIBRE);

        assertRechazada("Esa instalación no se reserva: es de acceso libre.", FECHA_TURNO, AHORA);
    }

    @Test
    void rechazaUnaInstalacionEnMantenimiento() {
        cancha.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);

        assertRechazada("La instalación no está disponible.", FECHA_TURNO, AHORA);
    }

    @Test
    void rechazaSiLaInstalacionNoTienePrecioParaElTipoDeSocio() {
        when(tarifaDAO.buscarVigente(1, 1, FECHA_TURNO)).thenReturn(null);

        assertRechazada("Esa instalación todavía no tiene un precio cargado para tu tipo de socio.", FECHA_TURNO, AHORA);
    }

    @Test
    void rechazaUnTurnoQueYaTomoOtroSocio() {
        Reserva ocupante = DatosDePrueba.reserva(55, DatosDePrueba.socio(8, Socio.Estado.ACTIVO, alumno), turno,
                FECHA_TURNO, Reserva.Estado.CONFIRMADA);
        when(reservaDAO.buscarActivaPorTurnoYFecha(5, FECHA_TURNO)).thenReturn(ocupante);

        assertRechazada("Ese turno acaba de ser reservado por otra persona. Elegí otro.", FECHA_TURNO, AHORA);
    }

    @Test
    void traduceElChoqueConLaRestriccionUnicaDeLaBaseEnUnMensajeParaElSocio() {
        doThrow(new PersistenceException("duplicate key turno_activo_key")).when(reservaDAO).insertar(any(Reserva.class));

        ReglaReservaException e = assertThrows(ReglaReservaException.class,
                () -> servicio.reservar(7, 5, FECHA_TURNO, AHORA));

        assertEquals("Ese turno acaba de ser reservado por otra persona. Elegí otro.", e.getMessage());
    }

    @Test
    void exigeElegirUnTurno() {
        ReglaReservaException e = assertThrows(ReglaReservaException.class,
                () -> servicio.reservar(7, null, FECHA_TURNO, AHORA));

        assertEquals("Elegí el turno que querés reservar.", e.getMessage());
    }

    // ---------- valoración pendiente (bloquea reservar) ----------

    private Reserva terminadaConfirmada(int id, LocalDate fecha) {
        return DatosDePrueba.reserva(id, socio, turno, fecha, Reserva.Estado.CONFIRMADA);
    }

    @Test
    void rechazaReservarSiHayUnTurnoTerminadoSinValorar() {
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(terminadaConfirmada(20, LocalDate.of(2026, 10, 10))));

        assertRechazada("Tenés 1 valoración pendiente: valorá tu turno anterior para poder reservar de nuevo.",
                FECHA_TURNO, AHORA);
    }

    @Test
    void noExigeValorarUnTurnoQueYaFueValorado() {
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(terminadaConfirmada(20, LocalDate.of(2026, 10, 10))));
        when(valoracionDAO.puntajesDelSocio(7)).thenReturn(Map.of(20, 5));

        Integer id = servicio.reservar(7, 5, FECHA_TURNO, AHORA);

        assertEquals(99, id);
    }

    @Test
    void noExigeValorarTurnosAnterioresALaFuncionDeValoraciones() {
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(terminadaConfirmada(21, LocalDate.of(2026, 9, 24))));

        Integer id = servicio.reservar(7, 5, FECHA_TURNO, AHORA);

        assertEquals(99, id);
    }

    // ---------- el socio suelta una reserva que no pagó ----------

    private Reserva esperandoElComprobante() {
        Reserva r = DatosDePrueba.reserva(10, socio, turno, FECHA_TURNO, Reserva.Estado.PENDIENTE_PAGO);
        r.setIdTarifa(tarifa);
        return r;
    }

    @Test
    void liberaUnaReservaPropiaQueEsperaElComprobante() {
        Reserva r = esperandoElComprobante();
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        servicio.liberar(7, 10);

        assertEquals(Reserva.Estado.CANCELADA, r.getEstado());
    }

    @Test
    void noLiberaUnaReservaQueYaTieneComprobante() {
        Reserva r = esperandoElComprobante();
        r.setIdPago(DatosDePrueba.pago(1, "20000", Pago.Estado.PENDIENTE_VALIDACION));
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        ReglaReservaException e = assertThrows(ReglaReservaException.class, () -> servicio.liberar(7, 10));

        assertEquals("Esa reserva ya tiene un comprobante: no se puede liberar.", e.getMessage());
        assertEquals(Reserva.Estado.PENDIENTE_PAGO, r.getEstado());
    }

    @Test
    void noLiberaUnaReservaDeOtroSocio() {
        when(reservaDAO.buscarParaResolver(10)).thenReturn(esperandoElComprobante());

        ReglaReservaException e = assertThrows(ReglaReservaException.class, () -> servicio.liberar(8, 10));

        assertEquals("La reserva no existe.", e.getMessage());
    }

    @Test
    void noLiberaUnaReservaQueYaNoEsperaElPago() {
        Reserva r = esperandoElComprobante();
        r.setEstado(Reserva.Estado.CONFIRMADA);
        when(reservaDAO.buscarParaResolver(10)).thenReturn(r);

        ReglaReservaException e = assertThrows(ReglaReservaException.class, () -> servicio.liberar(7, 10));

        assertEquals("Esa reserva ya no espera el pago (si pasó el plazo, el turno se liberó).", e.getMessage());
    }

    // ---------- liberación automática a los 60 minutos ----------

    /**
     * El servicio le pide al DAO las reservas creadas antes de "ahora - 60 minutos". Que el corte sea estricto
     * (exactamente 60 min no se libera) vive en la consulta JPQL del DAO y no se puede probar sin base de datos.
     */
    @Test
    void liberaLasReservasSinComprobanteCreadasAntesDeLos60Minutos() {
        Reserva a = esperandoElComprobante();
        Reserva b = DatosDePrueba.reserva(11, socio, turno, FECHA_TURNO.plusDays(1), Reserva.Estado.PENDIENTE_PAGO);
        when(reservaDAO.listarPendientesSinComprobanteAntesDe(AHORA.minusMinutes(60))).thenReturn(List.of(a, b));
        when(reservaDAO.buscarParaResolver(10)).thenReturn(a);
        when(reservaDAO.buscarParaResolver(11)).thenReturn(b);

        int liberadas = servicio.liberarVencidas(AHORA);

        assertEquals(2, liberadas);
        assertEquals(Reserva.Estado.CANCELADA, a.getEstado());
        assertEquals(Reserva.Estado.CANCELADA, b.getEstado());
    }

    @Test
    void noLiberaLaReservaCuyoComprobanteSeSubioJustoAntes() {
        Reserva enLista = esperandoElComprobante();
        Reserva bloqueada = esperandoElComprobante(); // la misma reserva, ya con pago al releerla con bloqueo
        bloqueada.setIdPago(DatosDePrueba.pago(1, "20000", Pago.Estado.PENDIENTE_VALIDACION));
        when(reservaDAO.listarPendientesSinComprobanteAntesDe(AHORA.minusMinutes(60))).thenReturn(List.of(enLista));
        when(reservaDAO.buscarParaResolver(10)).thenReturn(bloqueada);

        int liberadas = servicio.liberarVencidas(AHORA);

        assertEquals(0, liberadas);
        assertEquals(Reserva.Estado.PENDIENTE_PAGO, bloqueada.getEstado());
    }

    @Test
    void sinReservasVencidasNoLiberaNada() {
        when(reservaDAO.listarPendientesSinComprobanteAntesDe(AHORA.minusMinutes(60))).thenReturn(List.of());

        int liberadas = servicio.liberarVencidas(AHORA);

        assertEquals(0, liberadas);
        verify(reservaDAO, never()).buscarParaResolver(any());
    }

    @Test
    void elPrimerDiaReservableEsElDeDentroDeDosDias() {
        assertEquals(HOY.plusDays(2), PanelSocioServicio.primerDiaReservable(HOY));
    }
}
