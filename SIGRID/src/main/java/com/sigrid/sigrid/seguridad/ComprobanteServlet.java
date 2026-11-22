package com.sigrid.sigrid.seguridad;

import com.sigrid.sigrid.dao.ComprobantePagoDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.SuscripcionSocioDAO;
import com.sigrid.sigrid.repositorio.ComprobantePago;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import com.sigrid.sigrid.util.ComprobanteArchivos;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Muestra al administrador el comprobante que subió el socio para una reserva (/admin/comprobante?reserva=ID) o para
 * renovar su membresía (/admin/comprobante?suscripcion=ID). Cuelga de
 * /admin/*, así que AdminFilter ya lo restringe al rol ADMINISTRADOR. Como los DAO se piden por CDI.current(), igual
 * que en los filtros.
 */
@WebServlet("/admin/comprobante")
public class ComprobanteServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Path archivo = buscar(req);
        if (archivo == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "El comprobante no está disponible.");
            return;
        }
        String nombre = archivo.getFileName().toString();
        resp.setContentType(ComprobanteArchivos.tipoMime(nombre));
        resp.setContentLengthLong(Files.size(archivo));
        resp.setHeader("Content-Disposition", "inline; filename=\"" + nombre + "\"");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "private, no-store");
        Files.copy(archivo, resp.getOutputStream());
    }

    /** El archivo del comprobante de ?reserva=ID o de ?suscripcion=ID (renovación de membresía); null si no hay. */
    private static Path buscar(HttpServletRequest req) {
        Pago pago = null;
        Integer idReserva = id(req.getParameter("reserva"));
        Integer idSuscripcion = id(req.getParameter("suscripcion"));
        if (idReserva != null) {
            Reserva reserva = CDI.current().select(ReservaDAO.class).get().buscarPorId(idReserva);
            pago = reserva == null ? null : reserva.getIdPago();
        } else if (idSuscripcion != null) {
            SuscripcionSocio suscripcion = CDI.current().select(SuscripcionSocioDAO.class).get().buscarPorId(idSuscripcion);
            pago = suscripcion == null ? null : suscripcion.getIdPago();
        }
        if (pago == null) {
            return null;
        }
        ComprobantePago comprobante = CDI.current().select(ComprobantePagoDAO.class).get().buscarPorPago(pago.getIdPago());
        return comprobante == null ? null : ComprobanteArchivos.ruta(comprobante.getArchivoUrl());
    }

    private static Integer id(String texto) {
        return texto != null && texto.matches("\\d{1,9}") ? Integer.valueOf(texto) : null;
    }
}
