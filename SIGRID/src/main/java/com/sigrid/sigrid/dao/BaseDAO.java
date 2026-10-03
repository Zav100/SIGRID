package com.sigrid.sigrid.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;

/**
 * CRUD común de todos los DAO: cada uno es el único punto de acceso a su tabla
 * vía JPA (persistence unit "sigridPU", data source "sigridJNDI" de GlassFish).
 *
 * CDI simple (no EJB), igual que en REPS: los DAO son @Dependent con
 * @PersistenceContext y no abren transacciones; las abre el servicio con
 * @Transactional en los métodos que escriben.
 */
public abstract class BaseDAO<T> implements Serializable {

    @PersistenceContext(unitName = "sigridPU")
    protected EntityManager em;

    private final Class<T> clase;

    protected BaseDAO(Class<T> clase) {
        this.clase = clase;
    }

    public T buscarPorId(Integer id) {
        return em.find(clase, id);
    }

    /** Usa la NamedQuery "Entidad.findAll" de la entidad. */
    public List<T> listarTodos() {
        return em.createNamedQuery(clase.getSimpleName() + ".findAll", clase).getResultList();
    }

    public void crear(T entidad) {
        em.persist(entidad);
    }

    public T actualizar(T entidad) {
        return em.merge(entidad);
    }

    /** Manda ya los cambios pendientes a la base (no al cerrar la transacción). */
    public void flush() {
        em.flush();
    }

    /** Primer resultado de la consulta, o null si no hay ninguno. */
    protected <R> R primero(TypedQuery<R> consulta) {
        List<R> lista = consulta.setMaxResults(1).getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }
}
