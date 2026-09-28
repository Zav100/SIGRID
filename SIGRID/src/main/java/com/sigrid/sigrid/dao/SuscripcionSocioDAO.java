package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import jakarta.enterprise.context.Dependent;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO de SuscripcionSocio: historial de membresías (una fila por renovación).
 */
@Dependent
public class SuscripcionSocioDAO extends BaseDAO<SuscripcionSocio> {

    public SuscripcionSocioDAO() {
        super(SuscripcionSocio.class);
    }

    /** Historial del socio, la más reciente primero. */
    public List<SuscripcionSocio> listarPorSocio(Integer idSocio) {
        return em.createQuery(
                "SELECT s FROM SuscripcionSocio s WHERE s.idSocio.idSocio = :id ORDER BY s.fechaSolicitud DESC",
                SuscripcionSocio.class)
                .setParameter("id", idSocio)
                .getResultList();
    }

    /** La suscripción VIGENTE del socio a la fecha dada, o null si no tiene. */
    public SuscripcionSocio buscarVigente(Integer idSocio, LocalDate hoy) {
        return primero(em.createQuery(
                "SELECT s FROM SuscripcionSocio s WHERE s.idSocio.idSocio = :id "
                + "AND s.estado = :vigente AND s.fechaVencimiento >= :hoy ORDER BY s.fechaVencimiento DESC",
                SuscripcionSocio.class)
                .setParameter("id", idSocio)
                .setParameter("vigente", SuscripcionSocio.Estado.VIGENTE)
                .setParameter("hoy", hoy));
    }

    /** Panel de membresías del administrador: todas las suscripciones, la más reciente primero. */
    public List<SuscripcionSocio> listarTodas() {
        return em.createQuery("SELECT s FROM SuscripcionSocio s ORDER BY s.fechaSolicitud DESC, s.idSuscripcion DESC",
                SuscripcionSocio.class).getResultList();
    }

    /** Todas las suscripciones con su socio, su tipo de socio y su pago ya cargados: base de los reportes. */
    public List<SuscripcionSocio> listarTodasParaReportes() {
        return em.createQuery(
                "SELECT s FROM SuscripcionSocio s JOIN FETCH s.idSocio so JOIN FETCH so.idCategoriaSocio "
                + "LEFT JOIN FETCH s.idPago ORDER BY s.fechaSolicitud", SuscripcionSocio.class)
                .getResultList();
    }

    /** Las suscripciones VIGENTES del socio a la fecha dada (puede haber más de una si renovó por adelantado). */
    public List<SuscripcionSocio> listarVigentesDelSocio(Integer idSocio, LocalDate hoy) {
        return em.createQuery(
                "SELECT s FROM SuscripcionSocio s WHERE s.idSocio.idSocio = :id "
                + "AND s.estado = :vigente AND s.fechaVencimiento >= :hoy ORDER BY s.fechaVencimiento DESC",
                SuscripcionSocio.class)
                .setParameter("id", idSocio)
                .setParameter("vigente", SuscripcionSocio.Estado.VIGENTE)
                .setParameter("hoy", hoy)
                .getResultList();
    }

    /** La suscripción con bloqueo de escritura (dentro de una transacción): dos administradores no la resuelven a la vez. */
    public SuscripcionSocio buscarParaResolver(Integer idSuscripcion) {
        return em.find(SuscripcionSocio.class, idSuscripcion, LockModeType.PESSIMISTIC_WRITE);
    }

    /** Filas {monto, fecha de validación, tipo de socio} de los pagos de membresía confirmados desde una fecha: los ingresos. */
    public List<Object[]> ingresosDesde(LocalDateTime desde) {
        return em.createQuery(
                "SELECT p.monto, p.fechaValidacion, s.idSocio.idCategoriaSocio.nombreCategoria FROM SuscripcionSocio s JOIN s.idPago p "
                + "WHERE p.estado = :confirmado AND p.fechaValidacion >= :desde", Object[].class)
                .setParameter("confirmado", Pago.Estado.CONFIRMADO)
                .setParameter("desde", desde)
                .getResultList();
    }

    /** Solicitudes de membresía con el comprobante ya cargado, que el administrador debe validar (contador del menú). */
    public long contarPendientesConComprobante() {
        return em.createQuery(
                "SELECT COUNT(s) FROM SuscripcionSocio s WHERE s.estado = :pendiente AND s.idPago.estado = :porValidar",
                Long.class)
                .setParameter("pendiente", SuscripcionSocio.Estado.PENDIENTE_PAGO)
                .setParameter("porValidar", Pago.Estado.PENDIENTE_VALIDACION)
                .getSingleResult();
    }

    /** Filas {idSocio, último vencimiento} de las membresías que llegaron a estar vigentes (VIGENTE o VENCIDA). */
    public List<Object[]> ultimosVencimientos() {
        return em.createQuery(
                "SELECT s.idSocio.idSocio, MAX(s.fechaVencimiento) FROM SuscripcionSocio s "
                + "WHERE s.estado IN :estados GROUP BY s.idSocio.idSocio", Object[].class)
                .setParameter("estados", List.of(SuscripcionSocio.Estado.VIGENTE, SuscripcionSocio.Estado.VENCIDA))
                .getResultList();
    }

    /** Vigentes que vencen entre las dos fechas (inclusive): alerta de vencimiento (RF-03.5). */
    public List<SuscripcionSocio> listarVigentesQueVencenEntre(LocalDate desde, LocalDate hasta) {
        return em.createQuery(
                "SELECT s FROM SuscripcionSocio s WHERE s.estado = :vigente "
                + "AND s.fechaVencimiento BETWEEN :desde AND :hasta ORDER BY s.fechaVencimiento",
                SuscripcionSocio.class)
                .setParameter("vigente", SuscripcionSocio.Estado.VIGENTE)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
    }

    /** Vigentes ya vencidas a la fecha dada: el job las pasa a VENCIDA y deja al socio NO_ACTIVO (RF-01.4.3). */
    public List<SuscripcionSocio> listarVigentesVencidasAntesDe(LocalDate hoy) {
        return em.createQuery(
                "SELECT s FROM SuscripcionSocio s WHERE s.estado = :vigente AND s.fechaVencimiento < :hoy",
                SuscripcionSocio.class)
                .setParameter("vigente", SuscripcionSocio.Estado.VIGENTE)
                .setParameter("hoy", hoy)
                .getResultList();
    }
}
