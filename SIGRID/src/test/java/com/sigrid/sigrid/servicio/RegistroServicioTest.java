package com.sigrid.sigrid.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

    @Test
    void rechazaCuandoLasContrasenasNoCoinciden() {
        assertEquals("Las contraseñas no coinciden.",
                servicio.validarPasoUno("martin.sosa@ejemplo.com", "Clave#2026", "Clave#2027"));
    }

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
        assertSame(socio.getValue(), carnet.getValue().getIdSocio());
    }

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
}
