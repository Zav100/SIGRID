import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dto.FiltroReservas;
import com.sigrid.sigrid.dto.PaginaReservas;
import com.sigrid.sigrid.dto.RegistroSocio;
import com.sigrid.sigrid.dto.ReporteItem;
import com.sigrid.sigrid.dto.ReporteKpi;
import com.sigrid.sigrid.dto.ReservaListado;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.Turno;
import com.sigrid.sigrid.repositorio.Usuario;
import com.sigrid.sigrid.servicio.PanelSocioServicio;
import com.sigrid.sigrid.servicio.RegistroServicio;
import com.sigrid.sigrid.servicio.ReglaReservaException;
import com.sigrid.sigrid.servicio.ReportesServicio;
import com.sigrid.sigrid.servicio.ReservaServicio;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Casos de nivel I (integración) del plan de garantía: CP-04, CP-12, CP-16, CP-42 y CP-52. Ejecuta los servicios y las
 * consultas reales de SIGRID contra una base MySQL de PRUEBA cargada con sigrid_schema.sql (trae los datos de la demo).
 * No es parte del proyecto Maven ni de las pruebas unitarias (esas corren sin base): se compila y se corre aparte, ver
 * README.md. Deja el resultado de cada caso en resultados.txt, que lee docs/calidad/generar_reporte_vv.py.
 */
public class PruebasIntegracion {

    static final String TOMADO = "Ese turno acaba de ser reservado por otra persona. Elegí otro.";
    static final String REPETIDO = "Ese comprobante ya fue usado en otra operación.";
    static final String CLAVE = "Clave#2026";
    static final int RONDAS = 20;

    static EntityManagerFactory emf;
    static final LocalDateTime AHORA = LocalDateTime.now().withNano(0);
    static final LocalDate HOY = AHORA.toLocalDate();
    static final List<String> resultados = new ArrayList<>();
    static final ExecutorService hilos = Executors.newFixedThreadPool(2);

    // ---------- armado sin contenedor ----------

