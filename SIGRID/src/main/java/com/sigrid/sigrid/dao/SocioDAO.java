package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.Socio;
import jakarta.enterprise.context.Dependent;
import java.util.List;

/**
 * DAO de Socio: único punto de acceso a la tabla "socio".
 */
@Dependent
public class SocioDAO extends BaseDAO<Socio> {

    public SocioDAO() {
        super(Socio.class);
    }

    /** La ficha de socio de una cuenta de usuario; null si esa cuenta no es de un socio (ej. un administrador). */
    public Socio buscarPorIdUsuario(Integer idUsuario) {
        return primero(em.createQuery(
                "SELECT s FROM Socio s WHERE s.idUsuario.idUsuario = :id", Socio.class)
                .setParameter("id", idUsuario));
    }

    /** Listado del panel del administrador: los socios que no están dados de baja, por apellido y nombre. */
    public List<Socio> listarSinBaja() {
        return em.createQuery(
                "SELECT s FROM Socio s WHERE s.bajaLogica = false "
                + "ORDER BY s.idUsuario.apellido, s.idUsuario.nombre, s.idUsuario.email", Socio.class)
                .getResultList();
    }

    public Socio buscarPorDni(String dni) {
        return primero(em.createNamedQuery("Socio.findByDni", Socio.class)
                .setParameter("dni", dni));
    }

    /** Para el informe de padrón: socios ACTIVO vs NO_ACTIVO (no cuenta las bajas lógicas). */
    public long contarPorEstado(Socio.Estado estado) {
        return em.createQuery(
                "SELECT COUNT(s) FROM Socio s WHERE s.estado = :estado AND s.bajaLogica = false", Long.class)
                .setParameter("estado", estado)
                .getSingleResult();
    }
}
