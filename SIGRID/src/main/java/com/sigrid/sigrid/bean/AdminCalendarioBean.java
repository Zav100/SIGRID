package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.DiaAgenda;
import com.sigrid.sigrid.dto.ReservaCalendario;
import com.sigrid.sigrid.servicio.CalendarioServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Bean de admin/calendario-dashboard-admin.xhtml. El historial se lee al abrir la página; cambiar de mes y
 * elegir un día (AJAX) solo vuelven a leer las reservas de ese mes o de ese día.
 */
@Named("adminCalendarioBean")
@ViewScoped
public class AdminCalendarioBean implements Serializable {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter DIA_LARGO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES);

    @Inject
    private CalendarioServicio servicio;

    private LocalDate hoy;
    private YearMonth mes;
    private LocalDate diaSeleccionado;

    private List<ReservaCalendario> proximas;
    private List<ReservaCalendario> anteriores;
    private List<DiaAgenda> dias;
    private List<ReservaCalendario> delDia;

    // reservas del mes que se está mirando
    private int confirmadasMes;
    private int completadasMes;
    private int pendientesMes;
    private int canceladasMes;

    @PostConstruct
    public void init() {
        hoy = LocalDate.now();
        mes = YearMonth.from(hoy);
        diaSeleccionado = hoy;
        LocalTime ahora = LocalTime.now();
        proximas = servicio.proximas(hoy, ahora);
        anteriores = servicio.anteriores(hoy, ahora);
        cargarMes();
        delDia = servicio.reservasDelDia(diaSeleccionado, hoy, ahora);
    }

    // ---------- acciones (AJAX) ----------

    public void mesAnterior() {
        mes = mes.minusMonths(1);
        cargarMes();
    }

    public void mesSiguiente() {
        mes = mes.plusMonths(1);
        cargarMes();
    }

    public void irAHoy() {
        seleccionarDia(hoy);
    }

    /** Elige un día: el calendario salta a su mes (si era un día de relleno) y el detalle pasa a mostrar ese día. */
    public void seleccionarDia(LocalDate fecha) {
        diaSeleccionado = fecha;
        mes = YearMonth.from(fecha);
        cargarMes();
        delDia = servicio.reservasDelDia(fecha, hoy, LocalTime.now());
    }

    private void cargarMes() {
        List<ReservaCalendario> reservas = servicio.reservasDelMes(mes, hoy, LocalTime.now());
        dias = servicio.calendario(reservas, mes, diaSeleccionado, hoy);
        confirmadasMes = 0;
        completadasMes = 0;
        pendientesMes = 0;
        canceladasMes = 0;
        for (ReservaCalendario r : reservas) {
            if (!YearMonth.from(r.getFecha()).equals(mes)) {
                continue; // día de relleno de otro mes
            }
            switch (r.getEstado()) {
                case CONFIRMADA:
                    if (r.isPasada()) {
                        completadasMes++;
                    } else {
                        confirmadasMes++;
                    }
                    break;
                case PENDIENTE_PAGO:
                    pendientesMes++;
                    break;
                default:
                    canceladasMes++;
            }
        }
    }

    // ---------- encabezado ----------

    public String getFechaLarga() {
        return capitalizar(hoy.format(FECHA_LARGA));
    }

    public String getResumen() {
        int n = proximas.size();
        return n == 0 ? "No hay reservas por delante."
                : "Hay " + n + (n == 1 ? " reserva por delante" : " reservas por delante") + " (las que siguen, en color).";
    }

    // ---------- calendario ----------

    public List<DiaAgenda> getDias() {
        return dias;
    }

    public String getTituloMes() {
        return capitalizar(mes.getMonth().getDisplayName(TextStyle.FULL, ES)) + " " + mes.getYear();
    }

    /** Para "en septiembre" en los contadores. */
    public String getNombreMes() {
        return mes.getMonth().getDisplayName(TextStyle.FULL, ES);
    }

    /** ¿El calendario ya está en el mes actual con el día de hoy elegido? (entonces no hace falta el botón "Hoy") */
    public boolean isEnHoy() {
        return mes.equals(YearMonth.from(hoy)) && diaSeleccionado.equals(hoy);
    }

    // ---------- detalle del día ----------

    public List<ReservaCalendario> getDelDia() {
        return delDia;
    }

    /** "Hoy, sábado 26 de septiembre", "Mañana, domingo 27 de septiembre" o "Viernes 25 de septiembre". */
    public String getTituloDia() {
        String largo = diaSeleccionado.format(DIA_LARGO);
        if (diaSeleccionado.equals(hoy)) {
            return "Hoy, " + largo;
        }
        if (diaSeleccionado.equals(hoy.plusDays(1))) {
            return "Mañana, " + largo;
        }
        return capitalizar(largo);
    }

    // ---------- historial ----------

    public List<ReservaCalendario> getProximas() {
        return proximas;
    }

    public List<ReservaCalendario> getAnteriores() {
        return anteriores;
    }

    public int getTotalHistorial() {
        return proximas.size() + anteriores.size();
    }

    // ---------- contadores del mes ----------

    public int getConfirmadasMes() {
        return confirmadasMes;
    }

    public int getCompletadasMes() {
        return completadasMes;
    }

    public int getPendientesMes() {
        return pendientesMes;
    }

    public int getCanceladasMes() {
        return canceladasMes;
    }

    private static String capitalizar(String texto) {
        return texto.substring(0, 1).toUpperCase(ES) + texto.substring(1);
    }
}
