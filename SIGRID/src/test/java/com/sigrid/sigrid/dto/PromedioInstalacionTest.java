package com.sigrid.sigrid.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Niveles del promedio (alto desde 4, medio desde 3,5) y la marca "para revisar" por debajo de 3,5. */
class PromedioInstalacionTest {

    private static PromedioInstalacion con(int cantidad, double promedio) {
        return new PromedioInstalacion("Cancha de Fútbol 11", "sports_soccer", "Fútbol", cantidad, promedio, "", "",
                "muted");
    }

    @Test
    void conPromedioDe4EsNivelAlto() {
        assertEquals("alto", con(3, 4.0).getNivel());
    }

    @Test
    void conPromedioDe3Coma5EsNivelMedioYNoParaRevisar() {
        PromedioInstalacion p = con(2, 3.5);

        assertEquals("medio", p.getNivel());
        assertFalse(p.isParaRevisar());
    }

    @Test
    void conPromedioPorDebajoDe3Coma5EsNivelBajoYParaRevisar() {
        PromedioInstalacion p = con(3, 3.49);

        assertEquals("bajo", p.getNivel());
        assertTrue(p.isParaRevisar());
    }

    @Test
    void sinValoracionesEsNivelSinYNoSeMarcaParaRevisar() {
        PromedioInstalacion p = con(0, 0);

        assertEquals("sin", p.getNivel());
        assertFalse(p.isParaRevisar());
    }

    @Test
    void elPorcentajeDeLaBarraEsElPromedioSobre5() {
        assertEquals(86, con(3, 4.3).getPorcentaje());
    }
}
