package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.GrupoMenu;
import com.sigrid.sigrid.dto.ItemMenu;
import com.sigrid.sigrid.servicio.DashboardServicio;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Menú lateral del panel de administración (templates/admin-dashboard.xhtml), definido una sola vez.
 *
 * Cada sección vive en /admin/{seccion}-dashboard-admin.xhtml. El menú solo enlaza las vistas que ya
 * existen (existe): las demás se ven como "Próximamente" y pasan a ser enlaces solas al crear el archivo.
 */
@Named("adminNavBean")
@RequestScoped
public class AdminNavBean {

    @Inject
    private DashboardServicio servicio;

    private List<GrupoMenu> grupos;

    public List<GrupoMenu> getGrupos() {
        if (grupos == null) {
            grupos = List.of(
                    new GrupoMenu("Inicio", List.of(
                            item("Dashboard", "space_dashboard", "inicio", 0),
                            item("Calendario", "calendar_month", "calendario", 0))),
                    new GrupoMenu("Gestión de socios", List.of(
                            item("Socios", "group", "socios", 0),
                            item("Membresías", "card_membership", "membresias", servicio.contarMembresiasPendientes()))),
                    new GrupoMenu("Gestión de reservas", List.of(
                            item("Reservas", "event_available", "reservas", 0),
                            item("Solicitudes pendientes", "pending_actions", "solicitudes",
                                    servicio.contarSolicitudesPendientes()),
                            item("Cancelaciones y reprogramaciones", "event_repeat", "cancelaciones",
                                    servicio.contarCambiosPendientes()))),
                    new GrupoMenu("Gestión del predio", List.of(
                            item("Instalaciones", "stadium", "instalaciones", 0))),
                    new GrupoMenu("Seguimiento", List.of(
                            item("Valoraciones", "star", "valoraciones", 0),
                            item("Reportes", "bar_chart", "reportes", 0))));
        }
        return grupos;
    }

    /** Vuelve a armar el menú (sus contadores) en la misma petición: lo llaman las acciones que cambian esos números. */
    public void refrescar() {
        grupos = null;
    }

    /** ¿Ya existe esa vista en la aplicación? (ruta sin extensión, ej. "/admin/reservas-dashboard-admin") */
    public boolean existe(String vista) {
        try {
            return FacesContext.getCurrentInstance().getExternalContext().getResource(vista + ".xhtml") != null;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private static ItemMenu item(String texto, String icono, String seccion, long contador) {
        return new ItemMenu(texto, icono, "/admin/" + seccion + "-dashboard-admin", contador);
    }
}
