package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.Rol;
import jakarta.enterprise.context.Dependent;

/**
 * DAO de Rol: catálogo de roles (SOCIO, ADMINISTRADOR).
 */
@Dependent
public class RolDAO extends BaseDAO<Rol> {

    public RolDAO() {
        super(Rol.class);
    }

    public Rol buscarPorNombre(String nombreRol) {
        return primero(em.createNamedQuery("Rol.findByNombreRol", Rol.class)
                .setParameter("nombreRol", nombreRol));
    }
}
