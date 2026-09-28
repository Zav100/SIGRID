package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Turno;
import jakarta.enterprise.context.Dependent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO de Turno: franjas horarias fijas de cada instalación.
 */
@Dependent
public class TurnoDAO extends BaseDAO<Turno> {

    public TurnoDAO() {
        super(Turno.class);
    }

    /** Franjas que se pueden reservar cada día: las de las instalaciones HABILITADAS (denominador de la ocupación). */
    public long contarDeInstalacionesHabilitadas() {
        return em.createQuery(
                "SELECT COUNT(t) FROM Turno t WHERE t.idInstalacion.estado = :habilitada", Long.class)
                .setParameter("habilitada", Instalacion.Estado.HABILITADA)
                .getSingleResult();
    }

    /** Cantidad de franjas reservables de cada instalación (por id); las que no se reservan no aparecen. */
    public Map<Integer, Long> contarPorInstalacion() {
        Map<Integer, Long> cantidades = new HashMap<>();
        for (Object[] fila : em.createQuery(
                "SELECT t.idInstalacion.idInstalacion, COUNT(t) FROM Turno t GROUP BY t.idInstalacion.idInstalacion",
                Object[].class).getResultList()) {
            cantidades.put((Integer) fila[0], (Long) fila[1]);
        }
        return cantidades;
    }

    /** Las franjas de una instalación, en orden horario. */
    public List<Turno> listarPorInstalacion(Integer idInstalacion) {
        return em.createQuery(
                "SELECT t FROM Turno t WHERE t.idInstalacion.idInstalacion = :id ORDER BY t.horaInicio",
                Turno.class)
                .setParameter("id", idInstalacion)
                .getResultList();
    }
}
