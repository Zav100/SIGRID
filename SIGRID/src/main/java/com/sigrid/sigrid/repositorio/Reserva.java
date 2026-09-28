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
 * Reserva de un turno en un día concreto (fechaTurno). PENDIENTE_PAGO y CONFIRMADA ocupan el turno; CANCELADA y RECHAZADA lo liberan.
 * La columna generada turno_activo_key (UNIQUE) impide la doble reserva; no se mapea porque la escribe MySQL.
 * El monto sale de idTarifa (tarifa_alquiler.precio); idTarifa/idPago son NULL para instalaciones LIBRES sin pago.
 */
@Entity
@Table(name = "reserva")
@NamedQueries({
    @NamedQuery(name = "Reserva.findAll", query = "SELECT x FROM Reserva x"),
    @NamedQuery(name = "Reserva.findByIdReserva", query = "SELECT x FROM Reserva x WHERE x.idReserva = :idReserva")
})
public class Reserva implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Estado {
        PENDIENTE_PAGO, CONFIRMADA, CANCELADA, RECHAZADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_reserva")
    private Integer idReserva;
    @JoinColumn(name = "id_socio", referencedColumnName = "id_socio")
    @ManyToOne(optional = false)
    private Socio idSocio;
    @JoinColumn(name = "id_turno", referencedColumnName = "id_turno")
    @ManyToOne(optional = false)
    private Turno idTurno;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_turno")
    private LocalDate fechaTurno;
    @JoinColumn(name = "id_tarifa", referencedColumnName = "id_tarifa")
    @ManyToOne
    private TarifaAlquiler idTarifa;
    @JoinColumn(name = "id_pago", referencedColumnName = "id_pago", unique = true)
    @OneToOne
    private Pago idPago;
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado = Estado.PENDIENTE_PAGO;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_reserva")
    private LocalDateTime fechaReserva = LocalDateTime.now();
    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;
    @Column(name = "fecha_limite_reprogramacion")
    private LocalDate fechaLimiteReprogramacion;
    @JoinColumn(name = "id_admin_confirmador", referencedColumnName = "id_usuario")
    @ManyToOne
    private Usuario idAdminConfirmador;

    public Reserva() {
    }

    public Reserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public Socio getIdSocio() {
        return idSocio;
    }

    public void setIdSocio(Socio idSocio) {
        this.idSocio = idSocio;
    }

    public Turno getIdTurno() {
        return idTurno;
    }

    public void setIdTurno(Turno idTurno) {
        this.idTurno = idTurno;
    }

    public LocalDate getFechaTurno() {
        return fechaTurno;
    }

    public void setFechaTurno(LocalDate fechaTurno) {
        this.fechaTurno = fechaTurno;
    }

    public TarifaAlquiler getIdTarifa() {
        return idTarifa;
    }

    public void setIdTarifa(TarifaAlquiler idTarifa) {
        this.idTarifa = idTarifa;
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

    public LocalDateTime getFechaReserva() {
        return fechaReserva;
    }

    public void setFechaReserva(LocalDateTime fechaReserva) {
        this.fechaReserva = fechaReserva;
    }

    public LocalDateTime getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) {
        this.fechaConfirmacion = fechaConfirmacion;
    }

    public LocalDate getFechaLimiteReprogramacion() {
        return fechaLimiteReprogramacion;
    }

    public void setFechaLimiteReprogramacion(LocalDate fechaLimiteReprogramacion) {
        this.fechaLimiteReprogramacion = fechaLimiteReprogramacion;
    }

    public Usuario getIdAdminConfirmador() {
        return idAdminConfirmador;
    }

    public void setIdAdminConfirmador(Usuario idAdminConfirmador) {
        this.idAdminConfirmador = idAdminConfirmador;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idReserva != null ? idReserva.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Reserva)) {
            return false;
        }
        Reserva other = (Reserva) object;
        return !((this.idReserva == null && other.idReserva != null) || (this.idReserva != null && !this.idReserva.equals(other.idReserva)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.Reserva[ idReserva=" + idReserva + " ]";
    }

}
