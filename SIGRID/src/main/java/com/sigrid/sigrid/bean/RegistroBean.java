package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.RegistroSocio;
import com.sigrid.sigrid.servicio.RegistroServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

/**
 * Bean de register.xhtml (alta pública, CU-01). ViewScoped: el paso 1 y el paso 2 conviven en la
 * misma vista (la animación que los alterna es puramente visual, en el cliente), así que el estado
 * tiene que sobrevivir entre los dos AJAX sin volver a pedir los datos ya cargados.
 */
@Named("registroBean")
@ViewScoped
public class RegistroBean implements Serializable {

    @Inject
    private RegistroServicio registroServicio;

    private RegistroSocio datos;
    private boolean pasoUnoCompletado;
    private String errorPasoUno;
    private String errorPasoDos;

    @PostConstruct
    public void init() {
        datos = new RegistroSocio();
    }

    /** Valida el correo y la contraseña; si está todo bien, habilita el paso 2 (lo revela el JS). */
    public void validarPasoUno() {
        errorPasoUno = registroServicio.validarPasoUno(datos.getEmail(), datos.getPassword(), datos.getConfirmarPassword());
        pasoUnoCompletado = errorPasoUno == null;
    }

    /** Alta final. @return la redirección a /login si quedó creada; null (se queda en la página) si hubo un error. */
    public String finalizarRegistro() {
        if (!pasoUnoCompletado) {
            errorPasoDos = "Completá primero el correo y la contraseña.";
            return null;
        }
        errorPasoDos = registroServicio.registrar(datos);
        if (errorPasoDos != null) {
            return null;
        }
        FacesContext contexto = FacesContext.getCurrentInstance();
        contexto.getExternalContext().getFlash().setKeepMessages(true);
        contexto.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Cuenta creada. Ya podés ingresar con tu correo y contraseña.", null));
        return "/login?faces-redirect=true";
    }

    public RegistroSocio getDatos() {
        return datos;
    }

    public boolean isPasoUnoCompletado() {
        return pasoUnoCompletado;
    }

    public String getErrorPasoUno() {
        return errorPasoUno;
    }

    public String getErrorPasoDos() {
        return errorPasoDos;
    }
}
