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
 * Historial de precios de alquiler por instalación y categoría. Nunca se edita un precio: se agrega uno nuevo con vigencia.
 * vigenteHasta NULL = vigente actualmente.
 */
@Entity
@Table(name = "tarifa_alquiler")
@NamedQueries({
    @NamedQuery(name = "TarifaAlquiler.findAll", query = "SELECT x FROM TarifaAlquiler x"),
    @NamedQuery(name = "TarifaAlquiler.findByIdTarifa", query = "SELECT x FROM TarifaAlquiler x WHERE x.idTarifa = :idTarifa")
})
public class TarifaAlquiler implements Serializable, Tarifa {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_tarifa")
    private Integer idTarifa;
    @JoinColumn(name = "id_instalacion", referencedColumnName = "id_instalacion")
    @ManyToOne(optional = false)
    private Instalacion idInstalacion;
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

    public TarifaAlquiler() {
    }

    public TarifaAlquiler(Integer idTarifa) {
        this.idTarifa = idTarifa;
    }

    public Integer getIdTarifa() {
        return idTarifa;
    }

    public void setIdTarifa(Integer idTarifa) {
        this.idTarifa = idTarifa;
    }

    public Instalacion getIdInstalacion() {
        return idInstalacion;
    }

    public void setIdInstalacion(Instalacion idInstalacion) {
        this.idInstalacion = idInstalacion;
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
        hash += (idTarifa != null ? idTarifa.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof TarifaAlquiler)) {
            return false;
        }
        TarifaAlquiler other = (TarifaAlquiler) object;
        return !((this.idTarifa == null && other.idTarifa != null) || (this.idTarifa != null && !this.idTarifa.equals(other.idTarifa)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.TarifaAlquiler[ idTarifa=" + idTarifa + " ]";
    }

}
