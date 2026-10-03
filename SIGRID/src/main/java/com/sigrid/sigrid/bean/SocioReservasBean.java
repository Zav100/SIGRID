package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.DiaSocio;
import com.sigrid.sigrid.dto.MiReserva;
import com.sigrid.sigrid.dto.TurnoDisponible;
import com.sigrid.sigrid.repositorio.SolicitudCambioReserva;
import com.sigrid.sigrid.servicio.CambiosReservaServicio;
import com.sigrid.sigrid.servicio.PanelSocioServicio;
import com.sigrid.sigrid.servicio.ReglaReservaException;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean de socio/mis-reservas.xhtml: la lista de reservas del socio con su filtro, y las tres cosas que puede hacer
 * con ellas: subir el comprobante de pago, liberar un turno que todavía no pagó, pedir la cancelación de uno confirmado y
 * reprogramar con el crédito una cancelada (los pedidos le llegan al administrador en "Cancelaciones y reprogramaciones").
 * El formulario del comprobante es uno solo, de la reserva seleccionada.
 */
@Named("socioReservasBean")
@ViewScoped
public class SocioReservasBean implements Serializable {

    private static final DateTimeFormatter FORMATO_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Inject
    private SocioBean socioBean;
    @Inject
    private PanelSocioServicio servicio;
    @Inject
    private CambiosReservaServicio cambiosServicio;

    private List<MiReserva> reservas;
    private String filtro = "PROXIMAS";  // PROXIMAS, PASADAS o TODAS
    private Integer idSeleccionada;      // la reserva cuyo comprobante se está cargando
    private Part archivo;
    private String fechaOperacion;       // "2026-09-26T14:30" (input datetime-local)
    private Integer idValorando;      // la reserva que se está valorando
    private Integer puntaje;             // 1 a 5 (lo carga el widget de estrellas)
    private String comentario;
    private String motivo = "";          // motivo del pedido de cancelación (lo carga un prompt del navegador)
    private Integer idReprogramando;     // la reserva con crédito que se está reprogramando
    private LocalDate fechaNueva;
    private Integer idTurnoNuevo;
    private String motivoReprogramacion;
    private List<DiaSocio> diasReprogramar = Collections.emptyList();
    private List<TurnoDisponible> turnosReprogramar = Collections.emptyList();

    @PostConstruct
    public void init() {
        recargar();
        // viene de "Nueva reserva": deja lista la carga del comprobante de la que acaba de crear
        String nueva = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("nueva");
        if (nueva != null && nueva.matches("\\d{1,9}")) {
            idSeleccionada = Integer.valueOf(nueva);
        }
        idValorando = reservas.stream().filter(MiReserva::isValorable).map(MiReserva::getIdReserva).findFirst().orElse(null);
        if (getSeleccionada() == null) {
            idSeleccionada = reservas.stream().filter(MiReserva::isPuedeSubirComprobante)
                    .map(MiReserva::getIdReserva).findFirst().orElse(null);
        }
    }

    // ---------- reprogramar con el crédito ----------

    public List<MiReserva> getConCredito() {
        return reservas.stream().filter(MiReserva::isPuedeReprogramar).collect(Collectors.toList());
    }

    /** La reserva con crédito que se está reprogramando; null si no hay o ya no admite el pedido. */
    public MiReserva getReprogramando() {
        return reservas.stream().filter(r -> r.getIdReserva().equals(idReprogramando) && r.isPuedeReprogramar())
                .findFirst().orElse(null);
    }

    public void seleccionarReprogramacion(Integer idReserva) {
        idReprogramando = idReserva;
        idTurnoNuevo = null;
        motivoReprogramacion = null;
        fechaNueva = PanelSocioServicio.primerDiaReservable(LocalDate.now());
        diasReprogramar = servicio.diasReservables(LocalDate.now());
        cargarTurnosReprogramar();
    }

