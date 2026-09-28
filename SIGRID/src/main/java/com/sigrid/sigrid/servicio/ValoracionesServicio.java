package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.ValoracionReservaDAO;
import com.sigrid.sigrid.dto.PromedioInstalacion;
import com.sigrid.sigrid.dto.ValoracionFila;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Turno;
import com.sigrid.sigrid.repositorio.ValoracionReserva;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Lectura de las valoraciones que dejan los socios al terminar su reserva, para que el administrador vea cómo
 * viene cada instalación y qué mejorar. Es solo lectura: las valoraciones las crea el socio.
 *
 * CDI simple (no EJB) y Serializable, igual que ReservaServicio.
 */
@Dependent
public class ValoracionesServicio implements Serializable {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int VENTANA_DIAS = 30;

    @Inject
    private ValoracionReservaDAO valoracionDAO;
    @Inject
    private InstalacionDAO instalacionDAO;

    /** Todas las valoraciones, la más reciente primero. */
    public List<ValoracionFila> listar() {
        List<ValoracionFila> filas = new ArrayList<>();
        for (ValoracionReserva v : valoracionDAO.listarTodasParaAdmin()) {
            Reserva reserva = v.getIdReserva();
            Turno turno = reserva.getIdTurno();
            Instalacion instalacion = turno.getIdInstalacion();
            String turnoTexto = reserva.getFechaTurno().getDayOfWeek().getDisplayName(TextStyle.SHORT, ES) + " "
                    + reserva.getFechaTurno().format(FECHA) + " · " + turno.getHoraInicio().format(HORA) + " – "
                    + turno.getHoraFin().format(HORA);
            filas.add(new ValoracionFila(v.getFechaValoracion(), v.getFechaValoracion().format(FECHA),
                    DashboardServicio.nombreSocio(reserva.getIdSocio()),
                    reserva.getIdSocio().getIdCategoriaSocio().getNombreCategoria(), instalacion.getNombre(),
                    instalacion.getTipoDisciplina(), DashboardServicio.icono(instalacion.getTipoDisciplina()),
                    turnoTexto, v.getPuntaje(), v.getComentario() == null ? "" : v.getComentario().trim()));
        }
        return filas;
    }

    /**
     * Una fila por instalación que se reserva (aunque todavía no tenga valoraciones), con las peor valoradas primero
     * y las que no tienen datos al final. La tendencia compara los últimos {@value #VENTANA_DIAS} días con los
     * {@value #VENTANA_DIAS} anteriores.
     */
    public List<PromedioInstalacion> promedios(List<ValoracionFila> todas, LocalDateTime ahora) {
        LocalDateTime corte = ahora.minusDays(VENTANA_DIAS);
        LocalDateTime corteAnterior = ahora.minusDays(2L * VENTANA_DIAS);
        List<PromedioInstalacion> filas = new ArrayList<>();
        for (Instalacion i : instalacionDAO.listarReservables()) {
            int cantidad = 0;
            int suma = 0;
            int recientes = 0;
            int sumaRecientes = 0;
            int previas = 0;
            int sumaPrevias = 0;
            for (ValoracionFila v : todas) {
                if (!v.getInstalacion().equals(i.getNombre())) {
                    continue;
                }
                cantidad++;
                suma += v.getPuntaje();
                if (!v.getFecha().isBefore(corte)) {
                    recientes++;
                    sumaRecientes += v.getPuntaje();
                } else if (!v.getFecha().isBefore(corteAnterior)) {
                    previas++;
                    sumaPrevias += v.getPuntaje();
                }
            }

            double promedio = cantidad == 0 ? 0 : (double) suma / cantidad;
            String tendencia = "";
            String clase = "muted";
            if (recientes > 0 && previas > 0) {
                double diferencia = (double) sumaRecientes / recientes - (double) sumaPrevias / previas;
                double redondeada = Math.round(diferencia * 10) / 10.0;
                tendencia = redondeada == 0 ? "Sin cambios en " + VENTANA_DIAS + " días"
                        : (redondeada > 0 ? "+" : "") + decimal(redondeada) + " en " + VENTANA_DIAS + " días";
                clase = redondeada > 0 ? "ok" : redondeada < 0 ? "off" : "muted";
            }
            filas.add(new PromedioInstalacion(i.getNombre(), DashboardServicio.icono(i.getTipoDisciplina()),
                    i.getTipoDisciplina(), cantidad, promedio, cantidad == 0 ? "—" : decimal(promedio), tendencia,
                    clase));
        }
        filas.sort(Comparator.comparing((PromedioInstalacion p) -> !p.isConDatos())
                .thenComparingDouble(PromedioInstalacion::getPromedio)
                .thenComparing(PromedioInstalacion::getNombre));
        return filas;
    }

    /** Un decimal y coma: 4,3. */
    public static String decimal(double valor) {
        return String.format(ES, "%.1f", valor);
    }
}
