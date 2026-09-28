package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.repositorio.Usuario;
import com.sigrid.sigrid.util.PasswordUtil;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.io.Serializable;

/**
 * Reglas de negocio del login. El DAO solo sabe consultar la tabla; acá se
 * decide qué hace que un login sea válido (contraseña correcta y cuenta habilitada).
 *
 * CDI simple (no EJB), mismo motivo que los DAO. Serializable porque LoginBean
 * (@SessionScoped, passivating) lo inyecta como dependencia @Dependent, y CDI
 * exige que esas dependencias sean serializables.
 */
@Dependent
public class UsuarioServicio implements Serializable {

    @Inject
    private UsuarioDAO usuarioDAO;

    /**
     * Un socio con la membresía vencida SÍ puede iniciar sesión (necesita entrar
     * para renovarla): lo único que bloquea el login es usuario.cuenta_habilitada.
     *
     * @return el Usuario autenticado, o null si el email no existe, la
     * contraseña no coincide o la cuenta está deshabilitada (baja lógica).
     */
    public Usuario autenticar(String email, String passwordPlano) {
        if (email == null || passwordPlano == null) {
            return null;
        }
        Usuario usuario = usuarioDAO.buscarPorEmail(email.trim());
        if (usuario == null || !usuario.isCuentaHabilitada()) {
            return null;
        }
        if (!PasswordUtil.verificar(passwordPlano, usuario.getPasswordHash())) {
            return null;
        }
        return usuario;
    }
}
