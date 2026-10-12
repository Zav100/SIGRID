package com.sigrid.sigrid.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprobantes de transferencia: el tipo real sale de los primeros bytes (no del nombre) y los archivos se guardan
 * en una carpeta controlada. El límite de 5 MB no está en esta clase: lo controla PanelSocioServicio.subirComprobante.
 */
class ComprobanteArchivosTest {

    private static final String PROPIEDAD = "sigrid.comprobantes";

    @TempDir
    Path raiz;

    private Path carpeta;
    private String propiedadOriginal;

    @BeforeEach
    void apuntarALaCarpetaTemporal() {
        propiedadOriginal = System.getProperty(PROPIEDAD);
        carpeta = raiz.resolve("comprobantes");
        System.setProperty(PROPIEDAD, carpeta.toString());
    }

    @AfterEach
    void restaurarLaPropiedad() {
        if (propiedadOriginal == null) {
            System.clearProperty(PROPIEDAD);
        } else {
            System.setProperty(PROPIEDAD, propiedadOriginal);
        }
    }

    private static byte[] bytes(int... valores) {
        byte[] datos = new byte[valores.length];
        for (int i = 0; i < valores.length; i++) {
            datos[i] = (byte) valores[i];
        }
        return datos;
    }

    // ---------- tipo real por los primeros bytes ----------

    @Test
    void detectaUnPdfPorSusPrimerosBytes() {
        byte[] pdf = "%PDF-1.4 transferencia".getBytes(StandardCharsets.US_ASCII);

        assertEquals("pdf", ComprobanteArchivos.extension(pdf));
    }

    @Test
    void detectaUnPngPorSusPrimerosBytes() {
        byte[] png = bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00);

        assertEquals("png", ComprobanteArchivos.extension(png));
    }

    @Test
    void detectaUnJpegPorSusPrimerosBytes() {
        byte[] jpg = bytes(0xFF, 0xD8, 0xFF, 0xE0);

        assertEquals("jpg", ComprobanteArchivos.extension(jpg));
    }

    @Test
    void elTipoSaleDeLosBytesYNoDelNombreDelArchivo() {
        // un PNG que el socio nombró "transferencia.pdf" sigue siendo PNG
        byte[] pngConNombreDePdf = bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00);

        String tipoReal = ComprobanteArchivos.extension(pngConNombreDePdf);

        assertEquals("png", tipoReal);
        assertEquals("image/png", ComprobanteArchivos.tipoMime("reserva-1-abc." + tipoReal));
    }

    @Test
    void rechazaUnEjecutableRenombradoComoPdf() {
        byte[] ejecutable = bytes('M', 'Z', 0x90, 0x00, 0x03, 0x00);

        assertNull(ComprobanteArchivos.extension(ejecutable));
    }

    @Test
    void rechazaUnTextoPlano() {
        byte[] texto = "transferencia realizada".getBytes(StandardCharsets.UTF_8);

        assertNull(ComprobanteArchivos.extension(texto));
    }

    @Test
    void rechazaUnArchivoVacio() {
        assertNull(ComprobanteArchivos.extension(new byte[0]));
    }

    // valores límite de largo mínimo que exige cada firma

    @Test
    void noReconoceUnPdfDeSoloCuatroBytes() {
        assertNull(ComprobanteArchivos.extension(bytes('%', 'P', 'D', 'F')));
    }

    @Test
    void reconoceUnPdfDeCincoBytes() {
        assertEquals("pdf", ComprobanteArchivos.extension(bytes('%', 'P', 'D', 'F', '-')));
    }

    @Test
    void noReconoceUnPngDeSoloOchoBytes() {
        assertNull(ComprobanteArchivos.extension(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)));
    }

    @Test
    void noReconoceUnJpegDeSoloTresBytes() {
        assertNull(ComprobanteArchivos.extension(bytes(0xFF, 0xD8, 0xFF)));
    }

    @Test
    void rechazaUnJpegCuyoTercerByteNoEsFF() {
        assertNull(ComprobanteArchivos.extension(bytes(0xFF, 0xD8, 0x00, 0xE0)));
    }

    @Test
    void asignaElTipoMimeSegunLaExtensionDelNombreGuardado() {
        assertEquals("application/pdf", ComprobanteArchivos.tipoMime("reserva-7-3fa9.pdf"));
        assertEquals("image/png", ComprobanteArchivos.tipoMime("reserva-7-3fa9.png"));
        assertEquals("image/jpeg", ComprobanteArchivos.tipoMime("reserva-7-3fa9.jpg"));
    }

    // ---------- archivos en la carpeta de comprobantes ----------

    @Test
    void guardaElArchivoYDevuelveSuRuta() throws IOException {
        byte[] datos = "%PDF-1.4 contenido".getBytes(StandardCharsets.US_ASCII);

        ComprobanteArchivos.guardar("reserva-7-3fa9.pdf", datos);

        Path ruta = ComprobanteArchivos.ruta("reserva-7-3fa9.pdf");
        assertNotNull(ruta);
        assertArrayEquals(datos, Files.readAllBytes(ruta));
    }

    @Test
    void laRutaEsNulaSiElArchivoNoExiste() {
        assertNull(ComprobanteArchivos.ruta("no-existe.pdf"));
    }

    @Test
    void laRutaEsNulaSiElNombreIntentaSalirseDeLaCarpeta() throws IOException {
        Files.createDirectories(carpeta);
        Files.write(raiz.resolve("secreto.txt"), "no debe poder leerse".getBytes(StandardCharsets.UTF_8));

        assertNull(ComprobanteArchivos.ruta("../secreto.txt"));
    }

    @Test
    void borraElArchivoGuardado() throws IOException {
        ComprobanteArchivos.guardar("reserva-7-3fa9.pdf", "%PDF-1.4".getBytes(StandardCharsets.US_ASCII));

        ComprobanteArchivos.borrar("reserva-7-3fa9.pdf");

        assertFalse(Files.exists(carpeta.resolve("reserva-7-3fa9.pdf")));
    }

    @Test
    void borrarUnArchivoInexistenteNoLanzaExcepcion() {
        assertDoesNotThrow(() -> ComprobanteArchivos.borrar("no-existe.pdf"));
    }
}
