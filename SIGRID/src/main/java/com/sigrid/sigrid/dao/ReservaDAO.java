package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import jakarta.enterprise.context.Dependent;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * DAO de Reserva. "Activa" = PENDIENTE_PAGO o CONFIRMADA: es la que ocupa el turno.
 * La doble reserva la impide la BD (UNIQUE turno_activo_key): el servicio debe
 * capturar el error del INSERT y avisar "el turno ya fue reservado".
 */
@Dependent
public class ReservaDAO extends BaseDAO<Reserva> {

    private static final List<Reserva.Estado> ACTIVAS
            = List.of(Reserva.Estado.PENDIENTE_PAGO, Reserva.Estado.CONFIRMADA);

    public ReservaDAO() {
        super(Reserva.class);
    }

    /** "Mis Reservas": todas las del socio, la más próxima primero. */
    public List<Reserva> listarPorSocio(Integer idSocio) {
        return em.createQuery(
                "SELECT r FROM Reserva r WHERE r.idSocio.idSocio = :id "
                + "ORDER BY r.fechaTurno DESC, r.idTurno.horaInicio", Reserva.class)
                .setParameter("id", idSocio)
                .getResultList();
    }

    public List<Reserva> listarPorSocioYEstado(Integer idSocio, Reserva.Estado estado) {
        return em.createQuery(
                "SELECT r FROM Reserva r WHERE r.idSocio.idSocio = :id AND r.estado = :estado "
                + "ORDER BY r.fechaTurno DESC, r.idTurno.horaInicio", Reserva.class)
                .setParameter("id", idSocio)
                .setParameter("estado", estado)
                .getResultList();
    }

    /** Guarda la reserva y manda el INSERT ya (no al cerrar la transacción): así la BD rechaza acá la doble reserva y el id queda asignado. */
    public void insertar(Reserva reserva) {
        em.persist(reserva);
        em.flush();
    }

    /** Reservas del socio que están esperando su comprobante: PENDIENTE_PAGO sin pago cargado. */
    public long contarSinComprobanteDe(Integer idSocio) {
        return em.createQuery(
                "SELECT COUNT(r) FROM Reserva r WHERE r.idSocio.idSocio = :id AND r.estado = :pendiente AND r.idPago IS NULL",
                Long.class)
                .setParameter("id", idSocio)
                .setParameter("pendiente", Reserva.Estado.PENDIENTE_PAGO)
                .getSingleResult();
    }

    /** La reserva activa de un turno en un día, o null si está libre. */
    public Reserva buscarActivaPorTurnoYFecha(Integer idTurno, LocalDate fecha) {
        return primero(em.createQuery(
                "SELECT r FROM Reserva r WHERE r.idTurno.idTurno = :turno AND r.fechaTurno = :fecha "
                + "AND r.estado IN :activas", Reserva.class)
                .setParameter("turno", idTurno)
                .setParameter("fecha", fecha)
                .setParameter("activas", ACTIVAS));
    }

    /** Reservas activas de una instalación en un rango de días: arma el calendario de disponibilidad. */
    public List<Reserva> listarActivasDeInstalacionEntre(Integer idInstalacion, LocalDate desde, LocalDate hasta) {
        return em.createQuery(
                "SELECT r FROM Reserva r WHERE r.idTurno.idInstalacion.idInstalacion = :instalacion "
                + "AND r.fechaTurno BETWEEN :desde AND :hasta AND r.estado IN :activas "
                + "ORDER BY r.fechaTurno, r.idTurno.horaInicio", Reserva.class)
                .setParameter("instalacion", idInstalacion)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .setParameter("activas", ACTIVAS)
                .getResultList();
    }

    /** Informe de uso diario: reservas confirmadas de un día, todas las instalaciones. */
    public List<Reserva> listarConfirmadasPorFecha(LocalDate fecha) {
        return em.createQuery(
                "SELECT r FROM Reserva r WHERE r.fechaTurno = :fecha AND r.estado = :confirmada "
                + "ORDER BY r.idTurno.idInstalacion.nombre, r.idTurno.horaInicio", Reserva.class)
                .setParameter("fecha", fecha)
                .setParameter("confirmada", Reserva.Estado.CONFIRMADA)
                .getResultList();
    }

