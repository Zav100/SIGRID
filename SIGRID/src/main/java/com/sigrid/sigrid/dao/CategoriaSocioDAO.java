package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.CategoriaSocio;
import jakarta.enterprise.context.Dependent;

/**
 * DAO de CategoriaSocio: Alumno UNSE, Docente UNSE, No Docente UNSE, Externo.
 */
@Dependent
public class CategoriaSocioDAO extends BaseDAO<CategoriaSocio> {

    public CategoriaSocioDAO() {
        super(CategoriaSocio.class);
    }

    public CategoriaSocio buscarPorNombre(String nombreCategoria) {
        return primero(em.createNamedQuery("CategoriaSocio.findByNombreCategoria", CategoriaSocio.class)
                .setParameter("nombreCategoria", nombreCategoria));
    }
}
