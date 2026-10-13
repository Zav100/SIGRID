package com.sigrid.sigrid.dto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Lo que el administrador eligió para el listado de reservas y para el PDF (CC-06): la consulta lo aplica en SQL.
 * Los textos vacíos y las fechas nulas significan "sin filtro".
 */
public class FiltroReservas implements Serializable {

    private String texto = "";        // buscador libre: socio, correo, DNI, instalación, disciplina, fecha o número
    private String estado = "";       // "", POR_CONFIRMAR, ESPERANDO_PAGO, CONFIRMADA, CANCELADA o RECHAZADA
    private String disciplina = "";   // "" o el tipo de instalación
    private LocalDate desde;          // primer día del turno (inclusive)
    private LocalDate hasta;          // último día del turno (inclusive)
    private Boolean finalizadas;      // null: todas; true: las que ya terminaron; false: las que todavía no

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto == null ? "" : texto.trim();
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado == null ? "" : estado;
    }

    public String getDisciplina() {
        return disciplina;
    }

    public void setDisciplina(String disciplina) {
        this.disciplina = disciplina == null ? "" : disciplina;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public void setDesde(LocalDate desde) {
        this.desde = desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public void setHasta(LocalDate hasta) {
        this.hasta = hasta;
    }

    public Boolean getFinalizadas() {
        return finalizadas;
    }

    public void setFinalizadas(Boolean finalizadas) {
        this.finalizadas = finalizadas;
    }

    /** ¿Hay alguna condición puesta? */
    public boolean isFiltrando() {
        return !texto.isEmpty() || !estado.isEmpty() || !disciplina.isEmpty() || desde != null || hasta != null
                || finalizadas != null;
    }
}
