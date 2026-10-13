package com.sigrid.sigrid.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.CarnetDigitalDAO;
import com.sigrid.sigrid.dao.CategoriaSocioDAO;
import com.sigrid.sigrid.dao.SuscripcionSocioDAO;
import com.sigrid.sigrid.dao.TarifaMembresiaDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.repositorio.CarnetDigital;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import com.sigrid.sigrid.repositorio.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Membresías: vigencia de un mes desde la validación (o desde que termina la vigente), y vencimiento que deja al
 * socio NO_ACTIVO y a su carnet INACTIVO. La fecha exacta del corte "vencida antes de hoy" vive en la consulta del
 * DAO y no se puede probar sin base de datos.
 */
@ExtendWith(MockitoExtension.class)
class MembresiasServicioTest {

    @Mock
    private SuscripcionSocioDAO suscripcionDAO;
    @Mock
    private CarnetDigitalDAO carnetDAO;
    @Mock
    private TarifaMembresiaDAO tarifaDAO;
    @Mock
    private CategoriaSocioDAO categoriaDAO;
    @Mock
    private UsuarioDAO usuarioDAO;

    @InjectMocks
    private MembresiasServicio servicio;

    private Socio socio;
    private CarnetDigital carnet;
    private Pago pago;
    private SuscripcionSocio solicitud;
    private Usuario admin;

    @BeforeEach
    void armarUnSocioConUnaSolicitudDeMembresiaPorValidar() {
        socio = DatosDePrueba.socio(7, Socio.Estado.NO_ACTIVO, DatosDePrueba.categoria(1, "Alumno UNSE", true));
        carnet = new CarnetDigital(70);
        carnet.setIdSocio(socio);
        carnet.setEstado(CarnetDigital.Estado.INACTIVO);
        pago = DatosDePrueba.pago(1, "5000", Pago.Estado.PENDIENTE_VALIDACION);
        solicitud = suscripcion(30, socio, SuscripcionSocio.Estado.PENDIENTE_PAGO, null, null);
        solicitud.setIdPago(pago);
        admin = DatosDePrueba.usuario(1, "Marcela", "Ibarra", "admin@polideportivo.ejemplo");

        lenient().when(suscripcionDAO.buscarParaResolver(30)).thenReturn(solicitud);
        lenient().when(carnetDAO.buscarPorSocio(7)).thenReturn(carnet);
        lenient().when(usuarioDAO.buscarPorId(1)).thenReturn(admin);
    }

    private static SuscripcionSocio suscripcion(int id, Socio socio, SuscripcionSocio.Estado estado, LocalDate inicio,
            LocalDate vencimiento) {
        SuscripcionSocio s = new SuscripcionSocio(id);
        s.setIdSocio(socio);
        s.setEstado(estado);
        s.setFechaInicio(inicio);
        s.setFechaVencimiento(vencimiento);
        return s;
    }

    private SuscripcionSocio vigenteHasta(int id, LocalDate vencimiento) {
        socio.setEstado(Socio.Estado.ACTIVO);
        return suscripcion(id, socio, SuscripcionSocio.Estado.VIGENTE, vencimiento.minusMonths(1), vencimiento);
    }

    // ---------- aceptar el comprobante: vigencia de un mes ----------

    @DisplayName("CP-32: la suscripción dura un mes desde la validación")
    @Test
    void laMembresiaEmpiezaElDiaDeLaValidacionYVenceUnMesDespues() {
        LocalDate hoy = LocalDate.of(2026, 9, 23);

        LocalDate vence = servicio.aceptar(30, 1, hoy);

        assertEquals(LocalDate.of(2026, 10, 23), vence);
        assertEquals(hoy, solicitud.getFechaInicio());
        assertEquals(LocalDate.of(2026, 10, 23), solicitud.getFechaVencimiento());
        assertEquals(SuscripcionSocio.Estado.VIGENTE, solicitud.getEstado());
    }

    @Test
    void siSePagaDespuesDelVencimientoLaMembresiaEmpiezaElDiaDelPago() {
        // la anterior venció el 23/10 y no hay otra vigente: la nueva arranca el día que se valida el pago
        LocalDate hoy = LocalDate.of(2026, 10, 27);

        LocalDate vence = servicio.aceptar(30, 1, hoy);

        assertEquals(hoy, solicitud.getFechaInicio());
        assertEquals(LocalDate.of(2026, 11, 27), vence);
    }

    @Test
    void unMesDesdeElTreintaYUnoDeEneroTerminaElUltimoDiaDeFebrero() {
        LocalDate vence = servicio.aceptar(30, 1, LocalDate.of(2026, 1, 31));

        assertEquals(LocalDate.of(2026, 2, 28), vence);
    }

