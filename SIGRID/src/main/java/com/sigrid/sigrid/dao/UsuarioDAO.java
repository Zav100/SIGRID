package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.Usuario;
import jakarta.enterprise.context.Dependent;

/**
 * DAO de Usuario: único punto de acceso a la tabla "usuario".
 */
@Dependent
public class UsuarioDAO extends BaseDAO<Usuario> {

    public UsuarioDAO() {
        super(Usuario.class);
    }

    /** El email es único; la collation de MySQL lo compara sin distinguir mayúsculas. */
    public Usuario buscarPorEmail(String email) {
        return primero(em.createNamedQuery("Usuario.findByEmail", Usuario.class)
                .setParameter("email", email));
    }
}
