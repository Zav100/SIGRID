package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.SolicitudCambioReserva;
import jakarta.enterprise.context.Dependent;
import jakarta.persistence.LockModeType;
import java.util.List;

/** DAO de los pedidos de cancelación y reprogramación. */
@Dependent
public class SolicitudCambioReservaDAO extends BaseDAO<SolicitudCambioReserva> {

    public SolicitudCambioReservaDAO() {
        super(SolicitudCambioReserva.class);
    }

    /** Pedidos por resolver, el más antiguo primero; trae reserva, turnos, socio y pago de una vez. */
    public List<SolicitudCambioReserva> listarPendientes() {
        return em.createQuery(
                "SELECT c FROM SolicitudCambioReserva c JOIN FETCH c.idReserva r JOIN FETCH r.idTurno t "
                + "JOIN FETCH t.idInstalacion JOIN FETCH r.idSocio s JOIN FETCH s.idUsuario "
                + "LEFT JOIN FETCH r.idPago LEFT JOIN FETCH r.idTarifa LEFT JOIN FETCH c.idTurnoNuevo "
                + "WHERE c.estado = :pendiente ORDER BY c.fechaSolicitud", SolicitudCambioReserva.class)
                .setParameter("pendiente", SolicitudCambioReserva.Estado.PENDIENTE)
                .getResultList();
    }

    public long contarPendientes() {
        return em.createQuery("SELECT COUNT(c) FROM SolicitudCambioReserva c WHERE c.estado = :pendiente", Long.class)
                .setParameter("pendiente", SolicitudCambioReserva.Estado.PENDIENTE)
                .getSingleResult();
    }

    /** Los pedidos de un socio (en cualquier estado), el más reciente primero. */
    public List<SolicitudCambioReserva> listarDelSocio(Integer idSocio) {
        return em.createQuery(
                "SELECT c FROM SolicitudCambioReserva c WHERE c.idReserva.idSocio.idSocio = :id "
                + "ORDER BY c.fechaSolicitud DESC", SolicitudCambioReserva.class)
                .setParameter("id", idSocio)
                .getResultList();
    }

    /** ¿La reserva ya tiene un pedido sin resolver? Solo se admite uno a la vez. */
    public boolean hayPendienteDe(Integer idReserva) {
        return em.createQuery(
                "SELECT COUNT(c) FROM SolicitudCambioReserva c WHERE c.idReserva.idReserva = :id AND c.estado = :pendiente",
                Long.class)
                .setParameter("id", idReserva)
                .setParameter("pendiente", SolicitudCambioReserva.Estado.PENDIENTE)
                .getSingleResult() > 0;
    }

    /** El pedido con bloqueo de escritura (dentro de una transacción): dos administradores no lo resuelven a la vez. */
    public SolicitudCambioReserva buscarParaResolver(Integer idSolicitud) {
        return em.find(SolicitudCambioReserva.class, idSolicitud, LockModeType.PESSIMISTIC_WRITE);
    }
}