    @DisplayName("CP-32: al validar, el socio queda ACTIVO y su carnet activo")
    @Test
    void alAceptarElSocioQuedaActivoYSuCarnetSeActiva() {
        servicio.aceptar(30, 1, LocalDate.of(2026, 9, 23));

        assertEquals(Socio.Estado.ACTIVO, socio.getEstado());
        assertEquals(CarnetDigital.Estado.ACTIVO, carnet.getEstado());
    }

    @Test
    void alAceptarElPagoQuedaConfirmadoConQuienLoValido() {
        LocalDateTime antes = LocalDateTime.now();

        servicio.aceptar(30, 1, LocalDate.of(2026, 9, 23));

        assertEquals(Pago.Estado.CONFIRMADO, pago.getEstado());
        assertSame(admin, pago.getIdAdminValidador());
        assertNotNull(pago.getFechaValidacion());
        assertFalse(pago.getFechaValidacion().isBefore(antes));
    }

    @Test
    void alAceptarSinCarnetNoFallaYElSocioQuedaActivo() {
        when(carnetDAO.buscarPorSocio(7)).thenReturn(null);

        LocalDate vence = servicio.aceptar(30, 1, LocalDate.of(2026, 9, 23));

        assertNotNull(vence);
        assertEquals(Socio.Estado.ACTIVO, socio.getEstado());
    }

    @Test
    void laRenovacionPorAdelantadoNuncaEmpiezaAntesDeQueTermineLaVigente() {
        SuscripcionSocio vigente = vigenteHasta(29, LocalDate.of(2026, 10, 23));
        LocalDate hoy = LocalDate.of(2026, 10, 20);
        when(suscripcionDAO.listarVigentesDelSocio(7, hoy)).thenReturn(List.of(vigente));

        LocalDate vence = servicio.aceptar(30, 1, hoy);

        assertFalse(solicitud.getFechaInicio().isBefore(vigente.getFechaVencimiento()),
                "la nueva no puede pisar los días de la vigente");
        assertEquals(solicitud.getFechaInicio().plusMonths(1), vence);
    }

    /**
     * DEFECTO CONFIRMADO D-3 (confirmado por el responsable del proyecto el 12/10/2026): la membresía vence el mismo
     * día del mes siguiente (empezó el 23/09, vence el 23/10) y quien renueva antes de vencer continúa el ciclo: la
     * nueva empieza el mismo día en que vence la vigente (23/10) y vence un mes después (23/11). El código arranca la
     * nueva el día SIGUIENTE ({@code getFechaVencimiento().plusDays(1)}, MembresiasServicio.aceptar), de modo que cada
     * renovación corre el ciclo un día (23/10 -> 24/10 -> 24/11). La prueba expresa la regla correcta y está
     * deshabilitada porque el código actual no la cumple; al corregirlo hay que quitarle el {@code @Disabled}.
     */
    @Disabled("Defecto D-3 confirmado: la renovación por adelantado empieza el día siguiente al vencimiento, no el mismo día")
    @DisplayName("CP-32: la renovación por adelantado empieza el día del vencimiento (D-3)")
    @Test
    void laRenovacionPorAdelantadoMantieneElMismoDiaDelMes() {
        SuscripcionSocio vigente = vigenteHasta(29, LocalDate.of(2026, 10, 23));
        LocalDate hoy = LocalDate.of(2026, 10, 20);
        when(suscripcionDAO.listarVigentesDelSocio(7, hoy)).thenReturn(List.of(vigente));

        LocalDate vence = servicio.aceptar(30, 1, hoy);

        assertEquals(LocalDate.of(2026, 10, 23), solicitud.getFechaInicio());
        assertEquals(LocalDate.of(2026, 11, 23), vence);
    }

    /** Mismo defecto D-3, en el día límite: se paga el mismo día en que vence la vigente ("perfecto", dijo el cliente). */
    @Disabled("Defecto D-3 confirmado: pagando el mismo día del vencimiento la nueva empieza al día siguiente")
    @DisplayName("CP-32: pagando el día del vencimiento la nueva empieza ese día (D-3)")
    @Test
    void siSePagaElMismoDiaDelVencimientoLaNuevaEmpiezaEseDia() {
        SuscripcionSocio vigente = vigenteHasta(29, LocalDate.of(2026, 10, 23));
        LocalDate hoy = LocalDate.of(2026, 10, 23);
        when(suscripcionDAO.listarVigentesDelSocio(7, hoy)).thenReturn(List.of(vigente));

        LocalDate vence = servicio.aceptar(30, 1, hoy);

        assertEquals(LocalDate.of(2026, 10, 23), solicitud.getFechaInicio());
        assertEquals(LocalDate.of(2026, 11, 23), vence);
    }

