package com.sigrid.sigrid.servicio;

import static com.sigrid.sigrid.servicio.DatosDePrueba.AHORA;
import static com.sigrid.sigrid.servicio.DatosDePrueba.HOY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dto.DiaSocio;
import com.sigrid.sigrid.dto.InstalacionReservable;
import com.sigrid.sigrid.dto.MiReserva;
import com.sigrid.sigrid.dto.TurnoDisponible;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.TarifaMembresia;
import com.sigrid.sigrid.repositorio.Turno;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Consultas del panel del socio al reservar: instalaciones con su precio, franjas libres u ocupadas de un día (con la
 * regla de 48 h), días reservables (del tercer día hasta 30 días adelante) y cuota mensual.
 */
class PanelSocioServicioConsultasTest extends PanelSocioServicioPruebaBase {

    // ---------- socio de la sesión ----------

    @Test
    void buscaElSocioDeUnaCuentaActiva() {
        when(socioDAO.buscarPorIdUsuario(107)).thenReturn(socio);

        assertSame(socio, servicio.buscarSocio(107));
    }

    @Test
    void unaCuentaDeUnSocioDadoDeBajaNoTieneSocio() {
        Socio baja = DatosDePrueba.socio(8, Socio.Estado.NO_ACTIVO, alumno);
        baja.setBajaLogica(true);
        when(socioDAO.buscarPorIdUsuario(108)).thenReturn(baja);

        assertNull(servicio.buscarSocio(108));
    }

    @Test
    void unaCuentaQueNoEsDeUnSocioNoTieneSocio() {
        when(socioDAO.buscarPorIdUsuario(999)).thenReturn(null);

        assertNull(servicio.buscarSocio(999));
    }

    @Test
    void sinIdDeUsuarioNoHaySocio() {
        assertNull(servicio.buscarSocio(null));
    }

    // ---------- instalaciones reservables ----------

    private Instalacion instalacion(int id, String nombre, Instalacion.Estado estado) {
        Instalacion i = DatosDePrueba.instalacion(id, nombre, Instalacion.TipoAcceso.ARANCELADO, estado);
        i.setCapacidad(22);
        return i;
    }

    @Test
    void unaInstalacionHabilitadaConPrecioSePuedeReservar() {
        Instalacion c = instalacion(1, "Cancha de Fútbol 11", Instalacion.Estado.HABILITADA);
        when(instalacionDAO.listarReservables()).thenReturn(List.of(c));
        when(tarifaDAO.buscarVigente(1, 1, HOY)).thenReturn(tarifa);

        InstalacionReservable r = servicio.instalaciones(socio, HOY).get(0);

        assertTrue(r.isHabilitada());
        assertEquals("$ 20.000", r.getPrecio());
        assertEquals("Hasta 22 personas", r.getCapacidad());
        assertNull(r.getAviso());
    }

    @Test
    void unaInstalacionEnMantenimientoMuestraHastaCuando() {
        Instalacion c = instalacion(1, "Cancha de Fútbol 11", Instalacion.Estado.DESHABILITADA_MANTENIMIENTO);
        c.setFechaFinBaja(LocalDate.of(2026, 10, 20));
        when(instalacionDAO.listarReservables()).thenReturn(List.of(c));

        InstalacionReservable r = servicio.instalaciones(socio, HOY).get(0);

        assertFalse(r.isHabilitada());
        assertEquals("En mantenimiento hasta el 20/10/2026", r.getAviso());
    }

    @Test
    void unaInstalacionDadaDeBajaNoEstaDisponible() {
        Instalacion c = instalacion(1, "Cancha de Fútbol 11", Instalacion.Estado.DESHABILITADA);
        when(instalacionDAO.listarReservables()).thenReturn(List.of(c));

        InstalacionReservable r = servicio.instalaciones(socio, HOY).get(0);

        assertFalse(r.isHabilitada());
        assertEquals("No disponible", r.getAviso());
    }

    @Test
    void unaInstalacionSinPrecioParaElTipoDeSocioNoSePuedeReservar() {
        Instalacion c = instalacion(1, "Cancha de Fútbol 11", Instalacion.Estado.HABILITADA);
        when(instalacionDAO.listarReservables()).thenReturn(List.of(c));
        when(tarifaDAO.buscarVigente(1, 1, HOY)).thenReturn(null);

        InstalacionReservable r = servicio.instalaciones(socio, HOY).get(0);

        assertFalse(r.isHabilitada());
        assertEquals("Sin precio cargado", r.getAviso());
    }

