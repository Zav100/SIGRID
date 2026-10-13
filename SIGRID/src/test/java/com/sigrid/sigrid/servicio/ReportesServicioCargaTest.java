package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.CategoriaSocioDAO;
import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.SocioDAO;
import com.sigrid.sigrid.dao.SuscripcionSocioDAO;
import com.sigrid.sigrid.dao.TarifaAlquilerDAO;
import com.sigrid.sigrid.dao.TarifaMembresiaDAO;
import com.sigrid.sigrid.dao.TurnoDAO;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import com.sigrid.sigrid.repositorio.TarifaMembresia;
import com.sigrid.sigrid.repositorio.Turno;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lectura de la base para los reportes: qué pago cuenta como ingreso (confirmado y con fecha de validación, aunque la
 * reserva se haya cancelado), tarifa promedio por instalación y cuota vigente por tipo de socio.
 */
@ExtendWith(MockitoExtension.class)
class ReportesServicioCargaTest {

    @Mock
    private ReservaDAO reservaDAO;
    @Mock
    private SuscripcionSocioDAO suscripcionDAO;
    @Mock
    private InstalacionDAO instalacionDAO;
    @Mock
    private TurnoDAO turnoDAO;
    @Mock
    private SocioDAO socioDAO;
    @Mock
    private CategoriaSocioDAO categoriaDAO;
    @Mock
    private TarifaAlquilerDAO tarifaAlquilerDAO;
    @Mock
    private TarifaMembresiaDAO tarifaMembresiaDAO;
    @Mock
    private ValoracionesServicio valoracionesServicio;

    @InjectMocks
    private ReportesServicio servicio;

