package com.sigrid.sigrid.repositorio;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pedido del socio: CANCELACION de una reserva confirmada o REPROGRAMACION (usar el crédito de una reserva ya cancelada
 * en otro turno); una reserva confirmada no se reprograma directamente. El administrador solo aprueba o rechaza.
 * La anticipación de 48 h se mide contra fechaSolicitud (no contra el momento de la aprobación).
 * En una REPROGRAMACION, idTurnoNuevo + fechaNueva son el turno pedido (siempre de la misma instalación).
 */
@Entity
@Table(name = "solicitud_cambio_reserva")
@NamedQueries({
    @NamedQuery(name = "SolicitudCambioReserva.findAll", query = "SELECT x FROM SolicitudCambioReserva x")
})
public class SolicitudCambioReserva implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Tipo {
        CANCELACION, REPROGRAMACION
    }

    public enum Estado {
        PENDIENTE, APROBADA, RECHAZADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;
    @JoinColumn(name = "id_reserva", referencedColumnName = "id_reserva")
    @ManyToOne(optional = false)
    private Reserva idReserva;
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private Tipo tipo;
    @JoinColumn(name = "id_turno_nuevo", referencedColumnName = "id_turno")
    @ManyToOne
    private Turno idTurnoNuevo;
    @Column(name = "fecha_nueva")
    private LocalDate fechaNueva;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "motivo")
    private String motivo;
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado = Estado.PENDIENTE;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_solicitud")
    private LocalDateTime fechaSolicitud = LocalDateTime.now();
    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
    @JoinColumn(name = "id_admin_resolutor", referencedColumnName = "id_usuario")
    @ManyToOne
    private Usuario idAdminResolutor;
    @Size(max = 255)
    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    public SolicitudCambioReserva() {
    }

    public Integer getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(Integer idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public Reserva getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Reserva idReserva) {
        this.idReserva = idReserva;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public Turno getIdTurnoNuevo() {
        return idTurnoNuevo;
    }

    public void setIdTurnoNuevo(Turno idTurnoNuevo) {
        this.idTurnoNuevo = idTurnoNuevo;
    }

    public LocalDate getFechaNueva() {
        return fechaNueva;
    }

    public void setFechaNueva(LocalDate fechaNueva) {
        this.fechaNueva = fechaNueva;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(LocalDateTime fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }

    public Usuario getIdAdminResolutor() {
        return idAdminResolutor;
    }

    public void setIdAdminResolutor(Usuario idAdminResolutor) {
        this.idAdminResolutor = idAdminResolutor;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    @Override
    public int hashCode() {
        return idSolicitud != null ? idSolicitud.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SolicitudCambioReserva)) {
            return false;
        }
        SolicitudCambioReserva other = (SolicitudCambioReserva) object;
        return idSolicitud != null && idSolicitud.equals(other.idSolicitud);
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.SolicitudCambioReserva[ idSolicitud=" + idSolicitud + " ]";
    }
}
