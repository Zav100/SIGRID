package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.CarnetDigitalDAO;
import com.sigrid.sigrid.dao.CategoriaSocioDAO;
import com.sigrid.sigrid.dao.ReservaDAO;
import com.sigrid.sigrid.dao.SocioDAO;
import com.sigrid.sigrid.dao.SuscripcionSocioDAO;
import com.sigrid.sigrid.dao.TarifaMembresiaDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dto.Dato;
import com.sigrid.sigrid.dto.SocioDetalle;
import com.sigrid.sigrid.dto.SocioEdicion;
import com.sigrid.sigrid.dto.SocioFila;
import com.sigrid.sigrid.repositorio.CarnetDigital;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Pago;
import com.sigrid.sigrid.repositorio.Reserva;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.SuscripcionSocio;
import com.sigrid.sigrid.repositorio.TarifaMembresia;
import com.sigrid.sigrid.repositorio.Usuario;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.format.DateTimeParseException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Datos y acciones del panel de socios del administrador. La membresía se muestra tal como la deja
 * Socio.estado (ACTIVO = vigente, NO_ACTIVO = expirada); el vencimiento es el de la última suscripción.
 *
 * CDI simple (no EJB) y Serializable, igual que DashboardServicio. Las transacciones las abre este servicio.
 */
@Dependent
public class SociosServicio implements Serializable {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES_ANIO = DateTimeFormatter.ofPattern("MM/yyyy");

    @Inject
    private SocioDAO socioDAO;
    @Inject
    private SuscripcionSocioDAO suscripcionSocioDAO;
    @Inject
    private CategoriaSocioDAO categoriaSocioDAO;
    @Inject
    private CarnetDigitalDAO carnetDAO;
    @Inject
    private ReservaDAO reservaDAO;
    @Inject
    private TarifaMembresiaDAO tarifaMembresiaDAO;
    @Inject
    private UsuarioDAO usuarioDAO;

    /** Los socios que no están dados de baja; "porVencer" = vigentes que vencen dentro de los próximos días. */
    public List<SocioFila> listar(LocalDate hoy, int diasPorVencer) {
        Map<Integer, LocalDate> vencimientos = new HashMap<>();
        for (Object[] fila : suscripcionSocioDAO.ultimosVencimientos()) {
            vencimientos.put((Integer) fila[0], (LocalDate) fila[1]);
        }

        List<SocioFila> filas = new ArrayList<>();
        for (Socio s : socioDAO.listarSinBaja()) {
            Usuario u = s.getIdUsuario();
            LocalDate vence = vencimientos.get(s.getIdSocio());
            boolean vigente = s.getEstado() == Socio.Estado.ACTIVO;
            filas.add(new SocioFila(s.getIdSocio(), DashboardServicio.nombreSocio(s), iniciales(u), u.getEmail(),
                    s.getDni(), s.getIdCategoriaSocio().getNombreCategoria(), s.getLegajo(), vigente,
                    vigente && vence != null && !vence.isAfter(hoy.plusDays(diasPorVencer)),
                    vence == null ? null : vence.format(FECHA), u.getFechaAlta().format(FECHA)));
        }
        return filas;
    }

