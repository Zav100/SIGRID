package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.ComprobantePagoDAO;
import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.PagoDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.SocioDAO;
import com.sigrid.sigrid.dao.SolicitudCambioReservaDAO;
import com.sigrid.sigrid.dao.SuscripcionSocioDAO;
import com.sigrid.sigrid.dao.TarifaAlquilerDAO;
import com.sigrid.sigrid.dao.TarifaMembresiaDAO;
import com.sigrid.sigrid.dao.TurnoDAO;
import com.sigrid.sigrid.dao.ValoracionReservaDAO;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.TarifaAlquiler;
import com.sigrid.sigrid.repositorio.Turno;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lo común de las pruebas de PanelSocioServicio: los once DAO simulados y un socio activo con su cancha y su turno.
 * No es una prueba por sí misma (es abstracta y su nombre no termina en Test).
 */
@ExtendWith(MockitoExtension.class)
abstract class PanelSocioServicioPruebaBase {

    /** Turno de la cancha 10:00 a 11:00. Con AHORA = 12/10 10:00, el del 14/10 queda a exactamente 48 h. */
    protected static final LocalDate FECHA_TURNO = LocalDate.of(2026, 10, 14);

    @Mock
    protected SocioDAO socioDAO;
    @Mock
    protected SuscripcionSocioDAO suscripcionDAO;
    @Mock
    protected ReservaDAO reservaDAO;
    @Mock
    protected TurnoDAO turnoDAO;
    @Mock
    protected InstalacionDAO instalacionDAO;
    @Mock
    protected TarifaAlquilerDAO tarifaDAO;
    @Mock
    protected PagoDAO pagoDAO;
    @Mock
    protected ValoracionReservaDAO valoracionDAO;
    @Mock
    protected TarifaMembresiaDAO tarifaMembresiaDAO;
    @Mock
    protected ComprobantePagoDAO comprobanteDAO;
    @Mock
    protected SolicitudCambioReservaDAO solicitudDAO;

    @InjectMocks
    protected PanelSocioServicio servicio;

    protected final CategoriaSocio alumno = DatosDePrueba.categoria(1, "Alumno UNSE", true);
    protected final Socio socio = DatosDePrueba.socio(7, Socio.Estado.ACTIVO, alumno);
    protected final Instalacion cancha = DatosDePrueba.instalacion(1, "Cancha de Fútbol 11",
            Instalacion.TipoAcceso.ARANCELADO, Instalacion.Estado.HABILITADA);
    protected final Turno turno = DatosDePrueba.turno(5, cancha, LocalTime.of(10, 0), LocalTime.of(11, 0));
    protected final TarifaAlquiler tarifa = DatosDePrueba.tarifaAlquiler(3, cancha, alumno, "20000",
            LocalDate.of(2026, 1, 1), null);
}