    public void seleccionarDiaReprogramar(String iso) {
        try {
            LocalDate elegido = LocalDate.parse(iso);
            if (diasReprogramar.stream().anyMatch(d -> d.getFecha().equals(elegido))) {
                fechaNueva = elegido;
                idTurnoNuevo = null;
                cargarTurnosReprogramar();
            }
        } catch (DateTimeParseException e) {
            // un enlace armado a mano: se ignora
        }
    }

    public void seleccionarTurnoReprogramar(Integer id) {
        cargarTurnosReprogramar();
        idTurnoNuevo = turnosReprogramar.stream().anyMatch(t -> t.getIdTurno().equals(id) && t.isLibre()) ? id : null;
    }

    private void cargarTurnosReprogramar() {
        turnosReprogramar = servicio.turnosParaReprogramar(socioBean.getSocio().getIdSocio(), idReprogramando, fechaNueva,
                LocalDateTime.now());
    }

    /** El turno elegido para reprogramar; null si todavía no eligió. */
    public TurnoDisponible getTurnoNuevo() {
        return turnosReprogramar.stream().filter(t -> t.getIdTurno().equals(idTurnoNuevo)).findFirst().orElse(null);
    }

    /** Manda el pedido al administrador; la reserva sigue cancelada hasta que lo apruebe. */
    public void pedirReprogramacion() {
        if (idTurnoNuevo == null) {
            aviso(FacesMessage.SEVERITY_ERROR, "Elegí el turno al que querés pasar tu reserva.");
            return;
        }
        String texto = motivoReprogramacion == null || motivoReprogramacion.isBlank() ? "Reprogramación con el crédito" : motivoReprogramacion;
        String error = cambiosServicio.solicitar(socioBean.getSocio().getIdSocio(), idReprogramando,
                SolicitudCambioReserva.Tipo.REPROGRAMACION, idTurnoNuevo, fechaNueva, texto, LocalDateTime.now());
        if (error == null) {
            idReprogramando = null;
            aviso(FacesMessage.SEVERITY_INFO, "Enviamos tu pedido de reprogramación. Cuando el administrador lo apruebe, tu reserva queda confirmada en el turno nuevo.");
        } else {
            aviso(FacesMessage.SEVERITY_ERROR, error);
            cargarTurnosReprogramar(); // por si el turno se lo llevó otro
            idTurnoNuevo = null;
        }
        recargar();
    }

    public void cancelarReprogramacion() {
        idReprogramando = null;
    }

    private void recargar() {
        reservas = servicio.misReservas(socioBean.getSocio().getIdSocio(), LocalDateTime.now());
    }

    // ---------- lectura ----------

    public List<MiReserva> getVisibles() {
        return reservas.stream().filter(r -> "TODAS".equals(filtro) || filtro.equals(r.getGrupo()))
                .collect(Collectors.toList());
    }

    public long getTotalProximas() {
        return reservas.stream().filter(r -> "PROXIMAS".equals(r.getGrupo())).count();
    }

    public long getTotalPasadas() {
        return reservas.size() - getTotalProximas();
    }

    public int getTotal() {
        return reservas.size();
    }

    /** La reserva del formulario del comprobante; null si no hay o ya no admite comprobante. */
    public MiReserva getSeleccionada() {
        return reservas.stream().filter(r -> r.getIdReserva().equals(idSeleccionada) && r.isPuedeSubirComprobante())
                .findFirst().orElse(null);
    }

    /** La reserva que se está valorando; null si no hay o ya no admite valoración. */
    public MiReserva getValorando() {
        return reservas.stream().filter(r -> r.getIdReserva().equals(idValorando) && r.isValorable()).findFirst().orElse(null);
    }

    public long getTotalPorValorar() {
        return reservas.stream().filter(MiReserva::isValorable).count();
    }

    /** Tope para el campo de la transferencia: no puede ser futura. */
    public String getAhoraInput() {
        return LocalDateTime.now().format(FORMATO_INPUT);
    }

    // ---------- acciones ----------

    public void filtrar(String nuevo) {
        filtro = nuevo;
    }

    public void seleccionar(Integer idReserva) {
        idSeleccionada = idReserva;
        fechaOperacion = null;
    }

