package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.repositorio.Usuario;
import com.sigrid.sigrid.servicio.UsuarioServicio;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

/**
 * Bean de sesión: guarda el usuario logueado (si hay uno) y resuelve el
 * formulario de login. El rol sale de usuario.id_rol (tabla rol: SOCIO o
 * ADMINISTRADOR); las cuentas de administrador las crea el personal de
 * sistemas, no hay registro público para ellas.
 */
@Named("loginBean")
@SessionScoped
public class LoginBean implements Serializable {

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ROL_SOCIO = "SOCIO";

    @Inject
    private UsuarioServicio usuarioServicio;

    private String email;
    private String password;
    private Usuario usuarioLogueado;

    public String autenticar() {
        Usuario usuario = usuarioServicio.autenticar(email, password);
        password = null; // no lo dejamos en el bean de sesión

        if (usuario == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Correo o contraseña incorrectos.", null));
            return null;
        }

        this.usuarioLogueado = usuario;
        return isAdministrador()
                ? "/admin/inicio-dashboard-admin?faces-redirect=true"
                : "/socio/inicio?faces-redirect=true";
    }

    public String cerrarSesion() {
        this.usuarioLogueado = null;
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/index?faces-redirect=true";
    }

    public boolean isLogueado() {
        return usuarioLogueado != null;
    }

    public boolean isAdministrador() {
        return usuarioLogueado != null
                && ROL_ADMINISTRADOR.equals(usuarioLogueado.getIdRol().getNombreRol());
    }

    public boolean isSocio() {
        return usuarioLogueado != null
                && ROL_SOCIO.equals(usuarioLogueado.getIdRol().getNombreRol());
    }

    public Integer getIdUsuarioLogueado() {
        return usuarioLogueado != null ? usuarioLogueado.getIdUsuario() : null;
    }

    /** Nombre para el saludo; si todavía no completó el paso 2 del alta (nombre NULL), muestra el correo. */
    public String getNombreUsuarioLogueado() {
        if (usuarioLogueado == null) {
            return null;
        }
        String nombre = usuarioLogueado.getNombre();
        return (nombre == null || nombre.isBlank()) ? usuarioLogueado.getEmail() : nombre;
    }

    /** Iniciales para el avatar (nombre + apellido); si no completó el alta, la primera letra del correo. */
    public String getInicialesUsuarioLogueado() {
        if (usuarioLogueado == null) {
            return null;
        }
        String nombre = usuarioLogueado.getNombre();
        String apellido = usuarioLogueado.getApellido();
        String iniciales = (nombre == null || nombre.isBlank() ? "" : nombre.substring(0, 1))
                + (apellido == null || apellido.isBlank() ? "" : apellido.substring(0, 1));
        return (iniciales.isEmpty() ? usuarioLogueado.getEmail().substring(0, 1) : iniciales).toUpperCase();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
