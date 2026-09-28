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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pago por transferencia bancaria (membresía o alquiler), validado a mano por un administrador.
 * Es de MEMBRESÍA si lo referencia una SuscripcionSocio y de ALQUILER si lo referencia una Reserva.
 */
@Entity
@Table(name = "pago")
@NamedQueries({
    @NamedQuery(name = "Pago.findAll", query = "SELECT x FROM Pago x"),
    @NamedQuery(name = "Pago.findByIdPago", query = "SELECT x FROM Pago x WHERE x.idPago = :idPago"),
    @NamedQuery(name = "Pago.findByEstado", query = "SELECT x FROM Pago x WHERE x.estado = :estado")
})
public class Pago implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Estado {
        PENDIENTE_VALIDACION, CONFIRMADO, RECHAZADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_pago")
    private Integer idPago;
    @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario")
    @ManyToOne(optional = false)
    private Usuario idUsuario;
    @Basic(optional = false)
    @NotNull
    @Column(name = "monto")
    private BigDecimal monto;
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado = Estado.PENDIENTE_VALIDACION;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago = LocalDateTime.now();
    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;
    @JoinColumn(name = "id_admin_validador", referencedColumnName = "id_usuario")
    @ManyToOne
    private Usuario idAdminValidador;
    @Size(max = 255)
    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    public Pago() {
    }

    public Pago(Integer idPago) {
        this.idPago = idPago;
    }

    public Integer getIdPago() {
        return idPago;
    }

    public void setIdPago(Integer idPago) {
        this.idPago = idPago;
    }

    public Usuario getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Usuario idUsuario) {
        this.idUsuario = idUsuario;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    public void setFechaValidacion(LocalDateTime fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    public Usuario getIdAdminValidador() {
        return idAdminValidador;
    }

    public void setIdAdminValidador(Usuario idAdminValidador) {
        this.idAdminValidador = idAdminValidador;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idPago != null ? idPago.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Pago)) {
            return false;
        }
        Pago other = (Pago) object;
        return !((this.idPago == null && other.idPago != null) || (this.idPago != null && !this.idPago.equals(other.idPago)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.Pago[ idPago=" + idPago + " ]";
    }

}
