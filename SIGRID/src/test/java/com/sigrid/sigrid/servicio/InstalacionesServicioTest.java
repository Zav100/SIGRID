package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.TarifaAlquilerDAO;
import com.sigrid.sigrid.dao.TurnoDAO;
import com.sigrid.sigrid.dto.Conflictos;
import com.sigrid.sigrid.dto.Demanda;
import com.sigrid.sigrid.dto.FichaInstalacion;
import com.sigrid.sigrid.dto.PrecioFila;
import com.sigrid.sigrid.dto.PrecioVigente;
import com.sigrid.sigrid.dto.RankingItem;
import com.sigrid.sigrid.dto.ReservaFila;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.TarifaAlquiler;
import com.sigrid.sigrid.repositorio.Turno;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
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
 * Instalaciones del administrador: sacar una cancha de servicio (mantenimiento o baja) sin pisar reservas, reactivarla,
 * editarla, y los cálculos de ocupación, demanda, ranking y precios que muestra la vista.
 */
@ExtendWith(MockitoExtension.class)
class InstalacionesServicioTest {

    @Mock
    private InstalacionDAO instalacionDAO;
    @Mock
    private TurnoDAO turnoDAO;
    @Mock
    private TarifaAlquilerDAO tarifaDAO;
    @Mock
    private TarifasServicio tarifasServicio;
    @Mock
    private ReservaDAO reservaDAO;
    @Mock
    private HorarioServicio horarioServicio;
    @Mock
    private DashboardServicio dashboardServicio;
    @Mock
    private CambiosReservaServicio cambiosServicio;

    @InjectMocks
    private InstalacionesServicio servicio;

