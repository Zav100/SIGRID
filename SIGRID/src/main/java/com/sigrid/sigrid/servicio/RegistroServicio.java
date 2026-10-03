package com.sigrid.sigrid.servicio;

import com.sigrid.sigrid.dao.CarnetDigitalDAO;
import com.sigrid.sigrid.dao.CategoriaSocioDAO;
import com.sigrid.sigrid.dao.RolDAO;
import com.sigrid.sigrid.dao.SocioDAO;
import com.sigrid.sigrid.dao.UsuarioDAO;
import com.sigrid.sigrid.dto.RegistroSocio;
import com.sigrid.sigrid.repositorio.CarnetDigital;
import com.sigrid.sigrid.repositorio.CategoriaSocio;
import com.sigrid.sigrid.repositorio.Rol;
import com.sigrid.sigrid.repositorio.Socio;
import com.sigrid.sigrid.repositorio.Usuario;
import com.sigrid.sigrid.util.PasswordUtil;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Alta pública de socios (register.xhtml, CU-01): paso 1 crea la cuenta (email + contraseña),
 * paso 2 completa los datos personales. Todo se persiste junto recién al finalizar (usuario +
 * socio + carnet digital), para no dejar cuentas a medio crear si alguien abandona el formulario.
 */
@Dependent
public class RegistroServicio implements Serializable {

    private static final String ROL_SOCIO = "SOCIO";

    @Inject
    private UsuarioDAO usuarioDAO;
    @Inject
    private SocioDAO socioDAO;
    @Inject
    private RolDAO rolDAO;
    @Inject
    private CategoriaSocioDAO categoriaSocioDAO;
    @Inject
    private CarnetDigitalDAO carnetDigitalDAO;

    /** Paso 1: correo y contraseña. @return el motivo si no es válido; null si está todo bien. */
    public String validarPasoUno(String email, String password, String confirmarPassword) {
        String correo = email == null ? "" : email.trim();
        if (!correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            return "El correo no es válido.";
        }
        if (password == null || password.length() < 6) {
            return "La contraseña tiene que tener al menos 6 caracteres.";
        }
        if (!password.equals(confirmarPassword)) {
            return "Las contraseñas no coinciden.";
        }
        Usuario existente = usuarioDAO.buscarPorEmail(correo);
        if (existente != null && bajaDe(existente) == null) {
            return "Ese correo ya tiene una cuenta.";
        }
        return null;
    }

    /** El socio dado de baja de esa cuenta; null si la cuenta no es de un socio o no está dada de baja. */
    private Socio bajaDe(Usuario u) {
        Socio s = u == null ? null : socioDAO.buscarPorIdUsuario(u.getIdUsuario());
        return s != null && s.isBajaLogica() ? s : null;
    }

    /**
     * Un socio dado de baja no retiene su correo ni su DNI (la base los pide únicos): se archivan en su ficha vieja,
     * que queda con su historial, para que la persona pueda volver a registrarse con los mismos datos.
     */
    private void liberar(Socio baja) {
        if (baja == null) {
            return;
        }
        Usuario u = baja.getIdUsuario();
        u.setEmail(archivado(u.getEmail(), baja.getIdSocio(), 255));
        // ponytail: el DNI archivado se corta a 15 caracteres (columna dni); el prefijo con el id mantiene la unicidad
        baja.setDni(archivado(baja.getDni(), baja.getIdSocio(), 15));
        socioDAO.flush(); // el UPDATE sale antes del INSERT de la cuenta nueva, o chocaría con el UNIQUE
    }

    private static String archivado(String valor, Integer idSocio, int maximo) {
        String texto = "b" + idSocio + "-" + valor;
        return texto.length() > maximo ? texto.substring(0, maximo) : texto;
    }

