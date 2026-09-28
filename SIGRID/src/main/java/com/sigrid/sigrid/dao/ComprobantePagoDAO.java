package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.ComprobantePago;
import jakarta.enterprise.context.Dependent;

/**
 * DAO de ComprobantePago. El hash es UNIQUE en la BD: existePorHash sirve para
 * avisar antes ("ya fue registrado"), pero la garantía real es la restricción.
 */
@Dependent
public class ComprobantePagoDAO extends BaseDAO<ComprobantePago> {

    public ComprobantePagoDAO() {
        super(ComprobantePago.class);
    }

    public boolean existePorHash(String hashArchivo) {
        Long cantidad = em.createQuery(
                "SELECT COUNT(c) FROM ComprobantePago c WHERE c.hashArchivo = :hash", Long.class)
                .setParameter("hash", hashArchivo)
                .getSingleResult();
        return cantidad > 0;
    }

    public ComprobantePago buscarPorPago(Integer idPago) {
        return primero(em.createQuery(
                "SELECT c FROM ComprobantePago c WHERE c.idPago.idPago = :id", ComprobantePago.class)
                .setParameter("id", idPago));
    }
}
