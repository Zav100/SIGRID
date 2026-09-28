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
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Franja en que una instalación de acceso LIBRE (la pileta) está abierta. O se repite cada semana (diaSemana, 1 = lunes
 * a 7 = domingo) o vale para un día específico (fecha); una de las dos es NULL. Las de fecha se suman a las semanales.
 */
@Entity
@Table(name = "horario_apertura")
@NamedQueries({
    @NamedQuery(name = "HorarioApertura.findAll", query = "SELECT x FROM HorarioApertura x")
})
public class HorarioApertura implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_horario")
    private Integer idHorario;
    @JoinColumn(name = "id_instalacion", referencedColumnName = "id_instalacion")
    @ManyToOne(optional = false)
    private Instalacion idInstalacion;
    @Column(name = "dia_semana")
    private Integer diaSemana;
    @Column(name = "fecha")
    private LocalDate fecha;
    @Basic(optional = false)
    @NotNull
    @Column(name = "hora_apertura")
    private LocalTime horaApertura;
    @Basic(optional = false)
    @NotNull
    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    public HorarioApertura() {
    }

    public Integer getIdHorario() {
        return idHorario;
    }

    public void setIdHorario(Integer idHorario) {
        this.idHorario = idHorario;
    }

    public Instalacion getIdInstalacion() {
        return idInstalacion;
    }

    public void setIdInstalacion(Instalacion idInstalacion) {
        this.idInstalacion = idInstalacion;
    }

    public Integer getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(Integer diaSemana) {
        this.diaSemana = diaSemana;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public void setHoraApertura(LocalTime horaApertura) {
        this.horaApertura = horaApertura;
    }

    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    public void setHoraCierre(LocalTime horaCierre) {
        this.horaCierre = horaCierre;
    }

    /** ¿Esta franja rige ese día? (la semanal por su día de la semana, la específica por su fecha) */
    public boolean rigeEl(LocalDate dia) {
        return fecha != null ? fecha.equals(dia) : diaSemana != null && diaSemana == dia.getDayOfWeek().getValue();
    }

    @Override
    public int hashCode() {
        return idHorario != null ? idHorario.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof HorarioApertura)) {
            return false;
        }
        HorarioApertura other = (HorarioApertura) object;
        return idHorario != null && idHorario.equals(other.idHorario);
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.HorarioApertura[ idHorario=" + idHorario + " ]";
    }

}
