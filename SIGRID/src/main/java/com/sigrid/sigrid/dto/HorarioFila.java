package com.sigrid.sigrid.dto;

import java.io.Serializable;

/**
 * DTO de una franja de apertura tal como se lista en la ficha de la pileta: "Lun a Vie · 09:00 – 19:00" (semanal, agrupa los
 * días con el mismo horario) o "Sáb 05/10 · 20:00 – 23:00" (día específico). Quitarla borra todas sus filas (ids).
 */
public class HorarioFila implements Serializable {

    private final String dias;      // "Lun a Vie", "Sáb y Dom" o "Sáb 05/10"
    private final String horario;   // "09:00 – 19:00"
    private final String ids;       // ids de horario_apertura separados por coma
    private final boolean deFecha;  // true = día específico

    public HorarioFila(String dias, String horario, String ids, boolean deFecha) {
        this.dias = dias;
        this.horario = horario;
        this.ids = ids;
        this.deFecha = deFecha;
    }

    public String getDias() {
        return dias;
    }

    public String getHorario() {
        return horario;
    }

    public String getIds() {
        return ids;
    }

    public boolean isDeFecha() {
        return deFecha;
    }
}