    /** Arma un objeto de SIGRID y le inyecta sus dependencias (@Inject) y el EntityManager, sin contenedor. */
    static Object crear(Class<?> c, EntityManager em) throws Exception {
        Constructor<?> k = c.getDeclaredConstructor();
        k.setAccessible(true);
        Object o = k.newInstance();
        for (Class<?> x = c; x != null; x = x.getSuperclass()) {
            for (Field f : x.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.isAnnotationPresent(Inject.class)) {
                    f.set(o, crear(f.getType(), em));
                } else if (f.isAnnotationPresent(PersistenceContext.class)) {
                    f.set(o, em);
                }
            }
        }
        return o;
    }

    /** Lo que en la aplicación es un pedido: su propio EntityManager (su propia conexión) y sus servicios. */
    static class Sesion implements AutoCloseable {
        final EntityManager em = emf.createEntityManager();
        final PanelSocioServicio panel;
        final RegistroServicio registro;
        final ReservaServicio reservas;
        final ReportesServicio reportes;
        final ReservaDAO reservaDAO;

        Sesion() {
            try {
                panel = (PanelSocioServicio) crear(PanelSocioServicio.class, em);
                registro = (RegistroServicio) crear(RegistroServicio.class, em);
                reservas = (ReservaServicio) crear(ReservaServicio.class, em);
                reportes = (ReportesServicio) crear(ReportesServicio.class, em);
                reservaDAO = (ReservaDAO) crear(ReservaDAO.class, em);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        void empezar() {
            em.getTransaction().begin();
        }

        void confirmar() {
            em.getTransaction().commit();
        }

        void deshacer() {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.clear();
        }

        long contar(String sql, Object... parametros) {
            return ((Number) con(em.createNativeQuery(sql), parametros).getSingleResult()).longValue();
        }

        @SuppressWarnings("unchecked")
        Set<Integer> ids(String sql, Object... parametros) {
            return ((List<Object>) con(em.createNativeQuery(sql), parametros).getResultList()).stream()
                    .map(x -> ((Number) x).intValue()).collect(Collectors.toCollection(TreeSet::new));
        }

        void ejecutar(String sql, Object... parametros) {
            empezar();
            con(em.createNativeQuery(sql), parametros).executeUpdate();
            confirmar();
            emf.getCache().evictAll(); // el cambio fue por SQL: que nadie lea una copia vieja
        }

        @Override
        public void close() {
            deshacer();
            em.close();
        }
    }

    static Query con(Query q, Object... parametros) {
        for (int i = 0; i < parametros.length; i++) {
            q.setParameter(i + 1, parametros[i]);
        }
        return q;
    }

    // ---------- registro de resultados ----------

    interface Caso {
        String correr() throws Exception;
    }

    static void verificar(boolean condicion, String queFallo) {
        if (!condicion) {
            throw new AssertionError(queFallo);
        }
    }

    static void igual(Object esperado, Object obtenido, String que) {
        verificar(String.valueOf(esperado).equals(String.valueOf(obtenido)),
                que + ": se esperaba <" + esperado + "> y salió <" + obtenido + ">");
    }

    static void caso(String codigo, String titulo, Caso c) {
        String estado;
        String detalle;
        try {
            detalle = c.correr();
            estado = "Aprobado";
        } catch (Throwable t) {
            estado = "Falló";
            detalle = t instanceof AssertionError ? t.getMessage() : t.toString();
            if (!(t instanceof AssertionError)) {
                t.printStackTrace();
            }
        }
        detalle = detalle.replace('|', '/').replace('\n', ' ');
        resultados.add(codigo + "|" + estado + "|" + detalle);
        System.out.println(codigo + " " + titulo + "\n   -> " + estado.toUpperCase() + ": " + detalle + "\n");
    }

    // ---------- datos de apoyo ----------

    /** Un turno de una instalación en un día. */
    static class Franja {
        final int idTurno;
        final LocalDate fecha;

        Franja(int idTurno, LocalDate fecha) {
            this.idTurno = idTurno;
            this.fecha = fecha;
        }
    }

    static List<Franja> libres;
    static List<Integer> socios;

    /** Franjas libres de una instalación habilitada y socios que hoy pueden reservarla (se prueba y se deshace). */
    static void prepararReservas() {
        try (Sesion s = new Sesion()) {
            List<Turno> turnos = s.em.createQuery("SELECT t FROM Turno t WHERE t.idInstalacion.estado = :h "
                    + "AND t.idInstalacion.tipoAcceso = :a ORDER BY t.idInstalacion.idInstalacion, t.horaInicio", Turno.class)
                    .setParameter("h", Instalacion.Estado.HABILITADA)
                    .setParameter("a", Instalacion.TipoAcceso.ARANCELADO).getResultList();
            verificar(!turnos.isEmpty(), "la base de prueba no tiene instalaciones habilitadas con turnos");
            Integer instalacion = turnos.get(0).getIdInstalacion().getIdInstalacion();
            libres = new ArrayList<>();
            for (int dia = 5; dia <= 28; dia++) {
                for (Turno t : turnos) {
                    if (t.getIdInstalacion().getIdInstalacion().equals(instalacion)
                            && s.reservaDAO.buscarActivaPorTurnoYFecha(t.getIdTurno(), HOY.plusDays(dia)) == null) {
                        libres.add(new Franja(t.getIdTurno(), HOY.plusDays(dia)));
                    }
                }
            }
            verificar(libres.size() >= RONDAS + 6, "hacen falta " + (RONDAS + 6) + " franjas libres y hay " + libres.size());
            socios = new ArrayList<>();
            for (Socio x : s.em.createQuery("SELECT s FROM Socio s WHERE s.estado = :e AND s.bajaLogica = false "
                    + "ORDER BY s.idSocio", Socio.class).setParameter("e", Socio.Estado.ACTIVO).getResultList()) {
                if (s.reservaDAO.contarSinComprobanteDe(x.getIdSocio()) > 0) {
                    continue; // CP-16 le crea dos reservas sin comprobante y el tope es tres
                }
                s.empezar();
                try {
                    s.panel.reservar(x.getIdSocio(), libres.get(0).idTurno, libres.get(0).fecha, AHORA);
                    socios.add(x.getIdSocio());
                } catch (ReglaReservaException e) {
                    // este socio hoy no puede reservar (valoración pendiente, tres sin comprobante...): se usa otro
                }
                s.deshacer();
            }
            verificar(socios.size() >= 2, "hacen falta dos socios que puedan reservar y hay " + socios.size());
        }
    }

    static Franja siguienteLibre() {
        return libres.remove(0);
    }

    /** Un pedido completo de reserva, como lo hace la pantalla: null si quedó hecha; si no, el mensaje o el error. */
    static String reservar(int idSocio, Franja f, CyclicBarrier salida) {
        try (Sesion s = new Sesion()) {
            if (salida != null) {
                salida.await();
            }
            s.empezar();
            s.panel.reservar(idSocio, f.idTurno, f.fecha, AHORA);
            s.confirmar();
            return null;
        } catch (ReglaReservaException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "ERROR " + e;
        }
    }

    static final String VIVAS = "SELECT COUNT(*) FROM reserva WHERE id_turno = ?1 AND fecha_turno = ?2 "
            + "AND estado IN ('PENDIENTE_PAGO','CONFIRMADA')";

    /** La franja estaba libre antes de la prueba: lo que tenga ahora lo creó la prueba. */
    static void vaciar(Sesion s, Franja f) {
        s.ejecutar("DELETE FROM reserva WHERE id_turno = ?1 AND fecha_turno = ?2 AND estado = 'PENDIENTE_PAGO' "
                + "AND id_pago IS NULL", f.idTurno, f.fecha);
        igual(0L, s.contar(VIVAS, f.idTurno, f.fecha), "reservas vivas de la franja después de limpiar");
    }

    // ---------- CP-04 ----------

    static RegistroSocio alta(String email, String dni) {
        RegistroSocio r = new RegistroSocio();
        r.setEmail(email);
        r.setPassword(CLAVE);
        r.setConfirmarPassword(CLAVE);
        r.setNombre("Prueba");
        r.setApellido("Integración");
        r.setDni(dni);
        r.setFechaNacimiento("1995-04-12");
        r.setTelefono("3854000000");
        r.setTipoSocio("EXTERNO");
        return r;
    }

    /** Intenta el INSERT (dentro de la transacción abierta, o abre una) y lo deshace: qué UNIQUE de la base lo frenó. */
    static String restriccionQueFrena(Sesion s, Object entidad) {
        if (!s.em.getTransaction().isActive()) {
            s.empezar();
        }
        String frenado = "ninguna: la base lo aceptó";
        try {
            s.em.persist(entidad);
            s.em.flush();
        } catch (PersistenceException e) {
            Throwable raiz = e;
            while (raiz.getCause() != null) {
                raiz = raiz.getCause();
            }
            String m = String.valueOf(raiz.getMessage());
            frenado = m.contains("Duplicate entry") ? m.substring(m.lastIndexOf('.') + 1).replace("'", "") : m;
        }
        s.deshacer();
        return frenado;
    }

    static Usuario usuarioCopia(Sesion s, Integer idSocio, String email) {
        Usuario u = new Usuario();
        u.setIdRol(s.em.find(Socio.class, idSocio).getIdUsuario().getIdRol());
        u.setNombre("Copia");
        u.setApellido("Repetida");
        u.setEmail(email);
        u.setPasswordHash("x");
        return u;
    }

    static String cp04() {
        try (Sesion s = new Sesion()) {
            Socio existente = s.em.createQuery("SELECT s FROM Socio s WHERE s.bajaLogica = false ORDER BY s.idSocio",
                    Socio.class).setMaxResults(1).getSingleResult();
            String correo = existente.getIdUsuario().getEmail();
            String dni = existente.getDni();
            String correoNuevo = "integracion." + System.nanoTime() + "@ejemplo.com";
            String dniNuevo = String.valueOf(90_000_000 + new SecureRandom().nextInt(9_000_000));
            String cuentas = "SELECT (SELECT COUNT(*) FROM usuario) * 1000000 + (SELECT COUNT(*) FROM socio) * 1000 "
                    + "+ (SELECT COUNT(*) FROM carnet_digital)";
            long antes = s.contar(cuentas);

            s.empezar();
            igual("Ese correo ya tiene una cuenta.", s.registro.registrar(alta(correo, dniNuevo)), "correo repetido");
            igual("Ese correo ya tiene una cuenta.", s.registro.registrar(alta("  " + correo.toUpperCase() + " ", dniNuevo)),
                    "correo repetido en mayúsculas y con espacios");
            igual("Ese DNI ya pertenece a otro socio.", s.registro.registrar(alta(correoNuevo, dni)), "DNI repetido");
            s.confirmar();
            igual(antes, s.contar(cuentas), "usuarios, socios y carnets después de los tres rechazos");

            // la base también lo impide si algo se saltea el servicio (dos altas a la vez): UNIQUE de email y de dni
            Integer idSocio = existente.getIdSocio();
            s.empezar();
            igual("uq_usuario_email", restriccionQueFrena(s, usuarioCopia(s, idSocio, correo)),
                    "un segundo usuario con el mismo correo");
            s.empezar();
            Usuario otro = usuarioCopia(s, idSocio, correoNuevo);
            s.em.persist(otro);
            s.em.flush();
            Socio fichaCopia = new Socio();
            fichaCopia.setIdUsuario(otro);
            fichaCopia.setIdCategoriaSocio(s.em.find(Socio.class, idSocio).getIdCategoriaSocio());
            fichaCopia.setDni(dni);
            fichaCopia.setFechaNacimiento(LocalDate.of(1995, 4, 12));
            fichaCopia.setTelefono("3854000000");
            igual("uq_socio_dni", restriccionQueFrena(s, fichaCopia), "un segundo socio con el mismo DNI");

            // control: con correo y DNI nuevos la misma alta sí se crea (se deshace para no ensuciar la base)
            s.empezar();
            igual(null, s.registro.registrar(alta(correoNuevo, dniNuevo)), "alta con correo y DNI nuevos");
            s.em.flush();
            igual(antes + 1_001_001, s.contar(cuentas), "usuarios, socios y carnets con el alta nueva");
            s.deshacer();
            igual(antes, s.contar(cuentas), "usuarios, socios y carnets al terminar");
            return "Correo repetido (también en mayúsculas y con espacios) y DNI repetido: el servicio los rechaza con su "
                    + "mensaje y no se crea ninguna fila; si algo se saltea el servicio, la base rechaza el INSERT "
                    + "(uq_usuario_email y uq_socio_dni); con datos nuevos la misma alta se crea.";
        }
    }

    // ---------- CP-12 ----------

    static String cp12() throws Exception {
        int a = socios.get(0);
        int b = socios.get(1);
        try (Sesion s = new Sesion()) {
            // 1) paso a paso: A ya mandó su INSERT pero no cerró la transacción; B pide el mismo turno
            Franja f = siguienteLibre();
            Sesion deA = new Sesion();
            deA.empezar();
            deA.panel.reservar(a, f.idTurno, f.fecha, AHORA);
            Future<String> deB = hilos.submit(() -> reservar(b, f, null));
            Thread.sleep(1500);
            boolean esperando = !deB.isDone();
            deA.confirmar();
            deA.close();
            String respuestaB = deB.get(90, TimeUnit.SECONDS);
            verificar(esperando, "el segundo pedido no quedó esperando al primero: respondió <" + respuestaB + ">");
            igual(TOMADO, respuestaB, "respuesta al segundo socio");
            igual(1L, s.contar(VIVAS, f.idTurno, f.fecha), "reservas vivas del turno");
            igual(1L, s.contar(VIVAS + " AND id_socio = ?3", f.idTurno, f.fecha, a), "reservas vivas del primer socio");
            vaciar(s, f);

            // 2) de verdad a la vez: los dos pedidos salen juntos, cada uno con su conexión
            int ganoA = 0;
            for (int ronda = 1; ronda <= RONDAS; ronda++) {
                Franja g = siguienteLibre();
                CyclicBarrier salida = new CyclicBarrier(2);
                Future<String> ra = hilos.submit(() -> reservar(a, g, salida));
                Future<String> rb = hilos.submit(() -> reservar(b, g, salida));
                String sa = ra.get(90, TimeUnit.SECONDS);
                String sb = rb.get(90, TimeUnit.SECONDS);
                verificar((sa == null) != (sb == null), "ronda " + ronda + ": tenía que quedar exactamente una reserva y "
                        + "respondieron <" + sa + "> y <" + sb + ">");
                igual(TOMADO, sa == null ? sb : sa, "ronda " + ronda + ": respuesta al que perdió");
                igual(1L, s.contar(VIVAS, g.idTurno, g.fecha), "ronda " + ronda + ": reservas vivas del turno");
                ganoA += sa == null ? 1 : 0;
                vaciar(s, g);
            }
            return "Paso a paso: el segundo pedido pasa la consulta previa, queda esperando en el INSERT y, cuando el "
                    + "primero confirma, recibe \"" + TOMADO + "\"; queda una sola reserva viva. A la vez (" + RONDAS
                    + " rondas, dos conexiones): siempre una sola reserva y el otro socio recibe ese mensaje (ganó "
                    + ganoA + " veces uno y " + (RONDAS - ganoA) + " el otro).";
        }
    }

    // ---------- CP-16 ----------

    static byte[] png() {
        byte[] d = new byte[200];
        new SecureRandom().nextBytes(d);
        byte[] firma = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(firma, 0, d, 0, firma.length);
        return d;
    }

    static String sha256(byte[] datos) throws Exception {
        return String.format("%064x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(datos)));
    }

    /** Un pedido completo de subir comprobante: null si quedó cargado; si no, el mensaje o el error. */
    static String subir(int idSocio, int idReserva, byte[] datos) {
        try (Sesion s = new Sesion()) {
            s.empezar();
            s.panel.subirComprobante(idSocio, idReserva, datos, AHORA.minusHours(1), AHORA);
            s.confirmar();
            return null;
        } catch (ReglaReservaException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "ERROR " + e.getClass().getSimpleName();
        }
    }

    static int reservaPendiente(Sesion s, int idSocio) {
        Franja f = siguienteLibre();
        s.empezar();
        int id = s.panel.reservar(idSocio, f.idTurno, f.fecha, AHORA);
        s.confirmar();
        return id;
    }

    static String cp16() throws Exception {
        int a = socios.get(0);
        int b = socios.get(1);
        Path carpeta = Files.createTempDirectory("sigrid-integracion-comprobantes");
        System.setProperty("sigrid.comprobantes", carpeta.toString());
        String porHash = "SELECT COUNT(*) FROM comprobante_pago WHERE hash_archivo = ?1";
        String sinPago = "SELECT COUNT(*) FROM reserva WHERE id_reserva = ?1 AND id_pago IS NULL";
        List<Integer> creadas = new ArrayList<>();
        try (Sesion s = new Sesion()) {
            int r1 = reservaPendiente(s, a);
            int r2 = reservaPendiente(s, a);
            int rb = reservaPendiente(s, b);
            creadas.addAll(List.of(r1, r2, rb));
            byte[] archivo = png();
            String hash = sha256(archivo);

            igual(null, subir(a, r1, archivo), "primera carga del archivo");
            igual(1L, s.contar(porHash, hash), "comprobantes con ese archivo después de la primera carga");
            long pagos = s.contar("SELECT COUNT(*) FROM pago");
            igual(REPETIDO, subir(a, r2, archivo), "el mismo socio sube el mismo archivo a otra reserva");
            igual(REPETIDO, subir(b, rb, archivo), "otro socio sube el mismo archivo");
            igual(1L, s.contar(porHash, hash), "comprobantes con ese archivo al final");
            igual(pagos, s.contar("SELECT COUNT(*) FROM pago"), "pagos después de los dos rechazos");
            igual(1L, s.contar(sinPago, r2), "la segunda reserva del socio sigue sin pago");
            igual(1L, s.contar(sinPago, rb), "la reserva del otro socio sigue sin pago");
            long archivos;
            try (Stream<Path> lista = Files.list(carpeta)) {
                archivos = lista.count();
            }
            igual(1L, archivos, "archivos guardados en la carpeta de comprobantes");

            // el mismo archivo en dos cargas a la vez: la consulta previa no lo ve, lo frena el UNIQUE de la base
            byte[] otro = png();
            Sesion deA = new Sesion();
            deA.empezar();
            deA.panel.subirComprobante(a, r2, otro, AHORA.minusHours(1), AHORA);
            deA.em.flush();
            Future<String> deB = hilos.submit(() -> subir(b, rb, otro));
            Thread.sleep(1500);
            boolean esperando = !deB.isDone();
            deA.confirmar();
            deA.close();
            String respuestaB = deB.get(90, TimeUnit.SECONDS);
            verificar(esperando, "la segunda carga simultánea no quedó esperando a la primera: respondió <" + respuestaB + ">");
            igual(REPETIDO, respuestaB, "respuesta a la segunda de dos cargas simultáneas del mismo archivo (D-6)");
            igual(1L, s.contar(porHash, sha256(otro)), "comprobantes con el archivo subido a la vez");
            igual(1L, s.contar(sinPago, rb), "la reserva del que perdió sigue sin pago");
            return "Primera carga: pago y comprobante creados y archivo guardado. El mismo archivo en otra reserva (del "
                    + "mismo socio y de otro socio): \"" + REPETIDO + "\", sin pago ni comprobante nuevos. Dos cargas a la "
                    + "vez del mismo archivo: queda una sola (uq_comprobante_hash) y la otra también recibe \"" + REPETIDO
                    + "\" (D-6 corregido).";
        } finally {
            try (Sesion s = new Sesion()) {
                String ids = creadas.stream().map(String::valueOf).collect(Collectors.joining(","));
                if (!ids.isEmpty()) {
                    Set<Integer> pagos = s.ids("SELECT id_pago FROM reserva WHERE id_pago IS NOT NULL AND id_reserva IN (" + ids + ")");
                    String idsPago = pagos.stream().map(String::valueOf).collect(Collectors.joining(","));
                    s.ejecutar("DELETE FROM reserva WHERE id_reserva IN (" + ids + ")");
                    if (!idsPago.isEmpty()) {
                        s.ejecutar("DELETE FROM comprobante_pago WHERE id_pago IN (" + idsPago + ")");
                        s.ejecutar("DELETE FROM pago WHERE id_pago IN (" + idsPago + ")");
                    }
                }
            }
            borrarCarpeta(carpeta);
        }
    }

    static void borrarCarpeta(Path carpeta) throws IOException {
        try (Stream<Path> lista = Files.walk(carpeta)) {
            lista.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    // ---------- CP-42 ----------

    /** Turnos de una instalación en un día, de la base: {horario, socio que lo ocupa o null}. */
    @SuppressWarnings("unchecked")
    static List<Object[]> turnosDelDia(Sesion s, Integer idInstalacion, LocalDate dia) {
        return con(s.em.createNativeQuery("SELECT CONCAT(TIME_FORMAT(t.hora_inicio, '%H:%i'), ' – ', TIME_FORMAT(t.hora_fin, '%H:%i')), "
                + "(SELECT CONCAT(u.nombre, ' ', u.apellido) FROM reserva r JOIN socio s ON s.id_socio = r.id_socio "
                + "JOIN usuario u ON u.id_usuario = s.id_usuario WHERE r.id_turno = t.id_turno AND r.fecha_turno = ?2 "
                + "AND r.estado IN ('PENDIENTE_PAGO','CONFIRMADA')) FROM turno t WHERE t.id_instalacion = ?1 "
                + "ORDER BY t.hora_inicio"), idInstalacion, dia).getResultList();
    }

    static String cp42() throws Exception {
        try (Sesion s = new Sesion()) {
            List<Instalacion> instalaciones = s.em.createQuery("SELECT i FROM Instalacion i WHERE i.tipoAcceso = :a",
                    Instalacion.class).setParameter("a", Instalacion.TipoAcceso.ARANCELADO).getResultList();
            List<ReportesServicio.Rango> rangos = new ArrayList<>();
            for (String periodo : List.of("7D", "30D", "MES", "MES_ANT", "90D", "12M")) {
                rangos.add(s.reportes.rango(periodo, HOY));
            }
            // días sueltos: los cinco con más turnos ocupados de la base y el día de hoy
            @SuppressWarnings("unchecked")
            List<Object> dias = s.em.createNativeQuery("SELECT fecha_turno FROM reserva WHERE estado IN "
                    + "('PENDIENTE_PAGO','CONFIRMADA') GROUP BY fecha_turno ORDER BY COUNT(*) DESC, fecha_turno LIMIT 5")
                    .getResultList();
            // "Un día" de la pantalla: hoy y los cinco días con más turnos ocupados
            int sueltos = 1;
            rangos.add(s.reportes.rango("DIA", HOY, HOY));
            for (Object dia : dias) {
                rangos.add(s.reportes.rango("DIA", HOY, LocalDate.parse(dia.toString())));
                sueltos++;
            }
            int comparaciones = 0;
            int turnosComparados = 0;
            long turnosVistos = 0;
            for (ReportesServicio.Rango r : rangos) {
                ReportesServicio.Datos d = s.reportes.cargar(HOY, r);
                List<ReporteItem> items = s.reportes.ocupacionPorInstalacion(d, r);
                igual(instalaciones.size(), items.size(), r.getNombre() + ": instalaciones en el reporte");
                long total = 0;
                for (Instalacion i : instalaciones) {
                    long enBase = s.contar("SELECT COUNT(*) FROM reserva r JOIN turno t ON t.id_turno = r.id_turno "
                            + "WHERE t.id_instalacion = ?1 AND r.fecha_turno BETWEEN ?2 AND ?3 "
                            + "AND r.estado IN ('PENDIENTE_PAGO','CONFIRMADA')", i.getIdInstalacion(), r.getDesde(), r.getHasta());
                    long turnos = s.contar("SELECT COUNT(*) FROM turno WHERE id_instalacion = ?1", i.getIdInstalacion());
                    total += enBase;
                    ReporteItem item = items.stream().filter(x -> x.getEtiqueta().equals(i.getNombre())).findFirst()
                            .orElseThrow(() -> new AssertionError(r.getNombre() + ": falta " + i.getNombre()));
                    String donde = r.getNombre() + " (" + r.getDesde() + " a " + r.getHasta() + "), " + i.getNombre();
                    if (i.getEstado() == Instalacion.Estado.DESHABILITADA_MANTENIMIENTO || turnos == 0) {
                        // los turnos disponibles descuentan los días de mantenimiento: se compara solo lo reservado
                        verificar(item.getDetalle().startsWith(enBase + " de ") || item.getDetalle().startsWith("En mantenimiento"),
                                donde + ": la base tiene " + enBase + " turnos ocupados y el reporte dice <" + item.getDetalle() + ">");
                    } else {
                        igual(enBase + " de " + turnos * r.getDias() + " turnos", item.getDetalle(), donde);
                    }
                    comparaciones++;
                    // un día de una instalación (lo que se elige en la pantalla): turno por turno, contra la base
                    if (r.getDias() == 1) {
                        List<ReporteItem> porTurno = s.reportes.ocupacionPorTurno(d, r, i.getIdInstalacion());
                        List<Object[]> enLaBase = turnosDelDia(s, i.getIdInstalacion(), r.getDesde());
                        igual(enLaBase.size(), porTurno.size(), donde + ": turnos de la instalación");
                        for (int k = 0; k < enLaBase.size(); k++) {
                            ReporteItem t = porTurno.get(k);
                            String socio = (String) enLaBase.get(k)[1];
                            igual(enLaBase.get(k)[0], t.getEtiqueta(), donde + ": horario del turno " + (k + 1));
                            if ("En mantenimiento".equals(t.getValor())) {
                                verificar(i.getEstado() == Instalacion.Estado.DESHABILITADA_MANTENIMIENTO,
                                        donde + ": dice \"En mantenimiento\" y la instalación no lo está");
                            } else {
                                igual(socio == null ? "Libre" : "Reservado", t.getValor(), donde + ", " + t.getEtiqueta());
                                verificar(socio == null || t.getDetalle().startsWith(socio + " · "),
                                        donde + ", " + t.getEtiqueta() + ": la base dice " + socio + " y el reporte <" + t.getDetalle() + ">");
                            }
                            turnosComparados++;
                        }
                    }
                }
                ReporteKpi reservados = s.reportes.ocupacionKpis(d, r).stream()
                        .filter(k -> k.getEtiqueta().equals("Turnos reservados")).findFirst().orElseThrow();
                igual(String.valueOf(total), reservados.getValor(), r.getNombre() + " (" + r.getDesde() + " a " + r.getHasta()
                        + "): total de turnos reservados");
                turnosVistos += total;
            }
            return comparaciones + " comparaciones (" + instalaciones.size() + " instalaciones por los 6 períodos de la "
                    + "pantalla y " + sueltos + " días sueltos): los turnos reservados y disponibles de cada instalación y el "
                    + "total coinciden con lo contado por SQL (" + turnosVistos + " turnos ocupados sumando todos los "
                    + "períodos). Con el período \"Un día\" y una instalación elegida, como en la pantalla: " + turnosComparados
                    + " turnos comparados uno por uno (libre o reservado, y por quién) con la base.";
        }
    }

    // ---------- CP-52 ----------

    static final DateTimeFormatter DIA = DateTimeFormatter.ISO_LOCAL_DATE;

    /** La misma condición del filtro escrita en SQL sobre las tablas, sin pasar por JPA. */
    static String sql(String estado, String disciplina, LocalDate desde, LocalDate hasta, String instalacion, List<Object> p) {
        StringBuilder w = new StringBuilder("SELECT r.id_reserva FROM reserva r JOIN turno t ON t.id_turno = r.id_turno "
                + "JOIN instalacion i ON i.id_instalacion = t.id_instalacion JOIN socio s ON s.id_socio = r.id_socio "
                + "JOIN usuario u ON u.id_usuario = s.id_usuario LEFT JOIN pago p ON p.id_pago = r.id_pago WHERE 1 = 1");
        if (desde != null) {
            p.add(desde);
            w.append(" AND r.fecha_turno >= ?").append(p.size());
        }
        if (hasta != null) {
            p.add(hasta);
            w.append(" AND r.fecha_turno <= ?").append(p.size());
        }
        if (!disciplina.isEmpty()) {
            p.add(disciplina);
            w.append(" AND i.tipo_disciplina = ?").append(p.size());
        }
        switch (estado) {
            case "POR_CONFIRMAR":
                w.append(" AND r.estado = 'PENDIENTE_PAGO' AND p.estado = 'PENDIENTE_VALIDACION'");
                break;
            case "ESPERANDO_PAGO":
                w.append(" AND r.estado = 'PENDIENTE_PAGO' AND (r.id_pago IS NULL OR p.estado <> 'PENDIENTE_VALIDACION')");
                break;
            case "":
                break;
            default:
                w.append(" AND r.estado = '").append(estado).append("'");
        }
        if (!instalacion.isEmpty()) {
            p.add(instalacion);
            w.append(" AND i.nombre = ?").append(p.size());
        }
        return w.toString();
    }

    static String cp52() {
        try (Sesion s = new Sesion()) {
            List<String> estados = List.of("", "CONFIRMADA", "CANCELADA", "RECHAZADA", "POR_CONFIRMAR", "ESPERANDO_PAGO");
            List<String> disciplinas = new ArrayList<>(s.reservas.disciplinas());
            disciplinas.add(0, "");
            List<String> nombres = new ArrayList<>();
            nombres.add("");
            for (Object n : s.em.createNativeQuery("SELECT nombre FROM instalacion WHERE tipo_acceso = 'ARANCELADO'").getResultList()) {
                nombres.add(n.toString());
            }
            LocalDate[][] periodos = {{null, null}, {HOY.minusDays(30), HOY}, {HOY, HOY.plusDays(30)}, {HOY, HOY},
                {HOY.minusDays(7), null}, {null, HOY.minusDays(1)}};
            long todas = s.contar("SELECT COUNT(*) FROM reserva");
            int combinaciones = 0;
            int conFilas = 0;
            for (String estado : estados) {
                for (LocalDate[] periodo : periodos) {
                    for (int k = 0; k < disciplinas.size() + nombres.size() - 1; k++) {
                        // primero cada disciplina, después cada instalación (los dos desplegables de la pantalla)
                        String disciplina = k < disciplinas.size() ? disciplinas.get(k) : "";
                        String instalacion = k < disciplinas.size() ? "" : nombres.get(k - disciplinas.size() + 1);
                        FiltroReservas f = new FiltroReservas();
                        f.setEstado(estado);
                        f.setDisciplina(disciplina);
                        f.setInstalacion(instalacion);
                        f.setDesde(periodo[0]);
                        f.setHasta(periodo[1]);
                        String cual = "estado <" + estado + ">, disciplina <" + disciplina + ">, instalación <" + instalacion
                                + ">, de " + periodo[0] + " a " + periodo[1];
                        List<Object> p = new ArrayList<>();
                        Set<Integer> enBase = s.ids(sql(estado, disciplina, periodo[0], periodo[1], instalacion, p), p.toArray());

                        PaginaReservas completa = s.reservas.listarPagina(f, (int) todas + 1, HOY, AHORA.toLocalTime());
                        Set<Integer> listadas = completa.getFilas().stream().map(ReservaListado::getIdReserva)
                                .collect(Collectors.toCollection(TreeSet::new));
                        igual(enBase, listadas, cual + ": reservas listadas");
                        igual(enBase.size(), completa.getFilas().size(), cual + ": filas sin repetir");
                        for (ReservaListado fila : completa.getFilas()) {
                            verificar((periodo[0] == null || !fila.getFecha().isBefore(periodo[0]))
                                    && (periodo[1] == null || !fila.getFecha().isAfter(periodo[1])),
                                    cual + ": la reserva " + fila.getIdReserva() + " es del " + DIA.format(fila.getFecha()));
                            verificar(disciplina.isEmpty() || disciplina.equals(fila.getDisciplina()),
                                    cual + ": la reserva " + fila.getIdReserva() + " es de " + fila.getDisciplina());
                            verificar(instalacion.isEmpty() || instalacion.equals(fila.getInstalacion()),
                                    cual + ": la reserva " + fila.getIdReserva() + " es de " + fila.getInstalacion());
                            verificar(estado.isEmpty() || estado.equals(fila.getEstado()),
                                    cual + ": la reserva " + fila.getIdReserva() + " está " + fila.getEstado());
                        }
                        // la primera tanda de la pantalla (25): el "N de M" sale de otra consulta, la que cuenta
                        PaginaReservas tanda = s.reservas.listarPagina(f, 25, HOY, AHORA.toLocalTime());
                        igual((long) enBase.size(), tanda.getTotal(), cual + ": total informado");
                        igual(Math.min(25, enBase.size()), tanda.getFilas().size(), cual + ": filas de la primera tanda");
                        verificar(enBase.containsAll(tanda.getFilas().stream().map(ReservaListado::getIdReserva)
                                .collect(Collectors.toList())), cual + ": la primera tanda trae reservas que no cumplen el filtro");
                        combinaciones++;
                        conFilas += enBase.isEmpty() ? 0 : 1;
                    }
                }
            }
            return combinaciones + " combinaciones de estado (6), período (6) e instalación (las " + (nombres.size() - 1)
                    + " instalaciones del filtro, y también las " + (disciplinas.size() - 1) + " disciplinas) sobre " + todas
                    + " reservas: el listado trae exactamente las mismas reservas que la consulta SQL directa, sin repetidas, "
                    + "y el total informado coincide (" + conFilas + " combinaciones con resultados).";
        }
    }

    // ---------- principal ----------

    public static void main(String[] args) throws Exception {
        emf = Persistence.createEntityManagerFactory("pruebaPU");
        String base;
        String version;
        try (Sesion s = new Sesion()) {
            base = String.valueOf(s.em.createNativeQuery("SELECT DATABASE()").getSingleResult());
            version = String.valueOf(s.em.createNativeQuery("SELECT VERSION()").getSingleResult());
        }
        if (base.equalsIgnoreCase("sigrid")) {
            System.err.println("Esto escribe en la base: no se corre sobre la base real \"sigrid\". Usá una de prueba (DB_NAME).");
            System.exit(2);
        }
        System.out.println("Base de prueba: " + base + " (MySQL " + version + "), " + AHORA + "\n");

        caso("CP-04", "Registrar con un correo o un DNI ya existente", PruebasIntegracion::cp04);
        try {
            prepararReservas();
            caso("CP-12", "Dos socios reservan a la vez el mismo turno y día", PruebasIntegracion::cp12);
            caso("CP-16", "Subir dos veces el mismo archivo", PruebasIntegracion::cp16);
        } catch (AssertionError e) {
            caso("CP-12", "Dos socios reservan a la vez el mismo turno y día", () -> { throw e; });
            caso("CP-16", "Subir dos veces el mismo archivo", () -> { throw e; });
        }
        caso("CP-42", "Reporte de ocupación de un día para una instalación", PruebasIntegracion::cp42);
        caso("CP-52", "El administrador filtra el listado de reservas", PruebasIntegracion::cp52);

        hilos.shutdown();
        emf.close();
        Path salida = Paths.get(args.length > 0 ? args[0] : "resultados.txt");
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(salida, StandardCharsets.UTF_8))) {
            w.print("# Pruebas de integración de SIGRID. Ejecutadas el "
                    + AHORA.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " contra la base de prueba " + base
                    + " (MySQL " + version + "). Formato: caso|resultado|detalle\n");
            resultados.forEach(r -> w.print(r + "\n"));
        }
        System.out.println("Escrito " + salida.toAbsolutePath());
        System.exit(resultados.stream().anyMatch(r -> r.contains("|Falló|")) ? 1 : 0);
    }
}
