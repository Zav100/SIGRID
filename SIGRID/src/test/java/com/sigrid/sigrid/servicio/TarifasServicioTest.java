package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.CategoriaSocioDAO;
import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.TarifaAlquilerDAO;
import com.sigrid.sigrid.dao.TarifaMembresiaDAO;
import com.sigrid.sigrid.dto.HistorialTarifa;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.TarifaAlquiler;
import com.sigrid.sigrid.repositorio.TarifaMembresia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cambio de precios: el alumno es el más barato y el externo el más caro; un precio no se edita, se agrega uno nuevo
 * y el anterior se cierra el día previo; no se repite una fecha de vigencia.
 */
@ExtendWith(MockitoExtension.class)
class TarifasServicioTest {

    private static final LocalDate INICIO_DEL_ANIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate PROXIMO_MES = LocalDate.of(2026, 11, 1);

    private final CategoriaSocio alumno = DatosDePrueba.categoria(1, "Alumno UNSE", true);
    private final CategoriaSocio docente = DatosDePrueba.categoria(2, "Docente UNSE", true);
    private final CategoriaSocio noDocente = DatosDePrueba.categoria(3, "No Docente UNSE", true);
    private final CategoriaSocio externo = DatosDePrueba.categoria(4, "Externo", false);

    private final Instalacion cancha = DatosDePrueba.instalacion(1, "Cancha de Fútbol 11",
            Instalacion.TipoAcceso.ARANCELADO, Instalacion.Estado.HABILITADA);

    @Mock
    private TarifaAlquilerDAO alquilerDAO;
    @Mock
    private TarifaMembresiaDAO membresiaDAO;
    @Mock
    private CategoriaSocioDAO categoriaDAO;
    @Mock
    private InstalacionDAO instalacionDAO;

    @InjectMocks
    private TarifasServicio servicio;

    /** Lo que rige hoy en la cancha: alumno $10.000, docente y no docente $15.000, externo $25.000. */
    private List<TarifaAlquiler> historial;

    @BeforeEach
    void cargarDatosBase() {
        historial = new ArrayList<>();
        historial.add(DatosDePrueba.tarifaAlquiler(1, cancha, alumno, "10000", INICIO_DEL_ANIO, null));
        historial.add(DatosDePrueba.tarifaAlquiler(2, cancha, docente, "15000", INICIO_DEL_ANIO, null));
        historial.add(DatosDePrueba.tarifaAlquiler(3, cancha, noDocente, "15000", INICIO_DEL_ANIO, null));
        historial.add(DatosDePrueba.tarifaAlquiler(4, cancha, externo, "25000", INICIO_DEL_ANIO, null));

        lenient().when(categoriaDAO.listarTodos()).thenReturn(List.of(externo, noDocente, alumno, docente));
        lenient().when(instalacionDAO.buscarPorId(1)).thenReturn(cancha);
        lenient().when(alquilerDAO.listarPorInstalacion(1)).thenReturn(historial);
    }

    private static Map<Integer, String> precios(String alumno, String docente, String noDocente, String externo) {
        Map<Integer, String> precios = new LinkedHashMap<>();
        precios.put(1, alumno);
        precios.put(2, docente);
        precios.put(3, noDocente);
        precios.put(4, externo);
        return precios;
    }

    private static List<BigDecimal> importes(String... valores) {
        List<BigDecimal> importes = new ArrayList<>();
        for (String v : valores) {
            importes.add(v == null ? null : new BigDecimal(v));
        }
        return importes;
    }

    private List<CategoriaSocio> categoriasEnOrden() {
        return List.of(alumno, docente, noDocente, externo);
    }