    /** Agenda de un día para el panel del administrador: todas menos las RECHAZADAS, por horario. */
    public List<Reserva> listarDelDia(LocalDate fecha) {
        return em.createQuery(
                "SELECT r FROM Reserva r WHERE r.fechaTurno = :fecha AND r.estado <> :rechazada "
                + "ORDER BY r.idTurno.horaInicio, r.idTurno.idInstalacion.nombre", Reserva.class)
                .setParameter("fecha", fecha)
                .setParameter("rechazada", Reserva.Estado.RECHAZADA)
                .getResultList();
    }

    /** Calendario del administrador: reservas entre dos días (todas menos las RECHAZADAS), por horario. */
    public List<Reserva> listarEntre(LocalDate desde, LocalDate hasta) {
        return em.createQuery(
                "SELECT r FROM Reserva r WHERE r.fechaTurno BETWEEN :desde AND :hasta AND r.estado <> :rechazada "
                + "ORDER BY r.fechaTurno, r.idTurno.horaInicio, r.idTurno.idInstalacion.nombre", Reserva.class)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .setParameter("rechazada", Reserva.Estado.RECHAZADA)
                .getResultList();
    }

    /** Historial, hacia adelante: reservas desde un día (inclusive), la más cercana primero (sin RECHAZADAS). */
    public List<Reserva> listarDesde(LocalDate fecha, int maximo) {
        return em.createQuery(
                "SELECT r FROM Reserva r WHERE r.fechaTurno >= :fecha AND r.estado <> :rechazada "
                + "ORDER BY r.fechaTurno, r.idTurno.horaInicio", Reserva.class)
                .setParameter("fecha", fecha)
                .setParameter("rechazada", Reserva.Estado.RECHAZADA)
                .setMaxResults(maximo)
                .getResultList();
    }

    /** Historial, hacia atrás: reservas anteriores a un día, la más reciente primero (sin RECHAZADAS). */
    public List<Reserva> listarAntesDe(LocalDate fecha, int maximo) {
        return em.createQuery(
                "SELECT r FROM Reserva r WHERE r.fechaTurno < :fecha AND r.estado <> :rechazada "
                + "ORDER BY r.fechaTurno DESC, r.idTurno.horaInicio DESC", Reserva.class)
                .setParameter("fecha", fecha)
                .setParameter("rechazada", Reserva.Estado.RECHAZADA)
                .setMaxResults(maximo)
                .getResultList();
    }

    public long contarActivasDelDia(LocalDate fecha) {
        return em.createQuery(
                "SELECT COUNT(r) FROM Reserva r WHERE r.fechaTurno = :fecha AND r.estado IN :activas", Long.class)
                .setParameter("fecha", fecha)
                .setParameter("activas", ACTIVAS)
                .getSingleResult();
    }

    /**
     * Solicitudes por confirmar: PENDIENTE_PAGO con el comprobante ya cargado (pago PENDIENTE_VALIDACION),
     * el turno más cercano primero. Las que todavía no tienen comprobante esperan al socio, no al administrador.
     */
    public List<Reserva> listarPendientes() {
        return em.createQuery(
                "SELECT r FROM Reserva r WHERE r.estado = :pendiente AND r.idPago.estado = :porValidar "
                + "ORDER BY r.fechaTurno, r.idTurno.horaInicio", Reserva.class)
                .setParameter("pendiente", Reserva.Estado.PENDIENTE_PAGO)
                .setParameter("porValidar", Pago.Estado.PENDIENTE_VALIDACION)
                .getResultList();
    }

    public long contarPendientes() {
        return em.createQuery(
                "SELECT COUNT(r) FROM Reserva r WHERE r.estado = :pendiente AND r.idPago.estado = :porValidar",
                Long.class)
                .setParameter("pendiente", Reserva.Estado.PENDIENTE_PAGO)
                .setParameter("porValidar", Pago.Estado.PENDIENTE_VALIDACION)
                .getSingleResult();
    }

    /** La reserva con bloqueo de escritura (dentro de una transacción): dos administradores no resuelven la misma solicitud a la vez. */
    public Reserva buscarParaResolver(Integer idReserva) {
        return em.find(Reserva.class, idReserva, LockModeType.PESSIMISTIC_WRITE);
    }

