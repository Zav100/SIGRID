package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.Conflictos;
import com.sigrid.sigrid.dto.Demanda;
import com.sigrid.sigrid.dto.EspacioPlano;
import com.sigrid.sigrid.dto.EstadoInstalaciones;
import com.sigrid.sigrid.dto.FichaInstalacion;
import com.sigrid.sigrid.dto.HistorialTarifa;
import com.sigrid.sigrid.dto.PrecioFila;
import com.sigrid.sigrid.dto.RankingItem;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Instalacion;
import com.sigrid.sigrid.servicio.DashboardServicio;
import com.sigrid.sigrid.servicio.HorarioServicio;
import com.sigrid.sigrid.servicio.InstalacionesServicio;
import com.sigrid.sigrid.servicio.TarifasServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bean de admin/instalaciones-dashboard-admin.xhtml. Al abrirse lee todo una vez; elegir una instalación en el plano o en la
 * tabla de precios, cambiar el modo del plano y las acciones de la ficha (AJAX) vuelven a leer lo que cambia.
 * Cada acción muestra su resultado como mensaje en la ficha; si falla, los campos del formulario se conservan.
 */
@Named("adminInstalacionesBean")
@ViewScoped
public class AdminInstalacionesBean implements Serializable {

    @Inject
    private InstalacionesServicio servicio;
    @Inject
    private HorarioServicio horarioServicio;
    @Inject
    private DashboardServicio dashboardServicio;
    @Inject
    private TarifasServicio tarifasServicio;

    private LocalDateTime ahora;
    private List<Instalacion> instalaciones;
    private Map<Integer, Demanda> demanda;
    private List<CategoriaSocio> categorias;
    private List<PrecioFila> precios;
    private List<RankingItem> ranking;
    private List<EspacioPlano> plano;
    private EstadoInstalaciones estados;
    private FichaInstalacion ficha;
    private Conflictos conflictos = Conflictos.NINGUNO;
    private String abrir = ""; // formulario de la ficha que quedó a medias (data-panel): admin.js lo deja abierto tras el AJAX

    // lo que el administrador eligió: modo del plano ("estado" u "ocupacion") e instalación de la ficha
    private String modo = "estado";
    private Integer seleccionada;

    // formularios de la ficha
    private String descripcion;
    private String capacidad = "";
    private String motivo = "";
    private String hasta = "";
    private boolean cancelarReservas;
    private String motivoBaja = "";
    private boolean cancelarReservasBaja;
    private List<String> diasElegidos = new ArrayList<>();
    private String aperturaSemanal = "";
    private String cierreSemanal = "";
    private String fechaFranja = "";
    private String aperturaFecha = "";
    private String cierreFecha = "";
    private Map<Integer, String> preciosNuevos = new HashMap<>(); // id del tipo de socio -> precio que se está cargando
    private String desdePrecios = "";
    private List<HistorialTarifa> historialPrecios = List.of();

    @PostConstruct
    public void init() {
        categorias = servicio.categorias();
        recargar();
        seleccionada = instalaciones.isEmpty() ? null : masSolicitada().getIdInstalacion();
        cargarFicha();
    }

    /** La de más reservas en los últimos días (si nadie reservó, la primera): es la que se abre al entrar. */
    private Instalacion masSolicitada() {
        Instalacion elegida = instalaciones.get(0);
        long mejor = -1;
        for (Instalacion i : instalaciones) {
            long reservas = demanda.getOrDefault(i.getIdInstalacion(), Demanda.VACIA).getReservas();
            if (reservas > mejor) {
                mejor = reservas;
                elegida = i;
            }
        }
        return elegida;
    }

    /** Vuelve a leer todo lo que muestra la página menos la ficha, conservando el modo del plano y la instalación elegida. */
    private void recargar() {
        ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        instalaciones = servicio.listar();
        demanda = servicio.demanda(hoy);
        precios = servicio.precios(instalaciones, categorias, hoy);
        ranking = servicio.ranking(instalaciones, demanda);
        estados = dashboardServicio.estadoInstalaciones();
        recargarPlano();
    }

