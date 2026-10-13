package com.sigrid.sigrid.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Hash PBKDF2 de contraseñas: formato, sal por hash, verificación correcta e incorrecta y casos límite. */
class PasswordUtilTest {

    private static final String CLAVE = "Polideportivo#2026";

    @DisplayName("CP-07: el valor guardado es PBKDF2 con tres partes, no texto plano")
    @Test
    void generaElHashConTresPartesIteracionesSalYHash() {
        String hash = PasswordUtil.generarHash(CLAVE);

        String[] partes = hash.split(":");
        assertEquals(3, partes.length);
        assertEquals("120000", partes[0]);
        assertEquals(16, Base64.getDecoder().decode(partes[1]).length, "la sal es de 16 bytes");
        assertEquals(32, Base64.getDecoder().decode(partes[2]).length, "el hash es de 256 bits");
    }

    @DisplayName("CP-07: la misma contraseña genera hashes distintos (sal propia)")
    @Test
    void dosHashesDeLaMismaContrasenaUsanSalDistinta() {
        String primero = PasswordUtil.generarHash(CLAVE);
        String segundo = PasswordUtil.generarHash(CLAVE);

        assertNotEquals(primero.split(":")[1], segundo.split(":")[1], "la sal tiene que cambiar en cada hash");
        assertNotEquals(primero, segundo);
    }

    @Test
    void dosHashesDeLaMismaContrasenaSeVerificanAmbos() {
        String primero = PasswordUtil.generarHash(CLAVE);
        String segundo = PasswordUtil.generarHash(CLAVE);

        assertTrue(PasswordUtil.verificar(CLAVE, primero));
        assertTrue(PasswordUtil.verificar(CLAVE, segundo));
    }

    @Test
    void verificaLaContrasenaCorrecta() {
        String hash = PasswordUtil.generarHash(CLAVE);

        assertTrue(PasswordUtil.verificar(CLAVE, hash));
    }

    @Test
    void rechazaUnaContrasenaIncorrecta() {
        String hash = PasswordUtil.generarHash(CLAVE);

        assertFalse(PasswordUtil.verificar("Polideportivo#2027", hash));
    }

    @Test
    void distingueMayusculasDeMinusculas() {
        String hash = PasswordUtil.generarHash("ClaveSegura1");

        assertFalse(PasswordUtil.verificar("clavesegura1", hash));
    }

    @Test
    void verificaContrasenasConAcentosYEnie() {
        String hash = PasswordUtil.generarHash("Contraseña-Árbol-ñandú");

        assertTrue(PasswordUtil.verificar("Contraseña-Árbol-ñandú", hash));
    }

    @Test
    void rechazaUnaContrasenaNulaSinLanzarExcepcion() {
        String hash = PasswordUtil.generarHash(CLAVE);

        assertFalse(PasswordUtil.verificar(null, hash));
    }

    @Test
    void rechazaUnHashNuloSinLanzarExcepcion() {
        assertFalse(PasswordUtil.verificar(CLAVE, null));
    }

    @Test
    void rechazaUnHashConCantidadDePartesIncorrecta() {
        assertFalse(PasswordUtil.verificar(CLAVE, "120000:soloDosPartes"));
    }

    @Test
    void rechazaUnHashVacio() {
        assertFalse(PasswordUtil.verificar(CLAVE, ""));
    }

    @Test
    void laContrasenaVaciaSeHasheaYSoloVerificaConVacia() {
        String hash = PasswordUtil.generarHash("");

        assertTrue(PasswordUtil.verificar("", hash));
        assertFalse(PasswordUtil.verificar("x", hash));
    }

    /**
     * Caracteriza el comportamiento actual (no hay una regla escrita): una contraseña nula en generarHash lanza
     * NullPointerException. Ver observación O-3 del informe de resultados.
     */
    @Test
    void generarHashConContrasenaNulaLanzaNullPointerException() {
        assertThrows(NullPointerException.class, () -> PasswordUtil.generarHash(null));
    }

    /**
     * D-1 (corregido): verificar() devuelve false para hash nulo, vacío o con partes de menos y también con un hash de
     * tres partes mal formado (primera parte no numérica, Base64 inválido o iteraciones inválidas). Antes lanzaba
     * NumberFormatException o IllegalArgumentException: un dato corrupto en password_hash rompía el login con un error
     * en vez de rechazar la contraseña.
     */
    @DisplayName("CP-07: un hash mal formado no debería romper la verificación (D-1)")
    @Test
    void rechazaUnHashMalFormadoSinLanzarExcepcion() {
        assertFalse(PasswordUtil.verificar(CLAVE, "abc:xyz:123"));
    }
}
