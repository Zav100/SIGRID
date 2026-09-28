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
import java.io.Serializable;
import java.time.LocalTime;

/**
 * Franja horaria FIJA de una instalación (plantilla semanal, sin fecha). El día concreto vive en Reserva.fechaTurno.
 */
@Entity
@Table(name = "turno")
@NamedQueries({
    @NamedQuery(name = "Turno.findAll", query = "SELECT x FROM Turno x"),
    @NamedQuery(name = "Turno.findByIdTurno", query = "SELECT x FROM Turno x WHERE x.idTurno = :idTurno")
})
public class Turno implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_turno")
    private Integer idTurno;
    @JoinColumn(name = "id_instalacion", referencedColumnName = "id_instalacion")
    @ManyToOne(optional = false)
    private Instalacion idInstalacion;
    @Basic(optional = false)
    @NotNull
    @Column(name = "hora_inicio")
    private LocalTime horaInicio;
    @Basic(optional = false)
    @NotNull
    @Column(name = "hora_fin")
    private LocalTime horaFin;

    public Turno() {
    }

    public Turno(Integer idTurno) {
        this.idTurno = idTurno;
    }

    public Integer getIdTurno() {
        return idTurno;
    }

    public void setIdTurno(Integer idTurno) {
        this.idTurno = idTurno;
    }

    public Instalacion getIdInstalacion() {
        return idInstalacion;
    }

    public void setIdInstalacion(Instalacion idInstalacion) {
        this.idInstalacion = idInstalacion;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idTurno != null ? idTurno.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Turno)) {
            return false;
        }
        Turno other = (Turno) object;
        return !((this.idTurno == null && other.idTurno != null) || (this.idTurno != null && !this.idTurno.equals(other.idTurno)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.Turno[ idTurno=" + idTurno + " ]";
    }

}
