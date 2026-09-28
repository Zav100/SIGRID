package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.util.List;

/**
 * DTO de la ficha de una instalación (panel lateral de la vista de instalaciones): sus datos, cómo está hoy, cuánta demanda
 * tuvo y, según sea reservable o de acceso libre, sus turnos y próximas reservas o su horario de apertura.
 * Se arma en InstalacionesServicio.ficha, por eso tiene setters.
 */
public class FichaInstalacion implements Serializable {

    private Integer idInstalacion;
    private String nombre;
    private String icono;
    private String disciplina;
    private String descripcion;
    private Integer capacidad;
    private boolean reservable;        // false = acceso libre (no se reserva)
    private String estado;             // HABILITADA, DESHABILITADA_MANTENIMIENTO o DESHABILITADA
    private String estadoTexto;
    private String estadoClase;        // "ok", "warn" u "off"
    private String motivoBaja;
    private String bajaDetalle;        // "Desde el 26/09/2026 · reactivación estimada el 01/10/2026 (en 5 días)"
    private boolean reactivacionVencida;
    private String apertura;           // solo acceso libre: "Abierta hasta 19:00"
    private boolean abierta;
    private long reservas;             // últimos 30 días
    private int ocupacion;             // 0 a 100
    private String ingresos;           // "$ 25.000"
    private List<String> turnos;
    private List<ReservaFila> proximas;
    private List<HorarioFila> horarios;

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

    public String getIcono() {
        return icono;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public void setDisciplina(String disciplina) {
        this.disciplina = disciplina;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public boolean isReservable() {
        return reservable;
    }

    public void setReservable(boolean reservable) {
        this.reservable = reservable;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstadoTexto() {
        return estadoTexto;
    }

    public void setEstadoTexto(String estadoTexto) {
        this.estadoTexto = estadoTexto;
    }

    public String getEstadoClase() {
        return estadoClase;
    }

    public void setEstadoClase(String estadoClase) {
        this.estadoClase = estadoClase;
    }

    public String getMotivoBaja() {
        return motivoBaja;
    }

    public void setMotivoBaja(String motivoBaja) {
        this.motivoBaja = motivoBaja;
    }

    public String getBajaDetalle() {
        return bajaDetalle;
    }

    public void setBajaDetalle(String bajaDetalle) {
        this.bajaDetalle = bajaDetalle;
    }

    public boolean isReactivacionVencida() {
        return reactivacionVencida;
    }

    public void setReactivacionVencida(boolean reactivacionVencida) {
        this.reactivacionVencida = reactivacionVencida;
    }

    public String getApertura() {
        return apertura;
    }

    public void setApertura(String apertura) {
        this.apertura = apertura;
    }

    public boolean isAbierta() {
        return abierta;
    }

    public void setAbierta(boolean abierta) {
        this.abierta = abierta;
    }

    public long getReservas() {
        return reservas;
    }

    public void setReservas(long reservas) {
        this.reservas = reservas;
    }

    public int getOcupacion() {
        return ocupacion;
    }

    public void setOcupacion(int ocupacion) {
        this.ocupacion = ocupacion;
    }

    public String getIngresos() {
        return ingresos;
    }

    public void setIngresos(String ingresos) {
        this.ingresos = ingresos;
    }

    public List<String> getTurnos() {
        return turnos;
    }

    public void setTurnos(List<String> turnos) {
        this.turnos = turnos;
    }

    public List<ReservaFila> getProximas() {
        return proximas;
    }

    public void setProximas(List<ReservaFila> proximas) {
        this.proximas = proximas;
    }

    public List<HorarioFila> getHorarios() {
        return horarios;
    }

    public void setHorarios(List<HorarioFila> horarios) {
        this.horarios = horarios;
    }

    public boolean isHabilitada() {
        return "HABILITADA".equals(estado);
    }

    public boolean isEnMantenimiento() {
        return "DESHABILITADA_MANTENIMIENTO".equals(estado);
    }

    public boolean isDadaDeBaja() {
        return "DESHABILITADA".equals(estado);
    }
}
