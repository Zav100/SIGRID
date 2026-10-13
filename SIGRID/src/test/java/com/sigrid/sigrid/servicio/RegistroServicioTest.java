package com.sigrid.sigrid.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.CarnetDigitalDAO;
import com.sigrid.sigrid.dao.CategoriaSocioDAO;
import com.sigrid.sigrid.dao.RolDAO;
import com.sigrid.sigrid.dao.SocioDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dto.RegistroSocio;
import com.sigrid.sigrid.repositorio.CarnetDigital;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Rol;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.Usuario;
import com.sigrid.sigrid.util.PasswordUtil;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Alta pública de socios: validaciones del paso 1 (correo y contraseña), del paso 2 (datos personales) y creación de
 * usuario + socio + carnet digital en una sola operación. El único chequeo que usa el reloj del sistema es el de la
 * fecha de nacimiento "anterior a hoy".
 */
@ExtendWith(MockitoExtension.class)
class RegistroServicioTest {

    @Mock
    private UsuarioDAO usuarioDAO;
    @Mock
    private SocioDAO socioDAO;
    @Mock
    private RolDAO rolDAO;
    @Mock
    private CategoriaSocioDAO categoriaSocioDAO;
    @Mock
    private CarnetDigitalDAO carnetDigitalDAO;

    @InjectMocks
    private RegistroServicio servicio;

    private final CategoriaSocio alumno = DatosDePrueba.categoria(1, "Alumno UNSE", true);
    private final CategoriaSocio externo = DatosDePrueba.categoria(4, "Externo", false);
    private final Rol rolSocio = new Rol(2);

    @BeforeEach
    void prepararRolYCategorias() {
        lenient().when(rolDAO.buscarPorNombre("SOCIO")).thenReturn(rolSocio);
        lenient().when(categoriaSocioDAO.buscarPorNombre("Alumno UNSE")).thenReturn(alumno);
        lenient().when(categoriaSocioDAO.buscarPorNombre("Externo")).thenReturn(externo);
    }

    /** Un alta completa y válida de un alumno; cada prueba cambia solo el dato que quiere probar. */
    private RegistroSocio altaDeAlumno() {
        RegistroSocio r = new RegistroSocio();
        r.setEmail("  martin.sosa@ejemplo.com ");
        r.setPassword("Clave#2026");
        r.setConfirmarPassword("Clave#2026");
        r.setNombre("Martín");
        r.setApellido("Sosa");
        r.setDni("32456789");
        r.setFechaNacimiento("2001-05-14");
        r.setTelefono("3854123456");
        r.setTipoSocio("INTERNO");
        r.setCategoriaInterna("ALUMNO");
        r.setLegajo("A-1042");
        return r;
    }

    // ---------- paso 1: correo y contraseña ----------

    @ParameterizedTest
    @ValueSource(strings = {"sin-arroba", "a@b", "con espacio@ejemplo.com", "@ejemplo.com", ""})
    void rechazaUnCorreoConFormatoInvalido(String correo) {
        assertEquals("El correo no es válido.", servicio.validarPasoUno(correo, "Clave#2026", "Clave#2026"));
    }

    @Test
    void rechazaUnCorreoNulo() {
        assertEquals("El correo no es válido.", servicio.validarPasoUno(null, "Clave#2026", "Clave#2026"));
    }

    @Test
    void rechazaUnaContrasenaDeCincoCaracteres() {
        assertEquals("La contraseña tiene que tener al menos 6 caracteres.",
                servicio.validarPasoUno("martin.sosa@ejemplo.com", "12345", "12345"));
    }

    @Test
    void aceptaUnaContrasenaDeSeisCaracteres() {
        assertNull(servicio.validarPasoUno("martin.sosa@ejemplo.com", "123456", "123456"));
    }

    /**
     * RNF-03 y CP-02 del plan: la contraseña lleva letras, números y al menos un símbolo. Hoy el código solo pide
     * 6 caracteres. Está deshabilitada hasta decidir si se corrige el código o el plan; al corregirlo hay que
     * quitarle el {@code @Disabled} y actualizar {@link #aceptaUnaContrasenaDeSeisCaracteres()}.
     */
    @Disabled("Defecto D-4: el código solo exige 6 caracteres; el plan (RNF-03, CP-02) pide letras, números y un símbolo")
    @DisplayName("CP-02: rechaza una contraseña sin letras, sin números o sin símbolo")
    @ParameterizedTest
    @ValueSource(strings = {"Clave2026", "Clave#abcd", "123#456"})
    void rechazaUnaContrasenaSinLetrasNumerosOSimbolo(String contrasena) {
        assertNotNull(servicio.validarPasoUno("martin.sosa@ejemplo.com", contrasena, contrasena),
                "la contraseña \"" + contrasena + "\" debería rechazarse");
    }

