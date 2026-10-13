package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.CarnetDigitalDAO;
import com.sigrid.sigrid.dao.CategoriaSocioDAO;
import com.sigrid.sigrid.dao.SuscripcionSocioDAO;
import com.sigrid.sigrid.dao.TarifaMembresiaDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dto.IngresosMembresias;
import com.sigrid.sigrid.dto.SuscripcionFila;
import com.sigrid.sigrid.dto.TarifaFila;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import com.sigrid.sigrid.repositorio.TarifaMembresia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lo que ve el administrador en el panel de membresías: cada suscripción con su estado y vencimiento, las que vencen
 * pronto, los ingresos de los últimos meses (con variación contra el mes anterior) y las cuotas vigentes.
 */
@ExtendWith(MockitoExtension.class)
class MembresiasServicioListadosTest {

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

    private final CategoriaSocio alumno = DatosDePrueba.categoria(1, "Alumno UNSE", true);
    private final CategoriaSocio externo = DatosDePrueba.categoria(4, "Externo", false);
    private Socio socio;

    @BeforeEach
    void unSocio() {
        socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO, alumno);
    }

    private SuscripcionSocio suscripcion(int id, SuscripcionSocio.Estado estado, LocalDate inicio, LocalDate vence) {
        SuscripcionSocio s = new SuscripcionSocio(id);
        s.setIdSocio(socio);
        s.setEstado(estado);
        s.setFechaInicio(inicio);
        s.setFechaVencimiento(vence);
        return s;
    }

    private SuscripcionFila unica(SuscripcionSocio s) {
        when(suscripcionDAO.listarTodas()).thenReturn(List.of(s));
        return servicio.listar(HOY).get(0);
    }

    // ---------- listado ----------

    @Test
    void ordenaPendientesVigentesVencidasYCanceladasConservandoElOrdenPorFecha() {
        when(suscripcionDAO.listarTodas()).thenReturn(List.of(
                suscripcion(40, SuscripcionSocio.Estado.CANCELADA, null, null),
                suscripcion(41, SuscripcionSocio.Estado.VENCIDA, HOY.minusDays(40), HOY.minusDays(10)),
                suscripcion(42, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(5), HOY.plusDays(25)),
                suscripcion(43, SuscripcionSocio.Estado.PENDIENTE_PAGO, null, null),
                suscripcion(44, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(6), HOY.plusDays(24))));

        List<SuscripcionFila> filas = servicio.listar(HOY);

        List<Integer> ids = new ArrayList<>();
        for (SuscripcionFila f : filas) {
            ids.add(f.getIdSuscripcion());
        }
        assertEquals(List.of(43, 42, 44, 41, 40), ids);
    }

    @Test
    void unaVigenteQueVenceHoyOMananaLoDiceYEstaPorVencer() {
        SuscripcionFila hoy = unica(suscripcion(42, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(30), HOY));
        SuscripcionFila manana = unica(
                suscripcion(42, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(29), HOY.plusDays(1)));

        assertEquals("Vence hoy", hoy.getNota());
        assertTrue(hoy.isPorVencer());
        assertEquals("Vence mañana", manana.getNota());
    }

    @Test
    void unaVigenteEstaPorVencerHastaSieteDiasInclusive() {
        SuscripcionFila siete = unica(
                suscripcion(42, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(23), HOY.plusDays(7)));
        SuscripcionFila ocho = unica(
                suscripcion(42, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(22), HOY.plusDays(8)));

        assertTrue(siete.isPorVencer());
        assertEquals("Vence en 7 días", siete.getNota());
        assertFalse(ocho.isPorVencer());
    }

    @Test
    void unaPendienteConComprobanteDiceQueHayQueValidarlo() {
        SuscripcionSocio s = suscripcion(43, SuscripcionSocio.Estado.PENDIENTE_PAGO, null, null);
        s.setIdPago(DatosDePrueba.pago(1, "5000", Pago.Estado.PENDIENTE_VALIDACION));

        SuscripcionFila f = unica(s);

        assertEquals("Pendiente de pago", f.getEstadoTexto());
        assertEquals("warn", f.getEstadoClase());
        assertEquals("Comprobante para validar", f.getNota());
        assertTrue(f.isConComprobante());
        assertEquals("$ 5.000", f.getMonto());
    }

    @Test
    void unaPendienteSinComprobanteEstaEsperandolo() {
        SuscripcionFila f = unica(suscripcion(43, SuscripcionSocio.Estado.PENDIENTE_PAGO, null, null));

        assertEquals("Esperando el comprobante", f.getNota());
        assertFalse(f.isConComprobante());
        assertNull(f.getMonto());
    }

    @Test
    void unaCanceladaPorComprobanteDenegadoLoDiceYUnaBajaTambien() {
        SuscripcionSocio denegada = suscripcion(40, SuscripcionSocio.Estado.CANCELADA, null, null);
        denegada.setIdPago(DatosDePrueba.pago(1, "5000", Pago.Estado.RECHAZADO));
        SuscripcionSocio baja = suscripcion(41, SuscripcionSocio.Estado.CANCELADA, HOY.minusDays(5), HOY.plusDays(10));

        assertEquals("Comprobante denegado", unica(denegada).getNota());
        assertEquals("Baja de la membresía", unica(baja).getNota());
    }

    @Test
    void unaVencidaAvisaQueElCarnetSeCongelaSiNoRenovo() {
        SuscripcionFila f = unica(
                suscripcion(41, SuscripcionSocio.Estado.VENCIDA, HOY.minusDays(40), HOY.minusDays(10)));

        assertEquals("Vencida", f.getEstadoTexto());
        assertEquals("off", f.getEstadoClase());
        assertEquals("Carnet congelado si no renovó", f.getNota());
        assertEquals("02/10/2026", f.getVence());
    }

    @Test
    void laFilaMuestraElSocioSusInicialesYSuTipo() {
        SuscripcionFila f = unica(suscripcion(42, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(5), HOY.plusDays(25)));

        assertEquals("Lucía Gómez", f.getSocio());
        assertEquals("LG", f.getIniciales());
        assertEquals("30123456", f.getDni());
        assertEquals("Alumno UNSE", f.getTipo());
    }

    @Test
    void laListaDeProximosVencimientosPideAlDaoDesdeHoyHastaLaFechaLimite() {
        when(suscripcionDAO.listarVigentesQueVencenEntre(HOY, HOY.plusDays(7))).thenReturn(List.of(
                suscripcion(42, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(25), HOY.plusDays(5))));

        List<SuscripcionFila> filas = servicio.porVencer(HOY, 7);

        assertEquals(1, filas.size());
        verify(suscripcionDAO).listarVigentesQueVencenEntre(HOY, HOY.plusDays(7));
    }

    // ---------- ingresos por mes ----------

    private static Object[] pago(String monto, LocalDateTime fecha, String tipo) {
        return new Object[]{new BigDecimal(monto), fecha, tipo};
    }

    private void tiposDeSocio() {
        when(categoriaDAO.listarTodos()).thenReturn(List.of(alumno, externo));
    }

    @Test
    void elIngresoDelMesSeComparaConElMesAnteriorYSeReparteEntreLosTipos() {
        tiposDeSocio();
        List<Object[]> filas = new ArrayList<>();
        filas.add(pago("10000", LocalDateTime.of(2026, 8, 15, 10, 0), "Alumno UNSE"));
        filas.add(pago("20000", LocalDateTime.of(2026, 9, 10, 10, 0), "Externo"));
        filas.add(pago("15000", LocalDateTime.of(2026, 10, 2, 10, 0), "Alumno UNSE"));
        filas.add(pago("15000", LocalDateTime.of(2026, 10, 5, 10, 0), "Externo"));
        when(suscripcionDAO.ingresosDesde(LocalDateTime.of(2026, 8, 1, 0, 0))).thenReturn(filas);

        IngresosMembresias i = servicio.ingresos(YearMonth.of(2026, 10), 3);

        assertEquals("$ 30.000", i.getTotalMes());
        assertEquals("+50 % respecto de septiembre", i.getComparacion());
        assertEquals("up", i.getTendencia());
        assertEquals(3, i.getMeses().size());
        assertEquals(33, i.getMeses().get(0).getPorcentaje());
        assertEquals(67, i.getMeses().get(1).getPorcentaje());
        assertEquals(100, i.getMeses().get(2).getPorcentaje());
        assertTrue(i.getMeses().get(2).isActual());
        assertFalse(i.getMeses().get(0).isActual());
        assertEquals("$ 15.000", i.getPorTipo().get(0).getMonto());
        assertEquals(50, i.getPorTipo().get(0).getPorcentaje());
    }

    @Test
    void siElMesBajoLaComparacionEsNegativa() {
        tiposDeSocio();
        List<Object[]> filas = new ArrayList<>();
        filas.add(pago("40000", LocalDateTime.of(2026, 9, 10, 10, 0), "Alumno UNSE"));
        filas.add(pago("30000", LocalDateTime.of(2026, 10, 2, 10, 0), "Alumno UNSE"));
        when(suscripcionDAO.ingresosDesde(LocalDateTime.of(2026, 9, 1, 0, 0))).thenReturn(filas);

        IngresosMembresias i = servicio.ingresos(YearMonth.of(2026, 10), 2);

        assertEquals("-25 % respecto de septiembre", i.getComparacion());
        assertEquals("down", i.getTendencia());
    }

    @Test
    void siElMesAnteriorNoTuvoIngresosNoHayPorcentaje() {
        tiposDeSocio();
        List<Object[]> filas = new ArrayList<>();
        filas.add(pago("30000", LocalDateTime.of(2026, 10, 2, 10, 0), "Alumno UNSE"));
        when(suscripcionDAO.ingresosDesde(LocalDateTime.of(2026, 9, 1, 0, 0))).thenReturn(filas);

        IngresosMembresias i = servicio.ingresos(YearMonth.of(2026, 10), 2);

        assertEquals("Sin ingresos en septiembre", i.getComparacion());
        assertEquals("flat", i.getTendencia());
    }

    @Test
    void sinIngresosEnNingunMesLoDice() {
        tiposDeSocio();
        when(suscripcionDAO.ingresosDesde(LocalDateTime.of(2026, 9, 1, 0, 0))).thenReturn(new ArrayList<Object[]>());

        IngresosMembresias i = servicio.ingresos(YearMonth.of(2026, 10), 2);

        assertEquals("$ 0", i.getTotalMes());
        assertEquals("Todavía sin ingresos este mes", i.getComparacion());
        assertEquals(0, i.getMeses().get(1).getPorcentaje());
        assertEquals(0, i.getPorTipo().get(0).getPorcentaje());
    }

    // ---------- cuotas ----------

    @Test
    void lasCuotasSonLaTarifaVigenteDeCadaTipoONulaSiNoHay() {
        tiposDeSocio();
        TarifaMembresia cuota = new TarifaMembresia(1);
        cuota.setPrecio(new BigDecimal("5000"));
        when(tarifaDAO.buscarVigente(1, HOY)).thenReturn(cuota);
        when(tarifaDAO.buscarVigente(4, HOY)).thenReturn(null);

        List<TarifaFila> tarifas = servicio.tarifas(HOY);

        assertEquals("Alumno UNSE", tarifas.get(0).getTipo());
        assertEquals("$ 5.000", tarifas.get(0).getPrecio());
        assertNull(tarifas.get(1).getPrecio());
    }

    @Test
    void losTiposDeSocioSonLosNombresDeLasCategorias() {
        tiposDeSocio();

        assertEquals(List.of("Alumno UNSE", "Externo"), servicio.tiposDeSocio());
    }

    // ---------- vigencia de las cuotas (CC-03) ----------

    @DisplayName("CP-36: cada cuota muestra desde cuándo rige y el próximo cambio programado")
    @Test
    void cp36_cadaCuotaMuestraDesdeCuandoRigeYElProximoCambio() {
        tiposDeSocio();
        TarifaMembresia cuota = new TarifaMembresia(1);
        cuota.setPrecio(new BigDecimal("5000"));
        cuota.setVigenteDesde(LocalDate.of(2026, 8, 1));
        TarifaMembresia siguiente = new TarifaMembresia(2);
        siguiente.setPrecio(new BigDecimal("6000"));
        siguiente.setVigenteDesde(LocalDate.of(2026, 12, 1));
        when(tarifaDAO.buscarVigente(1, HOY)).thenReturn(cuota);
        when(tarifaDAO.buscarProxima(1, HOY)).thenReturn(siguiente);
        when(tarifaDAO.buscarVigente(4, HOY)).thenReturn(null);

        List<TarifaFila> tarifas = servicio.tarifas(HOY);

        assertEquals("Rige desde el 01/08/2026", tarifas.get(0).getVigencia());
        assertEquals("Desde el 01/12/2026: $ 6.000", tarifas.get(0).getProximo());
        assertNull(tarifas.get(1).getVigencia());
        assertEquals("Sin cambios programados", tarifas.get(1).getProximo());
    }
}
