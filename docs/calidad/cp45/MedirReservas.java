import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dto.FiltroReservas;
import com.sigrid.sigrid.dto.InformeReservas;
import com.sigrid.sigrid.dto.PaginaReservas;
import com.sigrid.sigrid.dto.ReporteKpi;
import com.sigrid.sigrid.dto.ReservaListado;
import com.sigrid.sigrid.dto.ResumenReservas;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.servicio.ReportesServicio;
import com.sigrid.sigrid.servicio.ReservaServicio;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Predicate;

/**
 * Medición del caso CP-45 del plan (CC-06): ejecuta las consultas y los servicios reales de SIGRID contra una base de
 * PRUEBA con 500.000 reservas (carga_500k.sql). No es parte del proyecto Maven ni de las pruebas unitarias: se compila y
 * se corre aparte (ver README.md). Modos: "nuevo" (tiempos y memoria de lo que usan hoy las pantallas), "anterior"
 * (cargar todas las reservas, como hacía la versión previa) y "equivalencia" (los filtros en SQL dan lo mismo que
 * clasificar cada reserva en memoria).
 */
public class MedirReservas {

    static EntityManager em;
    static long t0;

    /** Arma un objeto de SIGRID y le inyecta sus dependencias (@Inject) y el EntityManager, sin contenedor. */
    static Object crear(Class<?> c) throws Exception {
        Constructor<?> k = c.getDeclaredConstructor();
        k.setAccessible(true);
        Object o = k.newInstance();
        for (Class<?> x = c; x != null; x = x.getSuperclass()) {
            for (Field f : x.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.isAnnotationPresent(Inject.class)) {
                    f.set(o, crear(f.getType()));
                } else if (f.isAnnotationPresent(PersistenceContext.class)) {
                    f.set(o, em);
                }
            }
        }
        return o;
    }

    static void ini() {
        em.clear();
        System.gc();
        t0 = System.nanoTime();
    }

    static void fin(String que, Object dato) {
        long ms = (System.nanoTime() - t0) / 1_000_000;
        Runtime r = Runtime.getRuntime();
        System.out.println(String.format("%-72s %7d ms | %s | heap %d MB", que, ms, dato,
                (r.totalMemory() - r.freeMemory()) / (1024 * 1024)));
    }

    static String informe(InformeReservas inf) {
        return (inf.isListo() ? "LISTO" : "AVISO: " + inf.getAviso()) + " (" + inf.getFilas().size() + " filas)";
    }

    static FiltroReservas filtro(String estado, String disciplina, LocalDate desde, LocalDate hasta, Boolean finalizadas) {
        FiltroReservas f = new FiltroReservas();
        f.setEstado(estado);
        f.setDisciplina(disciplina);
        f.setDesde(desde);
        f.setHasta(hasta);
        f.setFinalizadas(finalizadas);
        return f;
    }

    public static void main(String[] args) throws Exception {
        String modo = args.length > 0 ? args[0] : "nuevo";
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("pruebaPU");
        em = emf.createEntityManager();
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 13, 10, 0);
        LocalDate hoy = ahora.toLocalDate();
        LocalTime hora = ahora.toLocalTime();
        System.out.println("memoria máxima: " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB; reservas en la base: "
                + em.createQuery("SELECT COUNT(r) FROM Reserva r", Long.class).getSingleResult());

        ReservaDAO dao = (ReservaDAO) crear(ReservaDAO.class);
        ReservaServicio srv = (ReservaServicio) crear(ReservaServicio.class);

        if (modo.equals("anterior")) {
            // la consulta que usaban la página de reservas y los reportes antes de CC-06: todas las filas, sin filtros
            ini();
            List<Reserva> todas = em.createQuery("SELECT r FROM Reserva r JOIN FETCH r.idTurno t JOIN FETCH t.idInstalacion "
                    + "JOIN FETCH r.idSocio s JOIN FETCH s.idUsuario LEFT JOIN FETCH r.idPago LEFT JOIN FETCH r.idTarifa "
                    + "ORDER BY r.fechaTurno DESC, t.horaInicio DESC, r.idReserva DESC", Reserva.class).getResultList();
            fin("ANTERIOR: cargar todas las reservas (con turno, socio, pago y tarifa)", todas.size() + " filas");
            return;
        }

        if (modo.equals("equivalencia")) {
            // todas las reservas como las clasifica el servicio en memoria (estado, finalizada, disciplina, fecha)
            PaginaReservas todas = srv.listarPagina(new FiltroReservas(), Integer.MAX_VALUE, hoy, hora);
            System.out.println("clasificadas en memoria: " + todas.getFilas().size());
            LocalDate d1 = LocalDate.of(2026, 9, 15);
            LocalDate d2 = LocalDate.of(2026, 10, 20);
            int malos = 0;
            String[][] casos = {{"CONFIRMADA"}, {"CANCELADA"}, {"RECHAZADA"}, {"POR_CONFIRMAR"}, {"ESPERANDO_PAGO"}};
            for (String[] c : casos) {
                malos += comparar("estado " + c[0], filtro(c[0], "", null, null, null), todas.getFilas(),
                        r -> r.getEstado().equals(c[0]), dao, hoy, hora);
                malos += comparar("estado " + c[0] + " entre " + d1 + " y " + d2, filtro(c[0], "", d1, d2, null),
                        todas.getFilas(), r -> r.getEstado().equals(c[0]) && !r.getFecha().isBefore(d1)
                        && !r.getFecha().isAfter(d2), dao, hoy, hora);
            }
            malos += comparar("ya terminaron", filtro("", "", null, null, true), todas.getFilas(),
                    ReservaListado::isFinalizada, dao, hoy, hora);
            malos += comparar("todavía no terminaron", filtro("", "", null, null, false), todas.getFilas(),
                    r -> !r.isFinalizada(), dao, hoy, hora);
            malos += comparar("hoy", filtro("", "", hoy, hoy, null), todas.getFilas(),
                    r -> r.getFecha().equals(hoy), dao, hoy, hora);
            for (String disciplina : new String[]{"Fútbol", "Vóley", "Paddle"}) {
                malos += comparar("disciplina " + disciplina + ", confirmadas, no terminaron",
                        filtro("CONFIRMADA", disciplina, null, null, false), todas.getFilas(),
                        r -> r.getDisciplina().equals(disciplina) && r.getEstado().equals("CONFIRMADA") && !r.isFinalizada(),
                        dao, hoy, hora);
            }
            System.out.println(malos == 0 ? "EQUIVALENCIA: todo coincide" : "EQUIVALENCIA: HAY " + malos + " DIFERENCIAS");
            emf.close();
            return;
        }

        FiltroReservas vacio = new FiltroReservas();
        ini();
        long n = dao.contarParaAdmin(vacio, hoy, hora);
        fin("contar reservas (sin filtros)", n + " reservas");
        ini();
        PaginaReservas p = srv.listarPagina(vacio, 25, hoy, hora);
        fin("listado, primera tanda de 25 (sin filtros)", p.getFilas().size() + " filas de " + p.getTotal());

        FiltroReservas mes = filtro("", "", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null);
        ini();
        p = srv.listarPagina(mes, 25, hoy, hora);
        fin("listado, octubre 2026", p.getFilas().size() + " filas de " + p.getTotal());

        for (String estado : new String[]{"CONFIRMADA", "CANCELADA", "RECHAZADA", "POR_CONFIRMAR", "ESPERANDO_PAGO"}) {
            ini();
            p = srv.listarPagina(filtro(estado, "", null, null, null), 25, hoy, hora);
            fin("listado, estado " + estado, p.getFilas().size() + " filas de " + p.getTotal());
        }
        ini();
        p = srv.listarPagina(filtro("", "Fútbol", null, null, false), 25, hoy, hora);
        fin("listado, Fútbol y todavía no terminaron", p.getFilas().size() + " filas de " + p.getTotal());
        ini();
        p = srv.listarPagina(filtro("", "", null, null, true), 25, hoy, hora);
        fin("listado, ya terminaron", p.getFilas().size() + " filas de " + p.getTotal());

        String apellido = em.createQuery(
                "SELECT u.apellido FROM Usuario u WHERE u.apellido IS NOT NULL ORDER BY u.idUsuario", String.class)
                .setMaxResults(1).getSingleResult();
        for (String texto : new String[]{apellido.toUpperCase(), "15/10/2026", "250000", "50% _x!"}) {
            FiltroReservas f = new FiltroReservas();
            f.setTexto(texto);
            ini();
            p = srv.listarPagina(f, 25, hoy, hora);
            fin("buscador '" + texto + "' (en todo el historial)", p.getFilas().size() + " filas de " + p.getTotal());
        }
        ini();
        fin("próximas confirmadas (pestaña Predio de cancelaciones)", srv.listarProximasConfirmadas(ahora).size() + " filas");

        ini();
        ResumenReservas r = srv.resumen(ahora);
        fin("indicadores, rankings y recaudación", "total " + r.getTotal() + ", por confirmar " + r.getPorConfirmar()
                + ", de hoy " + r.getDeHoy() + ", próximas " + r.getProximasConfirmadas() + ", canceladas y rechazadas "
                + r.getCanceladas() + ", cobrado hoy " + r.getRecaudacion().getTotalHoy());

        // PDF (CP-43 a CP-45)
        ini();
        fin("PDF de un día (14/10/2026)", informe(srv.informe(
                filtro("", "", LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 14), null), ahora)));
        ini();
        fin("PDF de una semana (19 al 25/10/2026)", informe(srv.informe(
                filtro("", "", LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 25), null), ahora)));
        ini();
        fin("PDF de un mes (octubre 2026)", informe(srv.informe(mes, ahora)));
        ini();
        fin("PDF de un período sin reservas (enero 2030)", informe(srv.informe(
                filtro("", "", LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31), null), ahora)));
        ini();
        fin("PDF de un año (2026)", informe(srv.informe(
                filtro("", "", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null), ahora)));
        ini();
        fin("PDF sin elegir período", informe(srv.informe(new FiltroReservas(), ahora)));

        // reportes: lo que se lee de la base para cada período (y para el período previo con el que se compara)
        ReportesServicio rep = (ReportesServicio) crear(ReportesServicio.class);
        for (String per : new String[]{"7D", "30D", "90D", "12M"}) {
            ReportesServicio.Rango rg = rep.rango(per, hoy);
            ini();
            List<Reserva> l = dao.listarParaReportes(rg.getPrevioDesde(), rg.getHasta());
            fin("reportes: reservas de " + per + " (" + rg.getPrevioDesde() + " a " + rg.getHasta() + ")", l.size() + " filas");
        }
        for (String per : new String[]{"30D", "12M"}) {
            ReportesServicio.Rango rg = rep.rango(per, hoy);
            ini();
            ReportesServicio.Datos d = rep.cargar(hoy, rg);
            List<ReporteKpi> kp = rep.resumen(d, rg);
            fin("reportes: cargar y calcular el resumen (" + per + ")", kp.size() + " cifras; ingresos "
                    + kp.get(0).getValor() + ", reservas " + kp.get(1).getValor() + ", ocupación " + kp.get(2).getValor());
        }
        emf.close();
    }

    /** Compara cuántas reservas dice la base con cuántas salen de aplicar la misma condición en memoria. */
    static int comparar(String que, FiltroReservas f, List<ReservaListado> todas, Predicate<ReservaListado> enMemoria,
            ReservaDAO dao, LocalDate hoy, LocalTime hora) {
        long sql = dao.contarParaAdmin(f, hoy, hora);
        long memoria = todas.stream().filter(enMemoria).count();
        System.out.println(String.format("%-62s SQL %7d | memoria %7d | %s", que, sql, memoria, sql == memoria ? "igual" : "DIFERENTE"));
        return sql == memoria ? 0 : 1;
    }
}
