package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.ValoracionReserva;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/**
 * Valoración de una reserva: puntaje de 1 a 5, comentario de máximo 500 caracteres (se recorta, regla confirmada),
 * una sola valoración por reserva y solo de un turno confirmado que ya terminó.
 */
class PanelSocioServicioValoracionTest extends PanelSocioServicioPruebaBase {

    private static final String MENSAJE_PUNTAJE = "Elegí de 1 a 5 estrellas para valorar.";

    private Reserva reserva;

    @BeforeEach
    void prepararUnTurnoTerminado() {
        // turno del 10/10 de 10:00 a 11:00, confirmado: ya terminó respecto de AHORA (12/10 10:00)
        reserva = DatosDePrueba.reserva(10, socio, turno, HOY.minusDays(2), Reserva.Estado.CONFIRMADA);
        lenient().when(reservaDAO.buscarPorId(10)).thenReturn(reserva);
        lenient().when(valoracionDAO.insertar(any(ValoracionReserva.class))).thenReturn(true);
    }

    private ValoracionReserva valoracionGuardada() {
        ArgumentCaptor<ValoracionReserva> guardada = ArgumentCaptor.forClass(ValoracionReserva.class);
        verify(valoracionDAO).insertar(guardada.capture());
        return guardada.getValue();
    }

    private void assertRechazada(String mensajeEsperado, Integer idSocio, Integer idReserva, Integer puntaje) {
        ReglaReservaException e = assertThrows(ReglaReservaException.class,
                () -> servicio.valorar(idSocio, idReserva, puntaje, "Muy buena", AHORA));
        assertEquals(mensajeEsperado, e.getMessage());
        verify(valoracionDAO, never()).insertar(any());
    }

    // ---------- puntaje de 1 a 5 ----------

    @Test
    void guardaLaValoracionConSuPuntajeComentarioYFecha() {
        servicio.valorar(7, 10, 4, "Buena cancha, el césped estaba parejo", AHORA);

        ValoracionReserva v = valoracionGuardada();
        assertEquals(4, v.getPuntaje());
        assertEquals("Buena cancha, el césped estaba parejo", v.getComentario());
        assertSame(reserva, v.getIdReserva());
        assertEquals(AHORA, v.getFechaValoracion());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void aceptaLosPuntajesDe1A5(int puntaje) {
        servicio.valorar(7, 10, puntaje, null, AHORA);

        assertEquals(puntaje, valoracionGuardada().getPuntaje());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 6, 10})
    void rechazaLosPuntajesFueraDeRango(int puntaje) {
        assertRechazada(MENSAJE_PUNTAJE, 7, 10, puntaje);
    }

    @Test
    void rechazaUnPuntajeNulo() {
        assertRechazada(MENSAJE_PUNTAJE, 7, 10, null);
    }

    // ---------- comentario de hasta 500 caracteres ----------

    @Test
    void guardaCompletoUnComentarioDeExactamente500Caracteres() {
        String comentario = "a".repeat(500);

        servicio.valorar(7, 10, 5, comentario, AHORA);

        assertEquals(comentario, valoracionGuardada().getComentario());
    }

    @Test
    void recortaA500CaracteresUnComentarioMasLargo() {
        servicio.valorar(7, 10, 5, "a".repeat(501), AHORA);

        assertEquals("a".repeat(500), valoracionGuardada().getComentario());
    }

    @Test
    void guardaComoNuloUnComentarioEnBlanco() {
        servicio.valorar(7, 10, 5, "    ", AHORA);

        assertNull(valoracionGuardada().getComentario());
    }

    @Test
    void guardaComoNuloUnComentarioNulo() {
        servicio.valorar(7, 10, 5, null, AHORA);

        assertNull(valoracionGuardada().getComentario());
    }

    @Test
    void quitaLosEspaciosDeLosExtremosDelComentario() {
        servicio.valorar(7, 10, 5, "   Muy buena atención   ", AHORA);

        assertEquals("Muy buena atención", valoracionGuardada().getComentario());
    }

    // ---------- una sola valoración por reserva ----------

    @Test
    void rechazaValorarUnaReservaQueYaFueValorada() {
        when(valoracionDAO.buscarPorReserva(10)).thenReturn(new ValoracionReserva(1));

        assertRechazada("Ya valoraste esta reserva.", 7, 10, 5);
    }

    @Test
    void rechazaLaValoracionSiLaBaseDetectaQueYaExistia() {
        when(valoracionDAO.insertar(any(ValoracionReserva.class))).thenReturn(false);

        ReglaReservaException e = assertThrows(ReglaReservaException.class,
                () -> servicio.valorar(7, 10, 5, "Muy buena", AHORA));

        assertEquals("Ya valoraste esta reserva.", e.getMessage());
    }

    // ---------- solo un turno confirmado y terminado, propio ----------

    @Test
    void rechazaValorarUnTurnoQueTodaviaNoTermino() {
        reserva.setFechaTurno(HOY); // turno de hoy de 10:00 a 11:00
        LocalDateTime unMinutoAntesDelFin = HOY.atTime(10, 59);

        ReglaReservaException e = assertThrows(ReglaReservaException.class,
                () -> servicio.valorar(7, 10, 5, "Muy buena", unMinutoAntesDelFin));

        assertEquals("Solo se puede valorar una reserva confirmada cuyo turno ya terminó.", e.getMessage());
        verify(valoracionDAO, never()).insertar(any());
    }

    @Test
    void aceptaValorarJustoCuandoTerminaElTurno() {
        reserva.setFechaTurno(HOY);

        servicio.valorar(7, 10, 5, "Muy buena", HOY.atTime(11, 0));

        assertEquals(5, valoracionGuardada().getPuntaje());
    }

    @Test
    void rechazaValorarUnaReservaQueNoEstaConfirmada() {
        reserva.setEstado(Reserva.Estado.CANCELADA);

        assertRechazada("Solo se puede valorar una reserva confirmada cuyo turno ya terminó.", 7, 10, 5);
    }

    @Test
    void rechazaValorarUnaReservaDeOtroSocio() {
        assertRechazada("La reserva no existe.", 8, 10, 5);
    }

    @Test
    void rechazaValorarUnaReservaInexistente() {
        when(reservaDAO.buscarPorId(99)).thenReturn(null);

        assertRechazada("La reserva no existe.", 7, 99, 5);
    }
}
