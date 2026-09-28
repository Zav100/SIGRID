package com.sigrid.sigrid.seguridad;

import com.sigrid.sigrid.bean.LoginBean;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Protege todo lo que cuelga de /admin/* (y la template del panel, que mostraría el menú
 * con el contador de solicitudes si se la pidiera directo): solo entra un usuario logueado
 * con rol ADMINISTRADOR (RNF-06, control de acceso basado en roles). Cualquier otro caso
 * (sin sesión, o socio) se redirige al login.
 *
 * Busca el bean vía CDI.current() en vez de @Inject de campo: un Filter
 * registrado por @WebFilter lo instancia el contenedor de servlets y esa
 * inyección no queda resuelta de forma confiable (mismo criterio que en REPS).
 */
@WebFilter(urlPatterns = {"/admin/*", "/templates/admin-dashboard.xhtml"})
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        LoginBean loginBean = CDI.current().select(LoginBean.class).get();

        if (loginBean.isAdministrador()) {
            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).sendRedirect(
                    ((HttpServletRequest) request).getContextPath() + "/login.xhtml");
        }
    }
}
