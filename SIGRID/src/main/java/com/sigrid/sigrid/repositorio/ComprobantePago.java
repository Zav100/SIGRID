package com.sigrid.sigrid.repositorio;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Comprobante de la transferencia (1 a 1 con Pago). hashArchivo es SHA-256 en hexadecimal y es UNIQUE en la BD:
 * un comprobante repetido falla al insertar (RF-03.6).
 */
@Entity
@Table(name = "comprobante_pago")
@NamedQueries({
    @NamedQuery(name = "ComprobantePago.findAll", query = "SELECT x FROM ComprobantePago x"),
    @NamedQuery(name = "ComprobantePago.findByIdComprobante", query = "SELECT x FROM ComprobantePago x WHERE x.idComprobante = :idComprobante"),
    @NamedQuery(name = "ComprobantePago.findByHashArchivo", query = "SELECT x FROM ComprobantePago x WHERE x.hashArchivo = :hashArchivo")
})
public class ComprobantePago implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_comprobante")
    private Integer idComprobante;
    @JoinColumn(name = "id_pago", referencedColumnName = "id_pago", unique = true)
    @OneToOne(optional = false)
    private Pago idPago;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "archivo_url")
    private String archivoUrl;
    @Basic(optional = false)
    @NotNull
    @Size(min = 64, max = 64)
    @Column(name = "hash_archivo")
    private String hashArchivo;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_operacion_declarada")
    private LocalDateTime fechaOperacionDeclarada;

    public ComprobantePago() {
    }

    public ComprobantePago(Integer idComprobante) {
        this.idComprobante = idComprobante;
    }

    public Integer getIdComprobante() {
        return idComprobante;
    }

    public void setIdComprobante(Integer idComprobante) {
        this.idComprobante = idComprobante;
    }

    public Pago getIdPago() {
        return idPago;
    }

    public void setIdPago(Pago idPago) {
        this.idPago = idPago;
    }

    public String getArchivoUrl() {
        return archivoUrl;
    }

    public void setArchivoUrl(String archivoUrl) {
        this.archivoUrl = archivoUrl;
    }

    public String getHashArchivo() {
        return hashArchivo;
    }

    public void setHashArchivo(String hashArchivo) {
        this.hashArchivo = hashArchivo;
    }

    public LocalDateTime getFechaOperacionDeclarada() {
        return fechaOperacionDeclarada;
    }

    public void setFechaOperacionDeclarada(LocalDateTime fechaOperacionDeclarada) {
        this.fechaOperacionDeclarada = fechaOperacionDeclarada;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idComprobante != null ? idComprobante.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ComprobantePago)) {
            return false;
        }
        ComprobantePago other = (ComprobantePago) object;
        return !((this.idComprobante == null && other.idComprobante != null) || (this.idComprobante != null && !this.idComprobante.equals(other.idComprobante)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.ComprobantePago[ idComprobante=" + idComprobante + " ]";
    }

}
