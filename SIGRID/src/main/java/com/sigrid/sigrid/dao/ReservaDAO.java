package com.sigrid.sigrid.dao;

import com.sigrid.sigrid.dto.FiltroReservas;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import jakarta.enterprise.context.Dependent;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Las confirmadas que todavía no terminaron (incluye la que está en curso), la más próxima primero, con turno,
     * instalación, socio, usuario y pago. Son pocas: no se reserva con más de 30 días de anticipación.
     */
    public List<Reserva> listarProximasConfirmadasParaAdmin(LocalDate hoy, LocalTime ahora) {
        return em.createQuery(
                "SELECT r FROM Reserva r" + UNIONES_CON_DATOS + " WHERE r.estado = :confirmada "
                + "AND (r.fechaTurno > :hoy OR (r.fechaTurno = :hoy AND t.horaFin > :ahora)) "
                + "ORDER BY r.fechaTurno, t.horaInicio, r.idReserva", Reserva.class)
                .setParameter("confirmada", Reserva.Estado.CONFIRMADA)
                .setParameter("hoy", hoy)
                .setParameter("ahora", ahora)
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
     * Los créditos de una instalación que seguían vigentes cuando empezó su mantenimiento (CC-02): CANCELADA, con fecha
     * límite igual o posterior al primer día del mantenimiento. Son los que hay que retomar al reactivarla.
     */
    public List<Reserva> listarConCreditoDesde(Integer idInstalacion, LocalDate desde) {
        return em.createQuery(
                "SELECT r FROM Reserva r JOIN FETCH r.idTurno t JOIN FETCH t.idInstalacion i "
                + "WHERE i.idInstalacion = :instalacion AND r.estado = :cancelada "
                + "AND r.fechaLimiteReprogramacion IS NOT NULL AND r.fechaLimiteReprogramacion >= :desde", Reserva.class)
                .setParameter("instalacion", idInstalacion)
                .setParameter("cancelada", Reserva.Estado.CANCELADA)
                .setParameter("desde", desde)
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

    // ---------- listado y PDF del administrador con filtros en SQL (CC-06) ----------

    private static final String UNIONES_CON_DATOS = " JOIN FETCH r.idTurno t JOIN FETCH t.idInstalacion i "
            + "JOIN FETCH r.idSocio s JOIN FETCH s.idUsuario u LEFT JOIN FETCH r.idPago p LEFT JOIN FETCH r.idTarifa";
    private static final java.time.format.DateTimeFormatter FECHA_ESCRITA
            = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Un texto para LIKE: sin comodines propios (el caracter de escape es "!"). */
    private static String comodin(String texto) {
        return "%" + texto.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    /**
     * Las condiciones del filtro (con la misma regla de estado que ve el administrador) y sus parámetros. El buscador
     * mira socio, correo, DNI, instalación y disciplina (la colación de la base ignora tildes y mayúsculas) y, si el
     * texto es un número o una fecha dd/mm/aaaa, también el número de reserva o el día del turno.
     */
    private static String donde(FiltroReservas f, LocalDate hoy, LocalTime ahora, Map<String, Object> parametros) {
        StringBuilder w = new StringBuilder(" WHERE 1 = 1");
        if (f.getDesde() != null) {
            w.append(" AND r.fechaTurno >= :desde");
            parametros.put("desde", f.getDesde());
        }
        if (f.getHasta() != null) {
            w.append(" AND r.fechaTurno <= :hasta");
            parametros.put("hasta", f.getHasta());
        }
        if (!f.getDisciplina().isEmpty()) {
            w.append(" AND i.tipoDisciplina = :disciplina");
            parametros.put("disciplina", f.getDisciplina());
        }
        if (!f.getInstalacion().isEmpty()) {
            w.append(" AND i.nombre = :instalacion");
            parametros.put("instalacion", f.getInstalacion());
        }
        switch (f.getEstado()) {
            case "CONFIRMADA":
                w.append(" AND r.estado = :estado");
                parametros.put("estado", Reserva.Estado.CONFIRMADA);
                break;
            case "CANCELADA":
                w.append(" AND r.estado = :estado");
                parametros.put("estado", Reserva.Estado.CANCELADA);
                break;
            case "RECHAZADA":
                w.append(" AND r.estado = :estado");
                parametros.put("estado", Reserva.Estado.RECHAZADA);
                break;
            case "POR_CONFIRMAR":
                w.append(" AND r.estado = :estado AND p.estado = :porValidar");
                parametros.put("estado", Reserva.Estado.PENDIENTE_PAGO);
                parametros.put("porValidar", Pago.Estado.PENDIENTE_VALIDACION);
                break;
            case "ESPERANDO_PAGO":
                w.append(" AND r.estado = :estado AND (p IS NULL OR p.estado <> :porValidar)");
                parametros.put("estado", Reserva.Estado.PENDIENTE_PAGO);
                parametros.put("porValidar", Pago.Estado.PENDIENTE_VALIDACION);
                break;
            default:
                break;
        }
        if (f.getFinalizadas() != null) {
            w.append(f.getFinalizadas()
                    ? " AND (r.fechaTurno < :hoy OR (r.fechaTurno = :hoy AND t.horaFin <= :ahora))"
                    : " AND (r.fechaTurno > :hoy OR (r.fechaTurno = :hoy AND t.horaFin > :ahora))");
            parametros.put("hoy", hoy);
            parametros.put("ahora", ahora);
        }
        if (!f.getTexto().isEmpty()) {
            w.append(" AND (CONCAT(u.nombre, ' ', u.apellido) LIKE :q ESCAPE '!' OR u.email LIKE :q ESCAPE '!' "
                    + "OR s.dni LIKE :q ESCAPE '!' OR i.nombre LIKE :q ESCAPE '!' OR i.tipoDisciplina LIKE :q ESCAPE '!'");
            parametros.put("q", comodin(f.getTexto()));
            if (f.getTexto().matches("\\d{1,9}")) {
                w.append(" OR r.idReserva = :numero");
                parametros.put("numero", Integer.valueOf(f.getTexto()));
            }
            try {
                LocalDate dia = LocalDate.parse(f.getTexto(), FECHA_ESCRITA);
                w.append(" OR r.fechaTurno = :dia");
                parametros.put("dia", dia);
            } catch (java.time.format.DateTimeParseException e) {
                // no es una fecha completa: se busca solo como texto
            }
            w.append(")");
        }
        return w.toString();
    }

    private static <T> TypedQuery<T> con(TypedQuery<T> consulta, Map<String, Object> parametros) {
        parametros.forEach(consulta::setParameter);
        return consulta;
    }

    /**
     * Reservas del administrador que cumplen el filtro, la de fecha más lejana primero, hasta "maximo" filas. Trae de
     * una vez turno, instalación, socio, usuario, pago y tarifa. La base aplica todos los filtros y el tope: no se
     * cargan las reservas que no se van a mostrar. Se ordena solo por columnas de la reserva (fecha y número) para
     * que la base recorra el índice de fecha_turno y corte al llegar al tope, sin ordenar toda la tabla.
     */
    public List<Reserva> buscarParaAdmin(FiltroReservas f, LocalDate hoy, LocalTime ahora, int maximo) {
        Map<String, Object> parametros = new HashMap<>();
        String donde = donde(f, hoy, ahora, parametros);
        return con(em.createQuery("SELECT r FROM Reserva r" + UNIONES_CON_DATOS + donde
                + " ORDER BY r.fechaTurno DESC, r.idReserva DESC", Reserva.class), parametros)
                .setMaxResults(maximo)
                .getResultList();
    }

    /** Las del PDF: mismas condiciones, en orden cronológico (día, hora y luego instalación), hasta "maximo" filas. */
    public List<Reserva> buscarParaInforme(FiltroReservas f, LocalDate hoy, LocalTime ahora, int maximo) {
        Map<String, Object> parametros = new HashMap<>();
        String donde = donde(f, hoy, ahora, parametros);
        return con(em.createQuery("SELECT r FROM Reserva r" + UNIONES_CON_DATOS + donde
                + " ORDER BY r.fechaTurno, t.horaInicio, i.nombre, r.idReserva", Reserva.class), parametros)
                .setMaxResults(maximo)
                .getResultList();
    }

    /** Cuántas reservas cumplen el filtro (para "mostrando N de M" y para saber si entran en un PDF). */
    public long contarParaAdmin(FiltroReservas f, LocalDate hoy, LocalTime ahora) {
        Map<String, Object> parametros = new HashMap<>();
        String donde = donde(f, hoy, ahora, parametros);
        // solo las uniones que el filtro necesita: cada reserva tiene turno, socio y usuario, así que no cambian la cuenta
        boolean texto = !f.getTexto().isEmpty();
        boolean instalacion = texto || !f.getDisciplina().isEmpty() || !f.getInstalacion().isEmpty();
        boolean pago = f.getEstado().equals("POR_CONFIRMAR") || f.getEstado().equals("ESPERANDO_PAGO");
        String uniones = (instalacion || f.getFinalizadas() != null ? " JOIN r.idTurno t" : "")
                + (instalacion ? " JOIN t.idInstalacion i" : "")
                + (texto ? " JOIN r.idSocio s JOIN s.idUsuario u" : "")
                + (pago ? " LEFT JOIN r.idPago p" : "");
        return con(em.createQuery("SELECT COUNT(r) FROM Reserva r" + uniones + donde, Long.class), parametros)
                .getSingleResult();
    }

    /** Filas {estado de la reserva, cantidad} de todas las reservas. */
    public List<Object[]> contarPorEstado() {
        return em.createQuery("SELECT r.estado, COUNT(r) FROM Reserva r GROUP BY r.estado", Object[].class)
                .getResultList();
    }

    /** Cuántas CONFIRMADAS todavía no terminaron (incluye la que está en curso). */
    public long contarProximasConfirmadas(LocalDate hoy, LocalTime ahora) {
        return em.createQuery(
                "SELECT COUNT(r) FROM Reserva r JOIN r.idTurno t WHERE r.estado = :confirmada "
                + "AND (r.fechaTurno > :hoy OR (r.fechaTurno = :hoy AND t.horaFin > :ahora))", Long.class)
                .setParameter("confirmada", Reserva.Estado.CONFIRMADA)
                .setParameter("hoy", hoy)
                .setParameter("ahora", ahora)
                .getSingleResult();
    }

    /**
     * Filas {id del turno, cantidad} de las reservas activas: una por franja (unas decenas de filas aunque haya
     * cientos de miles de reservas). De acá salen los rankings por instalación y por hora.
     */
    public List<Object[]> contarActivasPorTurno() {
        return em.createQuery(
                "SELECT r.idTurno.idTurno, COUNT(r) FROM Reserva r WHERE r.estado IN :activas "
                + "GROUP BY r.idTurno.idTurno", Object[].class)
                .setParameter("activas", ACTIVAS)
                .getResultList();
    }

    /** Las confirmadas desde un momento (con su pago): lo recaudado en los últimos días. */
    public List<Reserva> listarConfirmadasDesde(LocalDateTime desde) {
        return em.createQuery(
                "SELECT r FROM Reserva r JOIN FETCH r.idPago WHERE r.estado = :confirmada AND r.fechaConfirmacion >= :desde",
                Reserva.class)
                .setParameter("confirmada", Reserva.Estado.CONFIRMADA)
                .setParameter("desde", desde)
                .getResultList();
    }

    /**
     * Lo que necesitan los reportes de un período {desde, hasta} (con su período previo ya incluido en "desde"): las
     * reservas con turno en el rango, las que cobraron (pago validado) en el rango, las canceladas con crédito que
     * vence desde "desde" en adelante (o está en pausa por un mantenimiento) y las que esperan validación. Nada más:
     * no se carga toda la tabla.
     */
    public List<Reserva> listarParaReportes(LocalDate desde, LocalDate hasta) {
        return em.createQuery(
                "SELECT r FROM Reserva r" + UNIONES_CON_DATOS + " WHERE (r.fechaTurno BETWEEN :desde AND :hasta) "
                + "OR (p.estado = :confirmado AND p.fechaValidacion >= :desdeHora AND p.fechaValidacion < :hastaHora) "
                + "OR (r.estado = :cancelada AND r.fechaLimiteReprogramacion IS NOT NULL "
                + "AND (r.fechaLimiteReprogramacion >= :desde OR i.estado = :mantenimiento)) "
                + "OR (r.estado = :pendiente AND p.estado = :porValidar) "
                + "ORDER BY r.fechaTurno DESC, t.horaInicio DESC, r.idReserva DESC", Reserva.class)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .setParameter("desdeHora", desde.atStartOfDay())
                .setParameter("hastaHora", hasta.plusDays(1).atStartOfDay())
                .setParameter("confirmado", Pago.Estado.CONFIRMADO)
                .setParameter("cancelada", Reserva.Estado.CANCELADA)
                .setParameter("mantenimiento", Instalacion.Estado.DESHABILITADA_MANTENIMIENTO)
                .setParameter("pendiente", Reserva.Estado.PENDIENTE_PAGO)
                .setParameter("porValidar", Pago.Estado.PENDIENTE_VALIDACION)
                .getResultList();
    }
}
