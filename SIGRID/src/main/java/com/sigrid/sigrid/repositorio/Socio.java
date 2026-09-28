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
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Datos del socio (1 a 1 con Usuario). estado es la membresía (ACTIVO/NO_ACTIVO) y lo escribe la capa de servicio;
 * bajaLogica es la baja del perfil (a pedido del socio o del administrador).
 */
@Entity
@Table(name = "socio")
@NamedQueries({
    @NamedQuery(name = "Socio.findAll", query = "SELECT x FROM Socio x"),
    @NamedQuery(name = "Socio.findByIdSocio", query = "SELECT x FROM Socio x WHERE x.idSocio = :idSocio"),
    @NamedQuery(name = "Socio.findByDni", query = "SELECT x FROM Socio x WHERE x.dni = :dni"),
    @NamedQuery(name = "Socio.findByEstado", query = "SELECT x FROM Socio x WHERE x.estado = :estado")
})
public class Socio implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Estado {
        ACTIVO, NO_ACTIVO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_socio")
    private Integer idSocio;
    @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario", unique = true)
    @OneToOne(optional = false)
    private Usuario idUsuario;
    @JoinColumn(name = "id_categoria_socio", referencedColumnName = "id_categoria_socio")
    @ManyToOne(optional = false)
    private CategoriaSocio idCategoriaSocio;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 15)
    @Column(name = "dni")
    private String dni;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "telefono")
    private String telefono;
    @Size(max = 255)
    @Column(name = "foto_perfil_url")
    private String fotoPerfilUrl;
    @Size(max = 20)
    @Column(name = "legajo")
    private String legajo;
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado = Estado.NO_ACTIVO;
    @Basic(optional = false)
    @NotNull
    @Column(name = "baja_logica")
    private boolean bajaLogica = false;

    public Socio() {
    }

    public Socio(Integer idSocio) {
        this.idSocio = idSocio;
    }

    public Integer getIdSocio() {
        return idSocio;
    }

    public void setIdSocio(Integer idSocio) {
        this.idSocio = idSocio;
    }

    public Usuario getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Usuario idUsuario) {
        this.idUsuario = idUsuario;
    }

    public CategoriaSocio getIdCategoriaSocio() {
        return idCategoriaSocio;
    }

    public void setIdCategoriaSocio(CategoriaSocio idCategoriaSocio) {
        this.idCategoriaSocio = idCategoriaSocio;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFotoPerfilUrl() {
        return fotoPerfilUrl;
    }

    public void setFotoPerfilUrl(String fotoPerfilUrl) {
        this.fotoPerfilUrl = fotoPerfilUrl;
    }

    public String getLegajo() {
        return legajo;
    }

    public void setLegajo(String legajo) {
        this.legajo = legajo;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public boolean isBajaLogica() {
        return bajaLogica;
    }

    public void setBajaLogica(boolean bajaLogica) {
        this.bajaLogica = bajaLogica;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idSocio != null ? idSocio.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Socio)) {
            return false;
        }
        Socio other = (Socio) object;
        return !((this.idSocio == null && other.idSocio != null) || (this.idSocio != null && !this.idSocio.equals(other.idSocio)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.Socio[ idSocio=" + idSocio + " ]";
    }

}
