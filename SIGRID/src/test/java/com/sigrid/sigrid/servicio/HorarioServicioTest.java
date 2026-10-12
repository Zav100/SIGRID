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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sigrid.sigrid.dao.HorarioAperturaDAO;
import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dto.Apertura;
import com.sigrid.sigrid.dto.HorarioFila;
import com.sigrid.sigrid.repositorio.HorarioApertura;
import com.sigrid.sigrid.repositorio.Instalacion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Horario de apertura de la pileta (acceso libre): abre por franjas semanales o de un día puntual, se entra mientras
 * esté abierta. Las franjas de un día se suman a las semanales y no pueden superponerse.
 * "Ahora" es el lunes 12/10/2026 a las 10:00.
 */
@ExtendWith(MockitoExtension.class)
class HorarioServicioTest {

    private static final int LUNES = 1;
    private static final int MARTES = 2;
    private static final int VIERNES = 5;

    @Mock
    private HorarioAperturaDAO horarioDAO;
    @Mock
    private InstalacionDAO instalacionDAO;

    @InjectMocks
    private HorarioServicio servicio;

    private final Instalacion pileta = DatosDePrueba.instalacion(3, "Pileta", Instalacion.TipoAcceso.LIBRE,
            Instalacion.Estado.HABILITADA);
    private final List<HorarioApertura> franjas = new ArrayList<>();
    private int idHorario = 1;

    @BeforeEach
    void laPiletaTieneLasFranjasDeCadaTest() {
        lenient().when(horarioDAO.listarPorInstalacion(3)).thenReturn(franjas);
        lenient().when(instalacionDAO.buscarPorId(3)).thenReturn(pileta);
    }

    private HorarioApertura semanal(int dia, int apertura, int cierre) {
        HorarioApertura h = new HorarioApertura();
        h.setIdHorario(idHorario++);
        h.setIdInstalacion(pileta);
        h.setDiaSemana(dia);
        h.setHoraApertura(LocalTime.of(apertura, 0));
        h.setHoraCierre(LocalTime.of(cierre, 0));
        franjas.add(h);
        return h;
    }

    private HorarioApertura deFecha(LocalDate fecha, int apertura, int cierre) {
        HorarioApertura h = new HorarioApertura();
        h.setIdHorario(idHorario++);
        h.setIdInstalacion(pileta);
        h.setFecha(fecha);
        h.setHoraApertura(LocalTime.of(apertura, 0));
        h.setHoraCierre(LocalTime.of(cierre, 0));
        franjas.add(h);
        return h;
    }

    private Apertura aLas(int hora, int minuto) {
        return servicio.ahora(3, HOY.atTime(hora, minuto));
    }

    // ---------- ¿está abierta ahora? ----------

    @Test
    void dentroDeLaFranjaEstaAbiertaYDiceHastaCuando() {
        semanal(LUNES, 9, 19);

        Apertura a = servicio.ahora(3, AHORA);

        assertTrue(a.isAbierta());
        assertEquals("Abierta hasta 19:00", a.getTexto());
    }

    @Test
    void abreExactamenteALaHoraDeApertura() {
        semanal(LUNES, 9, 19);

        assertTrue(aLas(9, 0).isAbierta());
    }

    @Test
    void unMinutoAntesDeAbrirTodaviaEstaCerrada() {
        semanal(LUNES, 9, 19);

        Apertura a = aLas(8, 59);

        assertFalse(a.isAbierta());
        assertEquals("Cerrada · abre hoy a las 09:00", a.getTexto());
    }

    @Test
    void unMinutoAntesDelCierreSigueAbierta() {
        semanal(LUNES, 9, 19);

        assertTrue(aLas(18, 59).isAbierta());
    }

    @Test
    void aLaHoraDeCierreYaEstaCerradaYDiceCuandoAbreLaSemanaSiguiente() {
        semanal(LUNES, 9, 19);

        Apertura a = aLas(19, 0);

        assertFalse(a.isAbierta());
        assertEquals("Cerrada · abre lun 19/10 09:00", a.getTexto());
    }

    // ---------- cuándo abre ----------

    @Test
    void siAbreMasTardeHoyDiceLaHora() {
        semanal(LUNES, 14, 20);

        assertEquals("Cerrada · abre hoy a las 14:00", servicio.ahora(3, AHORA).getTexto());
    }

    @Test
    void siHayDosFranjasMasTardeDiceLaPrimera() {
        semanal(LUNES, 18, 19);
        semanal(LUNES, 14, 15);

        assertEquals("Cerrada · abre hoy a las 14:00", servicio.ahora(3, AHORA).getTexto());
    }