    private final CategoriaSocio alumno = DatosDePrueba.categoria(1, "Alumno UNSE", true);
    private final Socio socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO, alumno);
    private final Instalacion cancha = DatosDePrueba.instalacion(1, "Cancha de Fútbol 11",
            Instalacion.TipoAcceso.ARANCELADO, Instalacion.Estado.HABILITADA);
    private final Turno turno = DatosDePrueba.turno(5, cancha, LocalTime.of(10, 0), LocalTime.of(11, 0));

    private ReportesServicio.Rango r30;

    @BeforeEach
    void instalacionesYTurnos() {
        r30 = servicio.rango("30D", HOY);
        lenient().when(instalacionDAO.listarReservables()).thenReturn(List.of(cancha));
        lenient().when(turnoDAO.listarPorInstalacion(1)).thenReturn(List.of(turno));
    }

    private Reserva reserva(int id, Reserva.Estado estado, Pago pago) {
        Reserva r = DatosDePrueba.reserva(id, socio, turno, LocalDate.of(2026, 10, 20), estado);
        r.setIdPago(pago);
        return r;
    }

    private static Pago pago(int id, String monto, Pago.Estado estado, LocalDateTime validado) {
        Pago p = DatosDePrueba.pago(id, monto, estado);
        p.setFechaValidacion(validado);
        return p;
    }

    @Test
    void soloSonIngresosLosPagosConfirmadosYValidadosAunqueLaReservaSeHayaCancelado() {
        LocalDateTime validado = LocalDateTime.of(2026, 10, 10, 10, 0);
        SuscripcionSocio cuota = new SuscripcionSocio(30);
        cuota.setIdSocio(socio);
        cuota.setEstado(SuscripcionSocio.Estado.VIGENTE);
        cuota.setIdPago(pago(4, "5000", Pago.Estado.CONFIRMADO, validado));
        when(reservaDAO.listarParaReportes(r30.getPrevioDesde(), r30.getHasta())).thenReturn(List.of(
                reserva(1, Reserva.Estado.CONFIRMADA, pago(1, "20000", Pago.Estado.CONFIRMADO, validado)),
                reserva(2, Reserva.Estado.PENDIENTE_PAGO, pago(2, "20000", Pago.Estado.PENDIENTE_VALIDACION, null)),
                reserva(3, Reserva.Estado.RECHAZADA, pago(3, "20000", Pago.Estado.RECHAZADO, validado)),
                reserva(5, Reserva.Estado.CANCELADA, pago(5, "15000", Pago.Estado.CONFIRMADO, validado)),
                reserva(6, Reserva.Estado.CONFIRMADA, pago(6, "9999", Pago.Estado.CONFIRMADO, null)),
                reserva(7, Reserva.Estado.PENDIENTE_PAGO, null)));
        when(suscripcionDAO.listarTodasParaReportes()).thenReturn(List.of(cuota));

        ReportesServicio.Datos d = servicio.cargar(HOY, r30);

        assertEquals(3, d.ingresos.size());
        assertEquals(0, new BigDecimal("20000").compareTo(d.ingresos.get(0).monto));
        assertTrue(d.ingresos.get(0).reserva);
        assertEquals(0, new BigDecimal("15000").compareTo(d.ingresos.get(1).monto), "la cancelada conserva su pago");
        assertEquals(0, new BigDecimal("5000").compareTo(d.ingresos.get(2).monto));
        assertFalse(d.ingresos.get(2).reserva);
        assertEquals(LocalDate.of(2026, 10, 10), d.ingresos.get(2).fecha, "cuenta el día de la validación");
    }

    @Test
    void laTarifaPromedioDeUnaInstalacionEsElPromedioDeSusTarifasVigentes() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        when(tarifaAlquilerDAO.listarVigentes(HOY)).thenReturn(List.of(
                DatosDePrueba.tarifaAlquiler(1, cancha, alumno, "10000", desde, null),
                DatosDePrueba.tarifaAlquiler(2, cancha, DatosDePrueba.categoria(4, "Externo", false), "25000", desde,
                        null)));

        ReportesServicio.Datos d = servicio.cargar(HOY, r30);

        assertEquals(0, new BigDecimal("17500").compareTo(d.precioPromedio.get(1)));
    }

    @Test
    void laCuotaDeCadaTipoEsLaVigenteOCeroSiNoHay() {
        CategoriaSocio externo = DatosDePrueba.categoria(4, "Externo", false);
        TarifaMembresia cuota = new TarifaMembresia(1);
        cuota.setPrecio(new BigDecimal("5000"));
        when(categoriaDAO.listarTodos()).thenReturn(List.of(alumno, externo));
        when(tarifaMembresiaDAO.buscarVigente(1, HOY)).thenReturn(cuota);
        when(tarifaMembresiaDAO.buscarVigente(4, HOY)).thenReturn(null);

        ReportesServicio.Datos d = servicio.cargar(HOY, r30);

        assertEquals(List.of("Alumno UNSE", "Externo"), d.categorias);
        assertEquals(0, new BigDecimal("5000").compareTo(d.cuotas.get("Alumno UNSE")));
        assertEquals(0, BigDecimal.ZERO.compareTo(d.cuotas.get("Externo")));
    }

    @Test
    void cuentaLosSociosActivosYElTotalSinLosDadosDeBaja() {
        when(socioDAO.contarPorEstado(Socio.Estado.ACTIVO)).thenReturn(12L);
        when(socioDAO.listarSinBaja()).thenReturn(Collections.nCopies(15, socio));

        ReportesServicio.Datos d = servicio.cargar(HOY, r30);

        assertEquals(12, d.sociosActivos);
        assertEquals(15, d.sociosTotal);
    }

    @DisplayName("CP-45: la carga pide a la base solo el período y el previo, no toda la tabla de reservas")
    @Test
    void cp45_laCargaPideSoloElPeriodoYElPrevio() {
        servicio.cargar(HOY, r30);

        verify(reservaDAO).listarParaReportes(r30.getPrevioDesde(), r30.getHasta());
        verify(reservaDAO, never()).listarTodasParaAdmin();
    }

    @Test
    void cargaLosTurnosDeCadaInstalacionReservable() {
        ReportesServicio.Datos d = servicio.cargar(HOY, r30);

        assertEquals(List.of(turno), d.turnos.get(1));
        assertEquals(HOY, d.hoy);
    }
}
