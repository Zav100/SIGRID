package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.DiaSocio;
import com.sigrid.sigrid.dto.InstalacionReservable;
import com.sigrid.sigrid.dto.TurnoDisponible;
import com.sigrid.sigrid.servicio.PanelSocioServicio;
import com.sigrid.sigrid.servicio.ReglaReservaException;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Bean de socio/nueva-reserva.xhtml: el socio elige instalación, día y turno (AJAX) y confirma. Cada elección vuelve a
 * leer los turnos de la base, así ve siempre lo que sigue libre. Al confirmar, la reserva nace pendiente de pago y el
 * socio pasa a "Mis reservas" a subir el comprobante.
 */
@Named("socioNuevaReservaBean")
@ViewScoped
public class SocioNuevaReservaBean implements Serializable {

    private static final DateTimeFormatter FECHA_LARGA
            = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-AR"));

    @Inject
    private SocioBean socioBean;
    @Inject
    private PanelSocioServicio servicio;

    private List<InstalacionReservable> instalaciones;
    private List<DiaSocio> dias;
    private List<TurnoDisponible> turnos = Collections.emptyList();
    private Integer idInstalacion;
    private LocalDate fecha;
    private Integer idTurno;

    @PostConstruct
    public void init() {
        LocalDate hoy = LocalDate.now();
        instalaciones = servicio.instalaciones(socioBean.getSocio(), hoy);
        dias = servicio.dias(Collections.emptyList(), hoy, PanelSocioServicio.DIAS_ADELANTE + 1);
    }

    // ---------- elecciones ----------

    public void seleccionarInstalacion(Integer id) {
        if (instalaciones.stream().anyMatch(i -> i.getIdInstalacion().equals(id) && i.isHabilitada())) {
            idInstalacion = id;
            fecha = LocalDate.now(); // ya muestra los turnos de hoy; el socio puede cambiar el día
            idTurno = null;
            cargarTurnos();
        }
    }

    public void seleccionarDia(String iso) {
        try {
            LocalDate elegido = LocalDate.parse(iso);
            if (idInstalacion != null && dias.stream().anyMatch(d -> d.getFecha().equals(elegido))) {
                fecha = elegido;
                idTurno = null;
                cargarTurnos();
            }
        } catch (DateTimeParseException e) {
            // un enlace armado a mano: se ignora
        }
    }

    public void seleccionarTurno(Integer id) {
        cargarTurnos();
        idTurno = turnos.stream().anyMatch(t -> t.getIdTurno().equals(id) && t.isLibre()) ? id : null;
    }

    private void cargarTurnos() {
        turnos = idInstalacion == null || fecha == null ? Collections.<TurnoDisponible>emptyList()
                : servicio.turnos(socioBean.getSocio().getIdSocio(), idInstalacion, fecha, LocalDateTime.now());
    }

    /** Confirma la reserva y lleva a "Mis reservas" para subir el comprobante. */
    public String confirmar() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            Integer id = servicio.reservar(socioBean.getSocio().getIdSocio(), idTurno, fecha, LocalDateTime.now());
            return "/socio/mis-reservas?faces-redirect=true&nueva=" + id;
        } catch (ReglaReservaException e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            idTurno = null;
            cargarTurnos(); // por si el turno se lo llevó otro
            return null;
        }
    }

    // ---------- lectura ----------

    public List<InstalacionReservable> getInstalaciones() {
        return instalaciones;
    }

    public List<DiaSocio> getDias() {
        return dias;
    }

    public List<TurnoDisponible> getTurnos() {
        return turnos;
    }

    public Integer getIdInstalacion() {
        return idInstalacion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Integer getIdTurno() {
        return idTurno;
    }

    public InstalacionReservable getInstalacion() {
        return instalaciones.stream().filter(i -> i.getIdInstalacion().equals(idInstalacion)).findFirst().orElse(null);
    }

    public TurnoDisponible getTurno() {
        return turnos.stream().filter(t -> t.getIdTurno().equals(idTurno)).findFirst().orElse(null);
    }

    /** El día elegido en texto largo: "sábado 26 de septiembre". */
    public String getFechaTexto() {
        return fecha == null ? null : fecha.format(FECHA_LARGA);
    }

    public String getPrecio() {
        return idInstalacion == null || fecha == null ? null
                : servicio.precio(socioBean.getSocio(), idInstalacion, fecha);
    }
}
