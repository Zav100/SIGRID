package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.CarnetDigital;
import jakarta.enterprise.context.Dependent;

/**
 * DAO de CarnetDigital: un carnet por socio.
 */
@Dependent
public class CarnetDigitalDAO extends BaseDAO<CarnetDigital> {

    public CarnetDigitalDAO() {
        super(CarnetDigital.class);
    }

    public CarnetDigital buscarPorSocio(Integer idSocio) {
        return primero(em.createQuery(
                "SELECT c FROM CarnetDigital c WHERE c.idSocio.idSocio = :id", CarnetDigital.class)
                .setParameter("id", idSocio));
    }
}
