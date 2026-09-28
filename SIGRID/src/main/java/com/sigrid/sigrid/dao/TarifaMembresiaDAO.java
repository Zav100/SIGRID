package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.TarifaMembresia;
import jakarta.enterprise.context.Dependent;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO de TarifaMembresia: historial de precios de la cuota mensual por categoría.
 */
@Dependent
public class TarifaMembresiaDAO extends BaseDAO<TarifaMembresia> {

    public TarifaMembresiaDAO() {
        super(TarifaMembresia.class);
    }

    /** La cuota vigente de una categoría en la fecha dada; null si no hay. */
    public TarifaMembresia buscarVigente(Integer idCategoriaSocio, LocalDate fecha) {
        return primero(em.createQuery(
                "SELECT t FROM TarifaMembresia t WHERE t.idCategoriaSocio.idCategoriaSocio = :categoria "
                + "AND t.vigenteDesde <= :fecha AND (t.vigenteHasta IS NULL OR t.vigenteHasta >= :fecha) "
                + "ORDER BY t.vigenteDesde DESC", TarifaMembresia.class)
                .setParameter("categoria", idCategoriaSocio)
                .setParameter("fecha", fecha));
    }

    /** Todo el historial de cuotas (todas las categorías), lo más reciente primero. */
    public List<TarifaMembresia> listarHistorial() {
        return em.createQuery("SELECT t FROM TarifaMembresia t ORDER BY t.vigenteDesde DESC", TarifaMembresia.class)
                .getResultList();
    }

    /** Historial de una categoría, lo más reciente primero. */
    public List<TarifaMembresia> listarPorCategoria(Integer idCategoriaSocio) {
        return em.createQuery(
                "SELECT t FROM TarifaMembresia t WHERE t.idCategoriaSocio.idCategoriaSocio = :id "
                + "ORDER BY t.vigenteDesde DESC", TarifaMembresia.class)
                .setParameter("id", idCategoriaSocio)
                .getResultList();
    }
}
