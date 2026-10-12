package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.ValoracionReservaDAO;
import com.sigrid.sigrid.dto.PromedioInstalacion;
import com.sigrid.sigrid.dto.ValoracionFila;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.Turno;
import com.sigrid.sigrid.repositorio.ValoracionReserva;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lectura de las valoraciones para el administrador: filas del listado y promedio por instalación con la tendencia
 * de los últimos 30 días contra los 30 anteriores (corte inclusivo: una valoración de exactamente 30 días es reciente).
 */
@ExtendWith(MockitoExtension.class)
class ValoracionesServicioTest {

    @Mock
    private ValoracionReservaDAO valoracionDAO;
    @Mock
    private InstalacionDAO instalacionDAO;

    @InjectMocks
    private ValoracionesServicio servicio;

    private static Instalacion instalacion(int id, String nombre) {
        return DatosDePrueba.instalacion(id, nombre, Instalacion.TipoAcceso.ARANCELADO, Instalacion.Estado.HABILITADA);
    }

    private static ValoracionFila fila(String instalacion, int puntaje, LocalDateTime fecha) {
        return new ValoracionFila(fecha, "", "Lucía Gómez", "Alumno UNSE", instalacion, "Fútbol", "sports_soccer",
                "turno", puntaje, "");
    }

    private PromedioInstalacion unicoResultado(List<ValoracionFila> todas) {
        List<PromedioInstalacion> r = servicio.promedios(todas, AHORA);
        assertEquals(1, r.size());
        return r.get(0);
    }

    // ---------- listado ----------

