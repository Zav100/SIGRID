package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dto.Aviso;
import com.sigrid.sigrid.dto.PerfilSocio;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.SolicitudCambioReserva;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Avisos y perfil del panel del socio: membresía vencida o por vencer (7 días), valoración pendiente, estado de cada
 * reserva y respuesta del administrador a sus pedidos.
 */
class PanelSocioServicioAvisosTest extends PanelSocioServicioPruebaBase {

    @BeforeEach
    void completarElAltaDelSocio() {
        socio.getIdUsuario().setFechaAlta(LocalDateTime.of(2026, 3, 15, 9, 0));
    }

    private static PerfilSocio perfil(boolean vigente, long diasRestantes, String vence) {
        return new PerfilSocio(7, "Lucía Gómez", "Lucía", "LG", "Alumno UNSE", "#00007", "03/2026", vigente,
                diasRestantes, vence, 50);
    }

    private List<Aviso> avisos(PerfilSocio perfil) {
        return servicio.avisos(socio, perfil, AHORA);
    }

    private static Aviso conTitulo(List<Aviso> avisos, String titulo) {
        for (Aviso a : avisos) {
            if (a.getTitulo().equals(titulo)) {
                return a;
            }
        }
        throw new AssertionError("no hay un aviso \"" + titulo + "\": " + titulos(avisos));
    }

    private static List<String> titulos(List<Aviso> avisos) {
        List<String> t = new ArrayList<>();
        for (Aviso a : avisos) {
            t.add(a.getTitulo());
        }
        return t;
    }

    private Reserva reserva(int id, LocalDate fecha, Reserva.Estado estado) {
        Reserva r = DatosDePrueba.reserva(id, socio, turno, fecha, estado);
        r.setIdTarifa(tarifa);
        return r;
    }

    // ---------- membresía ----------

    @Test
    void avisaQueLaMembresiaEstaVencidaYQueNoSePuedeReservar() {
        List<Aviso> a = avisos(perfil(false, -3, "09/10/2026"));

        Aviso aviso = a.get(0);
        assertEquals("Tu membresía está vencida", aviso.getTitulo());
        assertEquals("off", aviso.getClase());
        assertEquals(0, aviso.getOrden());
        assertTrue(aviso.getTexto().startsWith("Venció el 09/10/2026. "));
    }

    @Test
    void unSocioQueNuncaTuvoMembresiaRecibeElAvisoDeQueTodaviaNoLaTiene() {
        List<Aviso> a = avisos(perfil(false, 0, null));

        assertTrue(a.get(0).getTexto().startsWith("Todavía no tenés una membresía vigente. "));
    }

    @ParameterizedTest
    @CsvSource({"0,Tu membresía vence hoy", "1,Tu membresía vence mañana", "2,Tu membresía vence en 2 días",
        "7,Tu membresía vence en 7 días"})
    void avisaCuandoLaMembresiaVenceEnSieteDiasOMenos(long dias, String titulo) {
        List<Aviso> a = avisos(perfil(true, dias, "19/10/2026"));

        Aviso aviso = a.get(0);
        assertEquals(titulo, aviso.getTitulo());
        assertEquals("warn", aviso.getClase());
        assertEquals(1, aviso.getOrden());
    }

    @Test
    void noAvisaSiLaMembresiaVenceEnOchoDias() {
        assertTrue(avisos(perfil(true, 8, "20/10/2026")).isEmpty());
    }

    // ---------- valoración pendiente ----------

    @Test
    void avisaDeLaValoracionPendienteDeUnTurnoTerminado() {
        when(reservaDAO.listarPorSocio(7))
                .thenReturn(List.of(reserva(20, HOY.minusDays(2), Reserva.Estado.CONFIRMADA)));

        Aviso aviso = conTitulo(avisos(perfil(true, 20, "01/11/2026")), "Valoración pendiente");

        assertTrue(aviso.getTexto().endsWith("Valorala para poder hacer nuevas reservas."));
        assertEquals("warn", aviso.getClase());
    }

    // ---------- reservas ----------