    // ---------- franjas de un día ----------

    private Turno franja(int id, int hora) {
        return DatosDePrueba.turno(id, cancha, LocalTime.of(hora, 0), LocalTime.of(hora + 1, 0));
    }

    private Reserva ocupada(Turno t, LocalDate fecha, Socio quien) {
        return DatosDePrueba.reserva(99, quien, t, fecha, Reserva.Estado.CONFIRMADA);
    }

    private TurnoDisponible de(List<TurnoDisponible> lista, int idTurno) {
        for (TurnoDisponible t : lista) {
            if (t.getIdTurno() == idTurno) {
                return t;
            }
        }
        throw new AssertionError("falta la franja " + idTurno);
    }

    @Test
    void unaFranjaQueEmpezaEn48HorasExactasEstaLibre() {
        Turno t = franja(5, 10);
        when(turnoDAO.listarPorInstalacion(1)).thenReturn(List.of(t));

        TurnoDisponible d = servicio.turnos(7, 1, FECHA_TURNO, AHORA).get(0);

        assertTrue(d.isLibre());
        assertNull(d.getMotivo());
        assertEquals("10:00 – 11:00", d.getHorario());
    }

    @Test
    void unaFranjaQueEmpezaEn46HorasNoSePuedeElegir() {
        Turno t = franja(4, 8);
        when(turnoDAO.listarPorInstalacion(1)).thenReturn(List.of(t));

        TurnoDisponible d = servicio.turnos(7, 1, FECHA_TURNO, AHORA).get(0);

        assertFalse(d.isLibre());
        assertEquals("Menos de 48 h", d.getMotivo());
    }

    @Test
    void unaFranjaDeHoyQueYaEmpezoDiceYaPaso() {
        Turno t = franja(4, 8);
        when(turnoDAO.listarPorInstalacion(1)).thenReturn(List.of(t));

        TurnoDisponible d = servicio.turnos(7, 1, HOY, AHORA).get(0);

        assertFalse(d.isLibre());
        assertEquals("Ya pasó", d.getMotivo());
    }

    @Test
    void unaFranjaReservadaPorOtroSocioDiceReservado() {
        Turno t = franja(6, 18);
        Socio otro = DatosDePrueba.socio(8, Socio.Estado.ACTIVO, alumno);
        when(turnoDAO.listarPorInstalacion(1)).thenReturn(List.of(t));
        when(reservaDAO.listarActivasDeInstalacionEntre(1, FECHA_TURNO, FECHA_TURNO))
                .thenReturn(List.of(ocupada(t, FECHA_TURNO, otro)));

        TurnoDisponible d = servicio.turnos(7, 1, FECHA_TURNO, AHORA).get(0);

        assertFalse(d.isLibre());
        assertEquals("Reservado", d.getMotivo());
    }

    @Test
    void unaFranjaReservadaPorElPropioSocioDiceTuReserva() {
        Turno t = franja(6, 18);
        when(turnoDAO.listarPorInstalacion(1)).thenReturn(List.of(t));
        when(reservaDAO.listarActivasDeInstalacionEntre(1, FECHA_TURNO, FECHA_TURNO))
                .thenReturn(List.of(ocupada(t, FECHA_TURNO, socio)));

        assertEquals("Tu reserva", servicio.turnos(7, 1, FECHA_TURNO, AHORA).get(0).getMotivo());
    }

    @Test
    void lasFranjasDeUnDiaConviertenCadaMotivoSegunSuEstado() {
        Turno temprano = franja(4, 8);
        Turno justo = franja(5, 10);
        Turno tarde = franja(6, 18);
        Socio otro = DatosDePrueba.socio(8, Socio.Estado.ACTIVO, alumno);
        when(turnoDAO.listarPorInstalacion(1)).thenReturn(List.of(temprano, justo, tarde));
        when(reservaDAO.listarActivasDeInstalacionEntre(1, FECHA_TURNO, FECHA_TURNO))
                .thenReturn(List.of(ocupada(tarde, FECHA_TURNO, otro)));

        List<TurnoDisponible> lista = servicio.turnos(7, 1, FECHA_TURNO, AHORA);

        assertEquals("Menos de 48 h", de(lista, 4).getMotivo());
        assertTrue(de(lista, 5).isLibre());
        assertEquals("Reservado", de(lista, 6).getMotivo());
    }

