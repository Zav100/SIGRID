package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.HorarioApertura;
import jakarta.enterprise.context.Dependent;
import java.util.List;

/**
 * DAO de HorarioApertura: cuándo está abierta una instalación de acceso LIBRE (la pileta).
 */
@Dependent
public class HorarioAperturaDAO extends BaseDAO<HorarioApertura> {

    public HorarioAperturaDAO() {
        super(HorarioApertura.class);
    }

    /** Las franjas de una instalación: primero las semanales (lunes a domingo) y después las de días específicos. */
    public List<HorarioApertura> listarPorInstalacion(Integer idInstalacion) {
        return em.createQuery(
                "SELECT h FROM HorarioApertura h WHERE h.idInstalacion.idInstalacion = :id "
                + "ORDER BY h.fecha, h.diaSemana, h.horaApertura", HorarioApertura.class)
                .setParameter("id", idInstalacion)
                .getResultList();
    }

    public void quitar(Integer idHorario) {
        HorarioApertura h = buscarPorId(idHorario);
        if (h != null) {
            em.remove(h);
        }
    }
}