    @Test
    void rechazaCuandoLasContrasenasNoCoinciden() {
        assertEquals("Las contraseñas no coinciden.",
                servicio.validarPasoUno("martin.sosa@ejemplo.com", "Clave#2026", "Clave#2027"));
    }

    @DisplayName("CP-04: rechaza un correo que ya tiene cuenta (regla del servicio)")
    @Test
    void rechazaUnCorreoQueYaTieneUnaCuentaActiva() {
        Usuario existente = DatosDePrueba.usuario(5, "Lucía", "Gómez", "lucia.gomez@ejemplo.com");
        when(usuarioDAO.buscarPorEmail("lucia.gomez@ejemplo.com")).thenReturn(existente);
        when(socioDAO.buscarPorIdUsuario(5)).thenReturn(DatosDePrueba.socio(7, Socio.Estado.ACTIVO, alumno));

        assertEquals("Ese correo ya tiene una cuenta.",
                servicio.validarPasoUno("lucia.gomez@ejemplo.com", "Clave#2026", "Clave#2026"));
    }

    @Test
    void permiteRegistrarseConElCorreoDeUnSocioDadoDeBaja() {
        Usuario existente = DatosDePrueba.usuario(5, "Lucía", "Gómez", "lucia.gomez@ejemplo.com");
        Socio baja = DatosDePrueba.socio(7, Socio.Estado.NO_ACTIVO, alumno);
        baja.setBajaLogica(true);
        when(usuarioDAO.buscarPorEmail("lucia.gomez@ejemplo.com")).thenReturn(existente);
        when(socioDAO.buscarPorIdUsuario(5)).thenReturn(baja);

        assertNull(servicio.validarPasoUno("lucia.gomez@ejemplo.com", "Clave#2026", "Clave#2026"));
    }

    // ---------- paso 2: datos personales ----------

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567890123456", "30.123.456", "treinta"})
    void rechazaUnDniQueNoTieneEntre6Y15Digitos(String dni) {
        RegistroSocio r = altaDeAlumno();
        r.setDni(dni);

        assertEquals("El DNI debe tener solo dígitos, sin puntos.", servicio.registrar(r));
        verify(usuarioDAO, never()).crear(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "123456789012345"})
    void aceptaUnDniDeSeisYDeQuinceDigitos(String dni) {
        RegistroSocio r = altaDeAlumno();
        r.setDni(dni);

        assertNull(servicio.registrar(r));
    }

    @Test
    void rechazaUnaAltaSinNombre() {
        RegistroSocio r = altaDeAlumno();
        r.setNombre("   ");

        assertEquals("Completá nombre y apellido.", servicio.registrar(r));
    }

    @Test
    void rechazaUnTelefonoConGuiones() {
        RegistroSocio r = altaDeAlumno();
        r.setTelefono("385-412-3456");

        assertEquals("Ingresá un teléfono válido, solo dígitos.", servicio.registrar(r));
    }

    @Test
    void rechazaUnaFechaDeNacimientoVaciaOMalEscrita() {
        RegistroSocio r = altaDeAlumno();
        r.setFechaNacimiento("14/05/2001");

        assertEquals("Ingresá la fecha de nacimiento.", servicio.registrar(r));
    }

    @Test
    void rechazaUnaFechaDeNacimientoDeHoy() {
        RegistroSocio r = altaDeAlumno();
        r.setFechaNacimiento(LocalDate.now().toString());

        assertEquals("La fecha de nacimiento tiene que ser anterior a hoy.", servicio.registrar(r));
    }

    @DisplayName("CP-03: rechaza un socio interno (alumno) sin legajo")
    @Test
    void rechazaUnAlumnoSinLegajo() {
        RegistroSocio r = altaDeAlumno();
        r.setLegajo("  ");

        assertEquals("Un Alumno UNSE necesita legajo.", servicio.registrar(r));
    }

    @Test
    void rechazaUnaAltaSinTipoDeSocio() {
        RegistroSocio r = altaDeAlumno();
        r.setTipoSocio(null);

        assertEquals("Elegí el tipo de socio.", servicio.registrar(r));
    }

    @DisplayName("CP-04: rechaza un DNI repetido y no crea nada (regla del servicio)")
    @Test
    void rechazaUnDniQueYaPerteneceAOtroSocio() {
        when(socioDAO.buscarPorDni("32456789")).thenReturn(DatosDePrueba.socio(7, Socio.Estado.ACTIVO, alumno));

        assertEquals("Ese DNI ya pertenece a otro socio.", servicio.registrar(altaDeAlumno()));
        verify(usuarioDAO, never()).crear(any());
    }

