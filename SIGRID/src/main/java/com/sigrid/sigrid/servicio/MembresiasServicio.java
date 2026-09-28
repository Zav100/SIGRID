package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.CarnetDigitalDAO;
import com.sigrid.sigrid.dao.CategoriaSocioDAO;
import com.sigrid.sigrid.dao.SuscripcionSocioDAO;
import com.sigrid.sigrid.dao.TarifaMembresiaDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dto.IngresosMembresias;
import com.sigrid.sigrid.dto.SuscripcionFila;
import com.sigrid.sigrid.dto.TarifaFila;
import com.sigrid.sigrid.repositorio.CarnetDigital;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import com.sigrid.sigrid.repositorio.TarifaMembresia;
import com.sigrid.sigrid.repositorio.Usuario;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Panel de membresías del administrador: lectura (suscripciones, ingresos, tarifas) y las acciones que cambian
 * la vida de un socio en el sistema:
 * - aceptar el comprobante: la membresía pasa a VIGENTE por un mes, el socio queda ACTIVO y su carnet se activa;
 * - denegar el comprobante: la solicitud queda CANCELADA y el pago RECHAZADO;
 * - cancelar una vigente (baja voluntaria de la membresía, no del socio);
 * - vencer las que llegaron a su fecha: quedan VENCIDA y, si el socio no tiene otra vigente, pasa a NO_ACTIVO y
 *   su carnet se congela (INACTIVO). Lo ejecuta MembresiaJob cada hora y también al abrir el panel.
 *
 * CDI simple (no EJB) y Serializable, igual que ReservaServicio. Las transacciones las abre este servicio.
 */
@Dependent
public class MembresiasServicio implements Serializable {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Inject
    private SuscripcionSocioDAO suscripcionDAO;
    @Inject
    private CarnetDigitalDAO carnetDAO;
    @Inject
    private TarifaMembresiaDAO tarifaDAO;
    @Inject
    private CategoriaSocioDAO categoriaDAO;
    @Inject
    private UsuarioDAO usuarioDAO;

    // ---------- lectura ----------

    /** Todas las membresías: primero las pendientes, después vigentes, vencidas y canceladas (la más reciente primero). */
    public List<SuscripcionFila> listar(LocalDate hoy) {
        List<SuscripcionFila> filas = new ArrayList<>();
        for (SuscripcionSocio s : suscripcionDAO.listarTodas()) {
            filas.add(aFila(s, hoy));
        }
        filas.sort(Comparator.comparingInt(f -> orden(f.getEstado()))); // estable: conserva el orden por fecha
        return filas;
    }

    /** Vigentes que vencen dentro de los próximos días, la más cercana primero. */
    public List<SuscripcionFila> porVencer(LocalDate hoy, int dias) {
        List<SuscripcionFila> filas = new ArrayList<>();
        for (SuscripcionSocio s : suscripcionDAO.listarVigentesQueVencenEntre(hoy, hoy.plusDays(dias))) {
            filas.add(aFila(s, hoy));
        }
        return filas;
    }

    /** Lo cobrado en el mes (pagos de membresía confirmados) y en los meses anteriores. */
    public IngresosMembresias ingresos(YearMonth mes, int cantidadDeMeses) {
        YearMonth desde = mes.minusMonths(cantidadDeMeses - 1);
        Map<YearMonth, BigDecimal> porMes = new HashMap<>();
        Map<String, BigDecimal> porTipoDelMes = new HashMap<>();
        for (Object[] fila : suscripcionDAO.ingresosDesde(desde.atDay(1).atStartOfDay())) {
            YearMonth delPago = YearMonth.from((LocalDateTime) fila[1]);
            porMes.merge(delPago, (BigDecimal) fila[0], BigDecimal::add);
            if (delPago.equals(mes)) {
                porTipoDelMes.merge((String) fila[2], (BigDecimal) fila[0], BigDecimal::add);
            }
        }

        BigDecimal maximo = porMes.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<IngresosMembresias.Mes> meses = new ArrayList<>();
        for (int i = 0; i < cantidadDeMeses; i++) {
            YearMonth m = desde.plusMonths(i);
            BigDecimal monto = porMes.getOrDefault(m, BigDecimal.ZERO);
            int porcentaje = maximo.signum() == 0 ? 0 : monto.multiply(BigDecimal.valueOf(100))
                    .divide(maximo, 0, java.math.RoundingMode.HALF_UP).intValue();
            meses.add(new IngresosMembresias.Mes(nombreCorto(m), monto(monto), porcentaje, m.equals(mes)));
        }

        BigDecimal actual = porMes.getOrDefault(mes, BigDecimal.ZERO);
        BigDecimal anterior = porMes.getOrDefault(mes.minusMonths(1), BigDecimal.ZERO);
        String nombreAnterior = mes.minusMonths(1).getMonth().getDisplayName(TextStyle.FULL, ES);
        String comparacion;
        String tendencia;
        if (anterior.signum() == 0) {
            comparacion = actual.signum() == 0 ? "Todavía sin ingresos este mes" : "Sin ingresos en " + nombreAnterior;
            tendencia = "flat";
        } else {
            long variacion = actual.subtract(anterior).multiply(BigDecimal.valueOf(100))
                    .divide(anterior, 0, java.math.RoundingMode.HALF_UP).longValue();
            comparacion = (variacion > 0 ? "+" : "") + variacion + " % respecto de " + nombreAnterior;
            tendencia = variacion > 0 ? "up" : variacion < 0 ? "down" : "flat";
        }

        List<IngresosMembresias.Tipo> porTipo = new ArrayList<>();
        int indice = 0;
        for (String tipo : tiposDeSocio()) {
            BigDecimal monto = porTipoDelMes.getOrDefault(tipo, BigDecimal.ZERO);
            int porcentaje = actual.signum() == 0 ? 0 : monto.multiply(BigDecimal.valueOf(100))
                    .divide(actual, 0, java.math.RoundingMode.HALF_UP).intValue();
            porTipo.add(new IngresosMembresias.Tipo(tipo, monto(monto), porcentaje, indice++));
        }
        return new IngresosMembresias(monto(actual), comparacion, tendencia, meses, porTipo);
    }

