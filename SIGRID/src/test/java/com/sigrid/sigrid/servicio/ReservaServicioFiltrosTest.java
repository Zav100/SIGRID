package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.ComprobantePagoDAO;
import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.TurnoDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dto.FiltroReservas;
import com.sigrid.sigrid.dto.InformeReservas;
import com.sigrid.sigrid.dto.PaginaReservas;
import com.sigrid.sigrid.dto.ResumenReservas;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.Turno;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Listado, indicadores y PDF de reservas del administrador (CC-06): los filtros y el tope los aplica la base; el
 * servicio solo pide lo que se va a mostrar y arma el encabezado del PDF. "Ahora" es lunes 12/10/2026 10:00.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServicioFiltrosTest {

    private static final LocalTime HORA = AHORA.toLocalTime();

    @Mock
    private ReservaDAO reservaDAO;
    @Mock
    private UsuarioDAO usuarioDAO;
    @Mock
    private ComprobantePagoDAO comprobanteDAO;
    @Mock
    private InstalacionDAO instalacionDAO;
    @Mock
    private TurnoDAO turnoDAO;

    @InjectMocks
    private ReservaServicio servicio;

    private final Socio socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO,
            DatosDePrueba.categoria(1, "Alumno UNSE", true));
    private final Instalacion cancha = DatosDePrueba.instalacion(1, "Cancha de Fútbol 11",
            Instalacion.TipoAcceso.ARANCELADO, Instalacion.Estado.HABILITADA);
    private final Turno turno = DatosDePrueba.turno(5, cancha, LocalTime.of(10, 0), LocalTime.of(11, 0));

    private Reserva reserva(int id, LocalDate fecha, Reserva.Estado estado) {
        return DatosDePrueba.reserva(id, socio, turno, fecha, estado);
    }

    private static FiltroReservas periodo(LocalDate desde, LocalDate hasta) {
        FiltroReservas f = new FiltroReservas();
        f.setDesde(desde);
        f.setHasta(hasta);
        return f;
    }

    private static Object[] fila(Object... valores) {
        return valores;
    }

    // ---------- listado por tandas ----------

    @Test
    void unaTandaTraeLoQueLaBaseDevuelveYElTotalDeLaConsulta() {
        FiltroReservas f = new FiltroReservas();
        f.setEstado("CONFIRMADA");
        when(reservaDAO.buscarParaAdmin(f, HOY, HORA, 1))
                .thenReturn(List.of(reserva(10, HOY.plusDays(2), Reserva.Estado.CONFIRMADA)));
        when(reservaDAO.contarParaAdmin(f, HOY, HORA)).thenReturn(130L);

        PaginaReservas pagina = servicio.listarPagina(f, 1, HOY, HORA);

        assertEquals(1, pagina.getFilas().size());
        assertEquals("CONFIRMADA", pagina.getFilas().get(0).getEstado());
        assertEquals(130, pagina.getTotal());
        assertTrue(pagina.isHayMas());
    }

    @Test
    void sinMasFilasQueLasMostradasNoHayMas() {
        FiltroReservas f = new FiltroReservas();
        when(reservaDAO.buscarParaAdmin(f, HOY, HORA, 25))
                .thenReturn(List.of(reserva(10, HOY.plusDays(2), Reserva.Estado.CONFIRMADA)));

        PaginaReservas pagina = servicio.listarPagina(f, 25, HOY, HORA);

        assertFalse(pagina.isHayMas());
        assertEquals(1, pagina.getTotal(), "la tanda no se llenó: el total es lo que llegó, sin contar de nuevo");
        verify(reservaDAO, never()).contarParaAdmin(any(), any(), any());
    }

    @Test
    void losTiposDeInstalacionParaElFiltroSonLosQueSeReservanSinRepetir() {
        Instalacion otraCancha = DatosDePrueba.instalacion(2, "Cancha de Fútbol 5", Instalacion.TipoAcceso.ARANCELADO,
                Instalacion.Estado.HABILITADA);
        otraCancha.setTipoDisciplina(cancha.getTipoDisciplina());
        Instalacion quincho = DatosDePrueba.instalacion(3, "Quincho 1", Instalacion.TipoAcceso.ARANCELADO,
                Instalacion.Estado.HABILITADA);
        quincho.setTipoDisciplina("Quincho");
        when(instalacionDAO.listarReservables()).thenReturn(List.of(quincho, cancha, otraCancha));

        assertEquals(List.of(cancha.getTipoDisciplina(), "Quincho").stream().sorted().collect(java.util.stream.Collectors.toList()),
                servicio.disciplinas());
    }

    @Test
    void lasProximasConfirmadasSeListanConSuEstadoYSinTerminar() {
        when(reservaDAO.listarProximasConfirmadasParaAdmin(HOY, HORA))
                .thenReturn(List.of(reserva(10, HOY.plusDays(1), Reserva.Estado.CONFIRMADA),
                        reserva(11, HOY.plusDays(2), Reserva.Estado.CONFIRMADA)));

        var filas = servicio.listarProximasConfirmadas(AHORA);

        assertEquals(2, filas.size());
        assertEquals("CONFIRMADA", filas.get(0).getEstado());
        assertFalse(filas.get(0).isFinalizada());
        assertEquals(HOY.plusDays(2), filas.get(1).getFecha());
    }

    // ---------- indicadores y rankings ----------

    private final Instalacion quincho = DatosDePrueba.instalacion(3, "Quincho 1", Instalacion.TipoAcceso.ARANCELADO,
            Instalacion.Estado.HABILITADA);

    @Test
    void elResumenSaleDeLasConsultasDeAgregacionYNoDeCargarTodasLasReservas() {
        quincho.setTipoDisciplina("Quincho");
        when(reservaDAO.contarPorEstado()).thenReturn(List.of(
                fila(Reserva.Estado.CONFIRMADA, 40L), fila(Reserva.Estado.PENDIENTE_PAGO, 5L),
                fila(Reserva.Estado.CANCELADA, 10L), fila(Reserva.Estado.RECHAZADA, 5L)));
        when(reservaDAO.contarPendientes()).thenReturn(3L);
        when(reservaDAO.contarActivasDelDia(HOY)).thenReturn(6L);
        when(reservaDAO.contarProximasConfirmadas(HOY, HORA)).thenReturn(12L);
        // reservas activas por franja: la cancha 20 a las 10 y 10 a las 18; el quincho 5 a las 18 y 5 a las 19
        when(reservaDAO.contarActivasPorTurno()).thenReturn(List.of(
                fila(1, 20L), fila(2, 10L), fila(3, 5L), fila(4, 5L)));
        when(turnoDAO.listarConInstalacion()).thenReturn(List.of(
                DatosDePrueba.turno(1, cancha, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                DatosDePrueba.turno(2, cancha, LocalTime.of(18, 0), LocalTime.of(19, 0)),
                DatosDePrueba.turno(3, quincho, LocalTime.of(18, 0), LocalTime.of(19, 0)),
                DatosDePrueba.turno(4, quincho, LocalTime.of(19, 0), LocalTime.of(20, 0)),
                DatosDePrueba.turno(5, quincho, LocalTime.of(20, 0), LocalTime.of(21, 0))));
        when(reservaDAO.listarConfirmadasDesde(HOY.minusDays(6).atStartOfDay())).thenReturn(List.of());

        ResumenReservas r = servicio.resumen(AHORA);

        assertEquals(3, r.getPorConfirmar());
        assertEquals(6, r.getDeHoy());
        assertEquals(12, r.getProximasConfirmadas());
        assertEquals(15, r.getCanceladas(), "canceladas y rechazadas");
        assertEquals(60, r.getTotal());
        assertEquals(25, r.getPorcentajeCanceladas());
        assertEquals(2, r.getMasReservadas().size(), "la franja sin reservas no suma una instalación vacía");
        assertEquals("Cancha de Fútbol 11", r.getMasReservadas().get(0).getEtiqueta());
        assertEquals(30, r.getMasReservadas().get(0).getCantidad());
        assertEquals("Quincho 1", r.getMasReservadas().get(1).getEtiqueta());
        assertEquals(33, r.getMasReservadas().get(1).getPorcentaje(), "10 sobre 30");
        assertEquals("10:00 h", r.getHorasPico().get(0).getEtiqueta());
        assertEquals(20, r.getHorasPico().get(0).getCantidad());
        assertEquals("18:00 h", r.getHorasPico().get(1).getEtiqueta());
        assertEquals(15, r.getHorasPico().get(1).getCantidad(), "cancha y quincho a las 18");
        assertEquals(75, r.getHorasPico().get(1).getPorcentaje(), "15 sobre 20");
        assertEquals("19:00 h", r.getHorasPico().get(2).getEtiqueta());
    }

    @Test
    void lasHorasSeAgrupanPorHoraAunqueLosTurnosEmpiecenALasYMedia() {
        when(reservaDAO.contarPorEstado()).thenReturn(List.of());
        when(reservaDAO.contarActivasPorTurno()).thenReturn(List.of(fila(1, 4L), fila(2, 6L), fila(3, 3L)));
        when(turnoDAO.listarConInstalacion()).thenReturn(List.of(
                DatosDePrueba.turno(1, cancha, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                DatosDePrueba.turno(2, cancha, LocalTime.of(10, 30), LocalTime.of(11, 30)),
                DatosDePrueba.turno(3, cancha, LocalTime.of(11, 0), LocalTime.of(12, 0))));
        when(reservaDAO.listarConfirmadasDesde(HOY.minusDays(6).atStartOfDay())).thenReturn(List.of());

        ResumenReservas r = servicio.resumen(AHORA);

        assertEquals("10:00 h", r.getHorasPico().get(0).getEtiqueta());
        assertEquals(10, r.getHorasPico().get(0).getCantidad());
    }

    @Test
    void laRecaudacionDeLosUltimosSieteDiasSumaLoCobradoPorDiaDeConfirmacion() {
        when(reservaDAO.contarPorEstado()).thenReturn(List.of());
        Reserva hoy = confirmada(10, "20000", AHORA.minusHours(1));
        Reserva ayer = confirmada(11, "10000", AHORA.minusDays(1));
        when(reservaDAO.listarConfirmadasDesde(HOY.minusDays(6).atStartOfDay())).thenReturn(List.of(hoy, ayer));

        ResumenReservas r = servicio.resumen(AHORA);

        assertEquals("$ 20.000", r.getRecaudacion().getTotalHoy());
        assertEquals(1, r.getRecaudacion().getCantidadHoy());
        assertEquals("+100 % respecto de ayer", r.getRecaudacion().getComparacion());
        assertEquals(7, r.getRecaudacion().getDias().size());
        assertEquals("$ 10.000", r.getRecaudacion().getDias().get(5).getMonto());
    }

    private Reserva confirmada(int id, String monto, LocalDateTime confirmada) {
        Reserva r = reserva(id, HOY.plusDays(3), Reserva.Estado.CONFIRMADA);
        r.setIdPago(DatosDePrueba.pago(id, monto, Pago.Estado.CONFIRMADO));
        r.setFechaConfirmacion(confirmada);
        return r;
    }

    // ---------- PDF: período, filtros y tope ----------

    @DisplayName("CP-43: el informe de un solo día trae esas reservas con período y fecha de emisión")
    @Test
    void cp43_elInformeDeUnDiaTraeEseDiaConPeriodoYEmision() {
        FiltroReservas f = periodo(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 14));
        when(reservaDAO.contarParaAdmin(eq(f), eq(HOY), any(LocalTime.class))).thenReturn(2L);
        when(reservaDAO.buscarParaInforme(eq(f), eq(HOY), any(LocalTime.class), eq(ReservaServicio.MAXIMO_INFORME)))
                .thenReturn(List.of(reserva(10, LocalDate.of(2026, 10, 14), Reserva.Estado.CONFIRMADA),
                        reserva(11, LocalDate.of(2026, 10, 14), Reserva.Estado.PENDIENTE_PAGO)));

        InformeReservas informe = servicio.informe(f, AHORA);

        assertTrue(informe.isListo());
        assertNull(informe.getAviso());
        assertEquals("14/10/2026", informe.getPeriodo());
        assertEquals("12/10/2026 10:00", informe.getEmitido());
        assertEquals(2, informe.getFilas().size());
        assertEquals(2, informe.getTotal());
        assertEquals("", informe.getFiltros());
    }

    @DisplayName("CP-43: el período de varios días dice desde y hasta")
    @Test
    void cp43_unPeriodoDeVariosDiasDiceDesdeYHasta() {
        FiltroReservas f = periodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        when(reservaDAO.contarParaAdmin(eq(f), eq(HOY), any(LocalTime.class))).thenReturn(1L);
        when(reservaDAO.buscarParaInforme(eq(f), eq(HOY), any(LocalTime.class), eq(ReservaServicio.MAXIMO_INFORME)))
                .thenReturn(List.of(reserva(10, LocalDate.of(2026, 10, 14), Reserva.Estado.CONFIRMADA)));

        assertEquals("01/10/2026 al 31/10/2026", servicio.informe(f, AHORA).getPeriodo());
    }

    @DisplayName("CP-43: el informe dice qué filtros se aplicaron")
    @Test
    void cp43_elInformeDiceLosFiltrosAplicados() {
        FiltroReservas f = periodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        f.setEstado("CONFIRMADA");
        f.setDisciplina("Fútbol");
        f.setInstalacion("Cancha de Fútbol 11");
        f.setTexto("gomez");
        when(reservaDAO.contarParaAdmin(eq(f), eq(HOY), any(LocalTime.class))).thenReturn(1L);
        when(reservaDAO.buscarParaInforme(eq(f), eq(HOY), any(LocalTime.class), eq(ReservaServicio.MAXIMO_INFORME)))
                .thenReturn(List.of(reserva(10, LocalDate.of(2026, 10, 14), Reserva.Estado.CONFIRMADA)));

        assertEquals("Estado: Confirmadas · Disciplina: Fútbol · Instalación: Cancha de Fútbol 11 · Búsqueda: «gomez»",
                servicio.informe(f, AHORA).getFiltros());
    }

    @DisplayName("CP-52: elegir una instalación cuenta como filtro")
    @Test
    void cp52_elegirUnaInstalacionEsFiltrar() {
        FiltroReservas f = new FiltroReservas();
        assertFalse(f.isFiltrando());
        f.setInstalacion("Quincho 1");
        assertTrue(f.isFiltrando());
        f.setInstalacion(null);
        assertEquals("", f.getInstalacion());
    }

    @DisplayName("CP-52: el filtro ofrece cada instalación que se reserva, por nombre")
    @Test
    void cp52_elFiltroOfreceCadaInstalacionQueSeReservaPorNombre() {
        Instalacion quincho = DatosDePrueba.instalacion(3, "Quincho 1", Instalacion.TipoAcceso.ARANCELADO,
                Instalacion.Estado.HABILITADA);
        Instalacion beach = DatosDePrueba.instalacion(4, "Cancha de Beach Vóley", Instalacion.TipoAcceso.ARANCELADO,
                Instalacion.Estado.HABILITADA);
        when(instalacionDAO.listarReservables()).thenReturn(List.of(quincho, cancha, beach));

        assertEquals(List.of("Cancha de Beach Vóley", cancha.getNombre(), "Quincho 1"), servicio.instalaciones());
    }

    @DisplayName("CP-44: un período sin reservas avisa y no arma un documento vacío")
    @Test
    void cp44_unPeriodoSinReservasAvisaYNoArmaDocumento() {
        FiltroReservas f = periodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        when(reservaDAO.contarParaAdmin(eq(f), eq(HOY), any(LocalTime.class))).thenReturn(0L);

        InformeReservas informe = servicio.informe(f, AHORA);

        assertFalse(informe.isListo());
        assertTrue(informe.getFilas().isEmpty());
        assertEquals("No hay reservas en el período elegido (01/10/2026 al 31/10/2026).", informe.getAviso());
        verify(reservaDAO, never()).buscarParaInforme(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @DisplayName("CP-45: si el período trae más de lo que entra en un PDF pide acotarlo sin cargar las filas")
    @Test
    void cp45_siSuperaElMaximoPideAcotarSinCargarLasFilas() {
        FiltroReservas f = periodo(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        when(reservaDAO.contarParaAdmin(eq(f), eq(HOY), any(LocalTime.class)))
                .thenReturn((long) ReservaServicio.MAXIMO_INFORME + 1);

        InformeReservas informe = servicio.informe(f, AHORA);

        assertFalse(informe.isListo());
        assertEquals("El período tiene 2.001 reservas y un PDF admite hasta 2.000: acotá el período o los filtros.",
                informe.getAviso());
        verify(reservaDAO, never()).buscarParaInforme(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @DisplayName("CP-45: exactamente el máximo todavía entra en un PDF")
    @Test
    void cp45_exactamenteElMaximoEntra() {
        FiltroReservas f = periodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        when(reservaDAO.contarParaAdmin(eq(f), eq(HOY), any(LocalTime.class)))
                .thenReturn((long) ReservaServicio.MAXIMO_INFORME);
        when(reservaDAO.buscarParaInforme(eq(f), eq(HOY), any(LocalTime.class), eq(ReservaServicio.MAXIMO_INFORME)))
                .thenReturn(List.of(reserva(10, LocalDate.of(2026, 10, 14), Reserva.Estado.CONFIRMADA)));

        assertTrue(servicio.informe(f, AHORA).isListo());
    }

    @DisplayName("CP-43: sin período no se exporta: hay que elegirlo antes")
    @Test
    void cp43_sinPeriodoPideElegirlo() {
        InformeReservas informe = servicio.informe(new FiltroReservas(), AHORA);

        assertFalse(informe.isListo());
        assertEquals("Elegí el período (desde y hasta) antes de exportar.", informe.getAviso());
        verifyNoInteractions(reservaDAO);
    }

    @DisplayName("CP-43: un período con la fecha desde posterior a la hasta se rechaza")
    @Test
    void cp43_unPeriodoAlReves() {
        InformeReservas informe = servicio.informe(
                periodo(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1)), AHORA);

        assertEquals("El período está al revés: la fecha desde es posterior a la fecha hasta.", informe.getAviso());
        verifyNoInteractions(reservaDAO);
    }
}
