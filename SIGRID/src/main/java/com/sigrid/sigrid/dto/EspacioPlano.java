package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/**
 * DTO de un espacio del plano de instalaciones del dashboard: cómo se ve ese día y qué reservas tiene.
 * Su lugar en el plano lo define el CSS por la clave (.adm-space-{clave}).
 */
public class EspacioPlano implements Serializable {

    private final Integer id;               // id de la instalación; null si todavía no está cargada
    private final String clave;             // "futbol", "quincho1"...
    private final String nombre;
    private final String icono;             // ícono (Material Symbols) del espacio
    private final String estado;            // "off" (no disponible), "free" (libre), "ok" (con reservas) o "warn" (con alguna por confirmar)
    private final String detalle;           // "Libre", "En mantenimiento", "3 reservas"...
    private final List<ReservaFila> reservas; // las que ocupan el espacio ese día (confirmadas y pendientes)

    public EspacioPlano(String clave, String nombre, String icono, String estado, String detalle,
            List<ReservaFila> reservas) {
        this(null, clave, nombre, icono, estado, detalle, reservas);
    }

    public EspacioPlano(Integer id, String clave, String nombre, String icono, String estado, String detalle,
            List<ReservaFila> reservas) {
        this.id = id;
        this.clave = clave;
        this.nombre = nombre;
        this.icono = icono;
        this.estado = estado;
        this.detalle = detalle;
        this.reservas = reservas;
    }

    public Integer getId() {
        return id;
    }

    /** Solo un espacio ya cargado se puede elegir (vista de instalaciones). */
    public boolean isSeleccionable() {
        return id != null;
    }

    public String getClave() {
        return clave;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIcono() {
        return icono;
    }

    public String getEstado() {
        return estado;
    }

    public String getDetalle() {
        return detalle;
    }

    public List<ReservaFila> getReservas() {
        return reservas;
    }

    /** Solo los espacios con reservas responden al click (abren su tarjeta). */
    public boolean isConReservas() {
        return !reservas.isEmpty();
    }

    public int getCantidad() {
        return reservas.size();
    }
}
