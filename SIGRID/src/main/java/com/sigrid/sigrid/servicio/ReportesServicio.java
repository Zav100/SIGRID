package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.CategoriaSocioDAO;
import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.SocioDAO;
import com.sigrid.sigrid.dao.SuscripcionSocioDAO;
import com.sigrid.sigrid.dao.TarifaAlquilerDAO;
import com.sigrid.sigrid.dao.TarifaMembresiaDAO;
import com.sigrid.sigrid.dao.TurnoDAO;
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
import com.sigrid.sigrid.repositorio.TarifaAlquiler;
import com.sigrid.sigrid.repositorio.TarifaMembresia;
import com.sigrid.sigrid.repositorio.Turno;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Reportes del administrador: ingresos, ocupación y membresías para tomar decisiones. Es solo lectura.
 *
 * Se lee todo una vez (cargar) y cada reporte se calcula en memoria sobre esa foto y sobre un período (Rango),
 * comparándolo con el período anterior de igual largo. ponytail: con el volumen de un polideportivo alcanza; si las
 * reservas llegaran a decenas de miles, pasar los totales a consultas agrupadas en los DAO.
 *
 * Qué cuenta como ingreso: un pago CONFIRMADO, en el día en que el administrador validó el comprobante (cuando entró
 * el dinero). Una reserva cancelada conserva su pago (no hay devolución): sigue siendo ingreso, y si el crédito
 * de reprogramación vence sin usarse es dinero perdido.
 *
 * CDI simple (no EJB) y Serializable, igual que ReservaServicio.
 */
@Dependent
public class ReportesServicio implements Serializable {

    private static final Locale ES = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES_ANIO = DateTimeFormatter.ofPattern("MMM yy", ES);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MARGEN_RENOVACION_DIAS = 15; // una renovación tardía hasta esos días cuenta como renovación
    private static final double VALORACION_BAJA = 3.5;
    private static final int DIAS_DE_AVISO = 7;

    @Inject
    private ReservaDAO reservaDAO;
    @Inject
    private SuscripcionSocioDAO suscripcionDAO;
    @Inject
    private InstalacionDAO instalacionDAO;
    @Inject
    private TurnoDAO turnoDAO;
    @Inject
    private SocioDAO socioDAO;
    @Inject
    private CategoriaSocioDAO categoriaDAO;
    @Inject
    private TarifaAlquilerDAO tarifaAlquilerDAO;
    @Inject
    private TarifaMembresiaDAO tarifaMembresiaDAO;
    @Inject
    private ValoracionesServicio valoracionesServicio;

    // ---------- lo que se lee de la base ----------

    /** Un pago confirmado (ingreso), con lo que hace falta para agruparlo. */
    public static class Ingreso implements Serializable {

        final LocalDate fecha;
        final BigDecimal monto;
        final boolean reserva;     // true: reserva; false: membresía
        final String instalacion;  // null en las membresías
        final String categoria;
        final LocalDateTime pagado;
        final LocalDateTime validado;

        Ingreso(LocalDateTime pagado, LocalDateTime validado, BigDecimal monto, boolean reserva, String instalacion,
                String categoria) {
            this.fecha = validado.toLocalDate();
            this.pagado = pagado;
            this.validado = validado;
            this.monto = monto;
            this.reserva = reserva;
            this.instalacion = instalacion;
            this.categoria = categoria;
        }
    }

    /** La foto de la base sobre la que se calculan todos los reportes. */
    public static class Datos implements Serializable {

        LocalDate hoy;
        List<Reserva> reservas;
        List<SuscripcionSocio> suscripciones;
        List<Ingreso> ingresos = new ArrayList<>();
        List<Instalacion> instalaciones;
        Map<Integer, List<Turno>> turnos = new HashMap<>();
        Map<Integer, BigDecimal> precioPromedio = new HashMap<>();
        List<String> categorias = new ArrayList<>();
        Map<String, BigDecimal> cuotas = new HashMap<>();
        List<ValoracionFila> valoraciones;
        long sociosActivos;
        long sociosTotal;
    }

    /** Un período (y el anterior, de igual largo, con el que se compara). */
    public static class Rango implements Serializable {

        private final String nombre;
        private final LocalDate desde;
        private final LocalDate hasta;
        private final LocalDate previoDesde;
        private final LocalDate previoHasta;

        Rango(String nombre, LocalDate desde, LocalDate hasta, LocalDate previoDesde, LocalDate previoHasta) {
            this.nombre = nombre;
            this.desde = desde;
            this.hasta = hasta;
            this.previoDesde = previoDesde;
            this.previoHasta = previoHasta;
        }

        public String getNombre() {
            return nombre;
        }

        public String getTexto() {
            return desde.format(FECHA) + " al " + hasta.format(FECHA);
        }

        public String getTextoPrevio() {
            return previoDesde.format(FECHA) + " al " + previoHasta.format(FECHA);
        }

        public long getDias() {
            return ChronoUnit.DAYS.between(desde, hasta) + 1;
        }

        boolean contiene(LocalDate f) {
            return f != null && !f.isBefore(desde) && !f.isAfter(hasta);
        }

        boolean contienePrevio(LocalDate f) {
            return f != null && !f.isBefore(previoDesde) && !f.isAfter(previoHasta);
        }
    }

    public Datos cargar(LocalDate hoy) {
        Datos d = new Datos();
        d.hoy = hoy;
        d.reservas = reservaDAO.listarTodasParaAdmin();
        d.suscripciones = suscripcionDAO.listarTodasParaReportes();
        d.instalaciones = instalacionDAO.listarReservables();
        for (Instalacion i : d.instalaciones) {
            d.turnos.put(i.getIdInstalacion(), turnoDAO.listarPorInstalacion(i.getIdInstalacion()));
        }

        Map<Integer, List<BigDecimal>> precios = new HashMap<>();
        for (TarifaAlquiler t : tarifaAlquilerDAO.listarVigentes(hoy)) {
            precios.computeIfAbsent(t.getIdInstalacion().getIdInstalacion(), k -> new ArrayList<>()).add(t.getPrecio());
        }
        precios.forEach((id, lista) -> d.precioPromedio.put(id, lista.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(lista.size()), 2, RoundingMode.HALF_UP)));

        for (CategoriaSocio c : categoriaDAO.listarTodos()) {
            d.categorias.add(c.getNombreCategoria());
            TarifaMembresia t = tarifaMembresiaDAO.buscarVigente(c.getIdCategoriaSocio(), hoy);
            d.cuotas.put(c.getNombreCategoria(), t == null ? BigDecimal.ZERO : t.getPrecio());
        }
        d.valoraciones = valoracionesServicio.listar();
        d.sociosActivos = socioDAO.contarPorEstado(Socio.Estado.ACTIVO);
        d.sociosTotal = socioDAO.listarSinBaja().size();