    /** La ficha completa del socio (solo lectura); null si no existe o está dado de baja. */
    public SocioDetalle detalle(Integer idSocio, LocalDate hoy) {
        Socio socio = socioDAO.buscarPorId(idSocio);
        if (socio == null || socio.isBajaLogica()) {
            return null;
        }
        Usuario u = socio.getIdUsuario();
        String categoria = socio.getIdCategoriaSocio().getNombreCategoria();
        boolean vigente = socio.getEstado() == Socio.Estado.ACTIVO;
        List<SuscripcionSocio> suscripciones = suscripcionSocioDAO.listarPorSocio(idSocio); // la más reciente primero

        // "desde cuándo" y "vence" salen de las membresías que llegaron a estar vigentes
        LocalDate primera = null;
        LocalDate vence = null;
        int pagadas = 0;
        Pago ultimoPago = null;
        for (SuscripcionSocio s : suscripciones) {
            boolean llegoAVigente = s.getEstado() == SuscripcionSocio.Estado.VIGENTE
                    || s.getEstado() == SuscripcionSocio.Estado.VENCIDA;
            if (llegoAVigente && s.getFechaInicio() != null && (primera == null || s.getFechaInicio().isBefore(primera))) {
                primera = s.getFechaInicio();
            }
            if (llegoAVigente && s.getFechaVencimiento() != null && (vence == null || s.getFechaVencimiento().isAfter(vence))) {
                vence = s.getFechaVencimiento();
            }
            Pago p = s.getIdPago();
            if (p != null && p.getEstado() == Pago.Estado.CONFIRMADO && p.getFechaValidacion() != null) {
                pagadas++;
                if (ultimoPago == null || p.getFechaValidacion().isAfter(ultimoPago.getFechaValidacion())) {
                    ultimoPago = p;
                }
            }
        }
        TarifaMembresia tarifa = tarifaMembresiaDAO.buscarVigente(socio.getIdCategoriaSocio().getIdCategoriaSocio(), hoy);
        CarnetDigital carnet = carnetDAO.buscarPorSocio(idSocio);

        List<Dato> personales = new ArrayList<>();
        personales.add(new Dato("Nombre", vacio(u.getNombre())));
        personales.add(new Dato("Apellido", vacio(u.getApellido())));
        personales.add(new Dato("Correo", u.getEmail()));
        personales.add(new Dato("Teléfono", vacio(socio.getTelefono())));
        personales.add(new Dato("DNI", socio.getDni()));
        personales.add(new Dato("Nacimiento", socio.getFechaNacimiento() == null ? "—"
                : socio.getFechaNacimiento().format(FECHA) + " (" + Period.between(socio.getFechaNacimiento(), hoy).getYears() + " años)"));
        personales.add(new Dato("Legajo", socio.getLegajo() == null ? "No corresponde" : socio.getLegajo()));
        personales.add(new Dato("N.° de socio", numero(idSocio)));
        personales.add(new Dato("Alta en el sistema", u.getFechaAlta().format(FECHA)));
        personales.add(new Dato("Cuenta", u.isCuentaHabilitada() ? "Habilitada" : "Deshabilitada",
                u.isCuentaHabilitada() ? "ok" : "off"));

        List<Dato> membresia = new ArrayList<>();
        membresia.add(new Dato("Estado", vigente ? "Vigente" : vence == null ? "Sin membresía" : "Expirada",
                vigente ? "ok" : "off"));
        membresia.add(new Dato("Tipo de socio", categoria));
        membresia.add(new Dato("Socio desde", primera == null ? "Todavía no tuvo una membresía" : primera.format(FECHA)));
        membresia.add(new Dato("Vence", vence == null ? "—"
                : vence.format(FECHA) + " · " + textoDias(ChronoUnit.DAYS.between(hoy, vence))));
        membresia.add(new Dato("Cuota mensual", tarifa == null ? "—" : DashboardServicio.monto(tarifa.getPrecio())));
        membresia.add(new Dato("Cuotas pagadas", String.valueOf(pagadas)));
        membresia.add(new Dato("Último pago", ultimoPago == null ? "—"
                : DashboardServicio.monto(ultimoPago.getMonto()) + " · " + ultimoPago.getFechaValidacion().format(FECHA)));
        membresia.add(carnet == null ? new Dato("Carnet digital", "Sin carnet", "muted")
                : new Dato("Carnet digital", (carnet.getTipoCarnet() == CarnetDigital.TipoCarnet.ESTUDIANTIL ? "Estudiantil" : "General")
                + (carnet.getEstado() == CarnetDigital.Estado.ACTIVO ? " · Activo" : " · Congelado"),
                carnet.getEstado() == CarnetDigital.Estado.ACTIVO ? "ok" : "off"));

        List<Dato> historial = new ArrayList<>();
        for (SuscripcionSocio s : suscripciones.subList(0, Math.min(6, suscripciones.size()))) {
            String periodo = s.getFechaInicio() == null ? "Solicitada el " + s.getFechaSolicitud().format(FECHA)
                    : s.getFechaInicio().format(FECHA) + " al " + s.getFechaVencimiento().format(FECHA);
            String estado;
            String clase;
            switch (s.getEstado()) {
                case VIGENTE:
                    estado = "Vigente";
                    clase = "ok";
                    break;
                case VENCIDA:
                    estado = "Vencida";
                    clase = "muted";
                    break;
                case CANCELADA:
                    estado = "Cancelada";
                    clase = "off";
                    break;
                default:
                    estado = "Pendiente de pago";
                    clase = "warn";
            }
            historial.add(new Dato(periodo, estado + (s.getIdPago() == null ? ""
                    : " · " + DashboardServicio.monto(s.getIdPago().getMonto())), clase));
        }

        long confirmadas = 0;
        long canceladas = 0;
        Reserva ultima = null;
        for (Reserva r : reservaDAO.listarPorSocio(idSocio)) { // la más próxima primero
            if (r.getEstado() == Reserva.Estado.CONFIRMADA) {
                confirmadas++;
                if (ultima == null && !r.getFechaTurno().isAfter(hoy)) {
                    ultima = r;
                }
            } else if (r.getEstado() == Reserva.Estado.CANCELADA) {
                canceladas++;
            }
        }
        List<Dato> actividad = new ArrayList<>();
        actividad.add(new Dato("Reservas confirmadas", String.valueOf(confirmadas)));
        actividad.add(new Dato("Reservas canceladas", String.valueOf(canceladas)));
        actividad.add(new Dato("Última reserva", ultima == null ? "—" : ultima.getFechaTurno().format(FECHA) + " · "
                + ultima.getIdTurno().getIdInstalacion().getNombre()));

        return new SocioDetalle(iniciales(u), DashboardServicio.nombreSocio(socio), categoria, numero(idSocio),
                u.getFechaAlta().format(MES_ANIO), vigente, personales, membresia, historial, actividad);
    }

    static String numero(Integer idSocio) {
        return String.format("#%05d", idSocio);
    }

    private static String vacio(String texto) {
        return texto == null || texto.isBlank() ? "—" : texto;
    }