    /** Cuota mensual vigente de cada tipo de socio. */
    public List<TarifaFila> tarifas(LocalDate hoy) {
        List<TarifaFila> tarifas = new ArrayList<>();
        for (CategoriaSocio c : categoriaDAO.listarTodos()) {
            TarifaMembresia t = tarifaDAO.buscarVigente(c.getIdCategoriaSocio(), hoy);
            tarifas.add(new TarifaFila(c.getNombreCategoria(), t == null ? null : monto(t.getPrecio())));
        }
        return tarifas;
    }

    public List<String> tiposDeSocio() {
        List<String> tipos = new ArrayList<>();
        for (CategoriaSocio c : categoriaDAO.listarTodos()) {
            tipos.add(c.getNombreCategoria());
        }
        return tipos;
    }

    public String monto(BigDecimal monto) {
        return DashboardServicio.monto(monto);
    }

    // ---------- acciones ----------

    /**
     * Acepta el comprobante: la membresía dura un mes desde hoy (o desde que termina la vigente, si renovó por adelantado).
     * @return el vencimiento de la nueva membresía, o null si la solicitud ya no estaba por validar.
     */
    @Transactional
    public LocalDate aceptar(Integer idSuscripcion, Integer idAdmin, LocalDate hoy) {
        SuscripcionSocio s = suscripcionDAO.buscarParaResolver(idSuscripcion);
        if (!estaPorValidar(s)) {
            return null;
        }
        Socio socio = s.getIdSocio();
        List<SuscripcionSocio> vigentes = suscripcionDAO.listarVigentesDelSocio(socio.getIdSocio(), hoy);
        LocalDate inicio = vigentes.isEmpty() ? hoy : vigentes.get(0).getFechaVencimiento().plusDays(1);

        validarPago(s.getIdPago(), idAdmin, Pago.Estado.CONFIRMADO);
        s.setEstado(SuscripcionSocio.Estado.VIGENTE);
        s.setFechaInicio(inicio);
        s.setFechaVencimiento(inicio.plusMonths(1));
        socio.setEstado(Socio.Estado.ACTIVO);
        CarnetDigital carnet = carnetDAO.buscarPorSocio(socio.getIdSocio());
        if (carnet != null) {
            carnet.setEstado(CarnetDigital.Estado.ACTIVO);
        }
        return s.getFechaVencimiento();
    }

    /** Deniega el comprobante: el pago queda RECHAZADO y la solicitud CANCELADA. @return false si ya no estaba por validar. */
    @Transactional
    public boolean denegar(Integer idSuscripcion, Integer idAdmin) {
        SuscripcionSocio s = suscripcionDAO.buscarParaResolver(idSuscripcion);
        if (!estaPorValidar(s)) {
            return false;
        }
        validarPago(s.getIdPago(), idAdmin, Pago.Estado.RECHAZADO);
        s.setEstado(SuscripcionSocio.Estado.CANCELADA);
        return true;
    }

    /**
     * Cancela una membresía vigente (baja voluntaria de la membresía, RF-01.9). Si el socio no tiene otra vigente
     * queda NO_ACTIVO y su carnet se congela.
     * @return null si ya no estaba vigente; true si el socio quedó sin membresía (carnet congelado); false si le queda otra.
     */
    @Transactional
    public Boolean cancelar(Integer idSuscripcion, LocalDate hoy) {
        SuscripcionSocio s = suscripcionDAO.buscarParaResolver(idSuscripcion);
        if (s == null || s.getEstado() != SuscripcionSocio.Estado.VIGENTE) {
            return null;
        }
        s.setEstado(SuscripcionSocio.Estado.CANCELADA);
        return congelarSiCorresponde(s, hoy);
    }

