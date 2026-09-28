package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.TarifaAlquiler;
import jakarta.enterprise.context.Dependent;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO de TarifaAlquiler: historial de precios (nunca se edita, se agrega una nueva con vigencia).
 */
@Dependent
public class TarifaAlquilerDAO extends BaseDAO<TarifaAlquiler> {

    public TarifaAlquilerDAO() {
        super(TarifaAlquiler.class);
    }

    /** La tarifa vigente de una instalación para una categoría en la fecha dada; null si no hay. */
    public TarifaAlquiler buscarVigente(Integer idInstalacion, Integer idCategoriaSocio, LocalDate fecha) {
        return primero(em.createQuery(
                "SELECT t FROM TarifaAlquiler t WHERE t.idInstalacion.idInstalacion = :instalacion "
                + "AND t.idCategoriaSocio.idCategoriaSocio = :categoria AND t.vigenteDesde <= :fecha "
                + "AND (t.vigenteHasta IS NULL OR t.vigenteHasta >= :fecha) ORDER BY t.vigenteDesde DESC",
                TarifaAlquiler.class)
                .setParameter("instalacion", idInstalacion)
                .setParameter("categoria", idCategoriaSocio)
                .setParameter("fecha", fecha));
    }

    /** Todas las tarifas vigentes en la fecha dada (una por instalación y categoría; si hay dos, la de vigencia más reciente va última). */
    public List<TarifaAlquiler> listarVigentes(LocalDate fecha) {
        return em.createQuery(
                "SELECT t FROM TarifaAlquiler t WHERE t.vigenteDesde <= :fecha "
                + "AND (t.vigenteHasta IS NULL OR t.vigenteHasta >= :fecha) ORDER BY t.vigenteDesde", TarifaAlquiler.class)
                .setParameter("fecha", fecha)
                .getResultList();
    }

    /** Historial de precios de una instalación, lo más reciente primero. */
    public List<TarifaAlquiler> listarPorInstalacion(Integer idInstalacion) {
        return em.createQuery(
                "SELECT t FROM TarifaAlquiler t WHERE t.idInstalacion.idInstalacion = :id "
                + "ORDER BY t.vigenteDesde DESC", TarifaAlquiler.class)
                .setParameter("id", idInstalacion)
                .getResultList();
    }
}
