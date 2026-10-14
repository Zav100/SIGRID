package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigrid.sigrid.dto.ReporteAlerta;
import com.sigrid.sigrid.dto.ReporteBarra;
import com.sigrid.sigrid.dto.ReporteCalor;
import com.sigrid.sigrid.dto.ReporteItem;
import com.sigrid.sigrid.dto.ReporteKpi;
import com.sigrid.sigrid.dto.ValoracionFila;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import com.sigrid.sigrid.repositorio.Turno;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Reportes del administrador. Los valores esperados salen de las definiciones que documenta la clase: un ingreso es un
 * pago confirmado contado el día en que se validó; cada período se compara con el anterior de igual largo; la ocupación
 * es turnos reservados sobre turnos disponibles (descontando los días de mantenimiento).
 */
class ReportesServicioTest {

    private final ReportesServicio servicio = new ReportesServicio();

    private final CategoriaSocio alumno = DatosDePrueba.categoria(1, "Alumno UNSE", true);
    private final CategoriaSocio externo = DatosDePrueba.categoria(4, "Externo", false);
    private final Instalacion cancha = instalacion(1, "Cancha de Fútbol 11", Instalacion.Estado.HABILITADA);
    private final Instalacion rugby = instalacion(2, "Cancha de Rugby", Instalacion.Estado.HABILITADA);
    private final Turno t1 = DatosDePrueba.turno(5, cancha, LocalTime.of(10, 0), LocalTime.of(11, 0));
    private final Turno t2 = DatosDePrueba.turno(6, cancha, LocalTime.of(18, 0), LocalTime.of(19, 0));
    private final Turno t3 = DatosDePrueba.turno(7, rugby, LocalTime.of(18, 0), LocalTime.of(19, 0));

    private ReportesServicio.Datos d;
    private ReportesServicio.Rango r30;

    private static Instalacion instalacion(int id, String nombre, Instalacion.Estado estado) {
        return DatosDePrueba.instalacion(id, nombre, Instalacion.TipoAcceso.ARANCELADO, estado);
    }

    @BeforeEach
    void armarLaFoto() {
        d = new ReportesServicio.Datos();
        d.hoy = HOY;
        d.reservas = new ArrayList<>();
        d.suscripciones = new ArrayList<>();
        d.valoraciones = new ArrayList<>();
        d.instalaciones = new ArrayList<>(List.of(cancha, rugby));
        d.turnos.put(1, List.of(t1, t2));
        d.turnos.put(2, List.of(t3));
        d.categorias.add("Alumno UNSE");
        d.categorias.add("Externo");
        d.cuotas.put("Alumno UNSE", new BigDecimal("5000"));
        d.cuotas.put("Externo", new BigDecimal("8000"));
        r30 = servicio.rango("30D", HOY);
    }

    // ---------- armado de datos ----------

    /** Un ingreso: pagado una hora antes de validarse el día indicado. */
    private void ingreso(LocalDate validado, String monto, boolean reserva, String instalacion, String categoria) {
        d.ingresos.add(new ReportesServicio.Ingreso(validado.atTime(9, 0), validado.atTime(10, 0),
                new BigDecimal(monto), reserva, instalacion, categoria));
    }