    /**
     * Alta completa: vuelve a validar todo (paso 1 incluido, por si el cliente lo saltó) y crea
     * usuario + socio + carnet digital en una sola transacción.
     * @return el motivo si no se pudo crear la cuenta; null si quedó creada.
     */
    @Transactional
    public String registrar(RegistroSocio r) {
        String errorPasoUno = validarPasoUno(r.getEmail(), r.getPassword(), r.getConfirmarPassword());
        if (errorPasoUno != null) {
            return errorPasoUno;
        }

        String nombre = r.getNombre() == null ? "" : r.getNombre().trim();
        String apellido = r.getApellido() == null ? "" : r.getApellido().trim();
        String dni = r.getDni() == null ? "" : r.getDni().trim();
        String telefono = r.getTelefono() == null ? "" : r.getTelefono().trim();
        String legajo = r.getLegajo() == null ? "" : r.getLegajo().trim();

        if (nombre.isEmpty() || apellido.isEmpty()) {
            return "Completá nombre y apellido.";
        }
        if (!dni.matches("\\d{6,15}")) {
            return "El DNI debe tener solo dígitos, sin puntos.";
        }
        if (!telefono.matches("\\d{6,30}")) {
            return "Ingresá un teléfono válido, solo dígitos.";
        }
        LocalDate nacimiento;
        try {
            nacimiento = LocalDate.parse(r.getFechaNacimiento());
        } catch (DateTimeParseException | NullPointerException ex) {
            return "Ingresá la fecha de nacimiento.";
        }
        if (!nacimiento.isBefore(LocalDate.now())) {
            return "La fecha de nacimiento tiene que ser anterior a hoy.";
        }

        CategoriaSocio categoria = resolverCategoria(r.getTipoSocio(), r.getCategoriaInterna());
        if (categoria == null) {
            return "Elegí el tipo de socio.";
        }
        if (categoria.isRequiereLegajo() && legajo.isEmpty()) {
            return "Un " + categoria.getNombreCategoria() + " necesita legajo.";
        }
        Socio conEseDni = socioDAO.buscarPorDni(dni);
        if (conEseDni != null && !conEseDni.isBajaLogica()) {
            return "Ese DNI ya pertenece a otro socio.";
        }
        liberar(bajaDe(usuarioDAO.buscarPorEmail(r.getEmail().trim())));
        liberar(socioDAO.buscarPorDni(dni)); // se vuelve a buscar: si era la misma ficha, ya quedó archivada

        Rol rol = rolDAO.buscarPorNombre(ROL_SOCIO);

        Usuario usuario = new Usuario();
        usuario.setIdRol(rol);
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(r.getEmail().trim());
        usuario.setPasswordHash(PasswordUtil.generarHash(r.getPassword()));
        usuarioDAO.crear(usuario);

        Socio socio = new Socio();
        socio.setIdUsuario(usuario);
        socio.setIdCategoriaSocio(categoria);
        socio.setDni(dni);
        socio.setFechaNacimiento(nacimiento);
        socio.setTelefono(telefono);
        socio.setLegajo(categoria.isRequiereLegajo() ? legajo : null);
        socioDAO.crear(socio);

        CarnetDigital carnet = new CarnetDigital();
        carnet.setIdSocio(socio);
        carnet.setTipoCarnet("Alumno UNSE".equals(categoria.getNombreCategoria())
                ? CarnetDigital.TipoCarnet.ESTUDIANTIL : CarnetDigital.TipoCarnet.GENERAL);
        carnetDigitalDAO.crear(carnet);

        return null;
    }

    private CategoriaSocio resolverCategoria(String tipoSocio, String categoriaInterna) {
        if ("EXTERNO".equals(tipoSocio)) {
            return categoriaSocioDAO.buscarPorNombre("Externo");
        }
        if ("INTERNO".equals(tipoSocio) && categoriaInterna != null) {
            switch (categoriaInterna) {
                case "ALUMNO":
                    return categoriaSocioDAO.buscarPorNombre("Alumno UNSE");
                case "DOCENTE":
                    return categoriaSocioDAO.buscarPorNombre("Docente UNSE");
                case "NO_DOCENTE":
                    return categoriaSocioDAO.buscarPorNombre("No Docente UNSE");
                default:
                    return null;
            }
        }
        return null;
    }
}