    @Test
    void alRegistrarVuelveAValidarElPasoUno() {
        RegistroSocio r = altaDeAlumno();
        r.setConfirmarPassword("otra-clave");

        assertEquals("Las contraseñas no coinciden.", servicio.registrar(r));
        verify(usuarioDAO, never()).crear(any());
    }

    // ---------- alta completa ----------

    @DisplayName("CP-01: registra usuario, socio y carnet (inactivo) de un alumno con legajo")
    @Test
    void registraUsuarioSocioYCarnetEstudiantilParaUnAlumno() {
        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<Socio> socio = ArgumentCaptor.forClass(Socio.class);
        ArgumentCaptor<CarnetDigital> carnet = ArgumentCaptor.forClass(CarnetDigital.class);

        String error = servicio.registrar(altaDeAlumno());

        assertNull(error);
        verify(usuarioDAO).crear(usuario.capture());
        assertEquals("martin.sosa@ejemplo.com", usuario.getValue().getEmail());
        assertEquals("Martín", usuario.getValue().getNombre());
        assertSame(rolSocio, usuario.getValue().getIdRol());
        assertTrue(PasswordUtil.verificar("Clave#2026", usuario.getValue().getPasswordHash()),
                "la contraseña se guarda hasheada y verificable");

        verify(socioDAO).crear(socio.capture());
        assertEquals("32456789", socio.getValue().getDni());
        assertSame(alumno, socio.getValue().getIdCategoriaSocio());
        assertEquals("A-1042", socio.getValue().getLegajo());
        assertEquals(LocalDate.of(2001, 5, 14), socio.getValue().getFechaNacimiento());
        assertSame(usuario.getValue(), socio.getValue().getIdUsuario());

        verify(carnetDigitalDAO).crear(carnet.capture());
        assertEquals(CarnetDigital.TipoCarnet.ESTUDIANTIL, carnet.getValue().getTipoCarnet());
        assertEquals(CarnetDigital.Estado.INACTIVO, carnet.getValue().getEstado());
        assertSame(socio.getValue(), carnet.getValue().getIdSocio());
    }

    @DisplayName("CP-01: el socio nuevo queda NO_ACTIVO")
    @Test
    void elSocioNuevoNaceSinMembresiaHastaQueSeValideSuPago() {
        ArgumentCaptor<Socio> socio = ArgumentCaptor.forClass(Socio.class);

        servicio.registrar(altaDeAlumno());

        verify(socioDAO).crear(socio.capture());
        assertEquals(Socio.Estado.NO_ACTIVO, socio.getValue().getEstado());
    }

    @Test
    void registraUnExternoConCarnetGeneralYSinGuardarLegajo() {
        RegistroSocio r = altaDeAlumno();
        r.setTipoSocio("EXTERNO");
        r.setCategoriaInterna(null);
        r.setLegajo("A-1042");
        ArgumentCaptor<Socio> socio = ArgumentCaptor.forClass(Socio.class);
        ArgumentCaptor<CarnetDigital> carnet = ArgumentCaptor.forClass(CarnetDigital.class);

        String error = servicio.registrar(r);

        assertNull(error);
        verify(socioDAO).crear(socio.capture());
        assertSame(externo, socio.getValue().getIdCategoriaSocio());
        assertNull(socio.getValue().getLegajo());
        verify(carnetDigitalDAO).crear(carnet.capture());
        assertEquals(CarnetDigital.TipoCarnet.GENERAL, carnet.getValue().getTipoCarnet());
    }

    // ---------- re-registro de quien fue dado de baja ----------

    private Socio socioDadoDeBaja(String dni) {
        Socio baja = DatosDePrueba.socio(7, Socio.Estado.NO_ACTIVO, alumno);
        baja.setDni(dni);
        baja.setBajaLogica(true);
        return baja;
    }

    @Test
    void alVolverARegistrarseArchivaElCorreoYElDniDeLaFichaDadaDeBaja() {
        Socio baja = socioDadoDeBaja("30123456");
        Usuario usuarioViejo = baja.getIdUsuario();
        usuarioViejo.setIdUsuario(107);
        when(usuarioDAO.buscarPorEmail("lucia.gomez@ejemplo.com")).thenReturn(usuarioViejo);
        when(socioDAO.buscarPorIdUsuario(107)).thenReturn(baja);
        // la primera búsqueda por DNI encuentra la baja; la segunda, ya archivada, no encuentra a nadie
        when(socioDAO.buscarPorDni("30123456")).thenReturn(baja, null);
        RegistroSocio r = altaDeAlumno();
        r.setEmail("lucia.gomez@ejemplo.com");
        r.setDni("30123456");

        String error = servicio.registrar(r);

        assertNull(error);
        assertEquals("b7-lucia.gomez@ejemplo.com", usuarioViejo.getEmail());
        assertEquals("b7-30123456", baja.getDni());
        InOrder orden = inOrder(socioDAO);
        orden.verify(socioDAO, times(1)).flush();
        orden.verify(socioDAO).crear(any(Socio.class));
    }

