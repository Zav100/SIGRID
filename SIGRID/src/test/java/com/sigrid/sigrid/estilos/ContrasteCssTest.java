package com.sigrid.sigrid.estilos;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contraste de color de las hojas de estilo (CC-05, WCAG 2.1 AA): 4,5:1 para el texto y 3:1 para lo que no es texto
 * (series de los gráficos, indicadores, estrellas, foco). Lee los CSS reales y resuelve las variables, así que avisa si
 * alguien vuelve a poner un color que no alcanza. No reemplaza una medición sobre las pantallas desplegadas: mide los
 * pares de color declarados, no lo que resulta de la cascada de cada página.
 */
class ContrasteCssTest {

    private static final double TEXTO = 4.5;
    private static final double NO_TEXTO = 3.0;
    private static final Path CARPETA = Paths.get("src", "main", "webapp", "resources", "css");

    private static final Map<String, String> TOKENS = new HashMap<>();
    private static final Map<String, String> ARCHIVOS = new HashMap<>();

    @BeforeAll
    static void leerLasHojasDeEstilo() throws IOException {
        try (Stream<Path> lista = Files.list(CARPETA)) {
            for (Path p : (Iterable<Path>) lista.filter(f -> f.toString().endsWith(".css"))::iterator) {
                String css = new String(Files.readAllBytes(p), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
                ARCHIVOS.put(p.getFileName().toString(), css);
                Matcher m = Pattern.compile("(--[\\w-]+)\\s*:\\s*([^;}]+)").matcher(css);
                while (m.find()) {
                    TOKENS.putIfAbsent(m.group(1), m.group(2).trim());
                }
            }
        }
        assertTrue(TOKENS.containsKey("--text-3"), "no se leyeron las variables de color");
    }

    // ---------- lectura y color ----------

    /** El valor de una propiedad en la primera regla con exactamente ese selector (la última declaración gana). */
    private static String declaracion(String selector, String propiedad) {
        for (String css : ARCHIVOS.values()) {
            Matcher regla = Pattern.compile("(?:^|[}\\s])" + Pattern.quote(selector) + "\\s*\\{([^}]*)\\}").matcher(css);
            while (regla.find()) {
                String valor = null;
                for (String d : regla.group(1).split(";")) {
                    int i = d.indexOf(':');
                    if (i > 0 && d.substring(0, i).trim().equals(propiedad)) {
                        valor = d.substring(i + 1).trim();
                    }
                }
                if (valor != null) {
                    return valor;
                }
            }
        }
        return null;
    }

    /** Resuelve var(), #rgb, #rrggbb y rgb()/rgba() sobre el fondo dado (para mezclar la transparencia). */
    private static int[] color(String valor, int[] fondo) {
        assertNotNull(valor, "falta la declaración de color");
        String v = valor.trim().toLowerCase().replace("!important", "").trim();
        Matcher var = Pattern.compile("var\\((--[\\w-]+)\\s*(?:,\\s*(.+))?\\)").matcher(v);
        if (var.matches()) {
            String t = TOKENS.get(var.group(1));
            return color(t != null ? t : var.group(2), fondo);
        }
        if (v.startsWith("#")) {
            String h = v.substring(1);
            if (h.length() == 3) {
                h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
            }
            return new int[] {Integer.parseInt(h.substring(0, 2), 16), Integer.parseInt(h.substring(2, 4), 16),
                Integer.parseInt(h.substring(4, 6), 16)};
        }
        Matcher rgb = Pattern.compile("rgba?\\(([^)]+)\\)").matcher(v);
        if (rgb.matches()) {
            String[] p = rgb.group(1).split("[ ,/]+");
            double a = p.length > 3 ? Double.parseDouble(p[3]) : 1;
            int[] c = new int[3];
            for (int i = 0; i < 3; i++) {
                c[i] = (int) Math.round(Double.parseDouble(p[i]) * a + fondo[i] * (1 - a));
            }
            return c;
        }
        throw new IllegalArgumentException("color no reconocido: " + valor);
    }

    private static int[] token(String nombre) {
        return color("var(" + nombre + ")", new int[] {255, 255, 255});
    }

    private static final int[] BLANCO = {254, 253, 251};

    private static double luminancia(int[] c) {
        double[] l = new double[3];
        for (int i = 0; i < 3; i++) {
            double v = c[i] / 255.0;
            l[i] = v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * l[0] + 0.7152 * l[1] + 0.0722 * l[2];
    }

    private static double contraste(int[] a, int[] b) {
        double la = luminancia(a);
        double lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static String texto(int[] c) {
        return String.format("#%02X%02X%02X", c[0], c[1], c[2]);
    }

    /** Verifica un par; el mensaje dice cuál es y cuánto da. */
    private static Runnable par(String que, int[] primero, int[] segundo, double minimo) {
        return () -> {
            double r = contraste(primero, segundo);
            assertTrue(r >= minimo, String.format("%s: %s sobre %s da %.2f:1 y hace falta %.1f:1", que, texto(primero),
                    texto(segundo), r, minimo));
        };
    }

    private static void todos(List<Runnable> pares) {
        assertAll(pares.stream().map(p -> (org.junit.jupiter.api.function.Executable) p::run));
    }

    private static final String[] SUPERFICIES = {"--white", "--bg", "--surface", "--adm-soft", "--adm-soft-2"};

    // ---------- texto (CP-47 socio, CP-48 administrador) ----------

    @DisplayName("CP-47: el texto de las pantallas del socio llega a 4,5:1 sobre las superficies claras")
    @Test
    void cp47_elTextoDelSocioLlegaA4_5() {
        List<Runnable> pares = new ArrayList<>();
        for (String texto : new String[] {"--so-ink", "--so-ink-2", "--so-ink-3", "--so-accent", "--so-ok"}) {
            for (String s : SUPERFICIES) {
                pares.add(par(texto + " sobre " + s, token(texto), token(s), TEXTO));
            }
        }
        pares.add(par("--gold-700 (cuota por vencer) sobre --white", token("--gold-700"), token("--white"), TEXTO));
        todos(pares);
    }

    @DisplayName("CP-48: el texto de las pantallas del administrador llega a 4,5:1 sobre las superficies claras")
    @Test
    void cp48_elTextoDelAdministradorLlegaA4_5() {
        List<Runnable> pares = new ArrayList<>();
        for (String texto : new String[] {"--adm-ink", "--adm-ink-2", "--adm-ink-3", "--adm-accent", "--adm-ok",
            "--adm-warn", "--adm-off"}) {
            for (String s : SUPERFICIES) {
                pares.add(par(texto + " sobre " + s, token(texto), token(s), TEXTO));
            }
        }
        pares.add(par("--adm-ok sobre --adm-ok-bg", token("--adm-ok"), token("--adm-ok-bg"), TEXTO));
        pares.add(par("--adm-warn sobre --adm-warn-bg", token("--adm-warn"), token("--adm-warn-bg"), TEXTO));
        pares.add(par("--adm-off sobre --adm-off-bg", token("--adm-off"), token("--adm-off-bg"), TEXTO));
        pares.add(par("--ink-500 sobre --cream-100", token("--ink-500"), token("--cream-100"), TEXTO));
        todos(pares);
    }

    // ---------- gráficos (CP-48) ----------

    @DisplayName("CP-48: en las barras apiladas reservas y membresías se distinguen entre sí y del fondo (3:1)")
    @Test
    void cp48_lasSeriesDeLasBarrasSeDistinguen() {
        int[] reservas = color(declaracion(".rep-seg-r", "background-color"), BLANCO);
        int[] membresias = color(declaracion(".rep-seg-m", "background-color"), BLANCO);
        todos(List.of(
                par("reservas sobre el fondo", reservas, BLANCO, NO_TEXTO),
                par("membresías sobre el fondo", membresias, BLANCO, NO_TEXTO),
                par("reservas contra membresías", reservas, membresias, NO_TEXTO)));
    }

    @DisplayName("CP-48: la leyenda usa los mismos colores que las barras")
    @Test
    void cp48_laLeyendaCoincideConLasBarras() {
        assertTrue(declaracion(".rep-sw-r", "background-color").equals(declaracion(".rep-seg-r", "background-color")));
        assertTrue(declaracion(".rep-sw-m", "background-color").equals(declaracion(".rep-seg-m", "background-color")));
    }

    @DisplayName("CP-49: la serie de membresías lleva una división propia y no depende solo del color")
    @Test
    void cp49_laSerieDeMembresiasTieneDivision() {
        String borde = declaracion(".rep-seg-m", "box-shadow");
        assertNotNull(borde, "falta separar con una línea las dos partes de la barra");
    }

    @DisplayName("CP-48: los indicadores de bueno, atención y malo se ven sobre el fondo (3:1)")
    @Test
    void cp48_losIndicadoresSeVenSobreElFondo() {
        todos(List.of(
                par("indicador ok", token("--adm-ok-dot"), BLANCO, NO_TEXTO),
                par("indicador atención", token("--adm-warn-dot"), BLANCO, NO_TEXTO),
                par("indicador malo", token("--adm-off-dot"), BLANCO, NO_TEXTO)));
    }

    @DisplayName("CP-48: los cinco tramos de la dona se ven sobre el fondo (3:1)")
    @Test
    void cp48_losTramosDeLaDonaSeVenSobreElFondo() {
        List<Runnable> pares = new ArrayList<>();
        for (int n = 1; n <= 5; n++) {
            pares.add(par("tramo de " + n + " estrellas", token("--val-c" + n), BLANCO, NO_TEXTO));
        }
        todos(pares);
    }

    @DisplayName("CP-49: la estrella llena y la vacía se distinguen por la forma y el color llega a 3:1")
    @Test
    void cp49_lasEstrellasSeDistinguenPorLaForma() {
        String llena = declaracion(".val-stars .val-star.is-on", "font-variation-settings");
        String vacia = declaracion(".val-stars .val-star", "font-variation-settings");
        assertNotNull(llena, "la estrella llena tiene que declarar su relleno");
        assertNotNull(vacia, "la estrella vacía tiene que ser un contorno (otro relleno)");
        assertTrue(llena.contains("'FILL' 1") && vacia.contains("'FILL' 0"));
        todos(List.of(
                par("estrella llena", color(declaracion(".val-stars .val-star.is-on", "color"), BLANCO), BLANCO, NO_TEXTO),
                par("estrella vacía", color(declaracion(".val-stars .val-star", "color"), BLANCO), BLANCO,
                        NO_TEXTO)));
    }

    @DisplayName("CP-48: los números del mapa de calor se leen sobre su celda (4,5:1)")
    @Test
    void cp48_losNumerosDelMapaDeCalorSeLeen() {
        List<Runnable> pares = new ArrayList<>();
        for (int nivel = 0; nivel <= 5; nivel++) {
            String sel = ".rep-h" + nivel;
            int[] fondo = nivel == 0 ? BLANCO : color(declaracion(sel, "background"), BLANCO);
            pares.add(par("celda " + sel, color(declaracion(sel, "color"), fondo), fondo, TEXTO));
        }
        todos(pares);
    }

    // ---------- foco (CC-05: "con foco visible") ----------

    @DisplayName("CP-47: el foco del teclado se ve sobre el fondo claro (3:1) en el panel del socio y del administrador")
    @Test
    void cp47_elFocoSeVe() {
        List<Runnable> pares = new ArrayList<>();
        for (String selector : new String[] {".adm-body :focus-visible", ".so-body :focus-visible"}) {
            String contorno = declaracion(selector, "outline");
            assertNotNull(contorno, "falta el contorno de foco de " + selector);
            Matcher m = Pattern.compile("solid\\s+(.+)$").matcher(contorno);
            assertTrue(m.find(), "el contorno de foco tiene que ser una línea de color: " + contorno);
            pares.add(par("foco de " + selector, color(m.group(1), BLANCO), BLANCO, NO_TEXTO));
        }
        todos(pares);
    }
}
