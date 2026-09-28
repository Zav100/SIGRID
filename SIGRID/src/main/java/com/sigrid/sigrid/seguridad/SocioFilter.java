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
 * Protege todo lo que cuelga de /socio/* (y la plantilla del panel): solo entra un usuario logueado con
 * rol SOCIO (mismo criterio que AdminFilter, con el rol opuesto). Cualquier
 * otro caso (sin sesión, o administrador) se redirige al login.
 */
@WebFilter(urlPatterns = {"/socio/*", "/templates/socio.xhtml"})
public class SocioFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        LoginBean loginBean = CDI.current().select(LoginBean.class).get();

        if (loginBean.isSocio()) {
            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).sendRedirect(
                    ((HttpServletRequest) request).getContextPath() + "/login.xhtml");
        }
    }
}