    /**
     * Listado de reservas del administrador: todas, en cualquier estado, la de fecha más lejana primero.
     * Trae de una vez turno, instalación, socio, usuario, pago y tarifa para no consultar fila por fila.
     * ponytail: sin paginación en la BD; si el historial crece a decenas de miles, filtrar por fecha acá.
     */
    public List<Reserva> listarTodasParaAdmin() {
        return em.createQuery(
                "SELECT r FROM Reserva r JOIN FETCH r.idTurno t JOIN FETCH t.idInstalacion "
                + "JOIN FETCH r.idSocio s JOIN FETCH s.idUsuario LEFT JOIN FETCH r.idPago LEFT JOIN FETCH r.idTarifa "
                + "ORDER BY r.fechaTurno DESC, t.horaInicio DESC, r.idReserva DESC", Reserva.class)
                .getResultList();
    }

    /**
     * Reservas canceladas con crédito para reprogramar (vigente o ya vencido): CANCELADA con fecha límite de
     * reprogramación. El crédito ES la reserva: al reprogramarla vuelve a CONFIRMADA y la fecha límite se borra.
     */
    public List<Reserva> listarConCredito() {
        return em.createQuery(
                "SELECT r FROM Reserva r JOIN FETCH r.idTurno t JOIN FETCH t.idInstalacion "
                + "JOIN FETCH r.idSocio s JOIN FETCH s.idUsuario LEFT JOIN FETCH r.idPago LEFT JOIN FETCH r.idTarifa "
                + "WHERE r.estado = :cancelada AND r.fechaLimiteReprogramacion IS NOT NULL "
                + "ORDER BY r.fechaLimiteReprogramacion, r.idReserva", Reserva.class)
                .setParameter("cancelada", Reserva.Estado.CANCELADA)
                .getResultList();
    }

    /**
     * Demanda por instalación entre dos días: filas {id de la instalación, estado, cantidad, monto cobrado} de las
     * reservas activas (PENDIENTE_PAGO o CONFIRMADA). El monto es la suma de los pagos ya cargados (puede ser null).
     */
    public List<Object[]> resumenPorInstalacion(LocalDate desde, LocalDate hasta) {
        return em.createQuery(
                "SELECT t.idInstalacion.idInstalacion, r.estado, COUNT(r), SUM(p.monto) FROM Reserva r JOIN r.idTurno t "
                + "LEFT JOIN r.idPago p WHERE r.fechaTurno BETWEEN :desde AND :hasta AND r.estado IN :activas "
                + "GROUP BY t.idInstalacion.idInstalacion, r.estado", Object[].class)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .setParameter("activas", ACTIVAS)
                .getResultList();
    }

    /** Las próximas CONFIRMADAS a partir de ahora (incluye la que está en curso). */
    public List<Reserva> listarProximasConfirmadas(LocalDate hoy, LocalTime ahora, int maximo) {
        return em.createQuery(
                "SELECT r FROM Reserva r WHERE r.estado = :confirmada "
                + "AND (r.fechaTurno > :hoy OR (r.fechaTurno = :hoy AND r.idTurno.horaFin > :ahora)) "
                + "ORDER BY r.fechaTurno, r.idTurno.horaInicio", Reserva.class)
                .setParameter("confirmada", Reserva.Estado.CONFIRMADA)
                .setParameter("hoy", hoy)
                .setParameter("ahora", ahora)
                .setMaxResults(maximo)
                .getResultList();
    }

    /** Filas {fecha, estado, cantidad} entre dos días (sin RECHAZADAS): los puntitos del calendario. */
    public List<Object[]> contarPorFechaYEstado(LocalDate desde, LocalDate hasta) {
        return em.createQuery(
                "SELECT r.fechaTurno, r.estado, COUNT(r) FROM Reserva r "
                + "WHERE r.fechaTurno BETWEEN :desde AND :hasta AND r.estado <> :rechazada "
                + "GROUP BY r.fechaTurno, r.estado", Object[].class)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .setParameter("rechazada", Reserva.Estado.RECHAZADA)
                .getResultList();
    }

    /** Pendientes sin comprobante creadas antes del límite: el timer las cancela para liberar el turno. */
    public List<Reserva> listarPendientesSinComprobanteAntesDe(LocalDateTime limite) {
        return em.createQuery(
                "SELECT r FROM Reserva r WHERE r.estado = :pendiente AND r.idPago IS NULL AND r.fechaReserva < :limite",
                Reserva.class)
                .setParameter("pendiente", Reserva.Estado.PENDIENTE_PAGO)
                .setParameter("limite", limite)
                .getResultList();
    }
}
