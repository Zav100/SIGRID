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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Historial de membresías del socio: cada renovación es una fila nueva. idPago es NULL mientras está PENDIENTE_PAGO.
 */
@Entity
@Table(name = "suscripcion_socio")
@NamedQueries({
    @NamedQuery(name = "SuscripcionSocio.findAll", query = "SELECT x FROM SuscripcionSocio x"),
    @NamedQuery(name = "SuscripcionSocio.findByIdSuscripcion", query = "SELECT x FROM SuscripcionSocio x WHERE x.idSuscripcion = :idSuscripcion")
})
public class SuscripcionSocio implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Estado {
        PENDIENTE_PAGO, VIGENTE, VENCIDA, CANCELADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_suscripcion")
    private Integer idSuscripcion;
    @JoinColumn(name = "id_socio", referencedColumnName = "id_socio")
    @ManyToOne(optional = false)
    private Socio idSocio;
    @JoinColumn(name = "id_pago", referencedColumnName = "id_pago", unique = true)
    @OneToOne
    private Pago idPago;
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado = Estado.PENDIENTE_PAGO;
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_solicitud")
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    public SuscripcionSocio() {
    }

    public SuscripcionSocio(Integer idSuscripcion) {
        this.idSuscripcion = idSuscripcion;
    }

    public Integer getIdSuscripcion() {
        return idSuscripcion;
    }

    public void setIdSuscripcion(Integer idSuscripcion) {
        this.idSuscripcion = idSuscripcion;
    }

    public Socio getIdSocio() {
        return idSocio;
    }

    public void setIdSocio(Socio idSocio) {
        this.idSocio = idSocio;
    }

    public Pago getIdPago() {
        return idPago;
    }

    public void setIdPago(Pago idPago) {
        this.idPago = idPago;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idSuscripcion != null ? idSuscripcion.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SuscripcionSocio)) {
            return false;
        }
        SuscripcionSocio other = (SuscripcionSocio) object;
        return !((this.idSuscripcion == null && other.idSuscripcion != null) || (this.idSuscripcion != null && !this.idSuscripcion.equals(other.idSuscripcion)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.SuscripcionSocio[ idSuscripcion=" + idSuscripcion + " ]";
    }

}
