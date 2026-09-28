package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.Instalacion;
import jakarta.enterprise.context.Dependent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * DAO de Instalacion: espacios del polideportivo.
 */
@Dependent
public class InstalacionDAO extends BaseDAO<Instalacion> {

    public InstalacionDAO() {
        super(Instalacion.class);
    }

    public Instalacion buscarPorNombre(String nombre) {
        return primero(em.createNamedQuery("Instalacion.findByNombre", Instalacion.class)
                .setParameter("nombre", nombre));
    }

    /** Cantidad de instalaciones por estado; los estados sin ninguna no aparecen en el mapa. */
    public Map<Instalacion.Estado, Long> contarPorEstado() {
        Map<Instalacion.Estado, Long> cantidades = new EnumMap<>(Instalacion.Estado.class);
        for (Object[] fila : em.createQuery(
                "SELECT i.estado, COUNT(i) FROM Instalacion i GROUP BY i.estado", Object[].class).getResultList()) {
            cantidades.put((Instalacion.Estado) fila[0], (Long) fila[1]);
        }
        return cantidades;
    }

    /** Las que se reservan (la pileta es de acceso libre y no se valora), por nombre. */
    public List<Instalacion> listarReservables() {
        return em.createQuery(
                "SELECT i FROM Instalacion i WHERE i.tipoAcceso = :acceso ORDER BY i.nombre", Instalacion.class)
                .setParameter("acceso", Instalacion.TipoAcceso.ARANCELADO)
                .getResultList();
    }

    public List<Instalacion> listarPorEstado(Instalacion.Estado estado) {
        return em.createQuery(
                "SELECT i FROM Instalacion i WHERE i.estado = :estado ORDER BY i.nombre", Instalacion.class)
                .setParameter("estado", estado)
                .getResultList();
    }
}