    // ---------- reprogramar ----------

    @Test
    void lasFranjasParaReprogramarSonLasDeLaInstalacionDeLaReservaPropia() {
        Reserva r = DatosDePrueba.reserva(10, socio, turno, FECHA_TURNO, Reserva.Estado.CANCELADA);
        when(reservaDAO.buscarPorId(10)).thenReturn(r);
        when(turnoDAO.listarPorInstalacion(1)).thenReturn(List.of(franja(5, 10)));

        List<TurnoDisponible> lista = servicio.turnosParaReprogramar(7, 10, FECHA_TURNO, AHORA);

        assertEquals(1, lista.size());
    }

    @Test
    void noMuestraFranjasParaReprogramarUnaReservaAjena() {
        Reserva r = DatosDePrueba.reserva(10, socio, turno, FECHA_TURNO, Reserva.Estado.CANCELADA);
        when(reservaDAO.buscarPorId(10)).thenReturn(r);

        assertTrue(servicio.turnosParaReprogramar(8, 10, FECHA_TURNO, AHORA).isEmpty());
        assertTrue(servicio.turnosParaReprogramar(7, null, FECHA_TURNO, AHORA).isEmpty());
    }

    // ---------- días reservables ----------

    @Test
    void losDiasReservablesVanDelTercerDiaHastaDentroDe30Dias() {
        List<DiaSocio> dias = servicio.diasReservables(HOY);

        assertEquals(29, dias.size());
        assertEquals(HOY.plusDays(2), dias.get(0).getFecha());
        assertEquals(HOY.plusDays(30), dias.get(28).getFecha());
    }

    @Test
    void cuentaCuantasReservasProximasTieneElSocioCadaDia() {
        MiReserva proxima = new MiReserva(10, "Cancha de Fútbol 11", "sports_soccer", "Fútbol", "Mañana", "13/10/2026",
                "10:00 – 11:00", "$ 20.000", "Confirmada", "ok", "PROXIMAS", HOY.plusDays(1).atTime(10, 0), false, false,
                true, null, null, new ArrayList<>(), false, false, null);
        MiReserva pasada = new MiReserva(11, "Cancha de Fútbol 11", "sports_soccer", "Fútbol", "Ayer", "11/10/2026",
                "10:00 – 11:00", "$ 20.000", "Realizada", "muted", "PASADAS", HOY.minusDays(1).atTime(10, 0), false,
                false, false, null, null, new ArrayList<>(), false, false, null);

        List<DiaSocio> dias = servicio.dias(List.of(proxima, pasada), HOY.minusDays(1), 4);

        assertEquals(0, dias.get(0).getReservas(), "las pasadas no cuentan");
        assertEquals(0, dias.get(1).getReservas());
        assertEquals(1, dias.get(2).getReservas());
    }

    // ---------- cuota y precio ----------

    @Test
    void laCuotaMensualSaleDeLaTarifaVigenteDelTipoDeSocio() {
        TarifaMembresia cuota = new TarifaMembresia(1);
        cuota.setPrecio(new BigDecimal("5000"));
        when(tarifaMembresiaDAO.buscarVigente(1, HOY)).thenReturn(cuota);

        assertEquals("$ 5.000", servicio.cuota(socio, HOY));
    }

    @Test
    void sinTarifaVigenteNoHayCuotaNiPrecio() {
        when(tarifaMembresiaDAO.buscarVigente(anyInt(), any(LocalDate.class))).thenReturn(null);
        when(tarifaDAO.buscarVigente(anyInt(), anyInt(), any(LocalDate.class))).thenReturn(null);

        assertNull(servicio.cuota(socio, HOY));
        assertNull(servicio.precio(socio, 1, FECHA_TURNO));
    }

    @Test
    void elPrecioDelTurnoEsElDeLaTarifaVigenteEseDia() {
        when(tarifaDAO.buscarVigente(1, 1, FECHA_TURNO)).thenReturn(tarifa);

        assertEquals("$ 20.000", servicio.precio(socio, 1, FECHA_TURNO));
    }
}
