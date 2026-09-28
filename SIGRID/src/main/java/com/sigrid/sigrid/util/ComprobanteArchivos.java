package com.sigrid.sigrid.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Los comprobantes de transferencia que suben los socios se guardan como archivos en una carpeta del servidor
 * (propiedad del sistema sigrid.comprobantes; por defecto "sigrid-comprobantes" en el directorio del usuario que
 * corre GlassFish). En la base queda solo el nombre (comprobante_pago.archivo_url) y el hash.
 */
public final class ComprobanteArchivos {

    private ComprobanteArchivos() {
    }

    private static Path carpeta() {
        String propia = System.getProperty("sigrid.comprobantes");
        return (propia != null ? Paths.get(propia) : Paths.get(System.getProperty("user.home"), "sigrid-comprobantes"))
                .toAbsolutePath().normalize();
    }

    public static void guardar(String nombre, byte[] datos) throws IOException {
        Files.createDirectories(carpeta());
        Files.write(carpeta().resolve(nombre), datos); // el nombre lleva el hash: reescribirlo deja el mismo contenido
    }

    public static void borrar(String nombre) {
        try {
            Files.deleteIfExists(carpeta().resolve(nombre));
        } catch (IOException e) {
            // ponytail: si no se puede borrar queda un archivo huérfano; se limpia a mano
        }
    }

    /** La ruta del archivo, o null si el nombre intenta salirse de la carpeta o el archivo no existe. */
    public static Path ruta(String nombre) {
        Path p = carpeta().resolve(nombre).normalize();
        return p.startsWith(carpeta()) && Files.isRegularFile(p) ? p : null;
    }

    /** Tipo real del archivo según sus primeros bytes ("pdf", "png" o "jpg"); null si no es de un tipo admitido. */
    public static String extension(byte[] d) {
        if (d.length > 4 && d[0] == '%' && d[1] == 'P' && d[2] == 'D' && d[3] == 'F') {
            return "pdf";
        }
        if (d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return "png";
        }
        if (d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        return null;
    }

    public static String tipoMime(String nombre) {
        return nombre.endsWith(".pdf") ? "application/pdf" : nombre.endsWith(".png") ? "image/png" : "image/jpeg";
    }
}
