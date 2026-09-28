package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dto.DiaAgenda;
import com.sigrid.sigrid.dto.ReservaCalendario;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Turno;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Datos del calendario del administrador (admin/calendario-dashboard-admin.xhtml). Solo lectura: devuelve
 * DTO con los textos ya listos. Lo que ya terminó ("pasada") se marca aparte para mostrarlo en gris.
 *
 * CDI simple (no EJB) y Serializable porque lo inyecta un bean @ViewScoped (passivating), igual que DashboardServicio.
 */
@Dependent
public class CalendarioServicio implements Serializable {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DIA_LARGO = DateTimeFormatter.ofPattern("d 'de' MMMM", ES);
    private static final int DIAS_DEL_CALENDARIO = 42; // 6 semanas: la grilla no cambia de alto al pasar de mes
    private static final int MAX_HISTORIAL = 30;       // por lado (próximas y anteriores)

    @Inject
    private ReservaDAO reservaDAO;

    /** Reservas que cubre la grilla del mes (incluye los días de relleno). */
    public List<ReservaCalendario> reservasDelMes(YearMonth mes, LocalDate hoy, LocalTime ahora) {
        LocalDate inicio = inicioGrilla(mes);
        return aDto(reservaDAO.listarEntre(inicio, inicio.plusDays(DIAS_DEL_CALENDARIO - 1)), hoy, ahora);
    }

    public List<ReservaCalendario> reservasDelDia(LocalDate fecha, LocalDate hoy, LocalTime ahora) {
        return aDto(reservaDAO.listarEntre(fecha, fecha), hoy, ahora);
    }

    /** Grilla del mes (lunes a domingo, 6 semanas) con las reservas de cada día. */
    public List<DiaAgenda> calendario(List<ReservaCalendario> reservasDelMes, YearMonth mes, LocalDate seleccionado, LocalDate hoy) {
        Map<LocalDate, List<ReservaCalendario>> porDia = new HashMap<>();
        for (ReservaCalendario r : reservasDelMes) {
            porDia.computeIfAbsent(r.getFecha(), d -> new ArrayList<>()).add(r);
        }

        LocalDate inicio = inicioGrilla(mes);
        List<DiaAgenda> dias = new ArrayList<>(DIAS_DEL_CALENDARIO);
        for (int i = 0; i < DIAS_DEL_CALENDARIO; i++) {
            LocalDate dia = inicio.plusDays(i);
            List<ReservaCalendario> delDia = porDia.getOrDefault(dia, List.of());
            dias.add(new DiaAgenda(dia, YearMonth.from(dia).equals(mes), dia.equals(hoy), dia.isBefore(hoy),
                    dia.equals(seleccionado), delDia, etiquetaDia(dia, delDia.size())));
        }
        return dias;
    }

    /** Las que siguen (hoy en adelante y todavía no terminadas), la más cercana primero. */
    public List<ReservaCalendario> proximas(LocalDate hoy, LocalTime ahora) {
        List<ReservaCalendario> proximas = new ArrayList<>();
        for (ReservaCalendario r : aDto(reservaDAO.listarDesde(hoy, MAX_HISTORIAL), hoy, ahora)) {
            if (!r.isPasada()) {
                proximas.add(r);
            }
        }
        return proximas;
    }

    /** Las que ya terminaron, la más reciente primero. */
    public List<ReservaCalendario> anteriores(LocalDate hoy, LocalTime ahora) {
        List<ReservaCalendario> anteriores = new ArrayList<>();
        for (ReservaCalendario r : aDto(reservaDAO.listarDesde(hoy, MAX_HISTORIAL), hoy, ahora)) {
            if (r.isPasada()) {
                anteriores.add(0, r); // las de hoy que ya terminaron van primero
            }
        }
        anteriores.addAll(aDto(reservaDAO.listarAntesDe(hoy, MAX_HISTORIAL), hoy, ahora));
        return anteriores;
    }

    private static LocalDate inicioGrilla(YearMonth mes) {
        return mes.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private List<ReservaCalendario> aDto(List<Reserva> reservas, LocalDate hoy, LocalTime ahora) {
        List<ReservaCalendario> filas = new ArrayList<>();
        for (Reserva r : reservas) {
            Turno turno = r.getIdTurno();
            Instalacion instalacion = turno.getIdInstalacion();
            boolean pasada = r.getFechaTurno().isBefore(hoy)
                    || (r.getFechaTurno().equals(hoy) && !turno.getHoraFin().isAfter(ahora));
            filas.add(new ReservaCalendario(r.getIdReserva(), r.getFechaTurno(), turno.getHoraInicio(),
                    cuando(r.getFechaTurno(), hoy), turno.getHoraInicio().format(HORA),
                    turno.getHoraInicio().format(HORA) + " – " + turno.getHoraFin().format(HORA),
                    instalacion.getNombre(), instalacion.getNombre().replaceFirst("^Cancha de ", ""),
                    instalacion.getTipoDisciplina(), DashboardServicio.icono(instalacion.getTipoDisciplina()),
                    r.getEstado(), textoEstado(r.getEstado(), pasada), pasada ? "muted" : claseEstado(r.getEstado()),
                    pasada, r.getIdPago() == null ? null : DashboardServicio.monto(r.getIdPago().getMonto()),
                    DashboardServicio.nombreSocio(r.getIdSocio()), nota(r, pasada)));
        }
        return filas;
    }

    private static String cuando(LocalDate fecha, LocalDate hoy) {
        if (fecha.equals(hoy)) {
            return "Hoy";
        }
        if (fecha.equals(hoy.plusDays(1))) {
            return "Mañana";
        }
        if (fecha.equals(hoy.minusDays(1))) {
            return "Ayer";
        }
        return fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES) + " " + fecha.format(DIA_MES);
    }

    private static String textoEstado(Reserva.Estado estado, boolean pasada) {
        switch (estado) {
            case CONFIRMADA:
                return pasada ? "Completada" : "Confirmada";
            case PENDIENTE_PAGO:
                return "Pendiente";
            default:
                return "Cancelada";
        }
    }

    private static String claseEstado(Reserva.Estado estado) {
        switch (estado) {
            case CONFIRMADA:
                return "ok";
            case PENDIENTE_PAGO:
                return "warn";
            default:
                return "off";
        }
    }

    /** Aclaración del estado para el detalle del día. */
    private static String nota(Reserva r, boolean pasada) {
        switch (r.getEstado()) {
            case CONFIRMADA:
                return pasada ? "Reserva completada"
                        : r.getFechaConfirmacion() == null ? "Reserva confirmada"
                        : "Confirmada el " + r.getFechaConfirmacion().format(DIA_MES);
            case PENDIENTE_PAGO:
                return r.getIdPago() == null ? "Falta que el socio cargue el comprobante"
                        : "Comprobante cargado, por revisar";
            default:
                return "Reserva cancelada";
        }
    }

    private static String etiquetaDia(LocalDate dia, int reservas) {
        return dia.format(DIA_LARGO) + (reservas == 0 ? ": sin reservas"
                : ": " + reservas + (reservas == 1 ? " reserva" : " reservas"));
    }
}
