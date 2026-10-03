package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.DatoDestacado;
import com.sigrid.sigrid.dao.InstalacionDAO;
import com.sigrid.sigrid.dto.GaleriaItem;
import com.sigrid.sigrid.dto.HorarioFila;
import com.sigrid.sigrid.dto.InstalacionCard;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.servicio.HorarioServicio;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Bean de la home pública (index.xhtml).
 *
 * Casi todo lo que expone es CONTENIDO FIJO del sitio: no sale de ninguna tabla
 * (mismo criterio que HomeBean en REPS); la excepción es el horario de la pileta. Si
 * más adelante las instalaciones destacadas pasan a leerse de la BD, se
 * reemplaza la lista por una consulta a InstalacionDAO; el .xhtml no cambia.
 */
@Named("homeBean")
@ApplicationScoped
public class HomeBean implements Serializable {

    @Inject
    private InstalacionDAO instalacionDAO;
    @Inject
    private HorarioServicio horarioServicio;

    private List<InstalacionCard> instalaciones;
    private List<DatoDestacado> destacados;
    private List<String> beneficiosCarnet;
    private List<GaleriaItem> galeria;

    @PostConstruct
    public void init() {
        instalaciones = new ArrayList<>();
        instalaciones.add(new InstalacionCard("futbol11-noche.webp", "Cancha de Fútbol 11",
                "Cancha de Fútbol 11", "Predio UNSE", "Instalación del predio", "Capacidad: 22 jugadores"));
        instalaciones.add(new InstalacionCard("news-institucional.png", "Pileta del predio",
                "Pileta", "Predio UNSE", "Instalación del predio", "Capacidad: 40 personas"));
        instalaciones.add(new InstalacionCard("news-rugby.png", "Cancha de rugby del predio",
                "Cancha de Rugby", "Predio UNSE", "Instalación del predio", "Capacidad: 30 jugadores"));
        instalaciones.add(new InstalacionCard("news-voley.png", "Cancha de beach vóley del predio",
                "Cancha de Beach Vóley", "Predio UNSE", "Instalación del predio", "Capacidad: 4 por equipo"));

        destacados = new ArrayList<>();
        destacados.add(new DatoDestacado("100+", "Socios activos", false));
        destacados.add(new DatoDestacado("8+", "Años de trayectoria", false));
        destacados.add(new DatoDestacado("4.5", "Calificación de socios", true));

        beneficiosCarnet = List.of(
                "Ingreso rápido al predio sin carnet físico",
                "Descuentos exclusivos en actividades y torneos",
                "Se actualiza solo, sin renovaciones ni filas");

        galeria = new ArrayList<>();
        galeria.add(new GaleriaItem("hero-cancha.webp", null, "photo-real", "Cancha de fútbol al atardecer"));
        galeria.add(new GaleriaItem("basquet.webp", null, "photo-real", "Cancha de básquet"));
        galeria.add(new GaleriaItem("quincho.webp", null, "photo-real", "Los quinchos del predio"));
        galeria.add(new GaleriaItem("news-rugby.png", null, "photo-real", "Cancha de rugby"));
        galeria.add(new GaleriaItem("socios-entrenando.webp", null, "photo-real", "Socios entrenando en el predio"));
    }

    public List<InstalacionCard> getInstalaciones() {
        return instalaciones;
    }

    public List<DatoDestacado> getDestacados() {
        return destacados;
    }

    public List<String> getBeneficiosCarnet() {
        return beneficiosCarnet;
    }

    public List<GaleriaItem> getGaleria() {
        return galeria;
    }

    /** Lo único que no es fijo: las franjas de la pileta como las carga el administrador, leídas de la base en cada vista. */
    public List<HorarioFila> getHorariosPileta() {
        Instalacion pileta = instalacionDAO.buscarPorNombre("Pileta");
        return pileta == null ? List.of() : horarioServicio.listar(pileta.getIdInstalacion(), LocalDate.now());
    }

    /** Las mismas franjas en una frase: "Abre Lun a Vie de 09:00 – 19:00; Sáb y Dom de 10:00 – 20:00". */
    public String getHorariosPiletaTexto() {
        List<String> partes = new ArrayList<>();
        for (HorarioFila h : getHorariosPileta()) {
            partes.add((h.isDeFecha() ? "apertura extra el " : "") + h.getDias() + " de " + h.getHorario());
        }
        return partes.isEmpty() ? "El horario de la pileta se confirma en el predio" : "Abre " + String.join("; ", partes);
    }
}
