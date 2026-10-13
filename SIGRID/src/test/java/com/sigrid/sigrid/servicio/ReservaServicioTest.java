package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.ComprobantePagoDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.Turno;
import com.sigrid.sigrid.repositorio.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Resolución de solicitudes de reserva por el administrador (confirmar o rechazar) y textos auxiliares del listado.
 * Al resolver, el servicio usa el reloj del sistema (LocalDateTime.now()), por eso la fecha de validación se verifica
 * contra un rango "antes/después" y no contra un valor fijo.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServicioTest {

    @Mock
    private ReservaDAO reservaDAO;
    @Mock
    private UsuarioDAO usuarioDAO;
    @Mock
    private ComprobantePagoDAO comprobanteDAO;

    @InjectMocks
    private ReservaServicio servicio;

    private Reserva reserva;
    private Pago pago;
    private Usuario admin;

    @BeforeEach
    void armarUnaSolicitudConComprobante() {
        Socio socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO, DatosDePrueba.categoria(1, "Alumno UNSE", true));
        Instalacion cancha = DatosDePrueba.instalacion(1, "Cancha de Fútbol 11", Instalacion.TipoAcceso.ARANCELADO,
                Instalacion.Estado.HABILITADA);
        Turno turno = DatosDePrueba.turno(5, cancha, LocalTime.of(18, 0), LocalTime.of(19, 0));
        reserva = DatosDePrueba.reserva(10, socio, turno, HOY.plusDays(5), Reserva.Estado.PENDIENTE_PAGO);
        pago = DatosDePrueba.pago(1, "20000", Pago.Estado.PENDIENTE_VALIDACION);
        reserva.setIdPago(pago);
        admin = DatosDePrueba.usuario(1, "Marcela", "Ibarra", "admin@polideportivo.ejemplo");
        lenient().when(reservaDAO.buscarParaResolver(10)).thenReturn(reserva);
    }

    // ---------- confirmar ----------

    @DisplayName("CP-19: el administrador confirma: reserva y pago CONFIRMADOS con administrador y fecha")
    @Test
    void confirmarDejaLaReservaYElPagoConfirmadosConQuienLosValido() {
        when(usuarioDAO.buscarPorId(1)).thenReturn(admin);
        LocalDateTime antes = LocalDateTime.now();

        boolean confirmada = servicio.confirmar(10, 1);

        assertTrue(confirmada);
        assertEquals(Reserva.Estado.CONFIRMADA, reserva.getEstado());
        assertEquals(Pago.Estado.CONFIRMADO, pago.getEstado());
        assertSame(admin, reserva.getIdAdminConfirmador());
        assertSame(admin, pago.getIdAdminValidador());
        assertNotNull(reserva.getFechaConfirmacion());
        assertFalse(reserva.getFechaConfirmacion().isBefore(antes));
        assertFalse(pago.getFechaValidacion().isBefore(antes));
    }

    // ---------- rechazar ----------

    @DisplayName("CP-20: rechazar deja la reserva RECHAZADA y el turno libre")
    @Test
    void rechazarLiberaElTurnoYDejaElPagoRechazado() {
        boolean rechazada = servicio.rechazar(10, 1);

        assertTrue(rechazada);
        assertEquals(Reserva.Estado.RECHAZADA, reserva.getEstado());
        assertEquals(Pago.Estado.RECHAZADO, pago.getEstado());
        assertNull(reserva.getFechaConfirmacion());
    }

    @DisplayName("CP-20: rechazar con motivo guarda el motivo")
    @Test
    void rechazarGuardaElMotivoSinEspaciosSobrantes() {
        servicio.rechazar(10, 1, "  El monto no coincide con la transferencia  ");

        assertEquals("El monto no coincide con la transferencia", pago.getMotivoRechazo());
    }

    @Test
    void rechazarRecortaElMotivoA255Caracteres() {
        servicio.rechazar(10, 1, "m".repeat(300));

        assertEquals(255, pago.getMotivoRechazo().length());
    }

    @Test
    void rechazarConMotivoEnBlancoNoGuardaMotivo() {
        servicio.rechazar(10, 1, "   ");

        assertNull(pago.getMotivoRechazo());
    }

    /**
     * RF-05 y CP-20 del plan: el rechazo exige un motivo. Hoy el código lo deja opcional (la pantalla dice "Motivo
     * (opcional)"). El equipo decidió (13/10/2026) dejarlo así por ahora y exigirlo más adelante: la prueba queda
     * deshabilitada; al implementarlo hay que quitarle el {@code @Disabled} y actualizar
     * {@link #rechazarConMotivoEnBlancoNoGuardaMotivo()}.
     */
    @Disabled("D-5 diferido por decisión del equipo (13/10/2026): el motivo sigue siendo opcional; el plan (RF-05, CP-20) lo exige")
    @DisplayName("CP-20: rechazar sin motivo no se permite")
    @Test
    void rechazarSinMotivoNoSePermiteYLaSolicitudSigueEsperando() {
        boolean rechazada = servicio.rechazar(10, 1, "   ");

        assertFalse(rechazada);
        assertEquals(Reserva.Estado.PENDIENTE_PAGO, reserva.getEstado());
        assertEquals(Pago.Estado.PENDIENTE_VALIDACION, pago.getEstado());
    }

    // ---------- solicitudes que ya no se pueden resolver ----------

    @Test
    void noResuelveUnaSolicitudQueYaEstabaConfirmada() {
        reserva.setEstado(Reserva.Estado.CONFIRMADA);

        assertFalse(servicio.confirmar(10, 1));
        assertFalse(servicio.rechazar(10, 1));
        assertEquals(Pago.Estado.PENDIENTE_VALIDACION, pago.getEstado());
    }

    @Test
    void noResuelveUnaReservaSinComprobante() {
        reserva.setIdPago(null);

        assertFalse(servicio.confirmar(10, 1));
        assertEquals(Reserva.Estado.PENDIENTE_PAGO, reserva.getEstado());
    }

    @Test
    void noResuelveUnaSolicitudConElPagoYaValidado() {
        pago.setEstado(Pago.Estado.CONFIRMADO);

        assertFalse(servicio.confirmar(10, 1));
    }

    @Test
    void noResuelveUnaReservaInexistente() {
        when(reservaDAO.buscarParaResolver(99)).thenReturn(null);

        assertFalse(servicio.confirmar(99, 1));
    }

    // ---------- textos auxiliares ----------

    @ParameterizedTest
    @CsvSource({"0,recién", "1,hace 1 min", "59,hace 59 min", "60,hace 1 h", "1439,hace 23 h", "1440,hace 1 día",
        "2880,hace 2 días"})
    void textoEsperaUsaMinutosHorasYDias(long minutos, String esperado) {
        assertEquals(esperado, ReservaServicio.textoEspera(minutos));
    }

    @Test
    void cuandoDiceHoyYMananaParaLosDosPrimerosDias() {
        assertEquals("Hoy", ReservaServicio.cuando(HOY, HOY));
        assertEquals("Mañana", ReservaServicio.cuando(HOY.plusDays(1), HOY));
    }

    @Test
    void cuandoMuestraElDiaDeLaSemanaYLaFechaParaElResto() {
        assertEquals("vie 16/10/2026", ReservaServicio.cuando(LocalDate.of(2026, 10, 16), HOY));
    }

    @Test
    void normalizarQuitaTildesYMayusculasParaBuscar() {
        assertEquals("jose nunez", ReservaServicio.normalizar("  José Núñez "));
    }
}
