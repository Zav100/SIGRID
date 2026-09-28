package com.sigrid.sigrid.repositorio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lo que tienen en común los dos historiales de precios (TarifaAlquiler y TarifaMembresia): un precio por categoría de socio
 * con su vigencia. TarifasServicio los maneja juntos.
 */
public interface Tarifa {

    CategoriaSocio getIdCategoriaSocio();

    BigDecimal getPrecio();

    LocalDate getVigenteDesde();

    LocalDate getVigenteHasta();

    void setVigenteHasta(LocalDate vigenteHasta);
}
