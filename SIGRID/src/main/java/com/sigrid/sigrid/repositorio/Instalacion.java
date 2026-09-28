package com.sigrid.sigrid.repositorio;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Espacio del polideportivo. DESHABILITADA_MANTENIMIENTO es temporal; DESHABILITADA es la baja definitiva (no hay columna baja_logica).
 */
@Entity
@Table(name = "instalacion")
@NamedQueries({
    @NamedQuery(name = "Instalacion.findAll", query = "SELECT x FROM Instalacion x"),
    @NamedQuery(name = "Instalacion.findByIdInstalacion", query = "SELECT x FROM Instalacion x WHERE x.idInstalacion = :idInstalacion"),
    @NamedQuery(name = "Instalacion.findByNombre", query = "SELECT x FROM Instalacion x WHERE x.nombre = :nombre"),
    @NamedQuery(name = "Instalacion.findByEstado", query = "SELECT x FROM Instalacion x WHERE x.estado = :estado")
})
public class Instalacion implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum TipoAcceso {
        LIBRE, ARANCELADO
    }

    public enum Estado {
        HABILITADA, DESHABILITADA_MANTENIMIENTO, DESHABILITADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_instalacion")
    private Integer idInstalacion;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "nombre")
    private String nombre;
    @Lob
    @Column(name = "descripcion")
    private String descripcion;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "tipo_disciplina")
    private String tipoDisciplina;
    @Column(name = "capacidad")
    private Integer capacidad;
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acceso")
    private TipoAcceso tipoAcceso;
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado = Estado.HABILITADA;
    @Size(max = 255)
    @Column(name = "motivo_baja")
    private String motivoBaja;
    @Column(name = "fecha_inicio_baja")
    private LocalDate fechaInicioBaja;
    @Column(name = "fecha_fin_baja")
    private LocalDate fechaFinBaja;

    public Instalacion() {
    }

    public Instalacion(Integer idInstalacion) {
        this.idInstalacion = idInstalacion;
    }

    public Integer getIdInstalacion() {
        return idInstalacion;
    }

    public void setIdInstalacion(Integer idInstalacion) {
        this.idInstalacion = idInstalacion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipoDisciplina() {
        return tipoDisciplina;
    }

    public void setTipoDisciplina(String tipoDisciplina) {
        this.tipoDisciplina = tipoDisciplina;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public TipoAcceso getTipoAcceso() {
        return tipoAcceso;
    }

    public void setTipoAcceso(TipoAcceso tipoAcceso) {
        this.tipoAcceso = tipoAcceso;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getMotivoBaja() {
        return motivoBaja;
    }

    public void setMotivoBaja(String motivoBaja) {
        this.motivoBaja = motivoBaja;
    }

    public LocalDate getFechaInicioBaja() {
        return fechaInicioBaja;
    }

    public void setFechaInicioBaja(LocalDate fechaInicioBaja) {
        this.fechaInicioBaja = fechaInicioBaja;
    }

    public LocalDate getFechaFinBaja() {
        return fechaFinBaja;
    }

    public void setFechaFinBaja(LocalDate fechaFinBaja) {
        this.fechaFinBaja = fechaFinBaja;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idInstalacion != null ? idInstalacion.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Instalacion)) {
            return false;
        }
        Instalacion other = (Instalacion) object;
        return !((this.idInstalacion == null && other.idInstalacion != null) || (this.idInstalacion != null && !this.idInstalacion.equals(other.idInstalacion)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.Instalacion[ idInstalacion=" + idInstalacion + " ]";
    }

}
