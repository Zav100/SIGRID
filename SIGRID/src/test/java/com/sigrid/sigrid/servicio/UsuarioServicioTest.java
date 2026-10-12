package com.sigrid.sigrid.servicio;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.repositorio.Usuario;
import com.sigrid.sigrid.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Login: lo único que bloquea el ingreso es la contraseña incorrecta o la cuenta deshabilitada (baja lógica).
 * Un socio con la membresía vencida sí puede entrar para renovarla.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServicioTest {

    private static final String CLAVE = "Cancha#2026";
    private static final String EMAIL = "lucia.gomez@ejemplo.com";

    @Mock
    private UsuarioDAO usuarioDAO;

    @InjectMocks
    private UsuarioServicio servicio;

    private Usuario usuario;

    @BeforeEach
    void crearUnUsuarioConSuContrasenaHasheada() {
        usuario = DatosDePrueba.usuario(5, "Lucía", "Gómez", EMAIL);
        usuario.setPasswordHash(PasswordUtil.generarHash(CLAVE));
        usuario.setCuentaHabilitada(true);
    }

    @Test
    void autenticaConElCorreoYLaContrasenaCorrectos() {
        when(usuarioDAO.buscarPorEmail(EMAIL)).thenReturn(usuario);

        Usuario autenticado = servicio.autenticar(EMAIL, CLAVE);

        assertSame(usuario, autenticado);
    }

    @Test
    void ignoraLosEspaciosAlrededorDelCorreo() {
        when(usuarioDAO.buscarPorEmail(EMAIL)).thenReturn(usuario);

        Usuario autenticado = servicio.autenticar("  " + EMAIL + " ", CLAVE);

        assertSame(usuario, autenticado);
    }

    @Test
    void rechazaUnaContrasenaIncorrecta() {
        when(usuarioDAO.buscarPorEmail(EMAIL)).thenReturn(usuario);

        assertNull(servicio.autenticar(EMAIL, "Cancha#2027"));
    }

    @Test
    void rechazaUnaCuentaDeshabilitadaAunConLaContrasenaCorrecta() {
        usuario.setCuentaHabilitada(false);
        when(usuarioDAO.buscarPorEmail(EMAIL)).thenReturn(usuario);

        assertNull(servicio.autenticar(EMAIL, CLAVE));
    }

    @Test
    void rechazaUnCorreoQueNoExiste() {
        when(usuarioDAO.buscarPorEmail("nadie@ejemplo.com")).thenReturn(null);

        assertNull(servicio.autenticar("nadie@ejemplo.com", CLAVE));
    }

    @Test
    void rechazaUnCorreoNuloSinConsultarLaBase() {
        assertNull(servicio.autenticar(null, CLAVE));

        verifyNoInteractions(usuarioDAO);
    }

    @Test
    void rechazaUnaContrasenaNulaSinConsultarLaBase() {
        assertNull(servicio.autenticar(EMAIL, null));

        verifyNoInteractions(usuarioDAO);
    }
}