    @Test
    void siAbreMananaLoDiceAsi() {
        semanal(MARTES, 9, 19);

        assertEquals("Cerrada · abre mañana 09:00", servicio.ahora(3, AHORA).getTexto());
    }

    @Test
    void siAbreOtroDiaDiceElDiaYLaFecha() {
        semanal(VIERNES, 9, 19);

        assertEquals("Cerrada · abre vie 16/10 09:00", servicio.ahora(3, AHORA).getTexto());
    }

    @Test
    void unaFranjaDeUnDiaPuntualSeSumaALasSemanales() {
        semanal(LUNES, 9, 11);
        deFecha(HOY, 14, 18);

        assertEquals("Cerrada · abre hoy a las 14:00", aLas(12, 0).getTexto());
        assertEquals("Abierta hasta 18:00", aLas(15, 0).getTexto());
    }

    @Test
    void laFranjaDeUnDiaPuntualPuedeSerLaProximaApertura() {
        semanal(LUNES, 9, 19);
        deFecha(LocalDate.of(2026, 10, 14), 10, 12);

        assertEquals("Cerrada · abre mié 14/10 10:00", aLas(20, 0).getTexto());
    }

    @Test
    void buscaLaProximaAperturaHastaCatorceDiasAdelante() {
        deFecha(LocalDate.of(2026, 10, 26), 9, 12); // hoy + 14

        assertEquals("Cerrada · abre lun 26/10 09:00", servicio.ahora(3, AHORA).getTexto());
    }

    @Test
    void siLaProximaAperturaEstaAMasDeCatorceDiasDiceSoloCerrada() {
        deFecha(LocalDate.of(2026, 10, 27), 9, 12); // hoy + 15

        assertEquals("Cerrada", servicio.ahora(3, AHORA).getTexto());
    }

    @Test
    void sinHorariosCargadosLoDice() {
        Apertura a = servicio.ahora(3, AHORA);

        assertFalse(a.isAbierta());
        assertEquals("Sin horarios cargados", a.getTexto());
    }

    // ---------- el día elegido en el calendario ----------

    @Test
    void elDiaDeHoyMuestraComoEstaAhora() {
        semanal(LUNES, 9, 19);

        Apertura a = servicio.enDia(3, HOY, AHORA);

        assertTrue(a.isAbierta());
        assertEquals("Abierta hasta 19:00", a.getTexto());
    }

    @Test
    void otroDiaMuestraTodasSusFranjas() {
        semanal(3, 10, 12); // miércoles
        semanal(3, 15, 17);

        Apertura a = servicio.enDia(3, LocalDate.of(2026, 10, 14), AHORA);

        assertTrue(a.isAbierta());
        assertEquals("Abre 10:00 – 12:00 · 15:00 – 17:00", a.getTexto());
    }

    @Test
    void unDiaSinFranjasDiceCerradaEseDia() {
        semanal(LUNES, 9, 19);

        Apertura a = servicio.enDia(3, LocalDate.of(2026, 10, 14), AHORA);

        assertFalse(a.isAbierta());
        assertEquals("Cerrada ese día", a.getTexto());
    }

    // ---------- listado de franjas ----------

    @Test
    void agrupaLosDiasConElMismoHorarioYLosAbrevia() {
        for (int dia = 1; dia <= 5; dia++) {
            semanal(dia, 9, 19);
        }
        semanal(6, 10, 14);
        semanal(7, 10, 14);

        List<HorarioFila> filas = servicio.listar(3, HOY);

        assertEquals(2, filas.size());
        assertEquals("Lun a Vie", filas.get(0).getDias());
        assertEquals("09:00 – 19:00", filas.get(0).getHorario());
        assertEquals("1,2,3,4,5", filas.get(0).getIds());
        assertFalse(filas.get(0).isDeFecha());
        assertEquals("Sáb y Dom", filas.get(1).getDias());
        assertEquals("10:00 – 14:00", filas.get(1).getHorario());
    }

    @Test
    void despuesDeLasSemanalesVanLosDiasPuntualesQueTodaviaNoPasaron() {
        semanal(LUNES, 9, 19);
        deFecha(LocalDate.of(2026, 10, 11), 9, 12); // ayer: no se muestra
        deFecha(HOY, 20, 22); // hoy: se muestra
        deFecha(LocalDate.of(2026, 10, 14), 10, 12);

        List<HorarioFila> filas = servicio.listar(3, HOY);

        assertEquals(3, filas.size());
        assertEquals("Lun", filas.get(0).getDias());
        assertEquals("lun 12/10", filas.get(1).getDias());
        assertTrue(filas.get(1).isDeFecha());
        assertEquals("mié 14/10", filas.get(2).getDias());
    }