    private ValoracionReserva valoracion(Integer puntaje, String comentario) {
        CategoriaSocio alumno = DatosDePrueba.categoria(1, "Alumno UNSE", true);
        Socio socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO, alumno);
        Turno turno = DatosDePrueba.turno(5, instalacion(1, "Cancha de Fútbol 11"), LocalTime.of(10, 0),
                LocalTime.of(11, 0));
        Reserva reserva = DatosDePrueba.reserva(10, socio, turno, LocalDate.of(2026, 10, 10), Reserva.Estado.CONFIRMADA);
        ValoracionReserva v = new ValoracionReserva(1);
        v.setIdReserva(reserva);
        v.setPuntaje(puntaje);
        v.setComentario(comentario);
        v.setFechaValoracion(LocalDateTime.of(2026, 10, 11, 18, 30));
        return v;
    }

    @Test
    void armaLaFilaConSocioInstalacionTurnoPuntajeYFecha() {
        when(valoracionDAO.listarTodasParaAdmin()).thenReturn(List.of(valoracion(4, "Buena cancha")));

        ValoracionFila f = servicio.listar().get(0);

        assertEquals("Lucía Gómez", f.getSocio());
        assertEquals("Alumno UNSE", f.getCategoria());
        assertEquals("Cancha de Fútbol 11", f.getInstalacion());
        assertEquals("11/10/2026", f.getFechaTexto());
        assertEquals("sáb 10/10/2026 · 10:00 – 11:00", f.getTurno());
        assertEquals(4, f.getPuntaje());
        assertEquals("Buena cancha", f.getComentario());
    }

    @Test
    void unComentarioNuloQuedaVacioYSinMarcaDeComentario() {
        when(valoracionDAO.listarTodasParaAdmin()).thenReturn(List.of(valoracion(5, null)));

        ValoracionFila f = servicio.listar().get(0);

        assertEquals("", f.getComentario());
        assertFalse(f.isConComentario());
    }

    @Test
    void quitaLosEspaciosDelComentario() {
        when(valoracionDAO.listarTodasParaAdmin()).thenReturn(List.of(valoracion(5, "  Muy buena  ")));

        assertEquals("Muy buena", servicio.listar().get(0).getComentario());
    }

    // ---------- promedio por instalación ----------

    @Test
    void unaInstalacionSinValoracionesNoTienePromedioNiTendencia() {
        when(instalacionDAO.listarReservables()).thenReturn(List.of(instalacion(1, "Cancha de Fútbol 11")));

        PromedioInstalacion p = unicoResultado(new ArrayList<>());

        assertEquals(0, p.getCantidad());
        assertEquals("—", p.getPromedioTexto());
        assertEquals("", p.getTendencia());
        assertEquals("muted", p.getTendenciaClase());
        assertFalse(p.isConDatos());
    }

    @Test
    void calculaElPromedioConUnDecimalYComa() {
        when(instalacionDAO.listarReservables()).thenReturn(List.of(instalacion(1, "Cancha de Fútbol 11")));
        List<ValoracionFila> todas = List.of(fila("Cancha de Fútbol 11", 5, AHORA.minusDays(3)),
                fila("Cancha de Fútbol 11", 4, AHORA.minusDays(4)), fila("Cancha de Fútbol 11", 4, AHORA.minusDays(5)));

        PromedioInstalacion p = unicoResultado(todas);

        assertEquals(3, p.getCantidad());
        assertEquals("4,3", p.getPromedioTexto());
    }

    @Test
    void ignoraLasValoracionesDeOtraInstalacion() {
        when(instalacionDAO.listarReservables()).thenReturn(List.of(instalacion(1, "Cancha de Fútbol 11")));
        List<ValoracionFila> todas = List.of(fila("Cancha de Fútbol 11", 5, AHORA.minusDays(3)),
                fila("Cancha de Rugby", 1, AHORA.minusDays(3)));

        PromedioInstalacion p = unicoResultado(todas);

        assertEquals(1, p.getCantidad());
        assertEquals("5,0", p.getPromedioTexto());
    }

    // ---------- tendencia de 30 contra 30 días ----------

    @Test
    void laTendenciaEsPositivaSiMejoraronLosUltimos30Dias() {
        when(instalacionDAO.listarReservables()).thenReturn(List.of(instalacion(1, "Cancha de Fútbol 11")));
        List<ValoracionFila> todas = List.of(
                fila("Cancha de Fútbol 11", 5, AHORA.minusDays(5)), fila("Cancha de Fútbol 11", 5, AHORA.minusDays(10)),
                fila("Cancha de Fútbol 11", 3, AHORA.minusDays(40)), fila("Cancha de Fútbol 11", 4, AHORA.minusDays(50)));

        PromedioInstalacion p = unicoResultado(todas);

        assertEquals("+1,5 en 30 días", p.getTendencia());
        assertEquals("ok", p.getTendenciaClase());
    }

    @Test
    void laTendenciaEsNegativaSiEmpeoraronLosUltimos30Dias() {
        when(instalacionDAO.listarReservables()).thenReturn(List.of(instalacion(1, "Cancha de Fútbol 11")));
        List<ValoracionFila> todas = List.of(
                fila("Cancha de Fútbol 11", 3, AHORA.minusDays(5)), fila("Cancha de Fútbol 11", 3, AHORA.minusDays(10)),
                fila("Cancha de Fútbol 11", 5, AHORA.minusDays(40)), fila("Cancha de Fútbol 11", 5, AHORA.minusDays(50)));

        PromedioInstalacion p = unicoResultado(todas);

        assertEquals("-2,0 en 30 días", p.getTendencia());
        assertEquals("off", p.getTendenciaClase());
    }

    @Test
    void sinCambiosSiLosDosPeriodosTienenElMismoPromedio() {
        when(instalacionDAO.listarReservables()).thenReturn(List.of(instalacion(1, "Cancha de Fútbol 11")));
        List<ValoracionFila> todas = List.of(fila("Cancha de Fútbol 11", 4, AHORA.minusDays(5)),
                fila("Cancha de Fútbol 11", 4, AHORA.minusDays(40)));

        PromedioInstalacion p = unicoResultado(todas);

        assertEquals("Sin cambios en 30 días", p.getTendencia());
        assertEquals("muted", p.getTendenciaClase());
    }

    @Test
    void noHayTendenciaSiSoloHayValoracionesRecientes() {
        when(instalacionDAO.listarReservables()).thenReturn(List.of(instalacion(1, "Cancha de Fútbol 11")));

        PromedioInstalacion p = unicoResultado(List.of(fila("Cancha de Fútbol 11", 4, AHORA.minusDays(5))));

        assertEquals("", p.getTendencia());
    }

    @Test
    void unaValoracionDeExactamente30DiasCuentaComoRecienteYLaDe31ComoPrevia() {
        when(instalacionDAO.listarReservables()).thenReturn(List.of(instalacion(1, "Cancha de Fútbol 11")));
        List<ValoracionFila> todas = List.of(fila("Cancha de Fútbol 11", 5, AHORA.minusDays(30)),
                fila("Cancha de Fútbol 11", 1, AHORA.minusDays(31)));

        PromedioInstalacion p = unicoResultado(todas);

        assertEquals("+4,0 en 30 días", p.getTendencia());
    }

    @Test
    void unaValoracionDeExactamente60DiasCuentaComoPrevia() {
        when(instalacionDAO.listarReservables()).thenReturn(List.of(instalacion(1, "Cancha de Fútbol 11")));
        List<ValoracionFila> todas = List.of(fila("Cancha de Fútbol 11", 4, AHORA.minusDays(5)),
                fila("Cancha de Fútbol 11", 2, AHORA.minusDays(60)));

        PromedioInstalacion p = unicoResultado(todas);

        assertEquals("+2,0 en 30 días", p.getTendencia());
    }

    @Test
    void unaValoracionDeMasDe60DiasNoEntraEnLaTendencia() {
        when(instalacionDAO.listarReservables()).thenReturn(List.of(instalacion(1, "Cancha de Fútbol 11")));
        List<ValoracionFila> todas = List.of(fila("Cancha de Fútbol 11", 4, AHORA.minusDays(5)),
                fila("Cancha de Fútbol 11", 2, AHORA.minusDays(60).minusMinutes(1)));

        PromedioInstalacion p = unicoResultado(todas);

        assertEquals("", p.getTendencia());
        assertEquals(2, p.getCantidad(), "igual cuenta en el promedio general");
    }

    // ---------- orden ----------

    @Test
    void ordenaLasPeorValoradasPrimeroConLasDeMismoPromedioPorNombreYLasSinDatosAlFinal() {
        when(instalacionDAO.listarReservables()).thenReturn(List.of(instalacion(1, "Quincho 1"),
                instalacion(2, "Cancha de Rugby"), instalacion(3, "Cancha de Fútbol 11"), instalacion(4, "Beach Vóley")));
        List<ValoracionFila> todas = List.of(fila("Cancha de Rugby", 5, AHORA.minusDays(2)),
                fila("Cancha de Rugby", 4, AHORA.minusDays(3)), fila("Cancha de Fútbol 11", 3, AHORA.minusDays(2)),
                fila("Beach Vóley", 3, AHORA.minusDays(2)));

        List<PromedioInstalacion> r = servicio.promedios(todas, AHORA);

        assertEquals("Beach Vóley", r.get(0).getNombre());
        assertEquals("Cancha de Fútbol 11", r.get(1).getNombre());
        assertEquals("Cancha de Rugby", r.get(2).getNombre());
        assertEquals("Quincho 1", r.get(3).getNombre());
        assertTrue(r.get(0).isParaRevisar());
    }

    @Test
    void elPromedioSeMuestraConUnDecimal() {
        assertEquals("4,3", ValoracionesServicio.decimal(4.2857));
    }
}
