package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.servicio.PanelSocioServicio;
import com.sigrid.sigrid.servicio.ReglaReservaException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Formulario "Renová tu membresía" de socio/inicio.xhtml: sube el comprobante de la cuota y crea el pedido que el
 * administrador acepta o deniega en Membresías.
 */
@Named("socioMembresiaBean")
@RequestScoped
public class SocioMembresiaBean {

    private static final DateTimeFormatter FORMATO_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Inject
    private SocioBean socioBean;
    @Inject
    private PanelSocioServicio servicio;

    private Part archivo;
    private String fechaOperacion; // "2026-11-22T14:30" (input datetime-local)

    /** Envía el comprobante. Si sale bien vuelve a cargar Inicio (para no reenviar el archivo al refrescar). */
    public String renovar() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            if (archivo == null || archivo.getSize() == 0) {
                throw new ReglaReservaException("Elegí el archivo del comprobante.");
            }
            if (archivo.getSize() > 5L * 1024 * 1024) {
                throw new ReglaReservaException("El archivo pesa más de 5 MB.");
            }
            LocalDateTime operacion = fechaOperacion == null || fechaOperacion.isBlank() ? null
                    : LocalDateTime.parse(fechaOperacion, FORMATO_INPUT);
            servicio.renovarMembresia(socioBean.getSocio().getIdSocio(), archivo.getInputStream().readAllBytes(), operacion,
                    LocalDateTime.now());
            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Recibimos tu comprobante. Cuando el administrador valide el pago, tu membresía se renueva por un mes.", null));
            return "/socio/inicio?faces-redirect=true";
        } catch (ReglaReservaException e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        } catch (DateTimeParseException e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Indicá cuándo hiciste la transferencia.", null));
        } catch (IOException e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "No pudimos leer el archivo. Probá de nuevo.", null));
        }
        return null;
    }

    /** Tope para el campo de la transferencia: no puede ser futura. */
    public String getAhoraInput() {
        return LocalDateTime.now().format(FORMATO_INPUT);
    }

    public Part getArchivo() {
        return archivo;
    }

    public void setArchivo(Part archivo) {
        this.archivo = archivo;
    }

    public String getFechaOperacion() {
        return fechaOperacion;
    }

    public void setFechaOperacion(String fechaOperacion) {
        this.fechaOperacion = fechaOperacion;
    }
}