    private Reserva reserva(int id, Turno turno, LocalDate fecha, Reserva.Estado estado) {
        Socio socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO, alumno);
        Reserva r = DatosDePrueba.reserva(id, socio, turno, fecha, estado);
        d.reservas.add(r);
        return r;
    }

    private Pago pagoConfirmado(String monto) {
        Pago p = DatosDePrueba.pago(id(), monto, Pago.Estado.CONFIRMADO);
        p.setFechaValidacion(LocalDateTime.of(2026, 10, 1, 10, 0));
        return p;
    }

    private int siguiente = 100;

    private int id() {
        return siguiente++;
    }

    private SuscripcionSocio suscripcion(int idSocio, CategoriaSocio categoria, SuscripcionSocio.Estado estado,
            LocalDate inicio, LocalDate vence) {
        SuscripcionSocio s = new SuscripcionSocio(id());
        s.setIdSocio(DatosDePrueba.socio(idSocio, Socio.Estado.ACTIVO, categoria));
        s.setEstado(estado);
        s.setFechaInicio(inicio);
        s.setFechaVencimiento(vence);
        d.suscripciones.add(s);
        return s;
    }

    private static ReporteKpi kpi(List<ReporteKpi> kpis, String etiqueta) {
        for (ReporteKpi k : kpis) {
            if (k.getEtiqueta().equals(etiqueta)) {
                return k;
            }
        }
        throw new AssertionError("no hay el indicador " + etiqueta);
    }

    private static ReporteItem item(List<ReporteItem> items, String etiqueta) {
        for (ReporteItem i : items) {
            if (i.getEtiqueta().equals(etiqueta)) {
                return i;
            }
        }
        throw new AssertionError("no hay el ítem " + etiqueta);
    }

    // ---------- períodos ----------

    @DisplayName("CP-42: \"Un día\" es un período de un solo día, comparado con el mismo día de la semana anterior")
    @Test
    void cp42_unDiaEsUnPeriodoDeUnSoloDia() {
        LocalDate dia = LocalDate.of(2026, 10, 15);
        ReportesServicio.Rango r = servicio.rango("DIA", HOY, dia);

        assertEquals("Un día", r.getNombre());
        assertEquals(dia, r.getDesde());
        assertEquals(dia, r.getHasta());
        assertEquals(1, r.getDias());
        assertEquals(dia.minusDays(7), r.getPrevioDesde());
        assertEquals(dia.minusDays(7), r.getPrevioHasta());
    }

    @Test
    void unDiaSinFechaEsHoy() {
        ReportesServicio.Rango r = servicio.rango("DIA", HOY, null);
        assertEquals(HOY, r.getDesde());
        assertEquals(HOY, r.getHasta());
    }

    @Test
    void losDemasPeriodosNoUsanElDiaElegido() {
        assertEquals(servicio.rango("7D", HOY).getDesde(), servicio.rango("7D", HOY, LocalDate.of(2026, 1, 1)).getDesde());
    }

    @DisplayName("CP-42: la ocupación de un día de una instalación dice, turno por turno, si está libre o quién lo reservó")
    @Test
    void cp42_laOcupacionDeUnDiaDeUnaInstalacionVaTurnoPorTurno() {
        LocalDate dia = HOY.plusDays(2);
        reserva(1, t2, dia, Reserva.Estado.CONFIRMADA);
        reserva(2, t1, dia, Reserva.Estado.CANCELADA);   // no ocupa el turno
        reserva(3, t1, dia.plusDays(1), Reserva.Estado.CONFIRMADA); // otro día
        reserva(4, t3, dia, Reserva.Estado.CONFIRMADA);   // otra instalación
        ReportesServicio.Rango r = servicio.rango("DIA", HOY, dia);

        List<ReporteItem> turnos = servicio.ocupacionPorTurno(d, r, cancha.getIdInstalacion());

        assertEquals(2, turnos.size());
        ReporteItem manana = turnos.get(0);
        assertEquals("10:00 – 11:00", manana.getEtiqueta());
        assertEquals("Libre", manana.getValor());
        assertEquals(0, manana.getNumero());
        ReporteItem tarde = turnos.get(1);
        assertEquals("18:00 – 19:00", tarde.getEtiqueta());
        assertEquals("Reservado", tarde.getValor());
        assertEquals(1, tarde.getNumero());
        assertEquals("Lucía Gómez · Confirmada", tarde.getDetalle());
        assertEquals(100, tarde.getPorcentaje());
    }

    @Test
    void unaReservaEsperandoPagoTambienOcupaElTurno() {
        LocalDate dia = HOY.plusDays(2);
        reserva(1, t1, dia, Reserva.Estado.PENDIENTE_PAGO);

        ReporteItem i = servicio.ocupacionPorTurno(d, servicio.rango("DIA", HOY, dia), cancha.getIdInstalacion()).get(0);

        assertEquals("Reservado", i.getValor());
        assertEquals("Lucía Gómez · Pendiente de confirmación", i.getDetalle());
    }

    @Test
    void enUnPeriodoDeVariosDiasCuentaEnCuantosDiasSeReservoCadaTurno() {
        reserva(1, t2, HOY, Reserva.Estado.CONFIRMADA);
        reserva(2, t2, HOY.minusDays(1), Reserva.Estado.CONFIRMADA);
        ReportesServicio.Rango r = servicio.rango("7D", HOY);

        ReporteItem tarde = servicio.ocupacionPorTurno(d, r, cancha.getIdInstalacion()).get(1);

        assertEquals("2 de 7 días", tarde.getValor());
        assertEquals(2, tarde.getNumero());
        assertEquals(29, tarde.getPorcentaje());
    }

    @Test
    void unaInstalacionEnMantenimientoTodoElDiaNoTieneTurnosParaOfrecer() {
        rugby.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        rugby.setFechaInicioBaja(HOY.minusDays(3));

        ReporteItem i = servicio.ocupacionPorTurno(d, servicio.rango("DIA", HOY, HOY), rugby.getIdInstalacion()).get(0);

        assertEquals("En mantenimiento", i.getValor());
    }

    @Test
    void sinInstalacionElegidaNoHayTurnos() {
        assertTrue(servicio.ocupacionPorTurno(d, r30, null).isEmpty());
        assertTrue(servicio.ocupacionPorTurno(d, r30, 99).isEmpty());
    }

    @Test
    void losUltimos7DiasTerminanHoyYSeComparanConLos7Anteriores() {
        ReportesServicio.Rango r = servicio.rango("7D", HOY);

        assertEquals("Últimos 7 días", r.getNombre());
        assertEquals("06/10/2026 al 12/10/2026", r.getTexto());
        assertEquals("29/09/2026 al 05/10/2026", r.getTextoPrevio());
        assertEquals(7, r.getDias());
    }

    @Test
    void losUltimos30DiasIncluyenHoyYSeComparanConLos30Anteriores() {
        assertEquals("13/09/2026 al 12/10/2026", r30.getTexto());
        assertEquals("14/08/2026 al 12/09/2026", r30.getTextoPrevio());
        assertEquals(30, r30.getDias());
    }

    @Test
    void losUltimos90DiasSeComparanConLos90Anteriores() {
        ReportesServicio.Rango r = servicio.rango("90D", HOY);

        assertEquals("15/07/2026 al 12/10/2026", r.getTexto());
        assertEquals("16/04/2026 al 14/07/2026", r.getTextoPrevio());
        assertEquals(90, r.getDias());
    }

    @Test
    void losUltimos12MesesSeComparanConLos12Anteriores() {
        ReportesServicio.Rango r = servicio.rango("12M", HOY);

        assertEquals("13/10/2025 al 12/10/2026", r.getTexto());
        assertEquals("13/10/2024 al 12/10/2025", r.getTextoPrevio());
    }

    @Test
    void esteMesVaDelPrimeroAHoyYSeComparaConLosMismosDiasDelMesAnterior() {
        ReportesServicio.Rango r = servicio.rango("MES", HOY);

        assertEquals("Este mes", r.getNombre());
        assertEquals("01/10/2026 al 12/10/2026", r.getTexto());
        assertEquals("01/09/2026 al 12/09/2026", r.getTextoPrevio());
    }

    @Test
    void siElMesAnteriorEsMasCortoElPeriodoPrevioTerminaEnSuUltimoDia() {
        ReportesServicio.Rango r = servicio.rango("MES", LocalDate.of(2026, 3, 31));

        assertEquals("01/03/2026 al 31/03/2026", r.getTexto());
        assertEquals("01/02/2026 al 28/02/2026", r.getTextoPrevio());
    }

    @Test
    void elMesAnteriorSeComparaConElMesPrevioCompleto() {
        ReportesServicio.Rango r = servicio.rango("MES_ANT", HOY);

        assertEquals("Mes anterior", r.getNombre());
        assertEquals("01/09/2026 al 30/09/2026", r.getTexto());
        assertEquals("01/08/2026 al 31/08/2026", r.getTextoPrevio());
    }

    @Test
    void unPeriodoDesconocidoUsaLosUltimos30Dias() {
        assertEquals("Últimos 30 días", servicio.rango("XYZ", HOY).getNombre());
    }

    // ---------- ingresos ----------

    private void ingresosDeEjemplo() {
        ingreso(LocalDate.of(2026, 10, 10), "20000", true, "Cancha de Fútbol 11", "Alumno UNSE");
        ingreso(LocalDate.of(2026, 10, 5), "15000", true, "Cancha de Rugby", "Externo");
        ingreso(LocalDate.of(2026, 10, 1), "5000", false, null, "Alumno UNSE");
        ingreso(LocalDate.of(2026, 9, 1), "20000", true, "Cancha de Fútbol 11", "Alumno UNSE"); // período anterior
        ingreso(LocalDate.of(2026, 8, 1), "9999", true, "Cancha de Fútbol 11", "Alumno UNSE"); // fuera de los dos
    }

    @Test
    void elBalanceSumaReservasYMembresiasDelPeriodoYLoComparaConElAnterior() {
        ingresosDeEjemplo();

        List<ReporteKpi> kpis = servicio.balance(d, r30);

        ReporteKpi total = kpi(kpis, "Ingresos totales");
        assertEquals("$ 40.000", total.getValor());
        assertEquals("antes: $ 20.000", total.getSub());
        assertEquals("+100 % vs. período anterior", total.getVariacion());
        assertEquals("ok", total.getVariacionClase());
    }

    @Test
    void elBalanceSeparaLoCobradoPorReservasYPorMembresias() {
        ingresosDeEjemplo();

        List<ReporteKpi> kpis = servicio.balance(d, r30);

        ReporteKpi reservas = kpi(kpis, "Por reservas");
        assertEquals("$ 35.000", reservas.getValor());
        assertEquals("2 reservas cobradas", reservas.getSub());
        assertEquals("+75 % vs. período anterior", reservas.getVariacion());
        ReporteKpi membresias = kpi(kpis, "Por membresías");
        assertEquals("$ 5.000", membresias.getValor());
        assertEquals("1 cuota cobrada", membresias.getSub());
        assertEquals("Sin datos del período anterior", membresias.getVariacion());
        assertEquals("muted", membresias.getVariacionClase());
    }

    @Test
    void elTicketPromedioEsLoCobradoPorReservasSobreLaCantidadDeReservas() {
        ingresosDeEjemplo();

        ReporteKpi ticket = kpi(servicio.balance(d, r30), "Ticket promedio por reserva");

        assertEquals("$ 17.500", ticket.getValor());
        assertEquals("-12 % vs. período anterior", ticket.getVariacion());
        assertEquals("off", ticket.getVariacionClase());
    }

    @Test
    void sinIngresosEnNingunPeriodoNoHayCambios() {
        ReporteKpi total = kpi(servicio.balance(d, r30), "Ingresos totales");

        assertEquals("$ 0", total.getValor());
        assertEquals("Sin cambios", total.getVariacion());
        assertEquals("muted", total.getVariacionClase());
    }

    @Test
    void unIngresoSeCuentaElDiaEnQueSeValidaYNoElDiaEnQueSePago() {
        // pagado el 12/09 (período anterior) y validado el 13/09 (primer día del período actual)
        d.ingresos.add(new ReportesServicio.Ingreso(LocalDateTime.of(2026, 9, 12, 23, 0),
                LocalDateTime.of(2026, 9, 13, 9, 0), new BigDecimal("12000"), true, "Cancha de Fútbol 11", "Alumno UNSE"));

        assertEquals("$ 12.000", kpi(servicio.balance(d, r30), "Ingresos totales").getValor());
    }

    // ---------- evolución ----------

    @Test
    void unPeriodoDeUnMesSeAgrupaPorDiaConLaBarraMasAltaAl100() {
        ingreso(LocalDate.of(2026, 10, 10), "20000", true, "Cancha de Fútbol 11", "Alumno UNSE");
        ingreso(LocalDate.of(2026, 10, 1), "5000", false, null, "Alumno UNSE");

        List<ReporteBarra> barras = servicio.evolucion(d, r30, "");

        assertEquals(30, barras.size());
        assertEquals("13/09", barras.get(0).getEtiqueta());
        ReporteBarra diez = barras.get(27);
        assertEquals("10/10", diez.getEtiqueta());
        assertEquals(0, new BigDecimal("20000").compareTo(diez.getReservas()));
        assertEquals(100, diez.getAltoReservas());
        ReporteBarra uno = barras.get(18);
        assertEquals("01/10", uno.getEtiqueta());
        assertEquals(0, new BigDecimal("5000").compareTo(uno.getMembresias()));
        assertEquals(25, uno.getAltoMembresias());
    }

    @Test
    void unPeriodoDeTresMesesSeAgrupaPorSemanaDesdeElLunes() {
        List<ReporteBarra> barras = servicio.evolucion(d, servicio.rango("90D", HOY), "");

        assertEquals(14, barras.size());
        assertEquals("13/07", barras.get(0).getEtiqueta());
        assertTrue(barras.get(0).getTitulo().startsWith("Semana del 13/07"));
    }

    @Test
    void unPeriodoDeUnAnioSeAgrupaPorMes() {
        List<ReporteBarra> barras = servicio.evolucion(d, servicio.rango("12M", HOY), "");

        assertEquals(13, barras.size());
    }

    @Test
    void laGranularidadSePuedeForzarYSoloSeMuestraUnaEtiquetaDeCadaTres() {
        List<ReporteBarra> semanas = servicio.evolucion(d, servicio.rango("7D", HOY), "SEMANA");
        List<ReporteBarra> dias = servicio.evolucion(d, r30, "DIA");

        assertEquals(2, semanas.size());
        assertTrue(dias.get(0).isMostrarEtiqueta());
        assertFalse(dias.get(1).isMostrarEtiqueta());
        assertTrue(dias.get(3).isMostrarEtiqueta());
    }

    // ---------- ingresos por instalación y por tipo de socio ----------

    @Test
    void laInstalacionQueMasFacturaVaPrimeraConSuParticipacion() {
        ingresosDeEjemplo();

        List<ReporteItem> items = servicio.ingresosPorInstalacion(d, r30);

        assertEquals("Cancha de Fútbol 11", items.get(0).getEtiqueta());
        assertEquals("$ 20.000", items.get(0).getValor());
        assertEquals("1 reserva · 57 % del total", items.get(0).getDetalle());
        assertEquals(100, items.get(0).getPorcentaje());
        assertEquals("Cancha de Rugby", items.get(1).getEtiqueta());
        assertEquals("1 reserva · 43 % del total", items.get(1).getDetalle());
        assertEquals(75, items.get(1).getPorcentaje());
    }

    @Test
    void unaInstalacionSinIngresosApareceEnCero() {
        ingreso(LocalDate.of(2026, 10, 10), "20000", true, "Cancha de Fútbol 11", "Alumno UNSE");

        ReporteItem rugbySinIngresos = item(servicio.ingresosPorInstalacion(d, r30), "Cancha de Rugby");

        assertEquals("$ 0", rugbySinIngresos.getValor());
        assertEquals("0 reservas · 0 % del total", rugbySinIngresos.getDetalle());
    }

    @Test
    void lasMembresiasNoCuentanEnLosIngresosPorInstalacion() {
        ingreso(LocalDate.of(2026, 10, 1), "5000", false, null, "Alumno UNSE");

        assertEquals("$ 0", item(servicio.ingresosPorInstalacion(d, r30), "Cancha de Fútbol 11").getValor());
    }

    @Test
    void cadaTipoDeSocioAportaPorReservasYPorMembresias() {
        ingresosDeEjemplo();

        List<ReporteItem> items = servicio.ingresosPorCategoria(d, r30);

        ReporteItem a = items.get(0);
        assertEquals("Alumno UNSE", a.getEtiqueta());
        assertEquals("$ 25.000", a.getValor());
        assertEquals("Reservas $ 20.000 · Membresías $ 5.000 · 63 % del total", a.getDetalle());
        assertEquals(80, a.getPorcentaje());
        assertEquals(20, a.getPorcentaje2());
        assertEquals("Externo", items.get(1).getEtiqueta());
        assertEquals("Reservas $ 15.000 · Membresías $ 0 · 38 % del total", items.get(1).getDetalle());
    }

    // ---------- dinero en juego ----------

    private void creditoDeEjemplo(String monto, LocalDate limite) {
        Reserva r = reserva(id(), t1, LocalDate.of(2026, 10, 20), Reserva.Estado.CANCELADA);
        r.setIdPago(pagoConfirmado(monto));
        r.setFechaLimiteReprogramacion(limite);
    }

    @Test
    void elDineroPerdidoSonLosCreditosQueVencieronEnElPeriodoSinUsarse() {
        creditoDeEjemplo("20000", LocalDate.of(2026, 10, 5)); // venció dentro del período
        creditoDeEjemplo("10000", LocalDate.of(2026, 8, 20)); // venció en el período anterior

        ReporteKpi perdido = kpi(servicio.dineroEnJuego(d, r30), "Dinero perdido");

        assertEquals("$ 20.000", perdido.getValor());
        assertEquals("1 crédito venció sin usarse", perdido.getSub());
        assertEquals("+100 % vs. período anterior", perdido.getVariacion());
        assertEquals("off", perdido.getVariacionClase(), "perder más dinero es malo");
        assertTrue(perdido.isAlerta());
    }

    @Test
    void elDineroEnCreditosVigentesEsElQueTodaviaSePuedeReprogramar() {
        creditoDeEjemplo("15000", LocalDate.of(2026, 10, 20));
        creditoDeEjemplo("15000", HOY); // el último día todavía es vigente

        ReporteKpi retenido = kpi(servicio.dineroEnJuego(d, r30), "En créditos vigentes");

        assertEquals("$ 30.000", retenido.getValor());
        assertEquals("2 créditos por reprogramar; si vencen se pierde", retenido.getSub());
    }

    @DisplayName("CP-29: el crédito de una instalación en mantenimiento no cuenta como dinero perdido")
    @Test
    void cp29_unCreditoEnPausaNoSeCuentaComoPerdido() {
        cancha.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        cancha.setFechaInicioBaja(LocalDate.of(2026, 9, 20));
        creditoDeEjemplo("20000", LocalDate.of(2026, 10, 5)); // al empezar el mantenimiento le quedaban 15 días

        List<ReporteKpi> kpis = servicio.dineroEnJuego(d, r30);

        assertEquals("$ 0", kpi(kpis, "Dinero perdido").getValor());
        assertEquals("$ 20.000", kpi(kpis, "En créditos vigentes").getValor());
    }

    @Test
    void unaReservaCanceladaSinPagoConfirmadoNoGeneraDineroEnJuego() {
        Reserva sinPago = reserva(id(), t1, LocalDate.of(2026, 10, 20), Reserva.Estado.CANCELADA);
        sinPago.setFechaLimiteReprogramacion(LocalDate.of(2026, 10, 5));

        List<ReporteKpi> kpis = servicio.dineroEnJuego(d, r30);

        assertEquals("$ 0", kpi(kpis, "Dinero perdido").getValor());
        assertEquals("$ 0", kpi(kpis, "En créditos vigentes").getValor());
    }

    @Test
    void loPendienteDeValidarSumaComprobantesDeReservasYDeMembresias() {
        Reserva r = reserva(id(), t1, LocalDate.of(2026, 10, 20), Reserva.Estado.PENDIENTE_PAGO);
        r.setIdPago(DatosDePrueba.pago(id(), "20000", Pago.Estado.PENDIENTE_VALIDACION));
        SuscripcionSocio s = suscripcion(8, alumno, SuscripcionSocio.Estado.PENDIENTE_PAGO, null, null);
        s.setIdPago(DatosDePrueba.pago(id(), "5000", Pago.Estado.PENDIENTE_VALIDACION));

        ReporteKpi pendiente = kpi(servicio.dineroEnJuego(d, r30), "Pendiente de validar");

        assertEquals("$ 25.000", pendiente.getValor());
        assertEquals("2 comprobantes esperan al administrador", pendiente.getSub());
        assertTrue(pendiente.isAlerta());
    }

    @Test
    void laProyeccionCuentaLasQueVencenEnLosProximos30DiasConSuCuota() {
        suscripcion(8, alumno, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(20), HOY.plusDays(10));
        suscripcion(9, externo, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(1), HOY.plusDays(31));

        ReporteKpi proyeccion = kpi(servicio.dineroEnJuego(d, r30), "Proyección de membresías");

        assertEquals("próximos 30 días: vencen 1 ($ 5.000 si renuevan todas), según una renovación del 0 %",
                proyeccion.getSub());
    }

    // ---------- ocupación ----------

    private void nueveReservasConfirmadas() {
        LocalDate dia = LocalDate.of(2026, 9, 14);
        for (int i = 0; i < 3; i++) {
            reserva(id(), t1, dia.plusDays(i), Reserva.Estado.CONFIRMADA);
            reserva(id(), t2, dia.plusDays(i), Reserva.Estado.PENDIENTE_PAGO);
            reserva(id(), t3, dia.plusDays(i), Reserva.Estado.CONFIRMADA);
        }
    }

    @DisplayName("CP-42: la ocupación es reservados sobre disponibles (regla del servicio)")
    @Test
    void laOcupacionEsTurnosReservadosSobreTurnosDisponibles() {
        nueveReservasConfirmadas(); // 9 sobre (3 turnos por día x 30 días) = 90

        assertEquals(10.0, servicio.ocupacionGeneral(d, r30), 0.001);
    }

    @Test
    void lasCanceladasYRechazadasNoOcupanTurno() {
        nueveReservasConfirmadas();
        reserva(id(), t1, LocalDate.of(2026, 9, 20), Reserva.Estado.CANCELADA);
        reserva(id(), t2, LocalDate.of(2026, 9, 20), Reserva.Estado.RECHAZADA);

        assertEquals(10.0, servicio.ocupacionGeneral(d, r30), 0.001);
    }

    @Test
    void laOcupacionNoPasaDe100() {
        d.instalaciones = new ArrayList<>(List.of(cancha));
        d.turnos.put(1, List.of(t1));
        LocalDate dia = LocalDate.of(2026, 9, 14);
        for (int i = 0; i < 8; i++) { // 8 reservas del mismo turno en una semana con 7 turnos disponibles
            reserva(id(), t1, dia, Reserva.Estado.CONFIRMADA);
        }

        assertEquals(100.0, servicio.ocupacionGeneral(d, servicio.rango("7D", LocalDate.of(2026, 9, 14))), 0.001);
    }

    @Test
    void sinTurnosLaOcupacionEsCero() {
        d.instalaciones = new ArrayList<>();

        assertEquals(0.0, servicio.ocupacionGeneral(d, r30), 0.001);
    }

    @Test
    void losDiasDeMantenimientoSeDescuentanDeLosTurnosDisponibles() {
        rugby.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        rugby.setFechaInicioBaja(LocalDate.of(2026, 10, 3)); // 10 días en mantenimiento hasta hoy
        nueveReservasConfirmadas();

        List<ReporteKpi> kpis = servicio.ocupacionKpis(d, r30);

        assertEquals("de 80 disponibles", kpi(kpis, "Turnos reservados").getSub());
        assertEquals("10", kpi(kpis, "Días de mantenimiento").getValor());
    }

    @Test
    void elIngresoNoPercibidoEsLaTarifaPromedioPorLosTurnosSinOfrecer() {
        rugby.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        rugby.setFechaInicioBaja(LocalDate.of(2026, 10, 3));
        d.precioPromedio.put(2, new BigDecimal("12000"));

        ReporteKpi perdido = kpi(servicio.ocupacionKpis(d, r30), "Ingreso no percibido");

        assertEquals("$ 120.000", perdido.getValor(), "10 días x 1 turno x $ 12.000");
        assertTrue(perdido.isAlerta());
    }

    @Test
    void elIndicadorDeOcupacionSeComparaEnPuntosConElPeriodoAnterior() {
        nueveReservasConfirmadas();
        reserva(id(), t1, LocalDate.of(2026, 9, 12), Reserva.Estado.CONFIRMADA); // período anterior: 1 de 90

        ReporteKpi ocupacion = kpi(servicio.ocupacionKpis(d, r30), "Ocupación general");

        assertEquals("10 %", ocupacion.getValor());
        assertEquals("+9 puntos vs. período anterior", ocupacion.getVariacion());
    }

    @Test
    void cadaInstalacionSePintaVerdeDesdeEl50AmbarDesdeEl30YRojoPorDebajo() {
        Instalacion quincho = instalacion(3, "Quincho 1", Instalacion.Estado.HABILITADA);
        Turno tq = DatosDePrueba.turno(8, quincho, LocalTime.of(20, 0), LocalTime.of(23, 0));
        d.instalaciones.add(quincho);
        d.turnos.put(3, List.of(tq));
        LocalDate dia = LocalDate.of(2026, 9, 14);
        for (int i = 0; i < 9; i++) { // cancha: 18 de 60 = 30 %
            reserva(id(), t1, dia.plusDays(i), Reserva.Estado.CONFIRMADA);
            reserva(id(), t2, dia.plusDays(i), Reserva.Estado.CONFIRMADA);
        }
        for (int i = 0; i < 15; i++) { // rugby: 15 de 30 = 50 %
            reserva(id(), t3, dia.plusDays(i), Reserva.Estado.CONFIRMADA);
        }
        for (int i = 0; i < 3; i++) { // quincho: 3 de 30 = 10 %
            reserva(id(), tq, dia.plusDays(i), Reserva.Estado.CONFIRMADA);
        }

        List<ReporteItem> items = servicio.ocupacionPorInstalacion(d, r30);

        assertEquals("Quincho 1", items.get(0).getEtiqueta());
        assertEquals("off", items.get(0).getTono());
        assertEquals("Cancha de Fútbol 11", items.get(1).getEtiqueta());
        assertEquals("30 %", items.get(1).getValor());
        assertEquals("18 de 60 turnos", items.get(1).getDetalle());
        assertEquals("warn", items.get(1).getTono());
        assertEquals("Cancha de Rugby", items.get(2).getEtiqueta());
        assertEquals("ok", items.get(2).getTono());
    }

    @Test
    void unaInstalacionEnMantenimientoTodoElPeriodoNoTieneOcupacion() {
        cancha.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        cancha.setFechaInicioBaja(LocalDate.of(2026, 9, 1));

        ReporteItem i = item(servicio.ocupacionPorInstalacion(d, r30), "Cancha de Fútbol 11");

        assertEquals("—", i.getValor());
        assertEquals("En mantenimiento todo el período", i.getDetalle());
    }

    // ---------- horas pico, franjas vacías y mantenimiento ----------

    private void todosLosLunesALas18EnLaCancha() {
        for (String f : new String[]{"2026-09-14", "2026-09-21", "2026-09-28", "2026-10-05", "2026-10-12"}) {
            reserva(id(), t2, LocalDate.parse(f), Reserva.Estado.CONFIRMADA);
        }
    }

    @Test
    void laFranjaPicoEsLaDeMayorOcupacion() {
        todosLosLunesALas18EnLaCancha();

        List<ReporteItem> picos = servicio.franjas(d, r30, true);

        assertEquals(3, picos.size());
        assertEquals("lunes 18:00", picos.get(0).getEtiqueta());
        assertEquals("50 %", picos.get(0).getValor());
        assertEquals("5 de 10 turnos", picos.get(0).getDetalle());
        assertEquals("ok", picos.get(0).getTono());
    }

    @Test
    void laFranjaVaciaEsLaDeMenorOcupacion() {
        todosLosLunesALas18EnLaCancha();

        ReporteItem vacia = servicio.franjas(d, r30, false).get(0);

        assertEquals("0 %", vacia.getValor());
        assertEquals("off", vacia.getTono());
    }

    @Test
    void elMapaDeCalorMuestraLaOcupacionPorHoraYDiaDeLaSemana() {
        todosLosLunesALas18EnLaCancha();

        ReporteCalor mapa = servicio.mapaDeCalor(d, r30);

        assertEquals(List.of("lun", "mar", "mié", "jue", "vie", "sáb", "dom"), mapa.getDias());
        assertEquals("10:00", mapa.getFilas().get(0).getHora());
        assertEquals("18:00", mapa.getFilas().get(1).getHora());
        ReporteCalor.Celda lunes18 = mapa.getFilas().get(1).getCeldas().get(0);
        assertEquals("50 %", lunes18.getTexto());
        assertEquals(50, lunes18.getIntensidad());
        assertEquals("lun 18:00: 5 de 10 turnos", lunes18.getTitulo());
    }

    @Test
    void losTurnosSinReservasSonLosDeInstalacionesHabilitadasQueNadieReservo() {
        reserva(id(), t1, LocalDate.of(2026, 9, 20), Reserva.Estado.CONFIRMADA);
        rugby.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);

        List<ReporteItem> vacios = servicio.turnosSinReservas(d, r30);

        assertEquals(1, vacios.size());
        assertEquals("Cancha de Fútbol 11 · 18:00 – 19:00", vacios.get(0).getEtiqueta());
        assertEquals("en 30 días", vacios.get(0).getDetalle());
    }

    @Test
    void elMantenimientoMuestraLosDiasYElIngresoNoPercibidoEstimado() {
        rugby.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        rugby.setFechaInicioBaja(LocalDate.of(2026, 10, 3));
        rugby.setMotivoBaja("Cambio de césped");
        d.precioPromedio.put(2, new BigDecimal("12000"));

        List<ReporteItem> items = servicio.mantenimiento(d, r30);

        assertEquals(1, items.size());
        assertEquals("10 días", items.get(0).getValor());
        assertEquals("Ingreso no percibido estimado: $ 120.000 · Cambio de césped", items.get(0).getDetalle());
    }

    // ---------- membresías ----------

    @Test
    void cuentaLasSuscripcionesPorEstadoConLaMayorAl100() {
        suscripcion(7, alumno, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(5), HOY.plusDays(25));
        suscripcion(8, alumno, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(6), HOY.plusDays(24));
        suscripcion(9, alumno, SuscripcionSocio.Estado.VENCIDA, HOY.minusDays(40), HOY.minusDays(10));
        suscripcion(10, externo, SuscripcionSocio.Estado.CANCELADA, null, null);

        List<ReporteItem> items = servicio.estadoSuscripciones(d);

        assertEquals("Vigentes", items.get(0).getEtiqueta());
        assertEquals("2", items.get(0).getValor());
        assertEquals(100, items.get(0).getPorcentaje());
        assertEquals("1", items.get(1).getValor());
        assertEquals(50, items.get(1).getPorcentaje());
        assertEquals("0", items.get(3).getValor());
    }

    @Test
    void losVencimientosSeAgrupanPorSemanasDeLosProximos28Dias() {
        suscripcion(7, alumno, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(24), HOY.plusDays(6)); // semana 0
        suscripcion(8, externo, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(23), HOY.plusDays(7)); // semana 1
        suscripcion(9, alumno, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(3), HOY.plusDays(27)); // semana 3
        suscripcion(10, alumno, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(2), HOY.plusDays(28)); // fuera
        suscripcion(11, alumno, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(31), HOY.minusDays(1)); // ya venció

        List<ReporteItem> semanas = servicio.vencimientosSemanales(d);

        assertEquals("12/10 al 18/10", semanas.get(0).getEtiqueta());
        assertEquals("1", semanas.get(0).getValor());
        assertEquals("$ 5.000 si renuevan todas", semanas.get(0).getDetalle());
        assertEquals("warn", semanas.get(0).getTono());
        assertEquals("1", semanas.get(1).getValor());
        assertEquals("$ 8.000 si renuevan todas", semanas.get(1).getDetalle());
        assertEquals("0", semanas.get(2).getValor());
        assertEquals("1", semanas.get(3).getValor());
    }

    private void dosSociosQueTerminaronUnaMembresia(LocalDate inicioRenovacionDelSocio7, LocalDate inicioRenovacionDelSocio8) {
        suscripcion(7, alumno, SuscripcionSocio.Estado.VENCIDA, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1));
        suscripcion(7, alumno, SuscripcionSocio.Estado.VIGENTE, inicioRenovacionDelSocio7, HOY.plusDays(20));
        suscripcion(8, alumno, SuscripcionSocio.Estado.VENCIDA, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1));
        suscripcion(8, alumno, SuscripcionSocio.Estado.VIGENTE, inicioRenovacionDelSocio8, HOY.plusDays(20));
    }

    @Test
    void unaRenovacionHastaQuinceDiasTardeTodaviaCuentaComoRenovacion() {
        // vencieron el 01/09: el socio 7 renovó el 16/09 (15 días después) y el 8 el 17/09 (16 días)
        dosSociosQueTerminaronUnaMembresia(LocalDate.of(2026, 9, 16), LocalDate.of(2026, 9, 17));

        ReporteKpi tasa = kpi(servicio.membresiasKpis(d, servicio.rango("90D", HOY)), "Tasa de renovación");

        assertEquals("50 %", tasa.getValor());
        assertEquals("1 de 2 membresías que terminaron", tasa.getSub());
        assertTrue(tasa.isAlerta(), "menos del 60 % de renovación");
    }

    @Test
    void siRenovaronTodasLaTasaEsDel100SinAlerta() {
        dosSociosQueTerminaronUnaMembresia(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 10));

        ReporteKpi tasa = kpi(servicio.membresiasKpis(d, servicio.rango("90D", HOY)), "Tasa de renovación");

        assertEquals("100 %", tasa.getValor());
        assertFalse(tasa.isAlerta());
    }

    @Test
    void sinMembresiasTerminadasLaTasaNoSeCalcula() {
        ReporteKpi tasa = kpi(servicio.membresiasKpis(d, r30), "Tasa de renovación");

        assertEquals("—", tasa.getValor());
    }

    @Test
    void loCobradoPorMembresiasSeAgrupaPorTipoDeSocio() {
        ingreso(LocalDate.of(2026, 10, 1), "5000", false, null, "Alumno UNSE");
        ingreso(LocalDate.of(2026, 10, 2), "5000", false, null, "Alumno UNSE");
        ingreso(LocalDate.of(2026, 10, 3), "8000", false, null, "Externo");
        ingreso(LocalDate.of(2026, 10, 3), "20000", true, "Cancha de Rugby", "Externo"); // no es membresía

        List<ReporteItem> items = servicio.membresiasPorCategoria(d, r30);

        assertEquals("Alumno UNSE", items.get(0).getEtiqueta());
        assertEquals("$ 10.000", items.get(0).getValor());
        assertEquals("2 cuotas · cuota actual $ 5.000 · 56 % del total", items.get(0).getDetalle());
        assertEquals("Externo", items.get(1).getEtiqueta());
        assertEquals("1 cuota · cuota actual $ 8.000 · 44 % del total", items.get(1).getDetalle());
    }

    @Test
    void laValidacionEsElTiempoMedioEntreQueElSocioPagaYElAdministradorValida() {
        d.ingresos.add(new ReportesServicio.Ingreso(LocalDateTime.of(2026, 10, 10, 9, 30),
                LocalDateTime.of(2026, 10, 10, 10, 0), new BigDecimal("20000"), true, "Cancha de Fútbol 11", "Alumno UNSE"));
        d.ingresos.add(new ReportesServicio.Ingreso(LocalDateTime.of(2026, 10, 9, 8, 30),
                LocalDateTime.of(2026, 10, 9, 10, 0), new BigDecimal("15000"), true, "Cancha de Rugby", "Externo"));
        d.ingresos.add(new ReportesServicio.Ingreso(LocalDateTime.of(2026, 10, 1, 8, 0),
                LocalDateTime.of(2026, 10, 1, 10, 0), new BigDecimal("5000"), false, null, "Alumno UNSE"));

        List<ReporteItem> items = servicio.validacion(d, r30);

        assertEquals("1 h", items.get(0).getValor(), "(30 + 90) / 2 = 60 minutos");
        assertEquals("2 h", items.get(1).getValor());
        assertEquals(50, items.get(0).getPorcentaje());
        assertEquals(100, items.get(1).getPorcentaje());
    }

    @ParameterizedTest
    @CsvSource({"0,menos de 1 min", "0.4,menos de 1 min", "1,1 min", "59,59 min", "60,1 h", "90,1 h 30 min",
        "1439,23 h 59 min", "1440,1 d", "1500,1 d 1 h"})
    void laDuracionSeEscribeEnMinutosHorasODias(double minutos, String esperado) {
        assertEquals(esperado, ReportesServicio.duracion(minutos));
    }

    // ---------- resumen ----------

    @DisplayName("CP-41: el resumen de reportes dice \"Grado de satisfacción\"")
    @Test
    void elResumenTieneLasOchoCifrasEnSuOrden() {
        List<ReporteKpi> kpis = servicio.resumen(d, r30);

        List<String> etiquetas = new ArrayList<>();
        for (ReporteKpi k : kpis) {
            etiquetas.add(k.getEtiqueta());
        }
        assertEquals(List.of("Ingresos totales", "Reservas realizadas", "Ocupación", "Ticket promedio", "Socios activos",
                "Renovación de membresías", "Grado de satisfacción", "Cancelaciones"), etiquetas);
    }

    @Test
    void elResumenCuentaLasConfirmadasYElPorcentajeDeCancelaciones() {
        LocalDate dia = LocalDate.of(2026, 9, 20);
        reserva(id(), t1, dia, Reserva.Estado.CONFIRMADA);
        reserva(id(), t2, dia, Reserva.Estado.CONFIRMADA);
        reserva(id(), t3, dia, Reserva.Estado.CANCELADA);
        reserva(id(), t1, dia.plusDays(1), Reserva.Estado.RECHAZADA); // no cuenta como reserva del período

        List<ReporteKpi> kpis = servicio.resumen(d, r30);

        assertEquals("2", kpi(kpis, "Reservas realizadas").getValor());
        ReporteKpi cancelaciones = kpi(kpis, "Cancelaciones");
        assertEquals("1", cancelaciones.getValor());
        assertEquals("33 % de las reservas del período", cancelaciones.getSub());
    }

    @Test
    void laValoracionPromedioDelResumenSoloCuentaLasDelPeriodo() {
        d.valoraciones.add(valoracion("Cancha de Fútbol 11", 5, LocalDateTime.of(2026, 10, 1, 10, 0)));
        d.valoraciones.add(valoracion("Cancha de Fútbol 11", 4, LocalDateTime.of(2026, 9, 20, 10, 0)));
        d.valoraciones.add(valoracion("Cancha de Fútbol 11", 1, LocalDateTime.of(2026, 7, 20, 10, 0))); // fuera

        ReporteKpi v = kpi(servicio.resumen(d, r30), "Grado de satisfacción");

        assertEquals("4,5", v.getValor());
        assertFalse(v.isAlerta());
    }

    @Test
    void unaValoracionPromedioPorDebajoDe3Coma5SeMarcaComoAlerta() {
        d.valoraciones.add(valoracion("Cancha de Rugby", 3, LocalDateTime.of(2026, 10, 1, 10, 0)));
        d.valoraciones.add(valoracion("Cancha de Rugby", 3, LocalDateTime.of(2026, 10, 2, 10, 0)));

        assertTrue(kpi(servicio.resumen(d, r30), "Grado de satisfacción").isAlerta());
    }

    private static ValoracionFila valoracion(String instalacion, int puntaje, LocalDateTime fecha) {
        return new ValoracionFila(fecha, "", "Lucía Gómez", "Alumno UNSE", instalacion, "Fútbol", "sports_soccer",
                "turno", puntaje, "");
    }

    // ---------- alertas ----------

    private List<ReporteAlerta> alertasSinFranjasVacias() {
        d.instalaciones = new ArrayList<>(); // sin instalaciones no hay franjas vacías ni mantenimiento
        return servicio.alertas(d, r30);
    }

    @Test
    void sinNadaQueAtenderUnaSolaAlertaDiceQueEstaTodoEnOrden() {
        List<ReporteAlerta> alertas = alertasSinFranjasVacias();

        assertEquals(1, alertas.size());
        assertEquals("Todo en orden: no hay nada que requiera atención.", alertas.get(0).getTexto());
        assertEquals("ok", alertas.get(0).getClase());
    }

    @Test
    void avisaLasMembresiasQueVencenEnLosProximos7DiasYNoLasDe8() {
        suscripcion(7, alumno, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(23), HOY.plusDays(7));
        suscripcion(8, externo, SuscripcionSocio.Estado.VIGENTE, HOY.minusDays(22), HOY.plusDays(8));

        List<ReporteAlerta> alertas = alertasSinFranjasVacias();

        assertEquals("1 membresía vence en los próximos 7 días ($ 5.000 si renuevan)", alertas.get(0).getTexto());
    }

    @Test
    void avisaLosComprobantesQueEsperanValidacion() {
        Reserva r = reserva(id(), t1, HOY.plusDays(5), Reserva.Estado.PENDIENTE_PAGO);
        r.setIdPago(DatosDePrueba.pago(id(), "20000", Pago.Estado.PENDIENTE_VALIDACION));
        SuscripcionSocio s = suscripcion(8, alumno, SuscripcionSocio.Estado.PENDIENTE_PAGO, null, null);
        s.setIdPago(DatosDePrueba.pago(id(), "5000", Pago.Estado.PENDIENTE_VALIDACION));

        List<ReporteAlerta> alertas = alertasSinFranjasVacias();

        assertEquals("2 comprobantes esperan validación del administrador", alertas.get(0).getTexto());
    }

    @Test
    void avisaLasInstalacionesConValoracionPromedioPorDebajoDe3Coma5() {
        d.valoraciones.add(valoracion("Cancha de Rugby", 3, LocalDateTime.of(2026, 10, 1, 10, 0)));
        d.valoraciones.add(valoracion("Cancha de Rugby", 3, LocalDateTime.of(2026, 10, 2, 10, 0)));
        d.valoraciones.add(valoracion("Cancha de Fútbol 11", 3, LocalDateTime.of(2026, 10, 1, 10, 0)));
        d.valoraciones.add(valoracion("Cancha de Fútbol 11", 4, LocalDateTime.of(2026, 10, 2, 10, 0))); // 3,5: no es baja

        List<ReporteAlerta> alertas = alertasSinFranjasVacias();

        assertEquals("Grado de satisfacción bajo: Cancha de Rugby (3,0)", alertas.get(0).getTexto());
        assertEquals("off", alertas.get(0).getClase());
    }

    @Test
    void avisaLasFranjasSinNingunaReservaEnElPeriodo() {
        d.instalaciones = new ArrayList<>(List.of(cancha));
        d.turnos.put(1, List.of(t1));

        List<ReporteAlerta> alertas = servicio.alertas(d, r30);

        assertEquals("1 turno no tuvo ninguna reserva en el período (franjas vacías)", alertas.get(0).getTexto());
    }

    @Test
    void avisaLosCreditosQueVencenEnLosProximos7DiasYNoLosDe8() {
        creditoDeEjemplo("20000", HOY.plusDays(7));
        creditoDeEjemplo("30000", HOY.plusDays(8));

        List<ReporteAlerta> alertas = alertasSinFranjasVacias();

        assertEquals("1 crédito de reprogramación vence en 7 días ($ 20.000 que se perderían)", alertas.get(0).getTexto());
    }

    @Test
    void avisaLasInstalacionesEnMantenimientoConSuFechaDeReactivacion() {
        rugby.setEstado(Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        rugby.setFechaFinBaja(LocalDate.of(2026, 10, 20));
        d.instalaciones = new ArrayList<>(List.of(rugby));
        d.turnos.put(2, List.of());

        List<ReporteAlerta> alertas = servicio.alertas(d, r30);

        assertEquals("Cancha de Rugby está en mantenimiento hasta el 20/10/2026", alertas.get(0).getTexto());
    }
}
