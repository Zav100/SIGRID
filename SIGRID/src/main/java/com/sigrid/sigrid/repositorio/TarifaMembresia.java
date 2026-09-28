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
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Historial de precios de la cuota mensual por categoría. Nunca se edita un precio: se agrega uno nuevo con vigencia.
 * vigenteHasta NULL = vigente actualmente.
 */
@Entity
@Table(name = "tarifa_membresia")
@NamedQueries({
    @NamedQuery(name = "TarifaMembresia.findAll", query = "SELECT x FROM TarifaMembresia x"),
    @NamedQuery(name = "TarifaMembresia.findByIdTarifaMembresia", query = "SELECT x FROM TarifaMembresia x WHERE x.idTarifaMembresia = :idTarifaMembresia")
})
public class TarifaMembresia implements Serializable, Tarifa {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_tarifa_membresia")
    private Integer idTarifaMembresia;
    @JoinColumn(name = "id_categoria_socio", referencedColumnName = "id_categoria_socio")
    @ManyToOne(optional = false)
    private CategoriaSocio idCategoriaSocio;
    @Basic(optional = false)
    @NotNull
    @Column(name = "precio")
    private BigDecimal precio;
    @Basic(optional = false)
    @NotNull
    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;
    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    public TarifaMembresia() {
    }

    public TarifaMembresia(Integer idTarifaMembresia) {
        this.idTarifaMembresia = idTarifaMembresia;
    }

    public Integer getIdTarifaMembresia() {
        return idTarifaMembresia;
    }

    public void setIdTarifaMembresia(Integer idTarifaMembresia) {
        this.idTarifaMembresia = idTarifaMembresia;
    }

    public CategoriaSocio getIdCategoriaSocio() {
        return idCategoriaSocio;
    }

    public void setIdCategoriaSocio(CategoriaSocio idCategoriaSocio) {
        this.idCategoriaSocio = idCategoriaSocio;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public void setVigenteDesde(LocalDate vigenteDesde) {
        this.vigenteDesde = vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public void setVigenteHasta(LocalDate vigenteHasta) {
        this.vigenteHasta = vigenteHasta;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idTarifaMembresia != null ? idTarifaMembresia.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TarifaMembresia)) {
            return false;
        }
        TarifaMembresia other = (TarifaMembresia) object;
        return !((this.idTarifaMembresia == null && other.idTarifaMembresia != null) || (this.idTarifaMembresia != null && !this.idTarifaMembresia.equals(other.idTarifaMembresia)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.TarifaMembresia[ idTarifaMembresia=" + idTarifaMembresia + " ]";
    }

}
