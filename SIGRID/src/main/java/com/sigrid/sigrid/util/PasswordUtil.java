package com.sigrid.sigrid.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contraseñas con PBKDF2 (librería estándar del JDK, sin depender de
 * bcrypt/argon2 externos). El resultado de {@link #generarHash(String)} se
 * guarda tal cual en usuario.password_hash: "iteraciones:sal:hash", todo en
 * Base64, tres partes separadas por ':'.
 */
public final class PasswordUtil {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int LARGO_SAL_BYTES = 16;
    private static final int LARGO_HASH_BITS = 256;

    private PasswordUtil() {
    }

    public static String generarHash(String passwordPlano) {
        byte[] sal = new byte[LARGO_SAL_BYTES];
        new SecureRandom().nextBytes(sal);
        byte[] hash = hashear(passwordPlano, sal, ITERACIONES);
        return ITERACIONES + ":" + Base64.getEncoder().encodeToString(sal) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verificar(String passwordPlano, String hashAlmacenado) {
        if (passwordPlano == null || hashAlmacenado == null) {
            return false;
        }
        String[] partes = hashAlmacenado.split(":");
        if (partes.length != 3) {
            return false;
        }
        int iteraciones = Integer.parseInt(partes[0]);
        byte[] sal = Base64.getDecoder().decode(partes[1]);
        byte[] hashEsperado = Base64.getDecoder().decode(partes[2]);
        byte[] hashCalculado = hashear(passwordPlano, sal, iteraciones);
        return java.security.MessageDigest.isEqual(hashEsperado, hashCalculado);
    }

    private static byte[] hashear(String passwordPlano, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(passwordPlano.toCharArray(), sal, iteraciones, LARGO_HASH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }
}
