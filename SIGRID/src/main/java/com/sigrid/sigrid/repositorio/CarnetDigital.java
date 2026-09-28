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
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Carnet digital, uno por socio. codigoQrUrl está reservado para el QR diferido (ERS 2.6): la app no debe depender de que tenga valor.
 */
@Entity
@Table(name = "carnet_digital")
@NamedQueries({
    @NamedQuery(name = "CarnetDigital.findAll", query = "SELECT x FROM CarnetDigital x"),
    @NamedQuery(name = "CarnetDigital.findByIdCarnet", query = "SELECT x FROM CarnetDigital x WHERE x.idCarnet = :idCarnet")
})
public class CarnetDigital implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum TipoCarnet {
        GENERAL, ESTUDIANTIL
    }

    public enum Estado {
        ACTIVO, INACTIVO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_carnet")
    private Integer idCarnet;
    @JoinColumn(name = "id_socio", referencedColumnName = "id_socio", unique = true)
    @OneToOne(optional = false)
    private Socio idSocio;
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_carnet")
    private TipoCarnet tipoCarnet;
    @Size(max = 255)
    @Column(name = "codigo_qr_url")
    private String codigoQrUrl;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision = LocalDateTime.now();
    @Basic(optional = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private Estado estado = Estado.INACTIVO;

    public CarnetDigital() {
    }

    public CarnetDigital(Integer idCarnet) {
        this.idCarnet = idCarnet;
    }

    public Integer getIdCarnet() {
        return idCarnet;
    }

    public void setIdCarnet(Integer idCarnet) {
        this.idCarnet = idCarnet;
    }

    public Socio getIdSocio() {
        return idSocio;
    }

    public void setIdSocio(Socio idSocio) {
        this.idSocio = idSocio;
    }

    public TipoCarnet getTipoCarnet() {
        return tipoCarnet;
    }

    public void setTipoCarnet(TipoCarnet tipoCarnet) {
        this.tipoCarnet = tipoCarnet;
    }

    public String getCodigoQrUrl() {
        return codigoQrUrl;
    }

    public void setCodigoQrUrl(String codigoQrUrl) {
        this.codigoQrUrl = codigoQrUrl;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idCarnet != null ? idCarnet.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CarnetDigital)) {
            return false;
        }
        CarnetDigital other = (CarnetDigital) object;
        return !((this.idCarnet == null && other.idCarnet != null) || (this.idCarnet != null && !this.idCarnet.equals(other.idCarnet)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.CarnetDigital[ idCarnet=" + idCarnet + " ]";
    }

}