    @Test
    void noAceptaUnaSolicitudQueYaEstabaResuelta() {
        solicitud.setEstado(SuscripcionSocio.Estado.VIGENTE);

        LocalDate vence = servicio.aceptar(30, 1, LocalDate.of(2026, 9, 23));

        assertNull(vence);
        assertEquals(Socio.Estado.NO_ACTIVO, socio.getEstado());
    }

    @Test
    void noAceptaSiElPagoYaFueValidado() {
        pago.setEstado(Pago.Estado.CONFIRMADO);

        assertNull(servicio.aceptar(30, 1, LocalDate.of(2026, 9, 23)));
    }

    @Test
    void noAceptaUnaSolicitudSinComprobante() {
        solicitud.setIdPago(null);

        assertNull(servicio.aceptar(30, 1, LocalDate.of(2026, 9, 23)));
    }

    @Test
    void noAceptaUnaSolicitudInexistente() {
        when(suscripcionDAO.buscarParaResolver(99)).thenReturn(null);

        assertNull(servicio.aceptar(99, 1, LocalDate.of(2026, 9, 23)));
    }

    // ---------- denegar el comprobante ----------

    @Test
    void alDenegarElPagoQuedaRechazadoYLaSolicitudCancelada() {
        boolean denegada = servicio.denegar(30, 1);

        assertTrue(denegada);
        assertEquals(Pago.Estado.RECHAZADO, pago.getEstado());
        assertEquals(SuscripcionSocio.Estado.CANCELADA, solicitud.getEstado());
        assertEquals(Socio.Estado.NO_ACTIVO, socio.getEstado());
        assertEquals(CarnetDigital.Estado.INACTIVO, carnet.getEstado());
    }

    @Test
    void noDeniegaUnaSolicitudQueYaEstabaResuelta() {
        solicitud.setEstado(SuscripcionSocio.Estado.CANCELADA);

        assertFalse(servicio.denegar(30, 1));
        assertEquals(Pago.Estado.PENDIENTE_VALIDACION, pago.getEstado());
    }

    // ---------- baja voluntaria de una membresía vigente ----------

    @Test
    void cancelarLaUnicaVigenteCongelaAlSocio() {
        LocalDate hoy = LocalDate.of(2026, 10, 12);
        SuscripcionSocio vigente = vigenteHasta(40, LocalDate.of(2026, 10, 30));
        carnet.setEstado(CarnetDigital.Estado.ACTIVO);
        when(suscripcionDAO.buscarParaResolver(40)).thenReturn(vigente);
        when(suscripcionDAO.listarVigentesDelSocio(7, hoy)).thenReturn(List.of(vigente));

        Boolean congelado = servicio.cancelar(40, hoy);

        assertTrue(congelado);
        assertEquals(SuscripcionSocio.Estado.CANCELADA, vigente.getEstado());
        assertEquals(Socio.Estado.NO_ACTIVO, socio.getEstado());
        assertEquals(CarnetDigital.Estado.INACTIVO, carnet.getEstado());
    }

    @Test
    void cancelarUnaVigenteNoCongelaAlSocioSiTieneOtraRenovada() {
        LocalDate hoy = LocalDate.of(2026, 10, 12);
        SuscripcionSocio vigente = vigenteHasta(40, LocalDate.of(2026, 10, 30));
        SuscripcionSocio renovada = vigenteHasta(41, LocalDate.of(2026, 11, 30));
        carnet.setEstado(CarnetDigital.Estado.ACTIVO);
        when(suscripcionDAO.buscarParaResolver(40)).thenReturn(vigente);
        when(suscripcionDAO.listarVigentesDelSocio(7, hoy)).thenReturn(List.of(renovada, vigente));

        Boolean congelado = servicio.cancelar(40, hoy);

        assertFalse(congelado);
        assertEquals(Socio.Estado.ACTIVO, socio.getEstado());
        assertEquals(CarnetDigital.Estado.ACTIVO, carnet.getEstado());
    }

    @Test
    void noCancelaUnaMembresiaQueNoEstaVigente() {
        solicitud.setEstado(SuscripcionSocio.Estado.VENCIDA);

        assertNull(servicio.cancelar(30, LocalDate.of(2026, 10, 12)));
    }

    // ---------- vencimiento (lo ejecuta el job cada hora) ----------

