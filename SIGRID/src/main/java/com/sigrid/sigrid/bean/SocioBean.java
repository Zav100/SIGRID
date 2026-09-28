package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.Aviso;
import com.sigrid.sigrid.dto.DiaSocio;
import com.sigrid.sigrid.dto.InstalacionReservable;
import com.sigrid.sigrid.dto.MiReserva;
import com.sigrid.sigrid.dto.PerfilSocio;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.servicio.PanelSocioServicio;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Datos del socio logueado que usan todas las páginas de /socio/* (cabecera, menú, inicio, avisos). Se lee cada cosa
 * una sola vez por petición y solo si la página la pide. /socio/* solo lo abre un usuario con rol SOCIO (SocioFilter).
 */
@Named("socioBean")
@RequestScoped
public class SocioBean {

    @Inject
    private LoginBean loginBean;
    @Inject
    private PanelSocioServicio servicio;

    private final LocalDateTime ahora = LocalDateTime.now();
    private Socio socio;
    private PerfilSocio perfil;
    private List<MiReserva> reservas;
    private List<Aviso> avisos;

    public Socio getSocio() {
        if (socio == null) {
            socio = servicio.buscarSocio(loginBean.getIdUsuarioLogueado());
        }
        return socio;
    }

    public PerfilSocio getPerfil() {
        if (perfil == null) {
            perfil = servicio.perfil(getSocio(), ahora.toLocalDate());
        }
        return perfil;
    }

    /** Sus reservas, las de turno más lejano primero. */
    public List<MiReserva> getReservas() {
        if (reservas == null) {
            reservas = servicio.misReservas(getSocio().getIdSocio(), ahora);
        }
        return reservas;
    }

    public List<Aviso> getAvisos() {
        if (avisos == null) {
            avisos = servicio.avisos(getSocio(), getPerfil(), ahora);
        }
        return avisos;
    }

    public String getSaludo() {
        int hora = ahora.getHour();
        return hora < 6 ? "Buenas noches" : hora < 13 ? "Buen día" : hora < 20 ? "Buenas tardes" : "Buenas noches";
    }

    /** Lo que necesita su atención (vencida, por vencer, comprobante por subir, rechazos): globito del menú. */
    public long getAvisosUrgentes() {
        return getAvisos().stream().filter(a -> "warn".equals(a.getClase()) || "off".equals(a.getClase())).count();
    }

    public List<Aviso> getAvisosRecientes() {
        return getAvisos().stream().limit(3).collect(Collectors.toList());
    }

    /** La reserva vigente (confirmada o esperando pago) de turno más cercano. */
    public MiReserva getProxima() {
        return getReservas().stream().filter(r -> "PROXIMAS".equals(r.getGrupo()))
                .min(Comparator.comparing(MiReserva::getInicio)).orElse(null);
    }

    /** Cuota mensual de su tipo de socio (solo la suya: no ve las tarifas de otros tipos). */
    public String getCuota() {
        return servicio.cuota(getSocio(), ahora.toLocalDate());
    }

    /** Precio de alquiler de cada instalación para su tipo de socio. */
    public List<InstalacionReservable> getTarifas() {
        return servicio.instalaciones(getSocio(), ahora.toLocalDate());
    }

    /** Las 5 reservas de turno más lejano (ya vienen en ese orden): la card "Historial de reservas". */
    public List<MiReserva> getHistorial() {
        return getReservas().stream().limit(5).collect(Collectors.toList());
    }

    public List<DiaSocio> getProximosDias() {
        return servicio.dias(getReservas(), LocalDate.now(), 7);
    }

    public long getCantidadProximas() {
        return getReservas().stream().filter(r -> "PROXIMAS".equals(r.getGrupo())).count();
    }

    public long getCantidadRealizadas() {
        return getReservas().stream().filter(r -> "Realizada".equals(r.getEstadoTexto())).count();
    }

    /** Reservas terminadas que espera su valoración (mientras haya, no puede reservar). */
    public List<MiReserva> getPorValorar() {
        return getReservas().stream().filter(MiReserva::isValorable).collect(Collectors.toList());
    }

    public long getCantidadPorValorar() {
        return getPorValorar().size();
    }

    public long getCantidadPorPagar() {
        return getReservas().stream().filter(MiReserva::isPuedeSubirComprobante).count();
    }
}