    /** Pasa a VENCIDA las vigentes que ya llegaron a su fecha y congela a los socios que se quedaron sin membresía. @return cuántas. */
    @Transactional
    public int vencerVencidas(LocalDate hoy) {
        List<SuscripcionSocio> vencidas = suscripcionDAO.listarVigentesVencidasAntesDe(hoy);
        for (SuscripcionSocio s : vencidas) {
            s.setEstado(SuscripcionSocio.Estado.VENCIDA);
        }
        for (SuscripcionSocio s : vencidas) {
            congelarSiCorresponde(s, hoy);
        }
        return vencidas.size();
    }

    // ---------- internos ----------

    /** Si el socio ya no tiene otra membresía vigente: NO_ACTIVO y carnet INACTIVO. @return true si lo congeló. */
    private boolean congelarSiCorresponde(SuscripcionSocio dejada, LocalDate hoy) {
        Socio socio = dejada.getIdSocio();
        for (SuscripcionSocio otra : suscripcionDAO.listarVigentesDelSocio(socio.getIdSocio(), hoy)) {
            if (!otra.equals(dejada)) {
                return false;
            }
        }
        socio.setEstado(Socio.Estado.NO_ACTIVO);
        CarnetDigital carnet = carnetDAO.buscarPorSocio(socio.getIdSocio());
        if (carnet != null) {
            carnet.setEstado(CarnetDigital.Estado.INACTIVO);
        }
        return true;
    }

    private static boolean estaPorValidar(SuscripcionSocio s) {
        return s != null && s.getEstado() == SuscripcionSocio.Estado.PENDIENTE_PAGO
                && s.getIdPago() != null && s.getIdPago().getEstado() == Pago.Estado.PENDIENTE_VALIDACION;
    }

    private void validarPago(Pago pago, Integer idAdmin, Pago.Estado resultado) {
        pago.setEstado(resultado);
        pago.setFechaValidacion(LocalDateTime.now());
        pago.setIdAdminValidador(usuarioDAO.buscarPorId(idAdmin));
    }

    private SuscripcionFila aFila(SuscripcionSocio s, LocalDate hoy) {
        Socio socio = s.getIdSocio();
        Usuario u = socio.getIdUsuario();
        Pago pago = s.getIdPago();
        boolean conComprobante = estaPorValidar(s);
        long dias = s.getFechaVencimiento() == null ? 0 : ChronoUnit.DAYS.between(hoy, s.getFechaVencimiento());
        boolean porVencer = s.getEstado() == SuscripcionSocio.Estado.VIGENTE && dias <= 7;

        String texto;
        String clase;
        String nota;
        switch (s.getEstado()) {
            case PENDIENTE_PAGO:
                texto = "Pendiente de pago";
                clase = "warn";
                nota = conComprobante ? "Comprobante para validar" : "Esperando el comprobante";
                break;
            case VIGENTE:
                texto = "Vigente";
                clase = "ok";
                nota = dias == 0 ? "Vence hoy" : dias == 1 ? "Vence mañana" : "Vence en " + dias + " días";
                break;
            case VENCIDA:
                texto = "Vencida";
                clase = "off";
                nota = "Carnet congelado si no renovó";
                break;
            default:
                texto = "Cancelada";
                clase = "muted";
                nota = pago != null && pago.getEstado() == Pago.Estado.RECHAZADO ? "Comprobante denegado" : "Baja de la membresía";
        }
        return new SuscripcionFila(s.getIdSuscripcion(), DashboardServicio.nombreSocio(socio), iniciales(u), socio.getDni(),
                socio.getIdCategoriaSocio().getNombreCategoria(), s.getEstado().name(), texto, clase, nota,
                s.getFechaInicio() == null ? null : s.getFechaInicio().format(FECHA),
                s.getFechaVencimiento() == null ? null : s.getFechaVencimiento().format(FECHA),
                pago == null ? null : pago.getMonto(), pago == null ? null : monto(pago.getMonto()),
                conComprobante, porVencer);
    }

    private static int orden(String estado) {
        switch (SuscripcionSocio.Estado.valueOf(estado)) {
            case PENDIENTE_PAGO:
                return 0;
            case VIGENTE:
                return 1;
            case VENCIDA:
                return 2;
            default:
                return 3;
        }
    }

    private static String nombreCorto(YearMonth mes) {
        return mes.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
    }

    private static String iniciales(Usuario u) {
        String nombre = u.getNombre();
        String apellido = u.getApellido();
        String iniciales = (nombre == null || nombre.isBlank() ? "" : nombre.substring(0, 1))
                + (apellido == null || apellido.isBlank() ? "" : apellido.substring(0, 1));
        return (iniciales.isEmpty() ? u.getEmail().substring(0, 1) : iniciales).toUpperCase();
    }
}
