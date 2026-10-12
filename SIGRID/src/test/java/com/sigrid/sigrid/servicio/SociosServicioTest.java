package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.CarnetDigitalDAO;
import com.sigrid.sigrid.dao.CategoriaSocioDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.SocioDAO;
import com.sigrid.sigrid.dao.SuscripcionSocioDAO;
import com.sigrid.sigrid.dao.TarifaMembresiaDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dto.Dato;
import com.sigrid.sigrid.dto.SocioDetalle;
import com.sigrid.sigrid.dto.SocioEdicion;
import com.sigrid.sigrid.dto.SocioFila;
import com.sigrid.sigrid.repositorio.CarnetDigital;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import com.sigrid.sigrid.repositorio.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Panel de socios del administrador: listado con "por vencer", ficha de solo lectura, edición con DNI y correo
 * únicos, legajo obligatorio por categoría y baja lógica.
 */
@ExtendWith(MockitoExtension.class)
class SociosServicioTest {

    @Mock
    private SocioDAO socioDAO;
    @Mock
    private SuscripcionSocioDAO suscripcionSocioDAO;
    @Mock
    private CategoriaSocioDAO categoriaSocioDAO;
    @Mock
    private CarnetDigitalDAO carnetDAO;
    @Mock
    private ReservaDAO reservaDAO;
    @Mock
    private TarifaMembresiaDAO tarifaMembresiaDAO;
    @Mock
    private UsuarioDAO usuarioDAO;

    @InjectMocks
    private SociosServicio servicio;

    private final CategoriaSocio alumno = DatosDePrueba.categoria(1, "Alumno UNSE", true);
    private final CategoriaSocio externo = DatosDePrueba.categoria(4, "Externo", false);
    private Socio socio;