    @ParameterizedTest
    @CsvSource({"'1',Lun", "'1,2',Lun y Mar", "'1,2,3',Lun a Mié", "'1,3,5','Lun, Mié y Vie'", "'6,7',Sáb y Dom",
        "'1,2,3,5',Lun a Mié y Vie", "'1,2,4,5,6','Lun, Mar y Jue a Sáb'", "'1,2,3,4,5,6,7',Lun a Dom"})
    void elTextoDeDiasAbreviaLasRachasDeTresOMas(String dias, String esperado) {
        List<Integer> lista = new ArrayList<>();
        for (String d : dias.split(",")) {
            lista.add(Integer.valueOf(d));
        }

        assertEquals(esperado, HorarioServicio.textoDias(lista));
    }

    // ---------- agregar franjas semanales ----------

    @Test
    void agregaUnaFilaPorCadaDiaElegido() {
        ArgumentCaptor<HorarioApertura> creada = ArgumentCaptor.forClass(HorarioApertura.class);

        String error = servicio.agregarSemanal(3, Arrays.asList(1, 2, 3), LocalTime.of(9, 0), LocalTime.of(19, 0));

        assertNull(error);
        verify(horarioDAO, times(3)).crear(creada.capture());
        assertEquals(Arrays.asList(1, 2, 3), Arrays.asList(creada.getAllValues().get(0).getDiaSemana(),
                creada.getAllValues().get(1).getDiaSemana(), creada.getAllValues().get(2).getDiaSemana()));
        assertEquals(LocalTime.of(9, 0), creada.getValue().getHoraApertura());
        assertEquals(LocalTime.of(19, 0), creada.getValue().getHoraCierre());
        assertSame(pileta, creada.getValue().getIdInstalacion());
    }

    @Test
    void rechazaUnaInstalacionInexistente() {
        when(instalacionDAO.buscarPorId(99)).thenReturn(null);

        assertEquals("La instalación no existe.",
                servicio.agregarSemanal(99, Arrays.asList(1), LocalTime.of(9, 0), LocalTime.of(19, 0)));
        assertEquals("La instalación no existe.",
                servicio.agregarSemanal(null, Arrays.asList(1), LocalTime.of(9, 0), LocalTime.of(19, 0)));
    }

    @Test
    void rechazaUnaInstalacionQueSeReservaPorTurnos() {
        Instalacion cancha = DatosDePrueba.instalacion(1, "Cancha de Fútbol 11", Instalacion.TipoAcceso.ARANCELADO,
                Instalacion.Estado.HABILITADA);
        when(instalacionDAO.buscarPorId(1)).thenReturn(cancha);

        assertEquals("Esta instalación se reserva por turnos: no tiene horario de apertura.",
                servicio.agregarSemanal(1, Arrays.asList(1), LocalTime.of(9, 0), LocalTime.of(19, 0)));
    }

    @Test
    void exigeLaHoraDeAperturaYLaDeCierre() {
        assertEquals("Completá la hora de apertura y la de cierre.",
                servicio.agregarSemanal(3, Arrays.asList(1), null, LocalTime.of(19, 0)));
        assertEquals("Completá la hora de apertura y la de cierre.",
                servicio.agregarSemanal(3, Arrays.asList(1), LocalTime.of(9, 0), null));
    }

    @Test
    void elCierreTieneQueSerPosteriorALaApertura() {
        assertEquals("El cierre tiene que ser posterior a la apertura.",
                servicio.agregarSemanal(3, Arrays.asList(1), LocalTime.of(19, 0), LocalTime.of(19, 0)));
        assertEquals("El cierre tiene que ser posterior a la apertura.",
                servicio.agregarSemanal(3, Arrays.asList(1), LocalTime.of(19, 0), LocalTime.of(9, 0)));
    }

    @Test
    void exigeAlMenosUnDia() {
        assertEquals("Elegí al menos un día de la semana.",
                servicio.agregarSemanal(3, new ArrayList<Integer>(), LocalTime.of(9, 0), LocalTime.of(19, 0)));
        assertEquals("Elegí al menos un día de la semana.",
                servicio.agregarSemanal(3, null, LocalTime.of(9, 0), LocalTime.of(19, 0)));
    }

    @Test
    void rechazaUnaFranjaQueSeSuperponeConOtraDelMismoDia() {
        semanal(LUNES, 9, 13);

        String error = servicio.agregarSemanal(3, Arrays.asList(LUNES), LocalTime.of(12, 0), LocalTime.of(15, 0));

        assertEquals("Ya hay una franja abierta el lunes que se superpone con ese horario.", error);
        verify(horarioDAO, never()).crear(any());
    }