        for (Reserva r : d.reservas) {
            if (cobrado(r.getIdPago())) {
                d.ingresos.add(new Ingreso(r.getIdPago().getFechaPago(), r.getIdPago().getFechaValidacion(),
                        r.getIdPago().getMonto(), true, r.getIdTurno().getIdInstalacion().getNombre(),
                        r.getIdSocio().getIdCategoriaSocio().getNombreCategoria()));
            }
        }
        for (SuscripcionSocio s : d.suscripciones) {
            if (cobrado(s.getIdPago())) {
                d.ingresos.add(new Ingreso(s.getIdPago().getFechaPago(), s.getIdPago().getFechaValidacion(),
                        s.getIdPago().getMonto(), false, null,
                        s.getIdSocio().getIdCategoriaSocio().getNombreCategoria()));
            }
        }
        return d;
    }

    private static boolean cobrado(Pago p) {
        return p != null && p.getEstado() == Pago.Estado.CONFIRMADO && p.getFechaValidacion() != null;
    }

    // ---------- períodos ----------

    /** periodo: 7D, 30D, MES (el mes en curso), MES_ANT, 90D o 12M. */
    public Rango rango(String periodo, LocalDate hoy) {
        switch (periodo) {
            case "7D":
                return ultimosDias("Últimos 7 días", 7, hoy);
            case "90D":
                return ultimosDias("Últimos 90 días", 90, hoy);
            case "12M": {
                LocalDate desde = hoy.minusMonths(12).plusDays(1);
                LocalDate previoHasta = desde.minusDays(1);
                return new Rango("Últimos 12 meses", desde, hoy, previoHasta.minusMonths(12).plusDays(1), previoHasta);
            }
            case "MES": {
                LocalDate desde = hoy.withDayOfMonth(1);
                LocalDate previoDesde = desde.minusMonths(1);
                LocalDate previoHasta = previoDesde.plusDays(ChronoUnit.DAYS.between(desde, hoy));
                LocalDate finPrevio = previoDesde.with(TemporalAdjusters.lastDayOfMonth());
                return new Rango("Este mes", desde, hoy, previoDesde, previoHasta.isAfter(finPrevio) ? finPrevio : previoHasta);
            }
            case "MES_ANT": {
                LocalDate desde = hoy.withDayOfMonth(1).minusMonths(1);
                LocalDate previoDesde = desde.minusMonths(1);
                return new Rango("Mes anterior", desde, desde.with(TemporalAdjusters.lastDayOfMonth()), previoDesde,
                        previoDesde.with(TemporalAdjusters.lastDayOfMonth()));
            }
            default:
                return ultimosDias("Últimos 30 días", 30, hoy);
        }
    }

    private static Rango ultimosDias(String nombre, int dias, LocalDate hoy) {
        LocalDate desde = hoy.minusDays(dias - 1L);
        return new Rango(nombre, desde, hoy, desde.minusDays(dias), desde.minusDays(1));
    }

    // ---------- cifras con variación ----------

    /** {texto, clase}: cuánto cambió una cifra respecto del período anterior y si eso es bueno (ok) o malo (off). */
    private static String[] variacion(double actual, double previo, boolean masEsMejor) {
        if (previo == 0) {
            return new String[]{actual == 0 ? "Sin cambios" : "Sin datos del período anterior", "muted"};
        }
        long pct = Math.round((actual - previo) * 100 / previo);
        String clase = pct == 0 ? "muted" : ((pct > 0) == masEsMejor ? "ok" : "off");
        return new String[]{(pct > 0 ? "+" : "") + pct + " % vs. período anterior", clase};
    }

    /** Igual que variacion, pero para porcentajes: la diferencia se dice en puntos. */
    private static String[] variacionPuntos(double actual, double previo, boolean masEsMejor) {
        long puntos = Math.round(actual - previo);
        String clase = puntos == 0 ? "muted" : ((puntos > 0) == masEsMejor ? "ok" : "off");
        return new String[]{puntos == 0 ? "Sin cambios" : (puntos > 0 ? "+" : "") + puntos + " puntos vs. período anterior", clase};
    }

    private static ReporteKpi kpi(String etiqueta, String icono, String valor, double numero, String sub, String[] var,
            boolean alerta) {
        return new ReporteKpi(etiqueta, icono, valor, numero, sub, var == null ? "" : var[0], var == null ? "muted" : var[1],
                alerta);
    }

    private static String monto(BigDecimal m) {
        return DashboardServicio.monto(m);
    }

    private static String pct(double v) {
        return Math.round(v) + " %";
    }

    private static int porcentaje(BigDecimal parte, BigDecimal maximo) {
        return maximo.signum() == 0 ? 0 : parte.multiply(BigDecimal.valueOf(100)).divide(maximo, 0, RoundingMode.HALF_UP).intValue();
    }

    private static double promedio(BigDecimal total, long cantidad) {
        return cantidad == 0 ? 0 : total.doubleValue() / cantidad;
    }

    // ---------- ingresos ----------

    private List<Ingreso> ingresos(Datos d, Rango r, boolean previo, Predicate<Ingreso> filtro) {
        return d.ingresos.stream().filter(i -> previo ? r.contienePrevio(i.fecha) : r.contiene(i.fecha))
                .filter(filtro).collect(Collectors.toList());
    }

    private static BigDecimal suma(List<Ingreso> l) {
        return l.stream().map(i -> i.monto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total, reservas, membresías y ticket promedio del período, cada uno comparado con el anterior. */
    public List<ReporteKpi> balance(Datos d, Rango r) {
        List<Ingreso> res = ingresos(d, r, false, i -> i.reserva);
        List<Ingreso> resPrev = ingresos(d, r, true, i -> i.reserva);
        List<Ingreso> mem = ingresos(d, r, false, i -> !i.reserva);
        List<Ingreso> memPrev = ingresos(d, r, true, i -> !i.reserva);
        BigDecimal totalRes = suma(res);
        BigDecimal totalMem = suma(mem);
        BigDecimal total = totalRes.add(totalMem);
        BigDecimal totalPrev = suma(resPrev).add(suma(memPrev));
        double ticket = promedio(totalRes, res.size());
        double ticketPrev = promedio(suma(resPrev), resPrev.size());

        List<ReporteKpi> kpis = new ArrayList<>();
        kpis.add(kpi("Ingresos totales", "payments", monto(total), total.doubleValue(),
                "antes: " + monto(totalPrev), variacion(total.doubleValue(), totalPrev.doubleValue(), true), false));
        kpis.add(kpi("Por reservas", "event_available", monto(totalRes), totalRes.doubleValue(),
                res.size() + (res.size() == 1 ? " reserva cobrada" : " reservas cobradas"),
                variacion(totalRes.doubleValue(), suma(resPrev).doubleValue(), true), false));
        kpis.add(kpi("Por membresías", "card_membership", monto(totalMem), totalMem.doubleValue(),
                mem.size() + (mem.size() == 1 ? " cuota cobrada" : " cuotas cobradas"),
                variacion(totalMem.doubleValue(), suma(memPrev).doubleValue(), true), false));
        kpis.add(kpi("Ticket promedio por reserva", "sell", monto(BigDecimal.valueOf(ticket).setScale(0, RoundingMode.HALF_UP)),
                Math.round(ticket), "por reserva cobrada", variacion(ticket, ticketPrev, true), false));
        return kpis;
    }

    /** Barras apiladas (reservas y membresías) por día, semana o mes. gran: "" (según el largo del período), DIA, SEMANA o MES. */
    public List<ReporteBarra> evolucion(Datos d, Rango r, String gran) {
        String g = !gran.isEmpty() ? gran : r.getDias() <= 35 ? "DIA" : r.getDias() <= 120 ? "SEMANA" : "MES";
        Map<LocalDate, BigDecimal[]> cubos = new LinkedHashMap<>();
        for (LocalDate c = inicio(g, r.desde); !c.isAfter(r.hasta); c = siguiente(g, c)) {
            cubos.put(c, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Ingreso i : ingresos(d, r, false, x -> true)) {
            BigDecimal[] cubo = cubos.get(inicio(g, i.fecha));
            cubo[i.reserva ? 0 : 1] = cubo[i.reserva ? 0 : 1].add(i.monto);
        }
        BigDecimal maximo = cubos.values().stream().map(c -> c[0].add(c[1])).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        int paso = (int) Math.ceil(cubos.size() / 10.0);
        List<ReporteBarra> barras = new ArrayList<>();
        int n = 0;
        for (Map.Entry<LocalDate, BigDecimal[]> e : cubos.entrySet()) {
            String etiqueta = "MES".equals(g) ? e.getKey().format(MES_ANIO).replace(".", "") : e.getKey().format(DIA_MES);
            BigDecimal[] c = e.getValue();
            String titulo = ("SEMANA".equals(g) ? "Semana del " : "") + etiqueta + ": " + monto(c[0].add(c[1]))
                    + " (reservas " + monto(c[0]) + ", membresías " + monto(c[1]) + ")";
            barras.add(new ReporteBarra(etiqueta, n++ % paso == 0, c[0], c[1], porcentaje(c[0], maximo),
                    porcentaje(c[1], maximo), titulo));
        }
        return barras;
    }

    private static LocalDate inicio(String g, LocalDate f) {
        return "SEMANA".equals(g) ? f.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : "MES".equals(g) ? f.withDayOfMonth(1) : f;
    }

    private static LocalDate siguiente(String g, LocalDate f) {
        return "SEMANA".equals(g) ? f.plusWeeks(1) : "MES".equals(g) ? f.plusMonths(1) : f.plusDays(1);
    }

    /** Cuánto facturó cada instalación por reservas en el período (la que más y la que menos). */
    public List<ReporteItem> ingresosPorInstalacion(Datos d, Rango r) {
        List<Ingreso> lista = ingresos(d, r, false, i -> i.reserva);
        BigDecimal total = suma(lista);
        Map<String, BigDecimal> montos = new HashMap<>();
        Map<String, Long> cantidades = new HashMap<>();
        for (Ingreso i : lista) {
            montos.merge(i.instalacion, i.monto, BigDecimal::add);
            cantidades.merge(i.instalacion, 1L, Long::sum);
        }
        BigDecimal maximo = montos.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<ReporteItem> items = new ArrayList<>();
        for (Instalacion i : d.instalaciones) {
            BigDecimal m = montos.getOrDefault(i.getNombre(), BigDecimal.ZERO);
            long n = cantidades.getOrDefault(i.getNombre(), 0L);
            items.add(new ReporteItem(i.getNombre(), DashboardServicio.icono(i.getTipoDisciplina()), monto(m),
                    m.doubleValue(), n + (n == 1 ? " reserva" : " reservas") + " · " + pct(total.signum() == 0 ? 0
                    : m.doubleValue() * 100 / total.doubleValue()) + " del total", porcentaje(m, maximo), 0, ""));
        }
        items.sort(Comparator.comparingDouble(ReporteItem::getNumero).reversed());
        return items;
    }

    /** Lo que aporta cada tipo de socio: por reservas y por membresías, apilado. */
    public List<ReporteItem> ingresosPorCategoria(Datos d, Rango r) {
        List<Ingreso> lista = ingresos(d, r, false, i -> true);
        Map<String, BigDecimal[]> montos = new LinkedHashMap<>();
        for (String c : d.categorias) {
            montos.put(c, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Ingreso i : lista) {
            BigDecimal[] par = montos.get(i.categoria);
            par[i.reserva ? 0 : 1] = par[i.reserva ? 0 : 1].add(i.monto);
        }
        BigDecimal maximo = montos.values().stream().map(p -> p[0].add(p[1])).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal total = suma(lista);
        List<ReporteItem> items = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : montos.entrySet()) {
            BigDecimal[] p = e.getValue();
            BigDecimal suma = p[0].add(p[1]);
            items.add(new ReporteItem(e.getKey(), "group", monto(suma), suma.doubleValue(),
                    "Reservas " + monto(p[0]) + " · Membresías " + monto(p[1]) + " · " + pct(total.signum() == 0 ? 0
                    : suma.doubleValue() * 100 / total.doubleValue()) + " del total",
                    porcentaje(p[0], maximo), porcentaje(p[1], maximo), ""));
        }
        items.sort(Comparator.comparingDouble(ReporteItem::getNumero).reversed());
        return items;
    }

    /**
     * Dinero que no es ingreso del período pero importa: el perdido (créditos de reprogramación que vencieron sin usarse),
     * el retenido en créditos vigentes, el que espera validación y lo que se espera cobrar por renovaciones.
     */
    public List<ReporteKpi> dineroEnJuego(Datos d, Rango r) {
        BigDecimal perdido = BigDecimal.ZERO;
        BigDecimal perdidoPrev = BigDecimal.ZERO;
        BigDecimal retenido = BigDecimal.ZERO;
        int perdidos = 0;
        int vigentes = 0;
        for (Reserva x : d.reservas) {
            LocalDate limite = x.getFechaLimiteReprogramacion();
            if (x.getEstado() != Reserva.Estado.CANCELADA || limite == null || !cobrado(x.getIdPago())) {
                continue;
            }
            BigDecimal m = x.getIdPago().getMonto();
            if (limite.isBefore(d.hoy)) {
                if (r.contiene(limite)) {
                    perdido = perdido.add(m);
                    perdidos++;
                } else if (r.contienePrevio(limite)) {
                    perdidoPrev = perdidoPrev.add(m);
                }
            } else {
                retenido = retenido.add(m);
                vigentes++;
            }
        }

        BigDecimal pendiente = BigDecimal.ZERO;
        int pendientes = 0;
        for (Reserva x : d.reservas) {
            if (x.getEstado() == Reserva.Estado.PENDIENTE_PAGO && porValidar(x.getIdPago())) {
                pendiente = pendiente.add(x.getIdPago().getMonto());
                pendientes++;
            }
        }
        for (SuscripcionSocio s : d.suscripciones) {
            if (s.getEstado() == SuscripcionSocio.Estado.PENDIENTE_PAGO && porValidar(s.getIdPago())) {
                pendiente = pendiente.add(s.getIdPago().getMonto());
                pendientes++;
            }
        }

        BigDecimal potencial = BigDecimal.ZERO;
        int porRenovar = 0;
        for (SuscripcionSocio s : d.suscripciones) {
            if (s.getEstado() == SuscripcionSocio.Estado.VIGENTE && s.getFechaVencimiento() != null
                    && !s.getFechaVencimiento().isBefore(d.hoy) && !s.getFechaVencimiento().isAfter(d.hoy.plusDays(30))) {
                potencial = potencial.add(d.cuotas.getOrDefault(s.getIdSocio().getIdCategoriaSocio().getNombreCategoria(),
                        BigDecimal.ZERO));
                porRenovar++;
            }
        }
        double tasa = tasaRenovacion(d, LocalDate.of(2000, 1, 1), d.hoy);
        BigDecimal estimado = potencial.multiply(BigDecimal.valueOf(tasa)).setScale(0, RoundingMode.HALF_UP);

        List<ReporteKpi> kpis = new ArrayList<>();
        kpis.add(kpi("Dinero perdido", "event_busy", monto(perdido), perdido.doubleValue(),
                perdidos + (perdidos == 1 ? " crédito venció" : " créditos vencieron") + " sin usarse",
                variacion(perdido.doubleValue(), perdidoPrev.doubleValue(), false), perdido.signum() > 0));
        kpis.add(kpi("En créditos vigentes", "event_repeat", monto(retenido), retenido.doubleValue(),
                vigentes + (vigentes == 1 ? " crédito" : " créditos") + " por reprogramar; si vencen se pierde", null, false));
        kpis.add(kpi("Pendiente de validar", "pending_actions", monto(pendiente), pendiente.doubleValue(),
                pendientes + (pendientes == 1 ? " comprobante" : " comprobantes") + " esperan al administrador", null,
                pendientes > 0));
        kpis.add(kpi("Proyección de membresías", "trending_up", monto(estimado), estimado.doubleValue(),
                "próximos 30 días: vencen " + porRenovar + " (" + monto(potencial) + " si renuevan todas), según una renovación del "
                + pct(tasa * 100), null, false));
        return kpis;
    }

    private static boolean porValidar(Pago p) {
        return p != null && p.getEstado() == Pago.Estado.PENDIENTE_VALIDACION;
    }

    // ---------- ocupación ----------

    private static boolean ocupa(Reserva r) {
        return r.getEstado() == Reserva.Estado.CONFIRMADA || r.getEstado() == Reserva.Estado.PENDIENTE_PAGO;
    }

    /** Días del rango en que la instalación estuvo en mantenimiento (solo se conoce la baja vigente). */
    private static int diasMantenimiento(Datos d, Instalacion i, LocalDate desde, LocalDate hasta) {
        if (i.getEstado() != Instalacion.Estado.DESHABILITADA_MANTENIMIENTO || i.getFechaInicioBaja() == null) {
            return 0;
        }
        LocalDate fin = i.getFechaFinBaja() == null || i.getFechaFinBaja().isAfter(d.hoy) ? d.hoy : i.getFechaFinBaja();
        LocalDate ini = i.getFechaInicioBaja().isAfter(desde) ? i.getFechaInicioBaja() : desde;
        LocalDate topeFin = fin.isBefore(hasta) ? fin : hasta;
        return topeFin.isBefore(ini) ? 0 : (int) ChronoUnit.DAYS.between(ini, topeFin) + 1;
    }

    /** {turnos reservados, turnos disponibles} de todas las instalaciones en el rango. */
    private double[] ocupacion(Datos d, LocalDate desde, LocalDate hasta) {
        double reservados = 0;
        double disponibles = 0;
        for (Instalacion i : d.instalaciones) {
            disponibles += (double) d.turnos.get(i.getIdInstalacion()).size()
                    * (ChronoUnit.DAYS.between(desde, hasta) + 1 - diasMantenimiento(d, i, desde, hasta));
        }
        for (Reserva x : d.reservas) {
            if (ocupa(x) && !x.getFechaTurno().isBefore(desde) && !x.getFechaTurno().isAfter(hasta)) {
                reservados++;
            }
        }
        return new double[]{reservados, disponibles};
    }

    private static double porcentajeDe(double[] par) {
        return par[1] == 0 ? 0 : Math.min(100, par[0] * 100 / par[1]);
    }

    public double ocupacionGeneral(Datos d, Rango r) {
        return porcentajeDe(ocupacion(d, r.desde, r.hasta));
    }

    public List<ReporteKpi> ocupacionKpis(Datos d, Rango r) {
        double[] actual = ocupacion(d, r.desde, r.hasta);
        double[] previo = ocupacion(d, r.previoDesde, r.previoHasta);
        int diasMant = 0;
        BigDecimal perdido = BigDecimal.ZERO;
        for (Instalacion i : d.instalaciones) {
            int dias = diasMantenimiento(d, i, r.desde, r.hasta);
            diasMant += dias;
            perdido = perdido.add(d.precioPromedio.getOrDefault(i.getIdInstalacion(), BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf((long) dias * d.turnos.get(i.getIdInstalacion()).size())));
        }
        List<ReporteKpi> kpis = new ArrayList<>();
        kpis.add(kpi("Ocupación general", "stadium", pct(porcentajeDe(actual)), Math.round(porcentajeDe(actual)),
                "turnos reservados sobre disponibles", variacionPuntos(porcentajeDe(actual), porcentajeDe(previo), true), false));
        kpis.add(kpi("Turnos reservados", "event_available", String.valueOf(Math.round(actual[0])), actual[0],
                "de " + Math.round(actual[1]) + " disponibles", variacion(actual[0], previo[0], true), false));
        kpis.add(kpi("Días de mantenimiento", "build", String.valueOf(diasMant), diasMant,
                "sumando todas las instalaciones", null, diasMant > 0));
        kpis.add(kpi("Ingreso no percibido", "payments", monto(perdido), perdido.doubleValue(),
                "estimado: turnos sin poder ofrecer por la tarifa promedio", null, perdido.signum() > 0));
        return kpis;
    }

    /** Ocupación de cada instalación: verde desde 50 %, ámbar desde 30 %, rojo por debajo. */
    public List<ReporteItem> ocupacionPorInstalacion(Datos d, Rango r) {
        List<ReporteItem> items = new ArrayList<>();
        for (Instalacion i : d.instalaciones) {
            int turnos = d.turnos.get(i.getIdInstalacion()).size();
            double disponibles = (double) turnos * (r.getDias() - diasMantenimiento(d, i, r.desde, r.hasta));
            long reservados = d.reservas.stream().filter(x -> ocupa(x) && r.contiene(x.getFechaTurno())
                    && x.getIdTurno().getIdInstalacion().getIdInstalacion().equals(i.getIdInstalacion())).count();
            double p = disponibles == 0 ? 0 : Math.min(100, reservados * 100 / disponibles);
            String icono = DashboardServicio.icono(i.getTipoDisciplina());
            items.add(disponibles == 0
                    ? new ReporteItem(i.getNombre(), icono, "—", 0, "En mantenimiento todo el período", 0, 0, "")
                    : new ReporteItem(i.getNombre(), icono, pct(p), Math.round(p), reservados + " de "
                    + Math.round(disponibles) + " turnos", (int) Math.round(p), 0, p >= 50 ? "ok" : p >= 30 ? "warn" : "off"));
        }
        items.sort(Comparator.comparingDouble(ReporteItem::getNumero));
        return items;
    }

    /** [hora][día de la semana 0=lunes] = {reservados, disponibles} de todas las instalaciones. */
    private double[][][] matriz(Datos d, Rango r, List<LocalTime> horas) {
        double[][][] m = new double[horas.size()][7][2];
        for (LocalDate dia = r.desde; !dia.isAfter(r.hasta); dia = dia.plusDays(1)) {
            for (Instalacion i : d.instalaciones) {
                if (diasMantenimiento(d, i, dia, dia) > 0) {
                    continue;
                }
                for (Turno t : d.turnos.get(i.getIdInstalacion())) {
                    m[horas.indexOf(t.getHoraInicio())][dia.getDayOfWeek().getValue() - 1][1]++;
                }
            }
        }
        for (Reserva x : d.reservas) {
            if (ocupa(x) && r.contiene(x.getFechaTurno())) {
                m[horas.indexOf(x.getIdTurno().getHoraInicio())][x.getFechaTurno().getDayOfWeek().getValue() - 1][0]++;
            }
        }
        return m;
    }

    private static List<LocalTime> horas(Datos d) {
        TreeSet<LocalTime> horas = new TreeSet<>();
        d.turnos.values().forEach(l -> l.forEach(t -> horas.add(t.getHoraInicio())));
        return new ArrayList<>(horas);
    }

    /** Ocupación por día de la semana y hora de inicio: se ven las horas pico y las vacías de un vistazo. */
    public ReporteCalor mapaDeCalor(Datos d, Rango r) {
        List<LocalTime> horas = horas(d);
        double[][][] m = matriz(d, r, horas);
        List<String> dias = new ArrayList<>();
        for (DayOfWeek dow : DayOfWeek.values()) {
            dias.add(dow.getDisplayName(TextStyle.SHORT, ES).replace(".", ""));
        }
        List<ReporteCalor.Fila> filas = new ArrayList<>();
        for (int h = 0; h < horas.size(); h++) {
            List<ReporteCalor.Celda> celdas = new ArrayList<>();
            for (int k = 0; k < 7; k++) {
                double[] par = m[h][k];
                int p = (int) Math.round(porcentajeDe(par));
                celdas.add(par[1] == 0 ? new ReporteCalor.Celda("—", 0, true, "Sin turnos")
                        : new ReporteCalor.Celda(p + " %", p, false, dias.get(k) + " " + horas.get(h).format(HORA) + ": "
                        + Math.round(par[0]) + " de " + Math.round(par[1]) + " turnos"));
            }
            filas.add(new ReporteCalor.Fila(horas.get(h).format(HORA), celdas));
        }
        return new ReporteCalor(dias, filas);
    }

    /** Las franjas (día y hora) con más ocupación (pico) o con menos (vacías): tres de cada una. */
    public List<ReporteItem> franjas(Datos d, Rango r, boolean picos) {
        List<LocalTime> horas = horas(d);
        double[][][] m = matriz(d, r, horas);
        List<ReporteItem> todas = new ArrayList<>();
        for (int h = 0; h < horas.size(); h++) {
            for (int k = 0; k < 7; k++) {
                if (m[h][k][1] > 0) {
                    double p = porcentajeDe(m[h][k]);
                    todas.add(new ReporteItem(DayOfWeek.of(k + 1).getDisplayName(TextStyle.FULL, ES) + " " + horas.get(h).format(HORA),
                            "schedule", pct(p), Math.round(p), Math.round(m[h][k][0]) + " de " + Math.round(m[h][k][1]) + " turnos",
                            (int) Math.round(p), 0, picos ? "ok" : "off"));
                }
            }
        }
        todas.sort(picos ? Comparator.comparingDouble(ReporteItem::getNumero).reversed()
                : Comparator.comparingDouble(ReporteItem::getNumero));
        return new ArrayList<>(todas.subList(0, Math.min(3, todas.size())));
    }

    /** Turnos de instalaciones habilitadas que no tuvieron ninguna reserva en el período, para promocionarlos o reubicarlos. */
    public List<ReporteItem> turnosSinReservas(Datos d, Rango r) {
        List<ReporteItem> items = new ArrayList<>();
        for (Instalacion i : d.instalaciones) {
            if (i.getEstado() != Instalacion.Estado.HABILITADA) {
                continue;
            }
            for (Turno t : d.turnos.get(i.getIdInstalacion())) {
                boolean reservado = d.reservas.stream().anyMatch(x -> ocupa(x) && r.contiene(x.getFechaTurno())
                        && x.getIdTurno().getIdTurno().equals(t.getIdTurno()));
                if (!reservado) {
                    items.add(new ReporteItem(i.getNombre() + " · " + t.getHoraInicio().format(HORA) + " – "
                            + t.getHoraFin().format(HORA), DashboardServicio.icono(i.getTipoDisciplina()), "0 reservas", 0,
                            "en " + r.getDias() + " días", 0, 0, "off"));
                }
            }
        }
        return items;
    }

    /** Instalaciones en mantenimiento en el período: días sin servicio y lo que se dejó de ganar (estimado). */
    public List<ReporteItem> mantenimiento(Datos d, Rango r) {
        List<ReporteItem> items = new ArrayList<>();
        for (Instalacion i : d.instalaciones) {
            int dias = diasMantenimiento(d, i, r.desde, r.hasta);
            if (dias > 0) {
                BigDecimal perdido = d.precioPromedio.getOrDefault(i.getIdInstalacion(), BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf((long) dias * d.turnos.get(i.getIdInstalacion()).size()));
                items.add(new ReporteItem(i.getNombre(), "build", dias + (dias == 1 ? " día" : " días"), dias,
                        "Ingreso no percibido estimado: " + monto(perdido)
                        + (i.getMotivoBaja() == null ? "" : " · " + i.getMotivoBaja()), 0, 0, "warn"));
            }
        }
        return items;
    }

    // ---------- membresías ----------

    /** Cantidad de suscripciones por estado (de siempre). */
    public List<ReporteItem> estadoSuscripciones(Datos d) {
        Map<SuscripcionSocio.Estado, Long> cuentas = d.suscripciones.stream()
                .collect(Collectors.groupingBy(SuscripcionSocio::getEstado, Collectors.counting()));
        long maximo = cuentas.values().stream().max(Long::compareTo).orElse(0L);
        String[][] filas = {
            {"VIGENTE", "Vigentes", "check_circle", "ok"},
            {"VENCIDA", "Vencidas", "schedule", "warn"},
            {"CANCELADA", "Canceladas", "event_busy", "off"},
            {"PENDIENTE_PAGO", "Por validar o pagar", "pending_actions", ""}};
        List<ReporteItem> items = new ArrayList<>();
        for (String[] f : filas) {
            long n = cuentas.getOrDefault(SuscripcionSocio.Estado.valueOf(f[0]), 0L);
            items.add(new ReporteItem(f[1], f[2], String.valueOf(n), n, "", maximo == 0 ? 0 : (int) (n * 100 / maximo), 0, f[3]));
        }
        return items;
    }

    /** Vigentes que vencen en cada una de las próximas cuatro semanas, con lo que se cobraría si todas renuevan. */
    public List<ReporteItem> vencimientosSemanales(Datos d) {
        long[] cantidades = new long[4];
        BigDecimal[] montos = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        for (SuscripcionSocio s : d.suscripciones) {
            if (s.getEstado() != SuscripcionSocio.Estado.VIGENTE || s.getFechaVencimiento() == null
                    || s.getFechaVencimiento().isBefore(d.hoy)) {
                continue;
            }
            long semana = ChronoUnit.DAYS.between(d.hoy, s.getFechaVencimiento()) / 7;
            if (semana < 4) {
                cantidades[(int) semana]++;
                montos[(int) semana] = montos[(int) semana].add(d.cuotas.getOrDefault(
                        s.getIdSocio().getIdCategoriaSocio().getNombreCategoria(), BigDecimal.ZERO));
            }
        }
        long maximo = 0;
        for (long c : cantidades) {
            maximo = Math.max(maximo, c);
        }
        List<ReporteItem> items = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            LocalDate ini = d.hoy.plusDays(7L * i);
            items.add(new ReporteItem(ini.format(DIA_MES) + " al " + ini.plusDays(6).format(DIA_MES), "calendar_month",
                    String.valueOf(cantidades[i]), cantidades[i], monto(montos[i]) + " si renuevan todas",
                    maximo == 0 ? 0 : (int) (cantidades[i] * 100 / maximo), 0, i == 0 && cantidades[i] > 0 ? "warn" : ""));
        }
        return items;
    }

    /** Cuántas de las membresías que terminaron renovaron (una renovación tardía hasta 15 días también cuenta). */
    private double tasaRenovacion(Datos d, LocalDate desde, LocalDate hasta) {
        int[] par = renovacion(d, desde, hasta);
        return par[1] == 0 ? 0 : (double) par[0] / par[1];
    }

    /** {renovadas, terminadas} entre las membresías cuyo vencimiento cayó en el rango. */
    private int[] renovacion(Datos d, LocalDate desde, LocalDate hasta) {
        Map<Integer, List<SuscripcionSocio>> porSocio = new HashMap<>();
        for (SuscripcionSocio s : d.suscripciones) {
            if ((s.getEstado() == SuscripcionSocio.Estado.VIGENTE || s.getEstado() == SuscripcionSocio.Estado.VENCIDA)
                    && s.getFechaInicio() != null && s.getFechaVencimiento() != null) {
                porSocio.computeIfAbsent(s.getIdSocio().getIdSocio(), k -> new ArrayList<>()).add(s);
            }
        }
        int renovadas = 0;
        int terminadas = 0;
        for (List<SuscripcionSocio> lista : porSocio.values()) {
            for (SuscripcionSocio s : lista) {
                LocalDate fin = s.getFechaVencimiento();
                if (!fin.isBefore(d.hoy) || fin.isBefore(desde) || fin.isAfter(hasta)) {
                    continue;
                }
                terminadas++;
                if (lista.stream().anyMatch(o -> o != s && o.getFechaInicio().isAfter(s.getFechaInicio())
                        && !o.getFechaInicio().isAfter(fin.plusDays(MARGEN_RENOVACION_DIAS)))) {
                    renovadas++;
                }
            }
        }
        return new int[]{renovadas, terminadas};
    }

    /** Minutos medios entre que el socio pagó y el administrador validó el comprobante, de los pagos validados en el rango. */
    private double minutosDeValidacion(Datos d, Rango r, boolean previo, boolean reserva) {
        return ingresos(d, r, previo, i -> i.reserva == reserva).stream()
                .mapToLong(i -> Math.max(0, Duration.between(i.pagado, i.validado).toMinutes())).average().orElse(0);
    }

    static String duracion(double minutos) {
        long m = Math.round(minutos);
        if (m < 1) {
            return "menos de 1 min";
        }
        if (m < 60) {
            return m + " min";
        }
        if (m < 60 * 24) {
            return m / 60 + " h" + (m % 60 > 0 ? " " + m % 60 + " min" : "");
        }
        return m / (60 * 24) + " d" + (m / 60 % 24 > 0 ? " " + m / 60 % 24 + " h" : "");
    }

    public List<ReporteKpi> membresiasKpis(Datos d, Rango r) {
        long vigentes = d.suscripciones.stream().filter(s -> s.getEstado() == SuscripcionSocio.Estado.VIGENTE).count();
        int[] actual = renovacion(d, r.desde, r.hasta);
        int[] previo = renovacion(d, r.previoDesde, r.previoHasta);
        double tasa = actual[1] == 0 ? 0 : actual[0] * 100.0 / actual[1];
        double tasaPrevia = previo[1] == 0 ? 0 : previo[0] * 100.0 / previo[1];
        double minutos = minutosDeValidacion(d, r, false, false);
        double minutosPrevios = minutosDeValidacion(d, r, true, false);

        List<ReporteKpi> kpis = new ArrayList<>();
        kpis.add(kpi("Membresías vigentes", "card_membership", String.valueOf(vigentes), vigentes,
                "de " + d.sociosTotal + " socios", null, false));
        kpis.add(kpi("Tasa de renovación", "event_repeat", actual[1] == 0 ? "—" : pct(tasa), Math.round(tasa),
                actual[0] + " de " + actual[1] + " membresías que terminaron",
                actual[1] == 0 || previo[1] == 0 ? null : variacionPuntos(tasa, tasaPrevia, true), actual[1] > 0 && tasa < 60));
        kpis.add(kpi("Validación de comprobantes", "schedule", duracion(minutos), Math.round(minutos),
                "tiempo medio en las membresías", minutosPrevios == 0 ? null : variacion(minutos, minutosPrevios, false), false));
        return kpis;
    }

    /** Ingreso de cada tipo de socio por membresías: cuántas cuotas y a qué precio. */
    public List<ReporteItem> membresiasPorCategoria(Datos d, Rango r) {
        List<Ingreso> lista = ingresos(d, r, false, i -> !i.reserva);
        BigDecimal total = suma(lista);
        Map<String, BigDecimal> montos = new HashMap<>();
        Map<String, Long> cantidades = new HashMap<>();
        for (Ingreso i : lista) {
            montos.merge(i.categoria, i.monto, BigDecimal::add);
            cantidades.merge(i.categoria, 1L, Long::sum);
        }
        BigDecimal maximo = montos.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<ReporteItem> items = new ArrayList<>();
        for (String c : d.categorias) {
            BigDecimal m = montos.getOrDefault(c, BigDecimal.ZERO);
            long n = cantidades.getOrDefault(c, 0L);
            items.add(new ReporteItem(c, "group", monto(m), m.doubleValue(), n + (n == 1 ? " cuota" : " cuotas")
                    + " · cuota actual " + monto(d.cuotas.getOrDefault(c, BigDecimal.ZERO)) + " · "
                    + pct(total.signum() == 0 ? 0 : m.doubleValue() * 100 / total.doubleValue()) + " del total",
                    porcentaje(m, maximo), 0, ""));
        }
        items.sort(Comparator.comparingDouble(ReporteItem::getNumero).reversed());
        return items;
    }

    /** Tiempo medio de validación de los comprobantes de reservas y de membresías. */
    public List<ReporteItem> validacion(Datos d, Rango r) {
        double res = minutosDeValidacion(d, r, false, true);
        double mem = minutosDeValidacion(d, r, false, false);
        double maximo = Math.max(res, mem);
        List<ReporteItem> items = new ArrayList<>();
        items.add(new ReporteItem("Reservas", "event_available", duracion(res), Math.round(res),
                "desde que el socio paga hasta que el administrador valida", maximo == 0 ? 0 : (int) (res * 100 / maximo), 0, ""));
        items.add(new ReporteItem("Membresías", "card_membership", duracion(mem), Math.round(mem),
                "desde que el socio paga hasta que el administrador valida", maximo == 0 ? 0 : (int) (mem * 100 / maximo), 0, ""));
        return items;
    }

    // ---------- resumen ejecutivo ----------

    /** Las ocho cifras que importan, cada una con su variación. */
    public List<ReporteKpi> resumen(Datos d, Rango r) {
        List<Ingreso> actual = ingresos(d, r, false, i -> true);
        List<Ingreso> previo = ingresos(d, r, true, i -> true);
        BigDecimal total = suma(actual);
        BigDecimal totalPrevio = suma(previo);

        long confirmadas = d.reservas.stream().filter(x -> x.getEstado() == Reserva.Estado.CONFIRMADA
                && r.contiene(x.getFechaTurno())).count();
        long confirmadasPrevias = d.reservas.stream().filter(x -> x.getEstado() == Reserva.Estado.CONFIRMADA
                && r.contienePrevio(x.getFechaTurno())).count();

        List<Ingreso> res = ingresos(d, r, false, i -> i.reserva);
        List<Ingreso> resPrev = ingresos(d, r, true, i -> i.reserva);
        double ticket = promedio(suma(res), res.size());
        double ticketPrevio = promedio(suma(resPrev), resPrev.size());

        double ocupacion = porcentajeDe(ocupacion(d, r.desde, r.hasta));
        double ocupacionPrevia = porcentajeDe(ocupacion(d, r.previoDesde, r.previoHasta));

        int[] ren = renovacion(d, r.desde, r.hasta);
        int[] renPrev = renovacion(d, r.previoDesde, r.previoHasta);
        double tasa = ren[1] == 0 ? 0 : ren[0] * 100.0 / ren[1];
        double tasaPrevia = renPrev[1] == 0 ? 0 : renPrev[0] * 100.0 / renPrev[1];

        double valoracion = valoracionPromedio(d, r, false);
        double valoracionPrevia = valoracionPromedio(d, r, true);

        long canceladas = d.reservas.stream().filter(x -> x.getEstado() == Reserva.Estado.CANCELADA
                && r.contiene(x.getFechaTurno())).count();
        long canceladasPrevias = d.reservas.stream().filter(x -> x.getEstado() == Reserva.Estado.CANCELADA
                && r.contienePrevio(x.getFechaTurno())).count();
        long conTurno = d.reservas.stream().filter(x -> x.getEstado() != Reserva.Estado.RECHAZADA
                && r.contiene(x.getFechaTurno())).count();

        List<ReporteKpi> kpis = new ArrayList<>();
        kpis.add(kpi("Ingresos totales", "payments", monto(total), total.doubleValue(),
                "reservas y membresías", variacion(total.doubleValue(), totalPrevio.doubleValue(), true), false));
        kpis.add(kpi("Reservas realizadas", "event_available", String.valueOf(confirmadas), confirmadas,
                "confirmadas en el período", variacion(confirmadas, confirmadasPrevias, true), false));
        kpis.add(kpi("Ocupación", "stadium", pct(ocupacion), Math.round(ocupacion), "turnos reservados sobre disponibles",
                variacionPuntos(ocupacion, ocupacionPrevia, true), false));
        kpis.add(kpi("Ticket promedio", "sell", monto(BigDecimal.valueOf(ticket).setScale(0, RoundingMode.HALF_UP)),
                Math.round(ticket), "por reserva cobrada", variacion(ticket, ticketPrevio, true), false));
        kpis.add(kpi("Socios activos", "group", String.valueOf(d.sociosActivos), d.sociosActivos,
                "de " + d.sociosTotal + " socios registrados", null, false));
        kpis.add(kpi("Renovación de membresías", "card_membership", ren[1] == 0 ? "—" : pct(tasa), Math.round(tasa),
                ren[0] + " de " + ren[1] + " que terminaron",
                ren[1] == 0 || renPrev[1] == 0 ? null : variacionPuntos(tasa, tasaPrevia, true), ren[1] > 0 && tasa < 60));
        kpis.add(kpi("Valoración promedio", "star", valoracion == 0 ? "—" : ValoracionesServicio.decimal(valoracion),
                valoracion, "sobre 5 estrellas", valoracion == 0 || valoracionPrevia == 0 ? null
                : variacion(valoracion, valoracionPrevia, true), valoracion > 0 && valoracion < VALORACION_BAJA));
        kpis.add(kpi("Cancelaciones", "event_busy", String.valueOf(canceladas), canceladas,
                pct(conTurno == 0 ? 0 : canceladas * 100.0 / conTurno) + " de las reservas del período",
                variacion(canceladas, canceladasPrevias, false), false));
        return kpis;
    }

    private static double valoracionPromedio(Datos d, Rango r, boolean previo) {
        return d.valoraciones.stream().filter(v -> previo ? r.contienePrevio(v.getFecha().toLocalDate())
                : r.contiene(v.getFecha().toLocalDate())).mapToInt(ValoracionFila::getPuntaje).average().orElse(0);
    }

    /** Avisos de lo que conviene mirar hoy; si no hay ninguno, uno solo diciendo que está todo en orden. */
    public List<ReporteAlerta> alertas(Datos d, Rango r) {
        List<ReporteAlerta> lista = new ArrayList<>();

        long vencen = 0;
        BigDecimal enJuego = BigDecimal.ZERO;
        for (SuscripcionSocio s : d.suscripciones) {
            if (s.getEstado() == SuscripcionSocio.Estado.VIGENTE && s.getFechaVencimiento() != null
                    && !s.getFechaVencimiento().isBefore(d.hoy) && !s.getFechaVencimiento().isAfter(d.hoy.plusDays(DIAS_DE_AVISO))) {
                vencen++;
                enJuego = enJuego.add(d.cuotas.getOrDefault(s.getIdSocio().getIdCategoriaSocio().getNombreCategoria(), BigDecimal.ZERO));
            }
        }
        if (vencen > 0) {
            lista.add(new ReporteAlerta("card_membership", vencen + (vencen == 1 ? " membresía vence" : " membresías vencen")
                    + " en los próximos " + DIAS_DE_AVISO + " días (" + monto(enJuego) + " si renuevan)", "warn"));
        }

        long porValidar = d.reservas.stream().filter(x -> x.getEstado() == Reserva.Estado.PENDIENTE_PAGO && porValidar(x.getIdPago())).count()
                + d.suscripciones.stream().filter(s -> s.getEstado() == SuscripcionSocio.Estado.PENDIENTE_PAGO && porValidar(s.getIdPago())).count();
        if (porValidar > 0) {
            lista.add(new ReporteAlerta("pending_actions", porValidar + (porValidar == 1 ? " comprobante espera" : " comprobantes esperan")
                    + " validación del administrador", "warn"));
        }

        Map<String, Double> promedios = d.valoraciones.stream().collect(Collectors.groupingBy(ValoracionFila::getInstalacion,
                Collectors.averagingInt(ValoracionFila::getPuntaje)));
        String bajas = promedios.entrySet().stream().filter(e -> e.getValue() < VALORACION_BAJA)
                .sorted(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + ValoracionesServicio.decimal(e.getValue()) + ")").collect(Collectors.joining(", "));
        if (!bajas.isEmpty()) {
            lista.add(new ReporteAlerta("star", "Baja valoración de los socios: " + bajas, "off"));
        }

        long vacios = turnosSinReservas(d, r).size();
        if (vacios > 0) {
            lista.add(new ReporteAlerta("schedule", vacios + (vacios == 1 ? " turno no tuvo" : " turnos no tuvieron")
                    + " ninguna reserva en el período (franjas vacías)", "warn"));
        }

        BigDecimal porPerderse = BigDecimal.ZERO;
        long creditos = 0;
        for (Reserva x : d.reservas) {
            LocalDate limite = x.getFechaLimiteReprogramacion();
            if (x.getEstado() == Reserva.Estado.CANCELADA && limite != null && cobrado(x.getIdPago())
                    && !limite.isBefore(d.hoy) && !limite.isAfter(d.hoy.plusDays(DIAS_DE_AVISO))) {
                creditos++;
                porPerderse = porPerderse.add(x.getIdPago().getMonto());
            }
        }
        if (creditos > 0) {
            lista.add(new ReporteAlerta("event_repeat", creditos + (creditos == 1 ? " crédito de reprogramación vence" : " créditos de reprogramación vencen")
                    + " en " + DIAS_DE_AVISO + " días (" + monto(porPerderse) + " que se perderían)", "warn"));
        }

        for (Instalacion i : d.instalaciones) {
            if (i.getEstado() == Instalacion.Estado.DESHABILITADA_MANTENIMIENTO) {
                lista.add(new ReporteAlerta("build", i.getNombre() + " está en mantenimiento"
                        + (i.getFechaFinBaja() == null ? "" : " hasta el " + i.getFechaFinBaja().format(FECHA)), "warn"));
            }
        }

        if (lista.isEmpty()) {
            lista.add(new ReporteAlerta("task_alt", "Todo en orden: no hay nada que requiera atención.", "ok"));
        }
        return lista;
    }
}