    private void recargarPlano() {
        plano = servicio.plano("ocupacion".equals(modo), demanda, ahora);
    }

    /** Lee la ficha de la instalación elegida y deja los formularios como nuevos. */
    private void cargarFicha() {
        ficha = servicio.ficha(seleccionada, demanda, ahora);
        conflictos = Conflictos.NINGUNO;
        abrir = "";
        descripcion = ficha == null || ficha.getDescripcion() == null ? "" : ficha.getDescripcion();
        capacidad = ficha == null || ficha.getCapacidad() == null ? "" : String.valueOf(ficha.getCapacidad());
        motivo = "";
        hasta = "";
        cancelarReservas = false;
        motivoBaja = "";
        cancelarReservasBaja = false;
        diasElegidos = new ArrayList<>();
        aperturaSemanal = "";
        cierreSemanal = "";
        fechaFranja = "";
        aperturaFecha = "";
        cierreFecha = "";

        boolean conPrecios = ficha != null && ficha.isReservable();
        LocalDate hoy = LocalDate.now();
        preciosNuevos = conPrecios ? new HashMap<>(tarifasServicio.vigentesAlquiler(seleccionada, hoy)) : new HashMap<>();
        desdePrecios = hoy.toString();
        historialPrecios = conPrecios ? tarifasServicio.historialAlquiler(seleccionada, hoy) : List.of();
    }

    // ---------- plano y selección (AJAX) ----------

    public void elegirModo(String nuevo) {
        modo = nuevo;
        recargarPlano();
    }

    public void seleccionar(Integer idInstalacion) {
        seleccionada = idInstalacion;
        cargarFicha();
    }

    // ---------- acciones de la ficha (AJAX) ----------

    public void editar() {
        abrir = "editar";
        Integer capacidadNumero = null;
        if (capacidad != null && !capacidad.isBlank()) {
            try {
                capacidadNumero = Integer.valueOf(capacidad.trim());
            } catch (NumberFormatException e) {
                avisar("La capacidad tiene que ser un número entero.", false);
                return;
            }
        }
        resolver(servicio.editar(seleccionada, descripcion, capacidadNumero), "Guardaste los datos de " + ficha.getNombre() + ".");
    }

    /** Agrega un precio nuevo por tipo de socio (el anterior queda en el historial): rige desde la fecha elegida. */
    public void cambiarPrecios() {
        abrir = "precios";
        LocalDate desde;
        try {
            desde = desdePrecios == null || desdePrecios.isBlank() ? null : LocalDate.parse(desdePrecios.trim());
        } catch (DateTimeParseException e) {
            avisar("La fecha no es válida.", false);
            return;
        }
        String error = tarifasServicio.cambiarAlquiler(seleccionada, preciosNuevos, desde, LocalDate.now());
        resolver(error, "Cambiaste los precios de " + ficha.getNombre() + ": rigen desde el "
                + (desde == null ? "" : desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) + ".");
    }

    public void ponerEnMantenimiento() {
        abrir = "mant";
        LocalDate fin;
        try {
            fin = hasta == null || hasta.isBlank() ? null : LocalDate.parse(hasta.trim());
        } catch (DateTimeParseException e) {
            avisar("La fecha de reactivación no es válida.", false);
            return;
        }
        conflictos = servicio.conflictos(seleccionada, fin, ahora);
        int canceladas = cancelarReservas ? conflictos.getTotal() : 0;
        resolver(servicio.retirar(seleccionada, Instalacion.Estado.DESHABILITADA_MANTENIMIENTO, motivo, fin, cancelarReservas, ahora),
                ficha.getNombre() + " quedó en mantenimiento." + textoCanceladas(canceladas));
    }

    public void darDeBaja() {
        abrir = "baja";
        conflictos = servicio.conflictos(seleccionada, null, ahora);
        int canceladas = cancelarReservasBaja ? conflictos.getTotal() : 0;
        resolver(servicio.retirar(seleccionada, Instalacion.Estado.DESHABILITADA, motivoBaja, null, cancelarReservasBaja, ahora),
                ficha.getNombre() + " quedó dada de baja." + textoCanceladas(canceladas));
    }

