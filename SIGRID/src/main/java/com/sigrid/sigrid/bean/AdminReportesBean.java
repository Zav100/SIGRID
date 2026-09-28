package com.sigrid.sigrid.bean;

import com.sigrid.sigrid.dto.ReporteAlerta;
import com.sigrid.sigrid.dto.ReporteBarra;
import com.sigrid.sigrid.dto.ReporteCalor;
import com.sigrid.sigrid.dto.ReporteItem;
import com.sigrid.sigrid.dto.ReporteKpi;
import com.sigrid.sigrid.servicio.DashboardServicio;
import com.sigrid.sigrid.servicio.ReportesServicio;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Bean de admin/reportes-dashboard-admin.xhtml (solo lectura). Lee la base una vez al abrir la página; al cambiar el
 * período o el agrupado de los gráficos (AJAX) solo se vuelven a calcular los reportes en memoria. Las pestañas
 * (resumen, ingresos, ocupación y membresías) comparten el período. El botón "Exportar PDF" imprime la pestaña actual.
 */
@Named("adminReportesBean")
@ViewScoped
public class AdminReportesBean implements Serializable {

    private static final int MAXIMO_TURNOS_SIN_RESERVAS = 12;

    @Inject
    private ReportesServicio servicio;

    private LocalDate hoy;
    private ReportesServicio.Datos datos;
    private ReportesServicio.Rango rango;

    // lo que el administrador eligió
    private String pestana = "RESUMEN"; // RESUMEN, INGRESOS, OCUPACION o MEMBRESIAS
    private String periodo = "30D";     // 7D, 30D, MES, MES_ANT, 90D o 12M
    private String agrupado = "";       // "" (automático), DIA, SEMANA o MES

    // resumen
    private List<ReporteKpi> resumen;
    private List<ReporteAlerta> alertas;
    // ingresos
    private List<ReporteKpi> balance;
    private List<ReporteBarra> evolucion;
    private String maximoEvolucion;
    private List<ReporteItem> porInstalacion;
    private List<ReporteItem> porCategoria;
    private List<ReporteKpi> dinero;
    // ocupación
    private List<ReporteKpi> ocupacionKpis;
    private List<ReporteItem> ocupacionPorInstalacion;
    private ReporteCalor calor;
    private List<ReporteItem> picos;
    private List<ReporteItem> vacias;
    private List<ReporteItem> sinReservas;
    private List<ReporteItem> mantenimiento;
    // membresías
    private List<ReporteKpi> membresiasKpis;
    private List<ReporteItem> estados;
    private List<ReporteItem> vencimientos;
    private List<ReporteItem> membresiasPorCategoria;
    private List<ReporteItem> validacion;

    @PostConstruct
    public void init() {
        hoy = LocalDate.now();
        datos = servicio.cargar(hoy);
        recalcular();
    }

    /** Vuelve a calcular todos los reportes para el período y el agrupado elegidos. */
    public void recalcular() {
        rango = servicio.rango(periodo, hoy);
        resumen = servicio.resumen(datos, rango);
        alertas = servicio.alertas(datos, rango);

        balance = servicio.balance(datos, rango);
        evolucion = servicio.evolucion(datos, rango, agrupado);
        BigDecimal maximo = evolucion.stream().map(b -> b.getReservas().add(b.getMembresias()))
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        maximoEvolucion = DashboardServicio.monto(maximo);
        porInstalacion = servicio.ingresosPorInstalacion(datos, rango);
        porCategoria = servicio.ingresosPorCategoria(datos, rango);
        dinero = servicio.dineroEnJuego(datos, rango);

        ocupacionKpis = servicio.ocupacionKpis(datos, rango);
        ocupacionPorInstalacion = servicio.ocupacionPorInstalacion(datos, rango);
        calor = servicio.mapaDeCalor(datos, rango);
        picos = servicio.franjas(datos, rango, true);
        vacias = servicio.franjas(datos, rango, false);
        sinReservas = servicio.turnosSinReservas(datos, rango);
        mantenimiento = servicio.mantenimiento(datos, rango);

        membresiasKpis = servicio.membresiasKpis(datos, rango);
        estados = servicio.estadoSuscripciones(datos);
        vencimientos = servicio.vencimientosSemanales(datos);
        membresiasPorCategoria = servicio.membresiasPorCategoria(datos, rango);
        validacion = servicio.validacion(datos, rango);
    }

    public void elegir(String pestana) {
        this.pestana = pestana;
    }

    // ---------- lo que ve la página ----------

    public String getPestana() {
        return pestana;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo == null ? "30D" : periodo;
    }

    public String getAgrupado() {
        return agrupado;
    }

    public void setAgrupado(String agrupado) {
        this.agrupado = agrupado == null ? "" : agrupado;
    }

    public ReportesServicio.Rango getRango() {
        return rango;
    }

    public List<ReporteKpi> getResumen() {
        return resumen;
    }

    public List<ReporteAlerta> getAlertas() {
        return alertas;
    }

    public List<ReporteKpi> getBalance() {
        return balance;
    }

    public List<ReporteBarra> getEvolucion() {
        return evolucion;
    }

    public String getMaximoEvolucion() {
        return maximoEvolucion;
    }

    public List<ReporteItem> getPorInstalacion() {
        return porInstalacion;
    }

    public List<ReporteItem> getPorCategoria() {
        return porCategoria;
    }

    public List<ReporteKpi> getDinero() {
        return dinero;
    }

    public List<ReporteKpi> getOcupacionKpis() {
        return ocupacionKpis;
    }

    public List<ReporteItem> getOcupacionPorInstalacion() {
        return ocupacionPorInstalacion;
    }

    public ReporteCalor getCalor() {
        return calor;
    }

    public List<ReporteItem> getPicos() {
        return picos;
    }

    public List<ReporteItem> getVacias() {
        return vacias;
    }

    /** Los primeros turnos sin reservas; el total está en getTotalSinReservas. */
    public List<ReporteItem> getSinReservas() {
        return sinReservas.subList(0, Math.min(MAXIMO_TURNOS_SIN_RESERVAS, sinReservas.size()));
    }

    public int getTotalSinReservas() {
        return sinReservas.size();
    }

    public String getTextoSinReservas() {
        int total = sinReservas.size();
        return total == 0 ? "ninguno en el período"
                : total > MAXIMO_TURNOS_SIN_RESERVAS ? "primeros " + MAXIMO_TURNOS_SIN_RESERVAS + " de " + total
                : total + (total == 1 ? " turno" : " turnos");
    }

    public List<ReporteItem> getMantenimiento() {
        return mantenimiento;
    }

    public List<ReporteKpi> getMembresiasKpis() {
        return membresiasKpis;
    }

    public List<ReporteItem> getEstados() {
        return estados;
    }

    public List<ReporteItem> getVencimientos() {
        return vencimientos;
    }

    public List<ReporteItem> getMembresiasPorCategoria() {
        return membresiasPorCategoria;
    }

    public List<ReporteItem> getValidacion() {
        return validacion;
    }
}
