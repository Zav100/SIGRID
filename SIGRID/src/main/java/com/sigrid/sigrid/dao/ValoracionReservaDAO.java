package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.ValoracionReserva;
import jakarta.enterprise.context.Dependent;
import jakarta.persistence.PersistenceException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO de ValoracionReserva: una valoración por reserva (UNIQUE en la BD).
 */
@Dependent
public class ValoracionReservaDAO extends BaseDAO<ValoracionReserva> {

    public ValoracionReservaDAO() {
        super(ValoracionReserva.class);
    }

    public ValoracionReserva buscarPorReserva(Integer idReserva) {
        return primero(em.createQuery(
                "SELECT v FROM ValoracionReserva v WHERE v.idReserva.idReserva = :id", ValoracionReserva.class)
                .setParameter("id", idReserva));
    }

    /** Puntaje que dio el socio a cada reserva que ya valoró (id de la reserva -> puntaje). */
    public Map<Integer, Integer> puntajesDelSocio(Integer idSocio) {
        Map<Integer, Integer> puntajes = new HashMap<>();
        for (Object[] fila : em.createQuery(
                "SELECT v.idReserva.idReserva, v.puntaje FROM ValoracionReserva v WHERE v.idReserva.idSocio.idSocio = :id",
                Object[].class).setParameter("id", idSocio).getResultList()) {
            puntajes.put((Integer) fila[0], (Integer) fila[1]);
        }
        return puntajes;
    }

    /** Guarda y manda el INSERT ya; @return false si la reserva ya tenía valoración (UNIQUE de la BD). */
    public boolean insertar(ValoracionReserva valoracion) {
        try {
            em.persist(valoracion);
            em.flush();
            return true;
        } catch (PersistenceException e) {
            return false;
        }
    }

    /** Todas las valoraciones con su reserva, instalación y socio ya cargados, la más reciente primero. */
    public List<ValoracionReserva> listarTodasParaAdmin() {
        return em.createQuery(
                "SELECT v FROM ValoracionReserva v JOIN FETCH v.idReserva r JOIN FETCH r.idTurno t "
                + "JOIN FETCH t.idInstalacion JOIN FETCH r.idSocio s JOIN FETCH s.idUsuario "
                + "JOIN FETCH s.idCategoriaSocio ORDER BY v.fechaValoracion DESC, v.idValoracion DESC",
                ValoracionReserva.class)
                .getResultList();
    }

    /** Promedio de puntaje de una instalación (RF-08.2); null si todavía no tiene valoraciones. */
    public Double promedioPorInstalacion(Integer idInstalacion) {
        return em.createQuery(
                "SELECT AVG(v.puntaje) FROM ValoracionReserva v "
                + "WHERE v.idReserva.idTurno.idInstalacion.idInstalacion = :id", Double.class)
                .setParameter("id", idInstalacion)
                .getSingleResult();
    }

    /** Valoraciones (con comentario) de una instalación, la más reciente primero. */
    public List<ValoracionReserva> listarPorInstalacion(Integer idInstalacion) {
        return em.createQuery(
                "SELECT v FROM ValoracionReserva v "
                + "WHERE v.idReserva.idTurno.idInstalacion.idInstalacion = :id ORDER BY v.fechaValoracion DESC",
                ValoracionReserva.class)
                .setParameter("id", idInstalacion)
                .getResultList();
    }
}