    private static String textoDias(long dias) {
        return dias > 1 ? "faltan " + dias + " días" : dias == 1 ? "vence mañana" : dias == 0 ? "vence hoy"
                : dias == -1 ? "venció ayer" : "venció hace " + -dias + " días";
    }

    /** Los datos editables del socio; null si no existe o está dado de baja. */
    public SocioEdicion paraEditar(Integer idSocio) {
        Socio s = socioDAO.buscarPorId(idSocio);
        if (s == null || s.isBajaLogica()) {
            return null;
        }
        Usuario u = s.getIdUsuario();
        SocioEdicion e = new SocioEdicion();
        e.setIdSocio(idSocio);
        e.setNombre(u.getNombre());
        e.setApellido(u.getApellido());
        e.setEmail(u.getEmail());
        e.setTelefono(s.getTelefono());
        e.setDni(s.getDni());
        e.setNacimiento(s.getFechaNacimiento() == null ? "" : s.getFechaNacimiento().toString());
        e.setCategoria(s.getIdCategoriaSocio().getNombreCategoria());
        e.setLegajo(s.getLegajo());
        return e;
    }

    /** Guarda los cambios del socio. @return el motivo si no se pudo guardar; null si quedó guardado. */
    @Transactional
    public String guardar(SocioEdicion e, LocalDate hoy) {
        Socio socio = socioDAO.buscarPorId(e.getIdSocio());
        if (socio == null || socio.isBajaLogica()) {
            return "Ese socio ya no estaba en el listado.";
        }
        String nombre = e.getNombre() == null ? "" : e.getNombre().trim();
        String apellido = e.getApellido() == null ? "" : e.getApellido().trim();
        String email = e.getEmail() == null ? "" : e.getEmail().trim();
        String telefono = e.getTelefono() == null ? "" : e.getTelefono().trim();
        String dni = e.getDni() == null ? "" : e.getDni().trim();
        String legajo = e.getLegajo() == null ? "" : e.getLegajo().trim();
        if (nombre.isEmpty() || apellido.isEmpty() || telefono.isEmpty()) {
            return "Completá nombre, apellido y teléfono.";
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            return "El correo no es válido.";
        }
        if (!dni.matches("\\d{6,15}")) {
            return "El DNI debe tener solo dígitos, sin puntos.";
        }
        LocalDate nacimiento;
        try {
            nacimiento = LocalDate.parse(e.getNacimiento());
        } catch (DateTimeParseException | NullPointerException ex) {
            return "Ingresá la fecha de nacimiento.";
        }
        if (!nacimiento.isBefore(hoy)) {
            return "La fecha de nacimiento tiene que ser anterior a hoy.";
        }
        CategoriaSocio categoria = categoriaSocioDAO.buscarPorNombre(e.getCategoria());
        if (categoria == null) {
            return "Elegí el tipo de socio.";
        }
        if (categoria.isRequiereLegajo() && legajo.isEmpty()) {
            return "Un " + categoria.getNombreCategoria() + " necesita legajo.";
        }
        Usuario otroCorreo = usuarioDAO.buscarPorEmail(email);
        if (otroCorreo != null && !otroCorreo.getIdUsuario().equals(socio.getIdUsuario().getIdUsuario())) {
            return "Ese correo ya lo usa otra cuenta.";
        }
        Socio otroDni = socioDAO.buscarPorDni(dni);
        if (otroDni != null && !otroDni.getIdSocio().equals(socio.getIdSocio())) {
            return "Ese DNI ya pertenece a otro socio.";
        }
        Usuario u = socio.getIdUsuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setEmail(email);
        socio.setTelefono(telefono);
        socio.setDni(dni);
        socio.setFechaNacimiento(nacimiento);
        socio.setIdCategoriaSocio(categoria);
        socio.setLegajo(categoria.isRequiereLegajo() ? legajo : null);
        return null;
    }

    /** Nombres de las categorías (tipos de socio) para el filtro. */
    public List<String> tiposDeSocio() {
        List<String> tipos = new ArrayList<>();
        for (CategoriaSocio c : categoriaSocioDAO.listarTodos()) {
            tipos.add(c.getNombreCategoria());
        }
        return tipos;
    }

    /**
     * Baja lógica del socio (RF-01.10): deja de figurar en el listado y su cuenta ya no puede iniciar sesión.
     * @return false si el socio no existe o ya estaba dado de baja.
     */
    @Transactional
    public boolean darDeBaja(Integer idSocio) {
        Socio socio = socioDAO.buscarPorId(idSocio);
        if (socio == null || socio.isBajaLogica()) {
            return false;
        }
        socio.setBajaLogica(true);
        socio.getIdUsuario().setCuentaHabilitada(false);
        return true;
    }

    static String iniciales(Usuario u) {
        String nombre = u.getNombre();
        String apellido = u.getApellido();
        String iniciales = (nombre == null || nombre.isBlank() ? "" : nombre.substring(0, 1))
                + (apellido == null || apellido.isBlank() ? "" : apellido.substring(0, 1));
        return (iniciales.isEmpty() ? u.getEmail().substring(0, 1) : iniciales).toUpperCase();
    }
}
