package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.RedSocial;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

/**
 * Bean del footer (fijo en todas las páginas, vive en templates/public-home.xhtml).
 * Los accesos rápidos a las secciones salen de NavBean (los mismos del navbar).
 *
 * Igual que HomeBean: es contenido fijo del sitio (redes y textos no salen de
 * ninguna tabla), por eso no tiene DAO ni Servicio detrás.
 */
@Named("footerBean")
@ApplicationScoped
public class FooterBean implements Serializable {

    private List<RedSocial> redesSociales;

    /**
     * Textos de la fila legal. Todavía no tienen página: el template los muestra
     * como links vacíos ("#"). Cuando existan las vistas, pasarlos a Enlace y
     * usar h:link con outcome.
     */
    private List<String> textosLegales;

    @PostConstruct
    public void init() {
        redesSociales = new ArrayList<>();
        redesSociales.add(RedSocial.conImagen("Instagram", "https://www.instagram.com/unseoficial?stkn=MTF3dm05d2xubnYxOA==",
                "instagram-grey-outline-22754_256.png"));
        redesSociales.add(new RedSocial("Facebook", "https://www.facebook.com/PolideportivoUNSE/",
                "M13.5 21v-7.9h2.6l.4-3.1h-3V8.1c0-.9.25-1.5 1.55-1.5H16.6V3.8c-.28-.04-1.25-.12-2.37-.12-2.35 0-3.96 1.43-3.96 4.06v2.27H7.6v3.1h2.67V21h3.23Z"));
        redesSociales.add(new RedSocial("YouTube", "https://www.youtube.com/channel/UCN4TjpS0mDcLcExuv2zA-5g",
                "M22 12s0-3-.38-4.4a2.87 2.87 0 0 0-2-2C17.9 5.2 12 5.2 12 5.2s-5.9 0-7.62.4a2.87 2.87 0 0 0-2 2C2 9 2 12 2 12s0 3 .38 4.4a2.87 2.87 0 0 0 2 2c1.72.4 7.62.4 7.62.4s5.9 0 7.62-.4a2.87 2.87 0 0 0 2-2C22 15 22 12 22 12ZM10 15.2V8.8L15.5 12 10 15.2Z"));

        textosLegales = List.of("Contacto", "Términos y condiciones", "Privacidad");
    }

    /** Año actual, calculado solo (no hardcodeado), para que el copyright nunca quede desactualizado. */
    public int getAnioActual() {
        return Year.now().getValue();
    }

    public List<RedSocial> getRedesSociales() {
        return redesSociales;
    }

    public List<String> getTextosLegales() {
        return textosLegales;
    }
}