    public void reactivar() {
        resolver(servicio.reactivar(seleccionada), ficha.getNombre() + " volvió a estar habilitada.");
    }

    private static String textoCanceladas(int canceladas) {
        return canceladas == 0 ? "" : (canceladas == 1 ? " Se canceló 1 reserva" : " Se cancelaron " + canceladas + " reservas")
                + ": las confirmadas quedan con 30 días de crédito para reprogramar.";
    }

    // ---------- horario de apertura (solo las instalaciones de acceso libre) ----------

    public void agregarSemanal() {
        abrir = "semanal";
        List<Integer> dias = new ArrayList<>();
        for (String dia : diasElegidos) {
            dias.add(Integer.valueOf(dia));
        }
        dias.sort(null);
        try {
            resolver(horarioServicio.agregarSemanal(seleccionada, dias, hora(aperturaSemanal), hora(cierreSemanal)),
                    "Agregaste la franja de apertura.");
        } catch (DateTimeParseException e) {
            avisar("Las horas no son válidas.", false);
        }
    }

    public void agregarDeFecha() {
        abrir = "fecha";
        try {
            LocalDate dia = fechaFranja == null || fechaFranja.isBlank() ? null : LocalDate.parse(fechaFranja.trim());
            resolver(horarioServicio.agregarDeFecha(seleccionada, dia, hora(aperturaFecha), hora(cierreFecha), ahora.toLocalDate()),
                    "Agregaste el día de apertura.");
        } catch (DateTimeParseException e) {
            avisar("La fecha o las horas no son válidas.", false);
        }
    }

    public void quitarFranja(String ids) {
        List<Integer> lista = new ArrayList<>();
        for (String id : ids.split(",")) {
            lista.add(Integer.valueOf(id));
        }
        horarioServicio.quitar(seleccionada, lista);
        resolver(null, "Quitaste la franja de apertura.");
    }

    private static LocalTime hora(String texto) {
        return texto == null || texto.isBlank() ? null : LocalTime.parse(texto.trim());
    }

    /** Si la acción falló muestra el motivo y deja todo como estaba; si salió bien lo cuenta y vuelve a leer. */
    private void resolver(String error, String exito) {
        if (error != null) {
            avisar(error, false);
            return;
        }
        recargar();
        cargarFicha();
        avisar(exito, true);
    }

