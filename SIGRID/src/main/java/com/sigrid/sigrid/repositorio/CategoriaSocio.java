package com.sigrid.sigrid.repositorio;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * Categoría de socio (Alumno UNSE, Docente UNSE, No Docente UNSE, Externo). Define tarifas y si pide legajo.
 */
@Entity
@Table(name = "categoria_socio")
@NamedQueries({
    @NamedQuery(name = "CategoriaSocio.findAll", query = "SELECT x FROM CategoriaSocio x"),
    @NamedQuery(name = "CategoriaSocio.findByIdCategoriaSocio", query = "SELECT x FROM CategoriaSocio x WHERE x.idCategoriaSocio = :idCategoriaSocio"),
    @NamedQuery(name = "CategoriaSocio.findByNombreCategoria", query = "SELECT x FROM CategoriaSocio x WHERE x.nombreCategoria = :nombreCategoria")
})
public class CategoriaSocio implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_categoria_socio")
    private Integer idCategoriaSocio;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "nombre_categoria")
    private String nombreCategoria;
    @Basic(optional = false)
    @NotNull
    @Column(name = "requiere_legajo")
    private boolean requiereLegajo = false;

    public CategoriaSocio() {
    }

    public CategoriaSocio(Integer idCategoriaSocio) {
        this.idCategoriaSocio = idCategoriaSocio;
    }

    public Integer getIdCategoriaSocio() {
        return idCategoriaSocio;
    }

    public void setIdCategoriaSocio(Integer idCategoriaSocio) {
        this.idCategoriaSocio = idCategoriaSocio;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public boolean isRequiereLegajo() {
        return requiereLegajo;
    }

    public void setRequiereLegajo(boolean requiereLegajo) {
        this.requiereLegajo = requiereLegajo;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idCategoriaSocio != null ? idCategoriaSocio.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CategoriaSocio)) {
            return false;
        }
        CategoriaSocio other = (CategoriaSocio) object;
        return !((this.idCategoriaSocio == null && other.idCategoriaSocio != null) || (this.idCategoriaSocio != null && !this.idCategoriaSocio.equals(other.idCategoriaSocio)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.CategoriaSocio[ idCategoriaSocio=" + idCategoriaSocio + " ]";
    }

}