    @Test
    void elDniArchivadoSeCortaA15CaracteresParaEntrarEnLaColumna() {
        Socio baja = socioDadoDeBaja("123456789012345");
        when(socioDAO.buscarPorDni("123456789012345")).thenReturn(baja);
        RegistroSocio r = altaDeAlumno();
        r.setDni("123456789012345");

        servicio.registrar(r);

        assertEquals("b7-123456789012", baja.getDni());
        assertEquals(15, baja.getDni().length());
    }

    // ---------- categorías internas y datos nulos ----------

    @Test
    void registraUnDocenteInternoConSuCategoria() {
        CategoriaSocio docente = DatosDePrueba.categoria(2, "Docente UNSE", true);
        when(categoriaSocioDAO.buscarPorNombre("Docente UNSE")).thenReturn(docente);
        RegistroSocio r = altaDeAlumno();
        r.setCategoriaInterna("DOCENTE");
        ArgumentCaptor<Socio> socio = ArgumentCaptor.forClass(Socio.class);
        ArgumentCaptor<CarnetDigital> carnet = ArgumentCaptor.forClass(CarnetDigital.class);

        assertNull(servicio.registrar(r));

        verify(socioDAO).crear(socio.capture());
        assertSame(docente, socio.getValue().getIdCategoriaSocio());
        verify(carnetDigitalDAO).crear(carnet.capture());
        assertEquals(CarnetDigital.TipoCarnet.GENERAL, carnet.getValue().getTipoCarnet());
    }

    @Test
    void registraUnNoDocenteInternoConSuCategoria() {
        CategoriaSocio noDocente = DatosDePrueba.categoria(3, "No Docente UNSE", true);
        when(categoriaSocioDAO.buscarPorNombre("No Docente UNSE")).thenReturn(noDocente);
        RegistroSocio r = altaDeAlumno();
        r.setCategoriaInterna("NO_DOCENTE");
        ArgumentCaptor<Socio> socio = ArgumentCaptor.forClass(Socio.class);

        assertNull(servicio.registrar(r));

        verify(socioDAO).crear(socio.capture());
        assertSame(noDocente, socio.getValue().getIdCategoriaSocio());
    }

    @Test
    void rechazaUnaCategoriaInternaDesconocida() {
        RegistroSocio r = altaDeAlumno();
        r.setCategoriaInterna("JUBILADO");

        assertEquals("Elegí el tipo de socio.", servicio.registrar(r));
        verify(usuarioDAO, never()).crear(any());
    }

    @Test
    void rechazaUnSocioInternoSinCategoria() {
        RegistroSocio r = altaDeAlumno();
        r.setCategoriaInterna(null);

        assertEquals("Elegí el tipo de socio.", servicio.registrar(r));
    }

    @Test
    void rechazaUnaContrasenaNula() {
        assertEquals("La contraseña tiene que tener al menos 6 caracteres.",
                servicio.validarPasoUno("martin.sosa@ejemplo.com", null, null));
    }

    @Test
    void unNombreNuloSeTrataComoVacio() {
        RegistroSocio r = altaDeAlumno();
        r.setNombre(null);

        assertEquals("Completá nombre y apellido.", servicio.registrar(r));
    }

    @Test
    void unDniNuloSeTrataComoVacio() {
        RegistroSocio r = altaDeAlumno();
        r.setDni(null);

        assertEquals("El DNI debe tener solo dígitos, sin puntos.", servicio.registrar(r));
    }

    @Test
    void unLegajoNuloSeTrataComoVacio() {
        RegistroSocio r = altaDeAlumno();
        r.setLegajo(null);

        assertEquals("Un Alumno UNSE necesita legajo.", servicio.registrar(r));
    }

    @Test
    void unApellidoNuloSeTrataComoVacio() {
        RegistroSocio r = altaDeAlumno();
        r.setApellido(null);

        assertEquals("Completá nombre y apellido.", servicio.registrar(r));
    }

    @Test
    void unTelefonoNuloSeTrataComoVacio() {
        RegistroSocio r = altaDeAlumno();
        r.setTelefono(null);

        assertEquals("Ingresá un teléfono válido, solo dígitos.", servicio.registrar(r));
    }
}
