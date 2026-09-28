package com.sigrid.sigrid.servicio;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tarea programada: al arrancar la aplicación y luego cada hora, pasa a VENCIDA las membresías que llegaron a su
 * fecha y congela el carnet de los socios que se quedaron sin membresía vigente (RF-01.4.3). Usa el ejecutor
 * programado de la plataforma (no el servicio de timers de EJB, que pide su propia base de datos). El panel de
 * membresías hace lo mismo al abrirse, así que lo que ve el administrador siempre está al día.
 * Cada 5 minutos, además, cancela las reservas que pasaron el plazo para subir el comprobante y libera sus turnos.
 */
@ApplicationScoped
public class MembresiaJob {

    private static final Logger LOG = Logger.getLogger(MembresiaJob.class.getName());

    @Resource
    private ManagedScheduledExecutorService ejecutor;
    @Inject
    private MembresiasServicio servicio;
    @Inject
    private PanelSocioServicio reservas;

    private ScheduledFuture<?> tarea;
    private ScheduledFuture<?> tareaReservas;

    void iniciar(@Observes @Initialized(ApplicationScoped.class) Object arranque) {
        tarea = ejecutor.scheduleAtFixedRate(this::vencerMembresias, 1, 60, TimeUnit.MINUTES);
        tareaReservas = ejecutor.scheduleAtFixedRate(this::liberarReservas, 1, 5, TimeUnit.MINUTES);
    }

    @PreDestroy
    void detener() {
        if (tarea != null) {
            tarea.cancel(false);
        }
        if (tareaReservas != null) {
            tareaReservas.cancel(false);
        }
    }

    private void liberarReservas() {
        try {
            int liberadas = reservas.liberarVencidas(LocalDateTime.now());
            if (liberadas > 0) {
                LOG.log(Level.INFO, "Reservas liberadas por falta de comprobante: {0}", liberadas);
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudieron liberar las reservas sin comprobante", e); // se reintenta a los 5 minutos
        }
    }

    private void vencerMembresias() {
        try {
            int vencidas = servicio.vencerVencidas(LocalDate.now());
            if (vencidas > 0) {
                LOG.log(Level.INFO, "Membresías vencidas: {0}", vencidas);
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudieron vencer las membresías", e); // se reintenta a la hora siguiente
        }
    }
}
