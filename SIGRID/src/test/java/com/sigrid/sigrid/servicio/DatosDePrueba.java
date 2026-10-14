package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.TarifaAlquiler;
import com.sigrid.sigrid.repositorio.Turno;
import com.sigrid.sigrid.repositorio.Usuario;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Datos ficticios y fijos para las pruebas de los servicios. Los nombres, DNI y correos son inventados; los montos
 * están en pesos. El "ahora" de todas las pruebas es el mismo para que no dependan del reloj de la máquina.
 */
final class DatosDePrueba {

    /** Lunes 12/10/2026 a las 10:00. */
    static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 12, 10, 0);
    static final LocalDate HOY = AHORA.toLocalDate();

    private DatosDePrueba() {
    }

    static CategoriaSocio categoria(int id, String nombre, boolean requiereLegajo) {
        CategoriaSocio c = new CategoriaSocio(id);
        c.setNombreCategoria(nombre);
        c.setRequiereLegajo(requiereLegajo);
        return c;
    }

    static Usuario usuario(int id, String nombre, String apellido, String email) {
        Usuario u = new Usuario(id);
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setEmail(email);
        return u;
    }

    /** Socio de "Lucía Gómez", DNI 30123456, con el estado de membresía pedido. */
    static Socio socio(int id, Socio.Estado estado, CategoriaSocio categoria) {
        Socio s = new Socio(id);
        s.setIdUsuario(usuario(100 + id, "Lucía", "Gómez", "lucia.gomez@ejemplo.com"));
        s.setIdCategoriaSocio(categoria);
        s.setDni("30123456");
        s.setEstado(estado);
        return s;
    }

    static Instalacion instalacion(int id, String nombre, Instalacion.TipoAcceso acceso, Instalacion.Estado estado) {
        Instalacion i = new Instalacion(id);
        i.setNombre(nombre);
        i.setTipoDisciplina("Fútbol");
        i.setTipoAcceso(acceso);
        i.setEstado(estado);
        return i;
    }

    static Turno turno(int id, Instalacion instalacion, LocalTime inicio, LocalTime fin) {
        Turno t = new Turno(id);
        t.setIdInstalacion(instalacion);
        t.setHoraInicio(inicio);
        t.setHoraFin(fin);
        return t;
    }

    static TarifaAlquiler tarifaAlquiler(int id, Instalacion instalacion, CategoriaSocio categoria, String precio,
            LocalDate desde, LocalDate hasta) {
        TarifaAlquiler t = new TarifaAlquiler(id);
        t.setIdInstalacion(instalacion);
        t.setIdCategoriaSocio(categoria);
        t.setPrecio(new BigDecimal(precio));
        t.setVigenteDesde(desde);
        t.setVigenteHasta(hasta);
        return t;
    }

    static Reserva reserva(int id, Socio socio, Turno turno, LocalDate fechaTurno, Reserva.Estado estado) {
        Reserva r = new Reserva(id);
        r.setIdSocio(socio);
        r.setIdTurno(turno);
        r.setFechaTurno(fechaTurno);
        r.setEstado(estado);
        r.setFechaReserva(AHORA.minusDays(5));
        return r;
    }

    static Pago pago(int id, String monto, Pago.Estado estado) {
        Pago p = new Pago(id);
        p.setMonto(new BigDecimal(monto));
        p.setEstado(estado);
        p.setFechaPago(AHORA.minusDays(5));
        return p;
    }

    /** Un "PDF" del tamaño pedido: empieza con la firma %PDF- y el resto es relleno. */
    static byte[] pdf(int bytes) {
        return conFirma("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII), bytes);
    }

    /** Un "PNG" del tamaño pedido: empieza con la firma de 8 bytes de PNG. */
    static byte[] png(int bytes) {
        return conFirma(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}, bytes);
    }

    /** Un "JPG" del tamaño pedido: empieza con FF D8 FF E0. */
    static byte[] jpg(int bytes) {
        return conFirma(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, bytes);
    }

    private static byte[] conFirma(byte[] firma, int bytes) {
        byte[] datos = new byte[bytes];
        System.arraycopy(firma, 0, datos, 0, Math.min(firma.length, bytes));
        return datos;
    }

    /** El mismo SHA-256 en hexadecimal (64 caracteres) que calcula PanelSocioServicio. */
    static String sha256(byte[] datos) {
        try {
            return String.format("%064x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(datos)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
