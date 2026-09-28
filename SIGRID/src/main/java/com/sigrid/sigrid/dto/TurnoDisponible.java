package com.sigrid.sigrid.dto;

import java.io.Serializable;

/** Una franja horaria de una instalación en un día: libre o no (motivo dice por qué no). */
public class TurnoDisponible implements Serializable {

    private final Integer idTurno;
    private final String horario;
    private final boolean libre;
    private final String motivo;

    public TurnoDisponible(Integer idTurno, String horario, boolean libre, String motivo) {
        this.idTurno = idTurno;
        this.horario = horario;
        this.libre = libre;
        this.motivo = motivo;
    }

    public Integer getIdTurno() {
        return idTurno;
    }

    public String getHorario() {
        return horario;
    }

    public boolean isLibre() {
        return libre;
    }

    public String getMotivo() {
        return motivo;
    }
}
