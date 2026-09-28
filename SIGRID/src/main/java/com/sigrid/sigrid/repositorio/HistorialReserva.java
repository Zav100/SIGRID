package com.sigrid.sigrid.repositorio;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Rastro de cada reprogramación: turno y día anteriores de la reserva.
 */
@Entity
@Table(name = "historial_reserva")
@NamedQueries({
    @NamedQuery(name = "HistorialReserva.findAll", query = "SELECT x FROM HistorialReserva x"),
    @NamedQuery(name = "HistorialReserva.findByIdHistorial", query = "SELECT x FROM HistorialReserva x WHERE x.idHistorial = :idHistorial")
})
public class HistorialReserva implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_historial")
    private Integer idHistorial;
    @JoinColumn(name = "id_reserva", referencedColumnName = "id_reserva")
    @ManyToOne(optional = false)
    private Reserva idReserva;
    @JoinColumn(name = "id_turno_anterior", referencedColumnName = "id_turno")
    @ManyToOne(optional = false)
    private Turno idTurnoAnterior;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_anterior")
    private LocalDate fechaAnterior;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "motivo")
    private String motivo;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_cambio")
    private LocalDateTime fechaCambio = LocalDateTime.now();

    public HistorialReserva() {
    }

    public HistorialReserva(Integer idHistorial) {
        this.idHistorial = idHistorial;
    }

    public Integer getIdHistorial() {
        return idHistorial;
    }

    public void setIdHistorial(Integer idHistorial) {
        this.idHistorial = idHistorial;
    }

    public Reserva getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Reserva idReserva) {
        this.idReserva = idReserva;
    }

    public Turno getIdTurnoAnterior() {
        return idTurnoAnterior;
    }

    public void setIdTurnoAnterior(Turno idTurnoAnterior) {
        this.idTurnoAnterior = idTurnoAnterior;
    }

    public LocalDate getFechaAnterior() {
        return fechaAnterior;
    }

    public void setFechaAnterior(LocalDate fechaAnterior) {
        this.fechaAnterior = fechaAnterior;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idHistorial != null ? idHistorial.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof HistorialReserva)) {
            return false;
        }
        HistorialReserva other = (HistorialReserva) object;
        return !((this.idHistorial == null && other.idHistorial != null) || (this.idHistorial != null && !this.idHistorial.equals(other.idHistorial)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.HistorialReserva[ idHistorial=" + idHistorial + " ]";
    }

}
