package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.Enlace;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * Bean de navegación: las secciones de la home pública, definidas una sola vez
 * y usadas por el navbar y por los accesos rápidos del footer
 * (templates/public-home.xhtml), para que nunca queden distintos.
 *
 * Contenido fijo del sitio, sin DAO ni Servicio (igual que HomeBean).
 */
@Named("navBean")
@ApplicationScoped
public class NavBean implements Serializable {

    private List<Enlace> secciones;

    @PostConstruct
    public void init() {
        secciones = List.of(
                new Enlace("Inicio", "/index", "top"),
                new Enlace("Instalaciones", "/instalaciones"),
                new Enlace("Preguntas Frecuentes", "/faqpage"));
    }

    public List<Enlace> getSecciones() {
        return secciones;
    }
}