    @BeforeEach
    void armarUnSocioActivo() {
        socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO, alumno);
        socio.getIdUsuario().setFechaAlta(LocalDateTime.of(2026, 3, 15, 9, 0));
        socio.setTelefono("3854123456");
        socio.setLegajo("A-1042");
        socio.setFechaNacimiento(LocalDate.of(2000, 5, 14));
        lenient().when(socioDAO.buscarPorId(7)).thenReturn(socio);
        lenient().when(categoriaSocioDAO.buscarPorNombre("Alumno UNSE")).thenReturn(alumno);
        lenient().when(categoriaSocioDAO.buscarPorNombre("Externo")).thenReturn(externo);
    }

    // ---------- listado ----------

    private static Object[] vencimiento(int idSocio, LocalDate fecha) {
        return new Object[]{idSocio, fecha};
    }

    @Test
    void marcaPorVencerALosVigentesQueVencenDentroDelPlazo() {
        when(socioDAO.listarSinBaja()).thenReturn(List.of(socio));
        when(suscripcionSocioDAO.ultimosVencimientos())
                .thenReturn(Collections.singletonList(vencimiento(7, HOY.plusDays(7))));

        SocioFila f = servicio.listar(HOY, 7).get(0);

        assertTrue(f.isVigente());
        assertTrue(f.isPorVencer(), "vence justo dentro de 7 días");
        assertEquals("19/10/2026", f.getVence());
    }

    @Test
    void noMarcaPorVencerSiVenceUnDiaDespuesDelPlazo() {
        when(socioDAO.listarSinBaja()).thenReturn(List.of(socio));
        when(suscripcionSocioDAO.ultimosVencimientos())
                .thenReturn(Collections.singletonList(vencimiento(7, HOY.plusDays(8))));

        assertFalse(servicio.listar(HOY, 7).get(0).isPorVencer());
    }

    @Test
    void unSocioSinMembresiaVigenteNoEstaPorVencer() {
        socio.setEstado(Socio.Estado.NO_ACTIVO);
        when(socioDAO.listarSinBaja()).thenReturn(List.of(socio));
        when(suscripcionSocioDAO.ultimosVencimientos())
                .thenReturn(Collections.singletonList(vencimiento(7, HOY.plusDays(2))));

        SocioFila f = servicio.listar(HOY, 7).get(0);

        assertFalse(f.isVigente());
        assertFalse(f.isPorVencer());
    }

    @Test
    void unSocioQueNuncaTuvoMembresiaNoTieneFechaDeVencimiento() {
        socio.setEstado(Socio.Estado.NO_ACTIVO);
        when(socioDAO.listarSinBaja()).thenReturn(List.of(socio));
        when(suscripcionSocioDAO.ultimosVencimientos()).thenReturn(Collections.emptyList());

        SocioFila f = servicio.listar(HOY, 7).get(0);

        assertNull(f.getVence());
        assertEquals("15/03/2026", f.getAlta());
        assertEquals("Alumno UNSE", f.getCategoria());
    }

    // ---------- ficha (solo lectura) ----------

    private static String valorDe(List<Dato> datos, String etiqueta) {
        for (Dato d : datos) {
            if (d.getEtiqueta().equals(etiqueta)) {
                return d.getValor();
            }
        }
        throw new AssertionError("falta el dato " + etiqueta);
    }

    private SuscripcionSocio suscripcion(int id, SuscripcionSocio.Estado estado, LocalDate inicio, LocalDate vence,
            Pago pago) {
        SuscripcionSocio s = new SuscripcionSocio(id);
        s.setIdSocio(socio);
        s.setEstado(estado);
        s.setFechaInicio(inicio);
        s.setFechaVencimiento(vence);
        s.setIdPago(pago);
        s.setFechaSolicitud(LocalDateTime.of(2026, 9, 20, 12, 0));
        return s;
    }

    @Test
    void laFichaDeUnSocioVigenteMuestraCuantosDiasFaltan() {
        when(suscripcionSocioDAO.listarPorSocio(7)).thenReturn(List.of(
                suscripcion(30, SuscripcionSocio.Estado.VIGENTE, LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 23), null)));

        SocioDetalle d = servicio.detalle(7, HOY);

        assertTrue(d.isVigente());
        assertEquals("Vigente", valorDe(d.getMembresia(), "Estado"));
        assertEquals("23/10/2026 · faltan 11 días", valorDe(d.getMembresia(), "Vence"));
        assertEquals("23/09/2026", valorDe(d.getMembresia(), "Socio desde"));
    }

    @ParameterizedTest
    @CsvSource({"2026-10-13,vence mañana", "2026-10-12,vence hoy",
        "2026-10-11,venció ayer", "2026-10-09,venció hace 3 días"})
    void laFichaDescribeElVencimientoCercano(String vence, String esperado) {
        socio.setEstado(Socio.Estado.NO_ACTIVO);
        when(suscripcionSocioDAO.listarPorSocio(7)).thenReturn(List.of(
                suscripcion(30, SuscripcionSocio.Estado.VENCIDA, LocalDate.of(2026, 9, 1), LocalDate.parse(vence), null)));

        SocioDetalle d = servicio.detalle(7, HOY);

        assertTrue(valorDe(d.getMembresia(), "Vence").endsWith(esperado));
    }

    @Test
    void laFichaDeUnSocioSinSuscripcionesDiceSinMembresia() {
        socio.setEstado(Socio.Estado.NO_ACTIVO);
        when(suscripcionSocioDAO.listarPorSocio(7)).thenReturn(Collections.emptyList());

        SocioDetalle d = servicio.detalle(7, HOY);

        assertEquals("Sin membresía", valorDe(d.getMembresia(), "Estado"));
        assertEquals("Todavía no tuvo una membresía", valorDe(d.getMembresia(), "Socio desde"));
        assertEquals("—", valorDe(d.getMembresia(), "Vence"));
    }

    @Test
    void laFichaDeUnSocioConMembresiaVencidaDiceExpirada() {
        socio.setEstado(Socio.Estado.NO_ACTIVO);
        when(suscripcionSocioDAO.listarPorSocio(7)).thenReturn(List.of(
                suscripcion(30, SuscripcionSocio.Estado.VENCIDA, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), null)));

        assertEquals("Expirada", valorDe(servicio.detalle(7, HOY).getMembresia(), "Estado"));
    }

    @Test
    void cuentaSoloLasCuotasConPagoConfirmadoYValidado() {
        Pago confirmado = DatosDePrueba.pago(1, "5000", Pago.Estado.CONFIRMADO);
        confirmado.setFechaValidacion(LocalDateTime.of(2026, 9, 23, 10, 0));
        Pago pendiente = DatosDePrueba.pago(2, "5000", Pago.Estado.PENDIENTE_VALIDACION);
        when(suscripcionSocioDAO.listarPorSocio(7)).thenReturn(List.of(
                suscripcion(31, SuscripcionSocio.Estado.PENDIENTE_PAGO, null, null, pendiente),
                suscripcion(30, SuscripcionSocio.Estado.VIGENTE, LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 23),
                        confirmado)));

        SocioDetalle d = servicio.detalle(7, HOY);

        assertEquals("1", valorDe(d.getMembresia(), "Cuotas pagadas"));
        assertEquals("$ 5.000 · 23/09/2026", valorDe(d.getMembresia(), "Último pago"));
    }

    @Test
    void elCarnetCongeladoSeMuestraComoCongelado() {
        CarnetDigital carnet = new CarnetDigital(70);
        carnet.setTipoCarnet(CarnetDigital.TipoCarnet.ESTUDIANTIL);
        carnet.setEstado(CarnetDigital.Estado.INACTIVO);
        when(carnetDAO.buscarPorSocio(7)).thenReturn(carnet);

        assertEquals("Estudiantil · Congelado", valorDe(servicio.detalle(7, HOY).getMembresia(), "Carnet digital"));
    }

    @Test
    void noHayFichaDeUnSocioInexistenteNiDadoDeBaja() {
        when(socioDAO.buscarPorId(99)).thenReturn(null);
        socio.setBajaLogica(true);

        assertNull(servicio.detalle(99, HOY));
        assertNull(servicio.detalle(7, HOY));
    }

    // ---------- números e iniciales ----------

    @Test
    void elNumeroDeSocioLlevaCincoDigitos() {
        assertEquals("#00007", SociosServicio.numero(7));
        assertEquals("#12345", SociosServicio.numero(12345));
    }

    @Test
    void lasInicialesSonLasDelNombreYElApellido() {
        Usuario u = DatosDePrueba.usuario(1, "lucía", "gómez", "lucia.gomez@ejemplo.com");

        assertEquals("LG", SociosServicio.iniciales(u));
    }

    @Test
    void sinNombreNiApellidoLaInicialEsLaDelCorreo() {
        Usuario u = DatosDePrueba.usuario(1, null, "  ", "martin.sosa@ejemplo.com");

        assertEquals("M", SociosServicio.iniciales(u));
    }

    // ---------- edición ----------

    private SocioEdicion edicionValida() {
        SocioEdicion e = new SocioEdicion();
        e.setIdSocio(7);
        e.setNombre("Lucía Belén");
        e.setApellido("Gómez");
        e.setEmail("lucia.belen@ejemplo.com");
        e.setTelefono("3855001122");
        e.setDni("30123456");
        e.setNacimiento("2000-05-14");
        e.setCategoria("Alumno UNSE");
        e.setLegajo("A-2001");
        return e;
    }

    @Test
    void paraEditarDevuelveLosDatosActualesDelSocio() {
        SocioEdicion e = servicio.paraEditar(7);

        assertEquals("Lucía", e.getNombre());
        assertEquals("lucia.gomez@ejemplo.com", e.getEmail());
        assertEquals("30123456", e.getDni());
        assertEquals("2000-05-14", e.getNacimiento());
        assertEquals("Alumno UNSE", e.getCategoria());
        assertEquals("A-1042", e.getLegajo());
    }

    @Test
    void noSePuedeEditarUnSocioDadoDeBaja() {
        socio.setBajaLogica(true);

        assertNull(servicio.paraEditar(7));
        assertEquals("Ese socio ya no estaba en el listado.", servicio.guardar(edicionValida(), HOY));
    }

    @Test
    void guardaLosCambiosDelSocioYDeSuCuenta() {
        String error = servicio.guardar(edicionValida(), HOY);

        assertNull(error);
        assertEquals("Lucía Belén", socio.getIdUsuario().getNombre());
        assertEquals("lucia.belen@ejemplo.com", socio.getIdUsuario().getEmail());
        assertEquals("3855001122", socio.getTelefono());
        assertEquals("A-2001", socio.getLegajo());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void exigeUnTelefono(String vacio) {
        SocioEdicion e = edicionValida();
        e.setTelefono(vacio);

        assertEquals("Completá nombre, apellido y teléfono.", servicio.guardar(e, HOY));
        assertEquals("Lucía", socio.getIdUsuario().getNombre(), "no se modifica nada si algo falla");
    }

    @Test
    void exigeUnNombre() {
        SocioEdicion e = edicionValida();
        e.setNombre("   ");

        assertEquals("Completá nombre, apellido y teléfono.", servicio.guardar(e, HOY));
    }

    @Test
    void exigeUnApellido() {
        SocioEdicion e = edicionValida();
        e.setApellido(null);

        assertEquals("Completá nombre, apellido y teléfono.", servicio.guardar(e, HOY));
    }

    @Test
    void rechazaUnCorreoInvalido() {
        SocioEdicion e = edicionValida();
        e.setEmail("lucia.belen@");

        assertEquals("El correo no es válido.", servicio.guardar(e, HOY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "30.123.456", "1234567890123456"})
    void rechazaUnDniQueNoTieneEntre6Y15Digitos(String dni) {
        SocioEdicion e = edicionValida();
        e.setDni(dni);

        assertEquals("El DNI debe tener solo dígitos, sin puntos.", servicio.guardar(e, HOY));
    }

    @Test
    void rechazaUnaFechaDeNacimientoMalEscrita() {
        SocioEdicion e = edicionValida();
        e.setNacimiento("14/05/2000");

        assertEquals("Ingresá la fecha de nacimiento.", servicio.guardar(e, HOY));
    }

    @Test
    void rechazaUnaFechaDeNacimientoDeHoy() {
        SocioEdicion e = edicionValida();
        e.setNacimiento(HOY.toString());

        assertEquals("La fecha de nacimiento tiene que ser anterior a hoy.", servicio.guardar(e, HOY));
    }

    @Test
    void aceptaUnaFechaDeNacimientoDeAyer() {
        SocioEdicion e = edicionValida();
        e.setNacimiento(HOY.minusDays(1).toString());

        assertNull(servicio.guardar(e, HOY));
    }

    @Test
    void rechazaUnTipoDeSocioInexistente() {
        SocioEdicion e = edicionValida();
        e.setCategoria("Marciano");

        assertEquals("Elegí el tipo de socio.", servicio.guardar(e, HOY));
    }

    @Test
    void exigeLegajoSiLaCategoriaLoRequiere() {
        SocioEdicion e = edicionValida();
        e.setLegajo("  ");

        assertEquals("Un Alumno UNSE necesita legajo.", servicio.guardar(e, HOY));
    }

    @Test
    void noGuardaLegajoSiLaCategoriaNoLoRequiere() {
        SocioEdicion e = edicionValida();
        e.setCategoria("Externo");
        e.setLegajo("A-2001");

        assertNull(servicio.guardar(e, HOY));
        assertNull(socio.getLegajo());
        assertEquals(externo, socio.getIdCategoriaSocio());
    }

    @Test
    void rechazaUnCorreoQueYaUsaOtraCuenta() {
        when(usuarioDAO.buscarPorEmail("lucia.belen@ejemplo.com"))
                .thenReturn(DatosDePrueba.usuario(999, "Otra", "Persona", "lucia.belen@ejemplo.com"));

        assertEquals("Ese correo ya lo usa otra cuenta.", servicio.guardar(edicionValida(), HOY));
    }

    @Test
    void permiteConservarElPropioCorreo() {
        SocioEdicion e = edicionValida();
        e.setEmail("lucia.gomez@ejemplo.com");
        when(usuarioDAO.buscarPorEmail("lucia.gomez@ejemplo.com")).thenReturn(socio.getIdUsuario());

        assertNull(servicio.guardar(e, HOY));
    }

    @Test
    void rechazaUnDniQueYaTieneOtroSocio() {
        Socio otro = DatosDePrueba.socio(8, Socio.Estado.ACTIVO, alumno);
        when(socioDAO.buscarPorDni("30123456")).thenReturn(otro);

        assertEquals("Ese DNI ya pertenece a otro socio.", servicio.guardar(edicionValida(), HOY));
    }

    @Test
    void permiteConservarElPropioDni() {
        when(socioDAO.buscarPorDni("30123456")).thenReturn(socio);

        assertNull(servicio.guardar(edicionValida(), HOY));
    }

    // ---------- baja lógica ----------

    @Test
    void darDeBajaDejaLaBajaLogicaYDeshabilitaLaCuenta() {
        boolean baja = servicio.darDeBaja(7);

        assertTrue(baja);
        assertTrue(socio.isBajaLogica());
        assertFalse(socio.getIdUsuario().isCuentaHabilitada());
    }

    @Test
    void noDaDeBajaUnSocioQueYaEstabaDadoDeBaja() {
        socio.setBajaLogica(true);

        assertFalse(servicio.darDeBaja(7));
    }

    @Test
    void noDaDeBajaUnSocioInexistente() {
        when(socioDAO.buscarPorId(99)).thenReturn(null);

        assertFalse(servicio.darDeBaja(99));
    }
}