    @Test
    void unaFranjaQueEmpiezaJustoCuandoOtraTerminaNoSeSuperpone() {
        semanal(LUNES, 9, 13);

        assertNull(servicio.agregarSemanal(3, Arrays.asList(LUNES), LocalTime.of(13, 0), LocalTime.of(15, 0)));
    }

    @Test
    void unaFranjaQueTerminaJustoCuandoOtraEmpiezaNoSeSuperpone() {
        semanal(LUNES, 9, 13);

        assertNull(servicio.agregarSemanal(3, Arrays.asList(LUNES), LocalTime.of(7, 0), LocalTime.of(9, 0)));
    }

    @Test
    void lasFranjasDeOtroDiaNoSeSuperponen() {
        semanal(MARTES, 9, 13);

        assertNull(servicio.agregarSemanal(3, Arrays.asList(LUNES), LocalTime.of(9, 0), LocalTime.of(13, 0)));
    }

    @Test
    void siUnSoloDiaChocaNoSeAgregaNingunoDeLosOtros() {
        semanal(MARTES, 9, 13);

        String error = servicio.agregarSemanal(3, Arrays.asList(LUNES, MARTES), LocalTime.of(10, 0),
                LocalTime.of(12, 0));

        assertEquals("Ya hay una franja abierta el martes que se superpone con ese horario.", error);
        verify(horarioDAO, never()).crear(any());
    }

    // ---------- agregar un día puntual ----------

    @Test
    void agregaLaFranjaDeUnDiaPuntual() {
        ArgumentCaptor<HorarioApertura> creada = ArgumentCaptor.forClass(HorarioApertura.class);

        String error = servicio.agregarDeFecha(3, LocalDate.of(2026, 10, 17), LocalTime.of(10, 0), LocalTime.of(18, 0),
                HOY);

        assertNull(error);
        verify(horarioDAO).crear(creada.capture());
        assertEquals(LocalDate.of(2026, 10, 17), creada.getValue().getFecha());
        assertNull(creada.getValue().getDiaSemana());
    }

    @Test
    void elDiaPuntualPuedeSerHoyPeroNoUnDiaAnterior() {
        assertNull(servicio.agregarDeFecha(3, HOY, LocalTime.of(10, 0), LocalTime.of(18, 0), HOY));
        assertEquals("Ese día ya pasó.",
                servicio.agregarDeFecha(3, HOY.minusDays(1), LocalTime.of(10, 0), LocalTime.of(18, 0), HOY));
    }

    @Test
    void elDiaPuntualEsObligatorio() {
        assertEquals("Elegí el día.", servicio.agregarDeFecha(3, null, LocalTime.of(10, 0), LocalTime.of(18, 0), HOY));
    }

    @Test
    void elDiaPuntualNoSePuedeSuperponerConElHorarioSemanalDeEseDia() {
        semanal(3, 9, 13); // miércoles

        String error = servicio.agregarDeFecha(3, LocalDate.of(2026, 10, 14), LocalTime.of(12, 0), LocalTime.of(15, 0),
                HOY);

        assertEquals("Ese día ya está abierto en un horario que se superpone con ese.", error);
        verify(horarioDAO, never()).crear(any());
    }

    @Test
    void elDiaPuntualPuedeIrDespuesDelHorarioSemanalSinTocarlo() {
        semanal(3, 9, 13);

        assertNull(servicio.agregarDeFecha(3, LocalDate.of(2026, 10, 14), LocalTime.of(13, 0), LocalTime.of(15, 0), HOY));
    }

    // ---------- quitar ----------

    @Test
    void quitaSoloLasFranjasDeEsaInstalacion() {
        HorarioApertura propia = semanal(LUNES, 9, 19);
        Instalacion otra = DatosDePrueba.instalacion(4, "Quincho 1", Instalacion.TipoAcceso.LIBRE,
                Instalacion.Estado.HABILITADA);
        HorarioApertura ajena = new HorarioApertura();
        ajena.setIdHorario(50);
        ajena.setIdInstalacion(otra);
        when(horarioDAO.buscarPorId(propia.getIdHorario())).thenReturn(propia);
        when(horarioDAO.buscarPorId(50)).thenReturn(ajena);
        when(horarioDAO.buscarPorId(60)).thenReturn(null);

        servicio.quitar(3, Arrays.asList(propia.getIdHorario(), 50, 60));

        verify(horarioDAO).quitar(propia.getIdHorario());
        verify(horarioDAO, times(1)).quitar(anyInt());
    }
}
