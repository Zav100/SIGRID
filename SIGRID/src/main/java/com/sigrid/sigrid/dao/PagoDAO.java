package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.Pago;
import jakarta.enterprise.context.Dependent;
import java.util.List;

/**
 * DAO de Pago: pagos por transferencia validados a mano por el administrador.
 */
@Dependent
public class PagoDAO extends BaseDAO<Pago> {

    public PagoDAO() {
        super(Pago.class);
    }

    /** Bandeja "Confirmaciones pendientes": pagos en un estado, el más antiguo primero. */
    public List<Pago> listarPorEstado(Pago.Estado estado) {
        return em.createQuery(
                "SELECT p FROM Pago p WHERE p.estado = :estado ORDER BY p.fechaPago", Pago.class)
                .setParameter("estado", estado)
                .getResultList();
    }

    public List<Pago> listarPorUsuario(Integer idUsuario) {
        return em.createQuery(
                "SELECT p FROM Pago p WHERE p.idUsuario.idUsuario = :id ORDER BY p.fechaPago DESC", Pago.class)
                .setParameter("id", idUsuario)
                .getResultList();
    }
}
