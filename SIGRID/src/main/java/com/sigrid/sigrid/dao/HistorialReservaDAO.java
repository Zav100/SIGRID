package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.HistorialReserva;
import jakarta.enterprise.context.Dependent;
import java.util.List;

/**
 * DAO de HistorialReserva: rastro de las reprogramaciones.
 */
@Dependent
public class HistorialReservaDAO extends BaseDAO<HistorialReserva> {

    public HistorialReservaDAO() {
        super(HistorialReserva.class);
    }

    /** Cambios de una reserva, el más reciente primero. */
    public List<HistorialReserva> listarPorReserva(Integer idReserva) {
        return em.createQuery(
                "SELECT h FROM HistorialReserva h WHERE h.idReserva.idReserva = :id ORDER BY h.fechaCambio DESC",
                HistorialReserva.class)
                .setParameter("id", idReserva)
                .getResultList();
    }

    /** Reprogramaciones hechas (las que el motivo marca como tales), la más reciente primero. */
    public List<HistorialReserva> listarReprogramaciones(String prefijoMotivo) {
        return em.createQuery(
                "SELECT h FROM HistorialReserva h JOIN FETCH h.idReserva r JOIN FETCH r.idTurno t "
                + "JOIN FETCH t.idInstalacion JOIN FETCH r.idSocio s JOIN FETCH s.idUsuario JOIN FETCH h.idTurnoAnterior "
                + "WHERE h.motivo LIKE :prefijo ORDER BY h.fechaCambio DESC, h.idHistorial DESC",
                HistorialReserva.class)
                .setParameter("prefijo", prefijoMotivo + "%")
                .getResultList();
    }
}