    @DisplayName("CP-33: al vencer, suscripción VENCIDA, socio NO_ACTIVO y carnet inactivo")
    @Test
    void alVencerLaUnicaVigenteElSocioQuedaNoActivoYElCarnetInactivo() {
        LocalDate hoy = LocalDate.of(2026, 10, 24);
        SuscripcionSocio vencida = vigenteHasta(40, LocalDate.of(2026, 10, 23));
        carnet.setEstado(CarnetDigital.Estado.ACTIVO);
        when(suscripcionDAO.listarVigentesVencidasAntesDe(hoy)).thenReturn(List.of(vencida));

        int cantidad = servicio.vencerVencidas(hoy);

        assertEquals(1, cantidad);
        assertEquals(SuscripcionSocio.Estado.VENCIDA, vencida.getEstado());
        assertEquals(Socio.Estado.NO_ACTIVO, socio.getEstado());
        assertEquals(CarnetDigital.Estado.INACTIVO, carnet.getEstado());
    }

    @Test
    void alVencerNoCongelaAlSocioQueYaRenovo() {
        LocalDate hoy = LocalDate.of(2026, 10, 24);
        SuscripcionSocio vencida = vigenteHasta(40, LocalDate.of(2026, 10, 23));
        SuscripcionSocio renovada = vigenteHasta(41, LocalDate.of(2026, 11, 23));
        carnet.setEstado(CarnetDigital.Estado.ACTIVO);
        when(suscripcionDAO.listarVigentesVencidasAntesDe(hoy)).thenReturn(List.of(vencida));
        when(suscripcionDAO.listarVigentesDelSocio(7, hoy)).thenReturn(List.of(renovada));

        int cantidad = servicio.vencerVencidas(hoy);

        assertEquals(1, cantidad);
        assertEquals(SuscripcionSocio.Estado.VENCIDA, vencida.getEstado());
        assertEquals(Socio.Estado.ACTIVO, socio.getEstado());
        assertEquals(CarnetDigital.Estado.ACTIVO, carnet.getEstado());
    }

    @Test
    void alVencerCongelaACadaSocioQueQuedoSinMembresia() {
        LocalDate hoy = LocalDate.of(2026, 10, 24);
        Socio otro = DatosDePrueba.socio(8, Socio.Estado.ACTIVO, DatosDePrueba.categoria(2, "Externo", false));
        CarnetDigital carnetOtro = new CarnetDigital(80);
        carnetOtro.setEstado(CarnetDigital.Estado.ACTIVO);
        SuscripcionSocio primera = vigenteHasta(40, LocalDate.of(2026, 10, 23));
        SuscripcionSocio segunda = suscripcion(42, otro, SuscripcionSocio.Estado.VIGENTE, LocalDate.of(2026, 9, 23),
                LocalDate.of(2026, 10, 23));
        when(suscripcionDAO.listarVigentesVencidasAntesDe(hoy)).thenReturn(List.of(primera, segunda));
        when(carnetDAO.buscarPorSocio(8)).thenReturn(carnetOtro);

        int cantidad = servicio.vencerVencidas(hoy);

        assertEquals(2, cantidad);
        assertEquals(Socio.Estado.NO_ACTIVO, socio.getEstado());
        assertEquals(Socio.Estado.NO_ACTIVO, otro.getEstado());
        assertEquals(CarnetDigital.Estado.INACTIVO, carnetOtro.getEstado());
    }

    @Test
    void alVencerNoFallaSiElSocioNoTieneCarnet() {
        LocalDate hoy = LocalDate.of(2026, 10, 24);
        SuscripcionSocio vencida = vigenteHasta(40, LocalDate.of(2026, 10, 23));
        when(suscripcionDAO.listarVigentesVencidasAntesDe(hoy)).thenReturn(List.of(vencida));
        when(carnetDAO.buscarPorSocio(7)).thenReturn(null);

        int cantidad = servicio.vencerVencidas(hoy);

        assertEquals(1, cantidad);
        assertEquals(Socio.Estado.NO_ACTIVO, socio.getEstado());
    }

    @Test
    void sinMembresiasVencidasNoCambiaNada() {
        LocalDate hoy = LocalDate.of(2026, 10, 24);
        socio.setEstado(Socio.Estado.ACTIVO);
        when(suscripcionDAO.listarVigentesVencidasAntesDe(hoy)).thenReturn(List.of());

        int cantidad = servicio.vencerVencidas(hoy);

        assertEquals(0, cantidad);
        assertEquals(Socio.Estado.ACTIVO, socio.getEstado());
    }

    @Test
    void noCancelaUnaMembresiaInexistente() {
        assertNull(servicio.cancelar(99, LocalDate.of(2026, 10, 12)));
    }
}