    private final CategoriaSocio alumno = DatosDePrueba.categoria(1, "Alumno UNSE", true);
    private final Socio socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO, alumno);
    private Instalacion cancha;
    private Turno turnoTarde;

    @BeforeEach
    void armarUnaCanchaHabilitada() {
        cancha = DatosDePrueba.instalacion(1, "Cancha de Fútbol 11", Instalacion.TipoAcceso.ARANCELADO,
                Instalacion.Estado.HABILITADA);
        turnoTarde = DatosDePrueba.turno(5, cancha, LocalTime.of(18, 0), LocalTime.of(19, 0));
        lenient().when(instalacionDAO.buscarPorId(1)).thenReturn(cancha);
        // una fila por reserva, para que Conflictos cuente igual que en producción
        lenient().when(dashboardServicio.aFilas(anyList(), any(LocalDate.class))).thenAnswer(inv -> {
            List<?> reservas = inv.getArgument(0);
            List<ReservaFila> filas = new ArrayList<>();
            for (int i = 0; i < reservas.size(); i++) {
                filas.add(mock(ReservaFila.class));
            }
            return filas;
        });
    }

    private Reserva reserva(int id, LocalDate fecha, Reserva.Estado estado, Pago pago) {
        Reserva r = DatosDePrueba.reserva(id, socio, turnoTarde, fecha, estado);
        r.setIdPago(pago);
        return r;
    }

    private Pago pago(Pago.Estado estado) {
        return DatosDePrueba.pago(1, "20000", estado);
    }

    private void hayReservas(Reserva... reservas) {
        when(reservaDAO.listarActivasDeInstalacionEntre(eq(1), eq(HOY), any(LocalDate.class)))
                .thenReturn(List.of(reservas));
    }

    // ---------- sacar de servicio ----------

    @Test
    void poneEnMantenimientoUnaCanchaSinReservasEnElPeriodo() {
        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, "  Cambio de césped  ",
                HOY.plusDays(10), false, AHORA);

        assertNull(error);
        assertEquals(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, cancha.getEstado());
        assertEquals("Cambio de césped", cancha.getMotivoBaja());
        assertEquals(HOY, cancha.getFechaInicioBaja());
        assertEquals(HOY.plusDays(10), cancha.getFechaFinBaja());
    }

    @Test
    void laBajaDefinitivaNoGuardaFechaDeReactivacion() {
        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA, "Obra definitiva", HOY.plusDays(10), false,
                AHORA);

        assertNull(error);
        assertEquals(Instalacion.Estado.DESHABILITADA, cancha.getEstado());
        assertNull(cancha.getFechaFinBaja());
    }

    @Test
    void recortaElMotivoA255Caracteres() {
        servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, "m".repeat(300), null, false, AHORA);

        assertEquals(255, cancha.getMotivoBaja().length());
    }

    @Test
    void rechazaSacarDeServicioUnaInstalacionInexistente() {
        when(instalacionDAO.buscarPorId(99)).thenReturn(null);

        String error = servicio.retirar(99, Instalacion.Estado.DESHABILITADA, "Obra", null, false, AHORA);

        assertEquals("La instalación no existe.", error);
    }

    @Test
    void rechazaSacarDeServicioUnaInstalacionYaDadaDeBaja() {
        cancha.setEstado(Instalacion.Estado.DESHABILITADA);

        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, "Obra", null, false, AHORA);

        assertEquals("Esta instalación ya está dada de baja.", error);
    }

    @Test
    void rechazaPonerEnMantenimientoUnaInstalacionQueYaEstaEnMantenimiento() {
        cancha.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);

        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, "Obra", null, false, AHORA);

        assertEquals("Esta instalación ya está en mantenimiento.", error);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void exigeElMotivo(String motivo) {
        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, motivo, null, false, AHORA);

        assertEquals("Indicá el motivo.", error);
        assertEquals(Instalacion.Estado.HABILITADA, cancha.getEstado());
    }

    @Test
    void rechazaUnaFechaDeReactivacionAnteriorAHoy() {
        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, "Obra", HOY.minusDays(1),
                false, AHORA);

        assertEquals("La fecha de reactivación no puede ser anterior a hoy.", error);
    }

    @Test
    void aceptaUnaFechaDeReactivacionDeHoy() {
        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, "Obra", HOY, false, AHORA);

        assertNull(error);
    }

    // ---------- reservas que se pisan ----------

    @Test
    void bloqueaSiHayUnaSolicitudConComprobantePorConfirmar() {
        hayReservas(reserva(20, HOY.plusDays(3), Reserva.Estado.PENDIENTE_PAGO, pago(Pago.Estado.PENDIENTE_VALIDACION)));

        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, "Obra", null, true, AHORA);

        assertEquals("Hay 1 solicitud con comprobante por confirmar en ese período: resolvelas en Solicitudes "
                + "pendientes y volvé a intentar.", error);
        assertEquals(Instalacion.Estado.HABILITADA, cancha.getEstado());
    }

    @Test
    void laSolicitudPorConfirmarBloqueaAunqueSeMarqueCancelarReservas() {
        hayReservas(reserva(20, HOY.plusDays(3), Reserva.Estado.PENDIENTE_PAGO, pago(Pago.Estado.PENDIENTE_VALIDACION)),
                reserva(21, HOY.plusDays(4), Reserva.Estado.PENDIENTE_PAGO, pago(Pago.Estado.PENDIENTE_VALIDACION)));

        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA, "Obra", null, true, AHORA);

        assertTrue(error.startsWith("Hay 2 solicitudes con comprobante por confirmar"));
        verify(cambiosServicio, never()).cancelarPorElPredio(anyInt(), any(), any());
    }

    @Test
    void pideMarcarLaOpcionDeCancelarSiHayUnaReserva() {
        hayReservas(reserva(20, HOY.plusDays(3), Reserva.Estado.CONFIRMADA, pago(Pago.Estado.CONFIRMADO)));

        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, "Obra", null, false, AHORA);

        assertEquals("Hay 1 reserva en ese período. Marcá la opción de cancelarlas para continuar.", error);
        assertEquals(Instalacion.Estado.HABILITADA, cancha.getEstado());
    }

    @Test
    void pideMarcarLaOpcionDeCancelarSiHayVariasReservas() {
        hayReservas(reserva(20, HOY.plusDays(3), Reserva.Estado.CONFIRMADA, pago(Pago.Estado.CONFIRMADO)),
                reserva(21, HOY.plusDays(4), Reserva.Estado.PENDIENTE_PAGO, null));

        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, "Obra", null, false, AHORA);

        assertEquals("Hay 2 reservas en ese período. Marcá la opción de cancelarlas para continuar.", error);
    }

    @DisplayName("CP-31: al poner en mantenimiento, las confirmadas se cancelan por el predio")
    @Test
    void alCancelarLasConfirmadasVanPorElPredioYLasQueEsperanPagoSeLiberan() {
        Reserva confirmada = reserva(20, HOY.plusDays(3), Reserva.Estado.CONFIRMADA, pago(Pago.Estado.CONFIRMADO));
        Reserva esperandoPago = reserva(21, HOY.plusDays(4), Reserva.Estado.PENDIENTE_PAGO, null);
        hayReservas(confirmada, esperandoPago);
        when(cambiosServicio.cancelarPorElPredio(20, "Mantenimiento de Cancha de Fútbol 11: Cambio de césped", AHORA))
                .thenReturn(null);
        when(reservaDAO.buscarParaResolver(21)).thenReturn(esperandoPago);

        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, "Cambio de césped", null, true,
                AHORA);

        assertNull(error);
        assertEquals(Reserva.Estado.CANCELADA, esperandoPago.getEstado());
        assertEquals(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, cancha.getEstado());
    }

    @Test
    void laCausaDeUnaBajaDefinitivaEmpiezaConBajaDe() {
        hayReservas(reserva(20, HOY.plusDays(3), Reserva.Estado.CONFIRMADA, pago(Pago.Estado.CONFIRMADO)));
        when(cambiosServicio.cancelarPorElPredio(20, "Baja de Cancha de Fútbol 11: Obra definitiva", AHORA))
                .thenReturn(null);

        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA, "Obra definitiva", null, true, AHORA);

        assertNull(error);
        assertEquals(Instalacion.Estado.DESHABILITADA, cancha.getEstado());
    }

    @Test
    void siFallaLaCancelacionDeUnaReservaLaInstalacionSigueEnServicio() {
        hayReservas(reserva(20, HOY.plusDays(3), Reserva.Estado.CONFIRMADA, pago(Pago.Estado.CONFIRMADO)));
        when(cambiosServicio.cancelarPorElPredio(20, "Mantenimiento de Cancha de Fútbol 11: Obra", AHORA))
                .thenReturn("Esa reserva ya no está confirmada o el turno ya empezó.");

        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, "Obra", null, true, AHORA);

        assertEquals("Esa reserva ya no está confirmada o el turno ya empezó.", error);
        assertEquals(Instalacion.Estado.HABILITADA, cancha.getEstado());
    }

    @Test
    void unaReservaQueYaEmpezoHoyNoCuentaComoConflicto() {
        Turno temprano = DatosDePrueba.turno(6, cancha, LocalTime.of(8, 0), LocalTime.of(9, 0));
        Reserva yaEmpezo = DatosDePrueba.reserva(20, socio, temprano, HOY, Reserva.Estado.CONFIRMADA);
        hayReservas(yaEmpezo);

        Conflictos c = servicio.conflictos(1, null, AHORA);

        assertTrue(c.isVacio());
    }

    @Test
    void clasificaLosConflictosEnConfirmadasEsperandoPagoYPorConfirmar() {
        hayReservas(reserva(20, HOY.plusDays(3), Reserva.Estado.CONFIRMADA, pago(Pago.Estado.CONFIRMADO)),
                reserva(21, HOY.plusDays(4), Reserva.Estado.PENDIENTE_PAGO, null),
                reserva(22, HOY.plusDays(5), Reserva.Estado.PENDIENTE_PAGO, pago(Pago.Estado.PENDIENTE_VALIDACION)));

        Conflictos c = servicio.conflictos(1, HOY.plusDays(10), AHORA);

        assertEquals(1, c.getConfirmadas());
        assertEquals(1, c.getEsperandoPago());
        assertEquals(1, c.getPorConfirmar());
        assertEquals(3, c.getTotal());
    }

    @Test
    void sinFechaDeFinBuscaConflictosHastaCincoAniosAdelante() {
        servicio.conflictos(1, null, AHORA);

        verify(reservaDAO).listarActivasDeInstalacionEntre(1, HOY, HOY.plusYears(5));
    }

    // ---------- reactivar y editar ----------

    @Test
    void reactivarVuelveAHabilitarYLimpiaLosDatosDeLaBaja() {
        cancha.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        cancha.setMotivoBaja("Cambio de césped");
        cancha.setFechaInicioBaja(HOY.minusDays(3));
        cancha.setFechaFinBaja(HOY.plusDays(2));

        String error = servicio.reactivar(1, HOY);

        assertNull(error);
        assertEquals(Instalacion.Estado.HABILITADA, cancha.getEstado());
        assertNull(cancha.getMotivoBaja());
        assertNull(cancha.getFechaInicioBaja());
        assertNull(cancha.getFechaFinBaja());
    }

    @Test
    void laBajaDefinitivaNoSeReviertePorReactivar() {
        cancha.setEstado(Instalacion.Estado.DESHABILITADA);

        assertEquals("La baja definitiva no se revierte.", servicio.reactivar(1, HOY));
        assertEquals(Instalacion.Estado.DESHABILITADA, cancha.getEstado());
    }

    @Test
    void noReactivaUnaInstalacionQueYaEstaHabilitada() {
        assertEquals("Esta instalación ya está habilitada.", servicio.reactivar(1, HOY));
    }

    @Test
    void noReactivaUnaInstalacionInexistente() {
        when(instalacionDAO.buscarPorId(99)).thenReturn(null);

        assertEquals("La instalación no existe.", servicio.reactivar(99, HOY));
    }

    // ---------- créditos de reprogramación y mantenimiento (CC-02) ----------

    private void enMantenimientoDesde(LocalDate inicio) {
        cancha.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        cancha.setMotivoBaja("Cambio de césped");
        cancha.setFechaInicioBaja(inicio);
    }

    /** Reserva cancelada con un crédito al que, cuando empezó el mantenimiento, le quedaban esos días. */
    private Reserva creditoConDiasAlEmpezar(LocalDate inicioDelMantenimiento, int dias) {
        Reserva r = reserva(30, HOY.minusDays(45), Reserva.Estado.CANCELADA, pago(Pago.Estado.CONFIRMADO));
        r.setFechaLimiteReprogramacion(inicioDelMantenimiento.plusDays(dias));
        return r;
    }

    @DisplayName("CP-30: al reactivar, el crédito conserva los días que le quedaban, contados desde la reactivación real")
    @Test
    void cp30_alReactivarElCreditoRetomaLosDiasQueLeQuedaban() {
        LocalDate inicio = HOY.minusDays(40); // el mantenimiento duró 40 días
        enMantenimientoDesde(inicio);
        Reserva credito = creditoConDiasAlEmpezar(inicio, 25);
        when(reservaDAO.listarConCreditoDesde(1, inicio)).thenReturn(List.of(credito));

        String error = servicio.reactivar(1, HOY);

        assertNull(error);
        assertEquals(HOY.plusDays(25), credito.getFechaLimiteReprogramacion(), "25 días desde la reactivación, no 30");
    }

    @DisplayName("CP-30: un crédito que ya había vencido antes del mantenimiento no revive al reactivar")
    @Test
    void cp30_unCreditoVencidoAntesDelMantenimientoNoRevive() {
        LocalDate inicio = HOY.minusDays(10);
        enMantenimientoDesde(inicio);
        Reserva vencido = creditoConDiasAlEmpezar(inicio, -2);
        when(reservaDAO.listarConCreditoDesde(1, inicio)).thenReturn(List.of(vencido));

        servicio.reactivar(1, HOY);

        assertEquals(inicio.minusDays(2), vencido.getFechaLimiteReprogramacion());
    }

    @DisplayName("CP-30: reactivar el mismo día que empezó el mantenimiento no cambia el plazo")
    @Test
    void cp30_reactivarElMismoDiaNoCorreElPlazo() {
        enMantenimientoDesde(HOY);
        Reserva credito = creditoConDiasAlEmpezar(HOY, 30);
        when(reservaDAO.listarConCreditoDesde(1, HOY)).thenReturn(List.of(credito));

        servicio.reactivar(1, HOY);

        assertEquals(HOY.plusDays(30), credito.getFechaLimiteReprogramacion());
    }

    @DisplayName("CC-02: si el mantenimiento termina en baja definitiva, los créditos retoman su plazo ese día")
    @Test
    void laBajaDefinitivaDesdeMantenimientoRetomaElPlazoDeLosCreditos() {
        LocalDate inicio = HOY.minusDays(10);
        enMantenimientoDesde(inicio);
        Reserva credito = creditoConDiasAlEmpezar(inicio, 20);
        when(reservaDAO.listarConCreditoDesde(1, inicio)).thenReturn(List.of(credito));

        String error = servicio.retirar(1, Instalacion.Estado.DESHABILITADA, "Obra definitiva", null, false, AHORA);

        assertNull(error);
        assertEquals(Instalacion.Estado.DESHABILITADA, cancha.getEstado());
        assertEquals(HOY.plusDays(20), credito.getFechaLimiteReprogramacion());
    }

    @Test
    void editaLaDescripcionSinEspaciosYLaCapacidad() {
        String error = servicio.editar(1, "  Césped natural con iluminación  ", 22);

        assertNull(error);
        assertEquals("Césped natural con iluminación", cancha.getDescripcion());
        assertEquals(22, cancha.getCapacidad());
    }

    @Test
    void unaDescripcionEnBlancoQuedaNulaYLaCapacidadVaciaEsSinLimite() {
        cancha.setDescripcion("Antes");

        servicio.editar(1, "   ", null);

        assertNull(cancha.getDescripcion());
        assertNull(cancha.getCapacidad());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5})
    void rechazaUnaCapacidadQueNoEsMayorACero(int capacidad) {
        assertEquals("La capacidad tiene que ser un número mayor a cero.", servicio.editar(1, "Descripción", capacidad));
    }

    @Test
    void aceptaUnaDescripcionDeExactamente1000Caracteres() {
        assertNull(servicio.editar(1, "d".repeat(1000), 22));
    }

    @Test
    void rechazaUnaDescripcionDeMasDe1000Caracteres() {
        assertEquals("La descripción es demasiado larga (máximo 1000 caracteres).",
                servicio.editar(1, "d".repeat(1001), 22));
        assertNull(cancha.getDescripcion());
    }

    // ---------- demanda, ocupación y ranking ----------

    @Test
    void laDemandaSumaReservasConfirmadasEIngresosSoloDeLasConfirmadasEnLosUltimos30Dias() {
        List<Object[]> filas = new ArrayList<>();
        filas.add(new Object[]{1, Reserva.Estado.CONFIRMADA, 4L, new BigDecimal("80000")});
        filas.add(new Object[]{1, Reserva.Estado.PENDIENTE_PAGO, 2L, new BigDecimal("40000")});
        when(reservaDAO.resumenPorInstalacion(HOY.minusDays(29), HOY)).thenReturn(filas);

        Demanda d = servicio.demanda(HOY).get(1);

        assertEquals(6, d.getReservas());
        assertEquals(4, d.getConfirmadas());
        assertEquals(0, new BigDecimal("80000").compareTo(d.getIngresos()));
    }

    @Test
    void laOcupacionEsElPorcentajeDeLasFranjasDeLosUltimos30Dias() {
        Map<Integer, Long> turnos = new HashMap<>();
        turnos.put(1, 4L); // 4 franjas por día x 30 días = 120
        Demanda treintaReservas = new Demanda(30, 30, BigDecimal.ZERO);

        assertEquals(25, servicio.ocupacion(cancha, treintaReservas, turnos));
    }

    @Test
    void laOcupacionNoPasaDe100() {
        Map<Integer, Long> turnos = new HashMap<>();
        turnos.put(1, 1L);

        assertEquals(100, servicio.ocupacion(cancha, new Demanda(80, 80, BigDecimal.ZERO), turnos));
    }

    @Test
    void sinTurnosLaOcupacionEsCero() {
        assertEquals(0, servicio.ocupacion(cancha, new Demanda(5, 5, BigDecimal.ZERO), Collections.emptyMap()));
    }

    @Test
    void elRankingVaDeLaMasALaMenosSolicitadaConPorcentajesSobreLaPrimera() {
        Instalacion rugby = DatosDePrueba.instalacion(2, "Cancha de Rugby", Instalacion.TipoAcceso.ARANCELADO,
                Instalacion.Estado.HABILITADA);
        Instalacion pileta = DatosDePrueba.instalacion(3, "Pileta", Instalacion.TipoAcceso.LIBRE,
                Instalacion.Estado.HABILITADA);
        Map<Integer, Demanda> demanda = new HashMap<>();
        demanda.put(1, new Demanda(10, 10, BigDecimal.ZERO));
        demanda.put(2, new Demanda(40, 40, BigDecimal.ZERO));
        demanda.put(3, new Demanda(99, 0, BigDecimal.ZERO));

        List<RankingItem> ranking = servicio.ranking(List.of(cancha, rugby, pileta), demanda);

        assertEquals(2, ranking.size(), "la pileta (acceso libre) no entra");
        assertEquals("Cancha de Rugby", ranking.get(0).getEtiqueta());
        assertEquals(100, ranking.get(0).getPorcentaje());
        assertEquals("Cancha de Fútbol 11", ranking.get(1).getEtiqueta());
        assertEquals(25, ranking.get(1).getPorcentaje());
    }

    @Test
    void sinReservasElRankingMuestraCeroPorCiento() {
        List<RankingItem> ranking = servicio.ranking(List.of(cancha), new HashMap<Integer, Demanda>());

        assertEquals(0, ranking.get(0).getCantidad());
        assertEquals(0, ranking.get(0).getPorcentaje());
    }

    // ---------- precios ----------

    @Test
    void losPreciosSeMuestranPorTipoDeSocioYLasDeAccesoLibreNoTienenPrecio() {
        CategoriaSocio docente = DatosDePrueba.categoria(2, "Docente UNSE", true);
        CategoriaSocio noDocente = DatosDePrueba.categoria(3, "No Docente UNSE", true);
        CategoriaSocio externo = DatosDePrueba.categoria(4, "Externo", false);
        Instalacion pileta = DatosDePrueba.instalacion(3, "Pileta", Instalacion.TipoAcceso.LIBRE,
                Instalacion.Estado.HABILITADA);
        LocalDate desde = LocalDate.of(2026, 1, 1);
        when(tarifaDAO.listarVigentes(HOY)).thenReturn(List.of(
                DatosDePrueba.tarifaAlquiler(1, cancha, alumno, "10000", desde, null),
                DatosDePrueba.tarifaAlquiler(2, cancha, docente, "15000", desde, null),
                DatosDePrueba.tarifaAlquiler(3, cancha, noDocente, "15000", desde, null),
                DatosDePrueba.tarifaAlquiler(4, cancha, externo, "25000", desde, null)));

        List<PrecioFila> filas = servicio.precios(List.of(cancha, pileta), List.of(alumno, docente, noDocente, externo),
                HOY);

        assertEquals(List.of("$ 10.000", "$ 15.000", "$ 15.000", "$ 25.000"),
                filas.get(0).getPrecios().stream().map(PrecioVigente::getPrecio).collect(Collectors.toList()));
        assertTrue(filas.get(0).isOrdenOk());
        assertFalse(filas.get(0).isSinReserva());
        assertTrue(filas.get(1).isSinReserva());
    }

    @Test
    void marcaConOrdenIncorrectoLaFilaEnQueElAlumnoPagaMasQueElExterno() {
        CategoriaSocio externo = DatosDePrueba.categoria(4, "Externo", false);
        LocalDate desde = LocalDate.of(2026, 1, 1);
        when(tarifaDAO.listarVigentes(HOY)).thenReturn(List.of(
                DatosDePrueba.tarifaAlquiler(1, cancha, alumno, "30000", desde, null),
                DatosDePrueba.tarifaAlquiler(4, cancha, externo, "25000", desde, null)));

        List<PrecioFila> filas = servicio.precios(List.of(cancha), List.of(alumno, externo), HOY);

        assertFalse(filas.get(0).isOrdenOk());
    }

    // ---------- ficha ----------

    @Test
    void noHayFichaDeUnaInstalacionInexistente() {
        when(instalacionDAO.buscarPorId(99)).thenReturn(null);

        assertNull(servicio.ficha(99, new HashMap<Integer, Demanda>(), AHORA));
        assertNull(servicio.ficha(null, new HashMap<Integer, Demanda>(), AHORA));
    }

    @Test
    void laFichaDeUnaCanchaHabilitadaMuestraEstadoTurnosYOcupacion() {
        when(turnoDAO.listarPorInstalacion(1)).thenReturn(List.of(turnoTarde));
        Map<Integer, Long> porInstalacion = new HashMap<>();
        porInstalacion.put(1, 1L);
        when(turnoDAO.contarPorInstalacion()).thenReturn(porInstalacion);
        Map<Integer, Demanda> demanda = new HashMap<>();
        demanda.put(1, new Demanda(3, 3, new BigDecimal("60000")));

        FichaInstalacion f = servicio.ficha(1, demanda, AHORA);

        assertEquals("Habilitada", f.getEstadoTexto());
        assertEquals("ok", f.getEstadoClase());
        assertTrue(f.isReservable());
        assertEquals(List.of("18:00 – 19:00"), f.getTurnos());
        assertEquals(10, f.getOcupacion(), "3 reservas sobre 30 franjas");
        assertEquals("$ 60.000", f.getIngresos());
    }

    @Test
    void laFichaMuestraLaReactivacionEstimadaDeUnMantenimiento() {
        cancha.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        cancha.setMotivoBaja("Cambio de césped");
        cancha.setFechaInicioBaja(LocalDate.of(2026, 10, 5));
        cancha.setFechaFinBaja(LocalDate.of(2026, 10, 17));

        FichaInstalacion f = servicio.ficha(1, new HashMap<Integer, Demanda>(), AHORA);

        assertEquals("En mantenimiento", f.getEstadoTexto());
        assertEquals("warn", f.getEstadoClase());
        assertEquals("Desde el 05/10/2026 · reactivación estimada el 17/10/2026 (en 5 días)", f.getBajaDetalle());
        assertFalse(f.isReactivacionVencida());
    }

    @Test
    void laFichaAvisaQueLaFechaEstimadaDeReactivacionYaPaso() {
        cancha.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        cancha.setFechaInicioBaja(LocalDate.of(2026, 10, 1));
        cancha.setFechaFinBaja(HOY.minusDays(1));

        FichaInstalacion f = servicio.ficha(1, new HashMap<Integer, Demanda>(), AHORA);

        assertTrue(f.isReactivacionVencida());
        assertEquals("Desde el 01/10/2026 · la fecha estimada de reactivación (11/10/2026) ya pasó", f.getBajaDetalle());
    }

    @Test
    void laFichaDeUnaBajaDefinitivaLoDice() {
        cancha.setEstado(Instalacion.Estado.DESHABILITADA);
        cancha.setFechaInicioBaja(LocalDate.of(2026, 9, 1));

        FichaInstalacion f = servicio.ficha(1, new HashMap<Integer, Demanda>(), AHORA);

        assertEquals("Dada de baja", f.getEstadoTexto());
        assertEquals("off", f.getEstadoClase());
        assertEquals("Desde el 01/09/2026 · baja definitiva", f.getBajaDetalle());
        assertFalse(f.isReactivacionVencida());
    }

    @Test
    void laFichaMuestraSoloLasTresProximasReservasQueTodaviaNoTerminaron() {
        Turno temprano = DatosDePrueba.turno(6, cancha, LocalTime.of(8, 0), LocalTime.of(9, 0));
        Reserva terminada = DatosDePrueba.reserva(30, socio, temprano, HOY, Reserva.Estado.CONFIRMADA);
        Reserva r1 = reserva(31, HOY, Reserva.Estado.CONFIRMADA, pago(Pago.Estado.CONFIRMADO));
        Reserva r2 = reserva(32, HOY.plusDays(1), Reserva.Estado.CONFIRMADA, pago(Pago.Estado.CONFIRMADO));
        Reserva r3 = reserva(33, HOY.plusDays(2), Reserva.Estado.CONFIRMADA, pago(Pago.Estado.CONFIRMADO));
        Reserva r4 = reserva(34, HOY.plusDays(3), Reserva.Estado.CONFIRMADA, pago(Pago.Estado.CONFIRMADO));
        when(reservaDAO.listarActivasDeInstalacionEntre(1, HOY, HOY.plusDays(60)))
                .thenReturn(List.of(terminada, r1, r2, r3, r4));
        ArgumentCaptor<List<Reserva>> mostradas = ArgumentCaptor.forClass(List.class);

        servicio.ficha(1, new HashMap<Integer, Demanda>(), AHORA);

        verify(dashboardServicio).aFilas(mostradas.capture(), eq(HOY));
        assertEquals(List.of(r1, r2, r3), mostradas.getValue());
    }

    // ---------- vigencia de los precios (CC-03) ----------

    @DisplayName("CP-36: cada precio muestra desde cuándo rige y el próximo cambio programado")
    @Test
    void cp36_cadaPrecioMuestraDesdeCuandoRigeYElProximoCambio() {
        CategoriaSocio externo = DatosDePrueba.categoria(4, "Externo", false);
        when(tarifaDAO.listarVigentes(HOY)).thenReturn(List.of(
                DatosDePrueba.tarifaAlquiler(1, cancha, alumno, "10000", LocalDate.of(2026, 1, 1), null),
                DatosDePrueba.tarifaAlquiler(4, cancha, externo, "25000", LocalDate.of(2026, 3, 1), null)));
        // el más próximo primero: gana el de noviembre y el de diciembre no se muestra
        when(tarifaDAO.listarProgramadas(HOY)).thenReturn(List.of(
                DatosDePrueba.tarifaAlquiler(5, cancha, alumno, "12000", LocalDate.of(2026, 11, 1), null),
                DatosDePrueba.tarifaAlquiler(6, cancha, alumno, "14000", LocalDate.of(2026, 12, 1), null)));

        List<PrecioVigente> precios = servicio.precios(List.of(cancha), List.of(alumno, externo), HOY).get(0).getPrecios();

        assertEquals("$ 10.000", precios.get(0).getPrecio());
        assertEquals("Rige desde el 01/01/2026", precios.get(0).getVigencia());
        assertEquals("Desde el 01/11/2026: $ 12.000", precios.get(0).getProximo());
        assertEquals("$ 25.000", precios.get(1).getPrecio());
        assertEquals("Rige desde el 01/03/2026", precios.get(1).getVigencia());
        assertEquals("Sin cambios programados", precios.get(1).getProximo());
    }

    @DisplayName("CP-36: un precio que todavía no rige se ve como próximo cambio")
    @Test
    void cp36_unPrecioQueTodaviaNoRigeSeVeComoProximoCambio() {
        when(tarifaDAO.listarVigentes(HOY)).thenReturn(List.of());
        when(tarifaDAO.listarProgramadas(HOY)).thenReturn(List.of(
                DatosDePrueba.tarifaAlquiler(5, cancha, alumno, "12000", LocalDate.of(2026, 11, 1), null)));

        PrecioVigente precio = servicio.precios(List.of(cancha), List.of(alumno), HOY).get(0).getPrecios().get(0);

        assertNull(precio.getPrecio());
        assertNull(precio.getVigencia());
        assertEquals("Desde el 01/11/2026: $ 12.000", precio.getProximo());
    }
}