    /** Sube el comprobante. Si sale bien vuelve a cargar la página (para no reenviar el archivo al refrescar). */
    public String subir() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            if (archivo == null || archivo.getSize() == 0) {
                throw new ReglaReservaException("Elegí el archivo del comprobante.");
            }
            if (archivo.getSize() > 5L * 1024 * 1024) {
                throw new ReglaReservaException("El archivo pesa más de 5 MB.");
            }
            LocalDateTime operacion = fechaOperacion == null || fechaOperacion.isBlank() ? null
                    : LocalDateTime.parse(fechaOperacion, FORMATO_INPUT);
            servicio.subirComprobante(socioBean.getSocio().getIdSocio(), idSeleccionada,
                    archivo.getInputStream().readAllBytes(), operacion, LocalDateTime.now());
            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Recibimos tu comprobante. El administrador lo va a revisar y te avisamos cuando confirme la reserva.", null));
            return "/socio/mis-reservas?faces-redirect=true";
        } catch (ReglaReservaException e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (DateTimeParseException e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Indicá cuándo hiciste la transferencia.", null));
        } catch (IOException e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "No pudimos leer el archivo. Probá de nuevo.", null));
        }
        recargar();
        return null;
    }

    public void seleccionarValoracion(Integer idReserva) {
        idValorando = idReserva;
        puntaje = null;
        comentario = null;
    }

    /** Guarda la valoración. Si sale bien vuelve a cargar la página (queda la siguiente pendiente, si hay). */
    public String valorar() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            servicio.valorar(socioBean.getSocio().getIdSocio(), idValorando, puntaje, comentario, LocalDateTime.now());
            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "¡Gracias por tu valoración! Nos ayuda a mejorar el predio.", null));
            return "/socio/mis-reservas?faces-redirect=true";
        } catch (ReglaReservaException e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
            recargar();
            return null;
        }
    }

    /** Suelta una reserva que todavía no pagó. */
    public void liberar(Integer idReserva) {
        try {
            servicio.liberar(socioBean.getSocio().getIdSocio(), idReserva);
            aviso(FacesMessage.SEVERITY_INFO, "Liberaste el turno. Ya lo puede reservar otra persona.");
        } catch (ReglaReservaException e) {
            aviso(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
        recargar();
    }

    /** Pide cancelar una reserva confirmada; el administrador lo aprueba o rechaza. */
    public void pedirCancelacion(Integer idReserva) {
        String error = cambiosServicio.solicitar(socioBean.getSocio().getIdSocio(), idReserva,
                SolicitudCambioReserva.Tipo.CANCELACION, null, null, motivo, LocalDateTime.now());
        motivo = "";
        if (error == null) {
            aviso(FacesMessage.SEVERITY_INFO, "Enviamos tu pedido de cancelación. El administrador lo va a revisar.");
        } else {
            aviso(FacesMessage.SEVERITY_ERROR, error);
        }
        recargar();
    }

    private static void aviso(FacesMessage.Severity gravedad, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(gravedad, texto, null));
    }

    // ---------- estado del formulario ----------

    public String getFiltro() {
        return filtro;
    }

    public Part getArchivo() {
        return archivo;
    }

    public void setArchivo(Part archivo) {
        this.archivo = archivo;
    }

    public String getFechaOperacion() {
        return fechaOperacion;
    }

    public void setFechaOperacion(String fechaOperacion) {
        this.fechaOperacion = fechaOperacion;
    }

    public Integer getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(Integer puntaje) {
        this.puntaje = puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public List<DiaSocio> getDiasReprogramar() {
        return diasReprogramar;
    }

    public List<TurnoDisponible> getTurnosReprogramar() {
        return turnosReprogramar;
    }

    public LocalDate getFechaNueva() {
        return fechaNueva;
    }

    public Integer getIdTurnoNuevo() {
        return idTurnoNuevo;
    }

    public String getMotivoReprogramacion() {
        return motivoReprogramacion;
    }

    public void setMotivoReprogramacion(String motivoReprogramacion) {
        this.motivoReprogramacion = motivoReprogramacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