    private void avisar(String texto, boolean bien) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                bien ? FacesMessage.SEVERITY_INFO : FacesMessage.SEVERITY_WARN, texto, null));
    }

    // ---------- indicadores ----------

    public EstadoInstalaciones getEstados() {
        return estados;
    }

    /** Nombres de las que están en mantenimiento (subtítulo del indicador). */
    public String getEnMantenimiento() {
        List<String> nombres = new ArrayList<>();
        for (Instalacion i : instalaciones) {
            if (i.getEstado() == Instalacion.Estado.DESHABILITADA_MANTENIMIENTO) {
                nombres.add(i.getNombre());
            }
        }
        return nombres.isEmpty() ? "Ninguna por ahora" : String.join(", ", nombres);
    }

    /** La instalación con más reservas de los últimos días; null si nadie reservó. */
    public RankingItem getMasSolicitada() {
        return ranking.isEmpty() || ranking.get(0).getCantidad() == 0 ? null : ranking.get(0);
    }

    /** Qué parte de todas las reservas de los últimos días es de la más solicitada (0 a 100). */
    public int getParticipacion() {
        long total = ranking.stream().mapToLong(RankingItem::getCantidad).sum();
        return total == 0 ? 0 : (int) Math.round(100.0 * ranking.get(0).getCantidad() / total);
    }

    public long getTotalReservas() {
        return ranking.stream().mapToLong(RankingItem::getCantidad).sum();
    }

    public int getDiasDemanda() {
        return InstalacionesServicio.DIAS_DEMANDA;
    }

    // ---------- listas y estado de la página ----------

    public List<EspacioPlano> getPlano() {
        return plano;
    }

    public List<PrecioFila> getPrecios() {
        return precios;
    }

    public List<RankingItem> getRanking() {
        return ranking;
    }

    /** Los títulos de las columnas de precios: un tipo de socio cada una. */
    public List<String> getTipos() {
        List<String> tipos = new ArrayList<>();
        for (CategoriaSocio c : categorias) {
            tipos.add(c.getNombreCategoria());
        }
        return tipos;
    }

    public FichaInstalacion getFicha() {
        return ficha;
    }

    public Integer getSeleccionada() {
        return seleccionada;
    }

    /** Los tipos de socio: un campo de precio cada uno. */
    public List<CategoriaSocio> getCategorias() {
        return categorias;
    }

    public Map<Integer, String> getPreciosNuevos() {
        return preciosNuevos;
    }

    public String getDesdePrecios() {
        return desdePrecios;
    }

    public void setDesdePrecios(String desdePrecios) {
        this.desdePrecios = desdePrecios;
    }

    public List<HistorialTarifa> getHistorialPrecios() {
        return historialPrecios;
    }

    public String getAbrir() {
        return abrir;
    }

    public Conflictos getConflictos() {
        return conflictos;
    }

    public String getModo() {
        return modo;
    }

    public boolean isPorOcupacion() {
        return "ocupacion".equals(modo);
    }

    /** Días de la semana para elegir en el formulario de franjas (1 = lunes ... 7 = domingo). */
    public List<SelectItem> getDias() {
        String[] nombres = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        List<SelectItem> dias = new ArrayList<>();
        for (int i = 0; i < nombres.length; i++) {
            dias.add(new SelectItem(String.valueOf(i + 1), nombres[i]));
        }
        return dias;
    }

    /** Hoy en formato ISO: el mínimo de los selectores de fecha. */
    public String getHoy() {
        return LocalDate.now().toString();
    }

    // ---------- campos de los formularios ----------

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(String capacidad) {
        this.capacidad = capacidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getHasta() {
        return hasta;
    }

    public void setHasta(String hasta) {
        this.hasta = hasta;
    }

    public boolean isCancelarReservas() {
        return cancelarReservas;
    }

    public void setCancelarReservas(boolean cancelarReservas) {
        this.cancelarReservas = cancelarReservas;
    }

    public String getMotivoBaja() {
        return motivoBaja;
    }

    public void setMotivoBaja(String motivoBaja) {
        this.motivoBaja = motivoBaja;
    }

    public boolean isCancelarReservasBaja() {
        return cancelarReservasBaja;
    }

    public void setCancelarReservasBaja(boolean cancelarReservasBaja) {
        this.cancelarReservasBaja = cancelarReservasBaja;
    }

    public List<String> getDiasElegidos() {
        return diasElegidos;
    }

    public void setDiasElegidos(List<String> diasElegidos) {
        this.diasElegidos = diasElegidos;
    }

    public String getAperturaSemanal() {
        return aperturaSemanal;
    }

    public void setAperturaSemanal(String aperturaSemanal) {
        this.aperturaSemanal = aperturaSemanal;
    }

    public String getCierreSemanal() {
        return cierreSemanal;
    }

    public void setCierreSemanal(String cierreSemanal) {
        this.cierreSemanal = cierreSemanal;
    }

    public String getFechaFranja() {
        return fechaFranja;
    }

    public void setFechaFranja(String fechaFranja) {
        this.fechaFranja = fechaFranja;
    }

    public String getAperturaFecha() {
        return aperturaFecha;
    }

    public void setAperturaFecha(String aperturaFecha) {
        this.aperturaFecha = aperturaFecha;
    }

    public String getCierreFecha() {
        return cierreFecha;
    }

    public void setCierreFecha(String cierreFecha) {
        this.cierreFecha = cierreFecha;
    }
}