    @Test
    void sinComprobanteAvisaHastaQueHoraTieneParaSubirlo() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.PENDIENTE_PAGO);
        r.setFechaReserva(AHORA.minusMinutes(10));
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(r));

        Aviso aviso = conTitulo(avisos(perfil(true, 20, "01/11/2026")), "Subí el comprobante de tu reserva");

        assertTrue(aviso.getTexto().contains("Tenés hasta las 10:50 o el turno se libera."));
        assertEquals("hace 10 min", aviso.getCuando());
        assertEquals(2, aviso.getOrden());
    }

    @Test
    void conComprobanteAvisaQueSeEstaRevisando() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.PENDIENTE_PAGO);
        Pago pago = DatosDePrueba.pago(1, "20000", Pago.Estado.PENDIENTE_VALIDACION);
        pago.setFechaPago(AHORA.minusHours(2));
        r.setIdPago(pago);
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(r));

        Aviso aviso = conTitulo(avisos(perfil(true, 20, "01/11/2026")), "Estamos revisando tu comprobante");

        assertEquals("muted", aviso.getClase());
        assertEquals("hace 2 h", aviso.getCuando());
        assertEquals(8, aviso.getOrden());
    }

    @Test
    void unTurnoConfirmadoDeMananaAvisaQueEsManana() {
        Reserva r = reserva(10, HOY.plusDays(1), Reserva.Estado.CONFIRMADA);
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(r));

        Aviso aviso = conTitulo(avisos(perfil(true, 20, "01/11/2026")), "Tu turno es mañana");

        assertEquals("ok", aviso.getClase());
        assertTrue(aviso.getTexto().endsWith("Llevá tu carnet digital."));
    }

    @Test
    void unaConfirmadaLejanaYRecienteAvisaQueSeConfirmo() {
        Reserva r = reserva(10, HOY.plusDays(10), Reserva.Estado.CONFIRMADA);
        r.setFechaConfirmacion(AHORA.minusDays(2));
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(r));

        Aviso aviso = conTitulo(avisos(perfil(true, 20, "01/11/2026")), "Reserva confirmada");

        assertEquals("hace 2 días", aviso.getCuando());
        assertEquals(7, aviso.getOrden());
    }

    @Test
    void unaConfirmadaLejanaConfirmadaHaceMasDeUnaSemanaYaNoAvisa() {
        Reserva r = reserva(10, HOY.plusDays(10), Reserva.Estado.CONFIRMADA);
        r.setFechaConfirmacion(AHORA.minusDays(8));
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(r));

        assertTrue(avisos(perfil(true, 20, "01/11/2026")).isEmpty());
    }

    @Test
    void unaRechazadaRecienteAvisaConElMotivo() {
        Reserva r = reserva(10, HOY.plusDays(3), Reserva.Estado.RECHAZADA);
        Pago pago = DatosDePrueba.pago(1, "20000", Pago.Estado.RECHAZADO);
        pago.setMotivoRechazo("El monto no coincide");
        pago.setFechaValidacion(AHORA.minusHours(1));
        r.setIdPago(pago);
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(r));

        Aviso aviso = conTitulo(avisos(perfil(true, 20, "01/11/2026")), "Reserva rechazada");

        assertTrue(aviso.getTexto().contains("Motivo: El monto no coincide. El turno quedó libre."));
        assertEquals(3, aviso.getOrden());
    }

    @Test
    void unaRechazadaDeHaceMasDeDosSemanasYaNoAvisa() {
        Reserva r = reserva(10, HOY.minusDays(14), Reserva.Estado.RECHAZADA);
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(r));

        assertTrue(avisos(perfil(true, 20, "01/11/2026")).isEmpty());
    }

    @Test
    void unaCanceladaConCreditoVigenteAvisaHastaCuandoPuedeUsarlo() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.CANCELADA);
        r.setFechaLimiteReprogramacion(HOY.plusDays(10));
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(r));

        Aviso aviso = conTitulo(avisos(perfil(true, 20, "01/11/2026")), "Tenés un crédito para reprogramar");

        assertTrue(aviso.getTexto().endsWith("Podés usarlo hasta el 22/10/2026."));
        assertEquals(4, aviso.getOrden());
    }

    // ---------- respuesta a los pedidos ----------

    private SolicitudCambioReserva pedido(Reserva r, SolicitudCambioReserva.Tipo tipo,
            SolicitudCambioReserva.Estado estado) {
        SolicitudCambioReserva c = new SolicitudCambioReserva();
        c.setIdSolicitud(50);
        c.setIdReserva(r);
        c.setTipo(tipo);
        c.setEstado(estado);
        c.setFechaSolicitud(AHORA.minusHours(3));
        return c;
    }

    @Test
    void unPedidoDeCancelacionSinResponderAvisaQueEstaEnRevision() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.CONFIRMADA);
        when(solicitudDAO.listarDelSocio(7)).thenReturn(List.of(
                pedido(r, SolicitudCambioReserva.Tipo.CANCELACION, SolicitudCambioReserva.Estado.PENDIENTE)));

        Aviso aviso = conTitulo(avisos(perfil(true, 20, "01/11/2026")), "Pedido de cancelación en revisión");

        assertEquals("hace 3 h", aviso.getCuando());
        assertEquals(8, aviso.getOrden());
    }

    @Test
    void unaCancelacionAprobadaAvisaDelCreditoDe30Dias() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.CANCELADA);
        SolicitudCambioReserva c = pedido(r, SolicitudCambioReserva.Tipo.CANCELACION,
                SolicitudCambioReserva.Estado.APROBADA);
        c.setFechaResolucion(AHORA.minusDays(1));
        when(solicitudDAO.listarDelSocio(7)).thenReturn(List.of(c));

        Aviso aviso = conTitulo(avisos(perfil(true, 20, "01/11/2026")), "Cancelación aprobada");

        assertTrue(aviso.getTexto().endsWith("Quedó un crédito de 30 días para reprogramar."));
        assertEquals("ok", aviso.getClase());
    }

    @Test
    void unaCancelacionRechazadaAvisaElMotivoYQueLaReservaSigueEnPie() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.CONFIRMADA);
        SolicitudCambioReserva c = pedido(r, SolicitudCambioReserva.Tipo.CANCELACION,
                SolicitudCambioReserva.Estado.RECHAZADA);
        c.setFechaResolucion(AHORA.minusDays(1));
        c.setMotivoRechazo("Faltan menos de 48 horas");
        when(solicitudDAO.listarDelSocio(7)).thenReturn(List.of(c));

        Aviso aviso = conTitulo(avisos(perfil(true, 20, "01/11/2026")), "Cancelación rechazada");

        assertTrue(aviso.getTexto().endsWith("Motivo: Faltan menos de 48 horas"));
        assertEquals("off", aviso.getClase());
    }

    @Test
    void unaRespuestaDeHaceMasDeDosSemanasYaNoAvisa() {
        Reserva r = reserva(10, FECHA_TURNO, Reserva.Estado.CONFIRMADA);
        SolicitudCambioReserva c = pedido(r, SolicitudCambioReserva.Tipo.CANCELACION,
                SolicitudCambioReserva.Estado.RECHAZADA);
        c.setFechaResolucion(AHORA.minusDays(14));
        when(solicitudDAO.listarDelSocio(7)).thenReturn(List.of(c));

        assertFalse(titulos(avisos(perfil(true, 20, "01/11/2026"))).contains("Cancelación rechazada"));
    }

    // ---------- orden ----------

    @Test
    void ordenaLosAvisosDelMasAlMenosImportante() {
        Reserva sinComprobante = reserva(10, FECHA_TURNO, Reserva.Estado.PENDIENTE_PAGO);
        sinComprobante.setFechaReserva(AHORA.minusMinutes(5));
        Reserva enRevision = reserva(11, FECHA_TURNO.plusDays(1), Reserva.Estado.PENDIENTE_PAGO);
        enRevision.setIdPago(DatosDePrueba.pago(1, "20000", Pago.Estado.PENDIENTE_VALIDACION));
        when(reservaDAO.listarPorSocio(7)).thenReturn(List.of(enRevision, sinComprobante));

        List<Aviso> a = avisos(perfil(false, -1, "11/10/2026"));

        assertEquals(List.of("Tu membresía está vencida", "Subí el comprobante de tu reserva",
                "Estamos revisando tu comprobante"), titulos(a));
    }

    // ---------- perfil ----------

    private SuscripcionSocio vigente(LocalDate inicio, LocalDate vence) {
        SuscripcionSocio s = new SuscripcionSocio(30);
        s.setIdSocio(socio);
        s.setEstado(SuscripcionSocio.Estado.VIGENTE);
        s.setFechaInicio(inicio);
        s.setFechaVencimiento(vence);
        return s;
    }

    @Test
    void elPerfilDeUnSocioVigenteCalculaDiasRestantesYProgreso() {
        LocalDate hoy = LocalDate.of(2026, 10, 8);
        when(suscripcionDAO.listarVigentesDelSocio(7, hoy))
                .thenReturn(List.of(vigente(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 23))));

        PerfilSocio p = servicio.perfil(socio, hoy);

        assertTrue(p.isVigente());
        assertEquals("23/10/2026", p.getVence());
        assertEquals(15, p.getDiasRestantes());
        assertEquals(50, p.getProgreso());
    }

    @Test
    void elProgresoLlegaA100ElDiaDelVencimiento() {
        LocalDate hoy = LocalDate.of(2026, 10, 23);
        when(suscripcionDAO.listarVigentesDelSocio(7, hoy))
                .thenReturn(List.of(vigente(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 23))));

        PerfilSocio p = servicio.perfil(socio, hoy);

        assertEquals(0, p.getDiasRestantes());
        assertEquals(100, p.getProgreso());
    }

    @Test
    void elProgresoNuncaEsNegativoSiLaMembresiaTodaviaNoEmpezo() {
        LocalDate hoy = LocalDate.of(2026, 9, 1);
        when(suscripcionDAO.listarVigentesDelSocio(7, hoy))
                .thenReturn(List.of(vigente(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 23))));

        assertEquals(0, servicio.perfil(socio, hoy).getProgreso());
    }

    @Test
    void elPerfilDeUnSocioSinMembresiaMuestraLaUltimaVencida() {
        socio.setEstado(Socio.Estado.NO_ACTIVO);
        SuscripcionSocio antigua = vigente(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1));
        antigua.setEstado(SuscripcionSocio.Estado.VENCIDA);
        SuscripcionSocio ultima = vigente(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1));
        ultima.setEstado(SuscripcionSocio.Estado.VENCIDA);
        SuscripcionSocio cancelada = vigente(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 11, 2));
        cancelada.setEstado(SuscripcionSocio.Estado.CANCELADA);
        when(suscripcionDAO.listarPorSocio(7)).thenReturn(List.of(cancelada, ultima, antigua));

        PerfilSocio p = servicio.perfil(socio, HOY);

        assertFalse(p.isVigente());
        assertEquals("01/10/2026", p.getVence());
        assertEquals(-11, p.getDiasRestantes());
        assertEquals(0, p.getProgreso());
    }

    @Test
    void elPerfilUsaLaPrimeraPalabraDelNombreComoNombreDePila() {
        socio.getIdUsuario().setNombre("Lucía Belén");
        when(suscripcionDAO.listarVigentesDelSocio(7, HOY)).thenReturn(List.of());

        PerfilSocio p = servicio.perfil(socio, HOY);

        assertEquals("Lucía", p.getNombrePila());
        assertEquals("#00007", p.getNumero());
        assertEquals("03/2026", p.getDesde());
    }

    @Test
    void sinNombreElNombreDePilaEsLaParteLocalDelCorreo() {
        socio.getIdUsuario().setNombre(null);
        when(suscripcionDAO.listarVigentesDelSocio(7, HOY)).thenReturn(List.of());

        assertEquals("lucia.gomez", servicio.perfil(socio, HOY).getNombrePila());
    }

    @Test
    void unVigenteSinSuscripcionVigenteNoTieneFechaDeVencimiento() {
        when(suscripcionDAO.listarVigentesDelSocio(7, HOY)).thenReturn(List.of());

        PerfilSocio p = servicio.perfil(socio, HOY);

        assertNull(p.getVence());
        assertEquals(0, p.getDiasRestantes());
    }
}
