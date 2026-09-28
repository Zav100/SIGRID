package com.sigrid.sigrid.repositorio;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Valoración (1 a 5 + comentario opcional) de una reserva finalizada. Una sola por reserva (UNIQUE).
 */
@Entity
@Table(name = "valoracion_reserva")
@NamedQueries({
    @NamedQuery(name = "ValoracionReserva.findAll", query = "SELECT x FROM ValoracionReserva x"),
    @NamedQuery(name = "ValoracionReserva.findByIdValoracion", query = "SELECT x FROM ValoracionReserva x WHERE x.idValoracion = :idValoracion")
})
public class ValoracionReserva implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_valoracion")
    private Integer idValoracion;
    @JoinColumn(name = "id_reserva", referencedColumnName = "id_reserva", unique = true)
    @OneToOne(optional = false)
    private Reserva idReserva;
    @Basic(optional = false)
    @NotNull
    @Min(1)
    @Max(5)
    @Column(name = "puntaje")
    private Integer puntaje;
    @Lob
    @Column(name = "comentario")
    private String comentario;
    @Basic(optional = false)
    @NotNull
    @Column(name = "fecha_valoracion")
    private LocalDateTime fechaValoracion = LocalDateTime.now();

    public ValoracionReserva() {
    }

    public ValoracionReserva(Integer idValoracion) {
        this.idValoracion = idValoracion;
    }

    public Integer getIdValoracion() {
        return idValoracion;
    }

    public void setIdValoracion(Integer idValoracion) {
        this.idValoracion = idValoracion;
    }

    public Reserva getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Reserva idReserva) {
        this.idReserva = idReserva;
    }

    public Integer getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(Integer puntaje) {
        this.puntaje = puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDateTime getFechaValoracion() {
        return fechaValoracion;
    }

    public void setFechaValoracion(LocalDateTime fechaValoracion) {
        this.fechaValoracion = fechaValoracion;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idValoracion != null ? idValoracion.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ValoracionReserva)) {
            return false;
        }
        ValoracionReserva other = (ValoracionReserva) object;
        return !((this.idValoracion == null && other.idValoracion != null) || (this.idValoracion != null && !this.idValoracion.equals(other.idValoracion)));
    }

    @Override
    public String toString() {
        return "com.sigrid.sigrid.repositorio.ValoracionReserva[ idValoracion=" + idValoracion + " ]";
    }

}