    private static void assertPrecio(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "precio esperado " + esperado + " pero fue " + real);
    }

    // ---------- orden de precios: alumno el más barato, externo el más caro ----------

    @Test
    void ordenValidoCuandoElAlumnoPagaMenosYElExternoMas() {
        assertTrue(TarifasServicio.ordenValido(categoriasEnOrden(), importes("10000", "15000", "15000", "25000")));
    }

    @Test
    void ordenInvalidoCuandoElAlumnoPagaLoMismoQueElExterno() {
        assertFalse(TarifasServicio.ordenValido(categoriasEnOrden(), importes("20000", "20000", "20000", "20000")));
    }

    @Test
    void ordenInvalidoCuandoElAlumnoPagaMasQueUnDocente() {
        assertFalse(TarifasServicio.ordenValido(categoriasEnOrden(), importes("18000", "15000", "20000", "25000")));
    }

    @Test
    void ordenInvalidoCuandoElAlumnoPagaLoMismoQueUnNoDocente() {
        assertFalse(TarifasServicio.ordenValido(categoriasEnOrden(), importes("15000", "18000", "15000", "25000")));
    }

    @Test
    void ordenInvalidoCuandoElExternoPagaMenosQueUnNoDocente() {
        assertFalse(TarifasServicio.ordenValido(categoriasEnOrden(), importes("10000", "15000", "30000", "25000")));
    }

    @Test
    void ordenInvalidoCuandoElExternoPagaLoMismoQueUnDocente() {
        assertFalse(TarifasServicio.ordenValido(categoriasEnOrden(), importes("10000", "25000", "20000", "25000")));
    }

    @Test
    void ordenValidoSiTodaviaFaltaCargarUnPrecio() {
        assertTrue(TarifasServicio.ordenValido(categoriasEnOrden(), importes("10000", null, "15000", "25000")));
    }

    // ---------- cambios de alquiler ----------

    @Test
    void agregaUnPrecioNuevoPorTipoDeSocioConLaFechaDesdeLaQueRige() {
        ArgumentCaptor<TarifaAlquiler> nueva = ArgumentCaptor.forClass(TarifaAlquiler.class);

        String error = servicio.cambiarAlquiler(1, precios("12000", "18000", "18000", "30000"), PROXIMO_MES, HOY);

        assertNull(error);
        verify(alquilerDAO, times(4)).crear(nueva.capture());
        for (TarifaAlquiler t : nueva.getAllValues()) {
            assertEquals(PROXIMO_MES, t.getVigenteDesde());
            assertNull(t.getVigenteHasta());
            assertEquals(cancha, t.getIdInstalacion());
        }
        assertPrecio("12000", nueva.getAllValues().get(0).getPrecio());
        assertEquals(alumno, nueva.getAllValues().get(0).getIdCategoriaSocio());
        assertPrecio("30000", nueva.getAllValues().get(3).getPrecio());
        assertEquals(externo, nueva.getAllValues().get(3).getIdCategoriaSocio());
    }

    @Test
    void cierraElPrecioAnteriorElDiaPrevioALaNuevaVigencia() {
        servicio.cambiarAlquiler(1, precios("12000", "18000", "18000", "30000"), PROXIMO_MES, HOY);

        for (TarifaAlquiler anterior : historial) {
            assertEquals(LocalDate.of(2026, 10, 31), anterior.getVigenteHasta());
        }
    }

    @Test
    void nuncaEditaElPrecioDelRegistroAnterior() {
        servicio.cambiarAlquiler(1, precios("12000", "18000", "18000", "30000"), PROXIMO_MES, HOY);

        assertPrecio("10000", historial.get(0).getPrecio());
        assertPrecio("15000", historial.get(1).getPrecio());
        assertPrecio("15000", historial.get(2).getPrecio());
        assertPrecio("25000", historial.get(3).getPrecio());
    }

    @Test
    void agregaSoloLaFilaDeLosTiposDeSocioQueCambiaron() {
        ArgumentCaptor<TarifaAlquiler> nueva = ArgumentCaptor.forClass(TarifaAlquiler.class);

        String error = servicio.cambiarAlquiler(1, precios("11000", "15000", "15000", "25000"), PROXIMO_MES, HOY);

        assertNull(error);
        verify(alquilerDAO, times(1)).crear(nueva.capture());
        assertEquals(alumno, nueva.getValue().getIdCategoriaSocio());
        assertNull(historial.get(1).getVigenteHasta(), "el docente no cambió: su precio sigue abierto");
    }

    @Test
    void aceptaQueElCambioRijaDesdeHoyYCierraElAnteriorAyer() {
        String error = servicio.cambiarAlquiler(1, precios("12000", "18000", "18000", "30000"), HOY, HOY);

        assertNull(error);
        assertEquals(HOY.minusDays(1), historial.get(0).getVigenteHasta());
    }

    @Test
    void rechazaUnaFechaAnteriorAHoy() {
        String error = servicio.cambiarAlquiler(1, precios("12000", "18000", "18000", "30000"), HOY.minusDays(1), HOY);

        assertEquals("La fecha no puede ser anterior a hoy: los precios no se cambian hacia atrás.", error);
        verify(alquilerDAO, never()).crear(any());
    }

    @Test
    void rechazaElCambioSinFecha() {
        String error = servicio.cambiarAlquiler(1, precios("12000", "18000", "18000", "30000"), null, HOY);

        assertEquals("Elegí desde qué día rige el precio nuevo.", error);
        verify(alquilerDAO, never()).crear(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-100", "12500.555", "doce mil", "   ", "100000000"})
    void rechazaUnPrecioQueNoEsUnNumeroMayorACeroConHastaDosDecimales(String precioInvalido) {
        String error = servicio.cambiarAlquiler(1, precios(precioInvalido, "18000", "18000", "30000"), PROXIMO_MES, HOY);

        assertEquals("Completá el precio de Alumno UNSE con un número mayor a cero (hasta 2 decimales).", error);
        verify(alquilerDAO, never()).crear(any());
    }

    @Test
    void aceptaElPrecioMaximoQueEntraEnLaColumnaDeLaBase() {
        ArgumentCaptor<TarifaAlquiler> nueva = ArgumentCaptor.forClass(TarifaAlquiler.class);

        String error = servicio.cambiarAlquiler(1, precios("10000", "15000", "15000", "99999999.99"), PROXIMO_MES, HOY);

        assertNull(error);
        verify(alquilerDAO).crear(nueva.capture());
        assertPrecio("99999999.99", nueva.getValue().getPrecio());
    }

    @Test
    void aceptaLaComaComoSeparadorDecimal() {
        ArgumentCaptor<TarifaAlquiler> nueva = ArgumentCaptor.forClass(TarifaAlquiler.class);

        String error = servicio.cambiarAlquiler(1, precios("12000,50", "15000", "15000", "25000"), PROXIMO_MES, HOY);

        assertNull(error);
        verify(alquilerDAO).crear(nueva.capture());
        assertPrecio("12000.50", nueva.getValue().getPrecio());
    }

    @Test
    void rechazaCuandoNingunPrecioCambia() {
        String error = servicio.cambiarAlquiler(1, precios("10000", "15000", "15000", "25000"), PROXIMO_MES, HOY);

        assertEquals("No cambiaste ningún precio.", error);
        verify(alquilerDAO, never()).crear(any());
    }

    @Test
    void rechazaUnOrdenEnElQueElAlumnoPagaMasQueElExterno() {
        String error = servicio.cambiarAlquiler(1, precios("30000", "18000", "18000", "25000"), PROXIMO_MES, HOY);

        assertEquals("El alumno tiene que pagar menos que los demás y el externo más que los demás.", error);
        verify(alquilerDAO, never()).crear(any());
        assertNull(historial.get(0).getVigenteHasta(), "no se cierra nada si el cambio se rechaza");
    }

    @Test
    void rechazaUnaFechaIgualALaDeUnPrecioYaProgramado() {
        historial.add(DatosDePrueba.tarifaAlquiler(5, cancha, alumno, "10500", PROXIMO_MES, null));
        historial.get(0).setVigenteHasta(LocalDate.of(2026, 10, 31));

        String error = servicio.cambiarAlquiler(1, precios("12000", "15000", "15000", "25000"), PROXIMO_MES, HOY);

        assertEquals("Ya hay un precio de Alumno UNSE desde el 01/11/2026: elegí una fecha posterior.", error);
        verify(alquilerDAO, never()).crear(any());
    }

    @Test
    void rechazaUnaFechaAnteriorALaDeUnPrecioYaProgramado() {
        historial.add(DatosDePrueba.tarifaAlquiler(5, cancha, alumno, "10500", PROXIMO_MES, null));
        historial.get(0).setVigenteHasta(LocalDate.of(2026, 10, 31));

        String error = servicio.cambiarAlquiler(1, precios("12000", "15000", "15000", "25000"),
                LocalDate.of(2026, 10, 20), HOY);

        assertEquals("Ya hay un precio de Alumno UNSE desde el 01/11/2026: elegí una fecha posterior.", error);
        verify(alquilerDAO, never()).crear(any());
    }

    @Test
    void rechazaCambiarPreciosDeUnaInstalacionQueNoExiste() {
        when(instalacionDAO.buscarPorId(99)).thenReturn(null);

        String error = servicio.cambiarAlquiler(99, precios("12000", "18000", "18000", "30000"), PROXIMO_MES, HOY);

        assertEquals("La instalación no existe.", error);
    }

    @Test
    void rechazaCambiarPreciosSinIndicarLaInstalacion() {
        String error = servicio.cambiarAlquiler(null, precios("12000", "18000", "18000", "30000"), PROXIMO_MES, HOY);

        assertEquals("La instalación no existe.", error);
    }

    @Test
    void rechazaCambiarPreciosDeUnaInstalacionDeAccesoLibre() {
        Instalacion pileta = DatosDePrueba.instalacion(7, "Pileta", Instalacion.TipoAcceso.LIBRE,
                Instalacion.Estado.HABILITADA);
        when(instalacionDAO.buscarPorId(7)).thenReturn(pileta);

        String error = servicio.cambiarAlquiler(7, precios("12000", "18000", "18000", "30000"), PROXIMO_MES, HOY);

        assertEquals("Esta instalación no se reserva: no tiene precios.", error);
        verify(alquilerDAO, never()).crear(any());
    }

    // ---------- cuota de membresía ----------

    @Test
    void cambiaLaCuotaDeMembresiaCerrandoLaAnteriorYAgregandoLaNueva() {
        List<TarifaMembresia> cuotas = new ArrayList<>();
        cuotas.add(cuota(1, alumno, "5000"));
        cuotas.add(cuota(2, docente, "8000"));
        cuotas.add(cuota(3, noDocente, "8000"));
        cuotas.add(cuota(4, externo, "12000"));
        when(membresiaDAO.listarHistorial()).thenReturn(cuotas);
        ArgumentCaptor<TarifaMembresia> nueva = ArgumentCaptor.forClass(TarifaMembresia.class);

        String error = servicio.cambiarMembresia(precios("6000", "9000", "9000", "14000"), PROXIMO_MES, HOY);

        assertNull(error);
        verify(membresiaDAO, times(4)).crear(nueva.capture());
        assertPrecio("6000", nueva.getAllValues().get(0).getPrecio());
        assertEquals(PROXIMO_MES, nueva.getAllValues().get(0).getVigenteDesde());
        assertEquals(LocalDate.of(2026, 10, 31), cuotas.get(0).getVigenteHasta());
        assertPrecio("5000", cuotas.get(0).getPrecio());
    }

    @Test
    void exigeCargarLosCuatroTiposDeSocioJuntos() {
        when(membresiaDAO.listarHistorial()).thenReturn(new ArrayList<>());

        String error = servicio.cambiarMembresia(precios("6000", "9000", "9000", null), PROXIMO_MES, HOY);

        assertEquals("Completá el precio de Externo con un número mayor a cero (hasta 2 decimales).", error);
        verify(membresiaDAO, never()).crear(any());
    }

    private TarifaMembresia cuota(int id, CategoriaSocio categoria, String precio) {
        TarifaMembresia t = new TarifaMembresia(id);
        t.setIdCategoriaSocio(categoria);
        t.setPrecio(new BigDecimal(precio));
        t.setVigenteDesde(INICIO_DEL_ANIO);
        return t;
    }

    // ---------- lectura ----------

    @Test
    void muestraLoQueRigeHoyYNoElPrecioProgramado() {
        historial.get(0).setVigenteHasta(LocalDate.of(2026, 10, 31));
        historial.add(DatosDePrueba.tarifaAlquiler(5, cancha, alumno, "12000", PROXIMO_MES, null));

        Map<Integer, String> vigentes = servicio.vigentesAlquiler(1, HOY);

        assertEquals("10000", vigentes.get(1));
        assertEquals("15000", vigentes.get(2));
        assertEquals("25000", vigentes.get(4));
    }

    @Test
    void dejaVacioElTipoDeSocioQueTodaviaNoTienePrecio() {
        historial.remove(3); // sin precio para el externo

        Map<Integer, String> vigentes = servicio.vigentesAlquiler(1, HOY);

        assertEquals("", vigentes.get(4));
    }

    @Test
    void elHistorialMarcaComoProgramadoElCambioFuturo() {
        historial.get(0).setVigenteHasta(LocalDate.of(2026, 10, 31));
        historial.add(DatosDePrueba.tarifaAlquiler(5, cancha, alumno, "12000", PROXIMO_MES, null));

        List<HistorialTarifa> lista = servicio.historialAlquiler(1, HOY);

        assertEquals(2, lista.size());
        assertEquals("Desde el 01/11/2026", lista.get(0).getCuando(), "el más reciente va primero");
        assertTrue(lista.get(0).isProgramado());
        assertFalse(lista.get(1).isProgramado());
    }
}
