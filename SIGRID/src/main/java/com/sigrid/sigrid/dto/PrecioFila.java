package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/**
 * DTO de una fila del panel de precios: la instalación y lo que paga cada tipo de socio por reservarla (en el orden de las
 * columnas; null = sin tarifa cargada). Una instalación de acceso libre no se reserva: no tiene precios.
 */
public class PrecioFila implements Serializable {

    private final Integer idInstalacion;
    private final String nombre;
    private final String icono;
    private final boolean sinReserva;
    private final List<String> precios;
    private final boolean ordenOk;      // el alumno paga menos que todos y el externo más que todos

    public PrecioFila(Integer idInstalacion, String nombre, String icono, boolean sinReserva, List<String> precios,
            boolean ordenOk) {
        this.idInstalacion = idInstalacion;
        this.nombre = nombre;
        this.icono = icono;
        this.sinReserva = sinReserva;
        this.precios = precios;
        this.ordenOk = ordenOk;
    }

    public Integer getIdInstalacion() {
        return idInstalacion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIcono() {
        return icono;
    }

    public boolean isSinReserva() {
        return sinReserva;
    }

    public List<String> getPrecios() {
        return precios;
    }

    public boolean isOrdenOk() {
        return ordenOk;
    }
}
