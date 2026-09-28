package com.sigrid.sigrid.seguridad;

import com.sigrid.sigrid.dao.ComprobantePagoDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.repositorio.ComprobantePago;
import com.sigrid.sigrid.repositorio.Reserva;
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
 * Muestra al administrador el comprobante que subió el socio para una reserva (/admin/comprobante?reserva=ID). Cuelga de
 * /admin/*, así que AdminFilter ya lo restringe al rol ADMINISTRADOR. Como los DAO se piden por CDI.current(), igual
 * que en los filtros.
 */
@WebServlet("/admin/comprobante")
public class ComprobanteServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Path archivo = buscar(req.getParameter("reserva"));
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

    private static Path buscar(String idReserva) {
        if (idReserva == null || !idReserva.matches("\\d{1,9}")) {
            return null;
        }
        Reserva reserva = CDI.current().select(ReservaDAO.class).get().buscarPorId(Integer.valueOf(idReserva));
        if (reserva == null || reserva.getIdPago() == null) {
            return null;
        }
        ComprobantePago comprobante = CDI.current().select(ComprobantePagoDAO.class).get()
                .buscarPorPago(reserva.getIdPago().getIdPago());
        return comprobante == null ? null : ComprobanteArchivos.ruta(comprobante.getArchivoUrl());
    }
}
