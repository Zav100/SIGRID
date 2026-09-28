package com.sigrid.sigrid.seguridad;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;

/**
 * Todo lo que llega por formulario se lee como UTF-8. Sin esto el contenedor asume ISO-8859-1 y un comentario con
 * "ñ" o tildes se guarda como "Ã±" (motivos de cancelación, comentarios de valoración, búsquedas con tildes).
 */
@WebFilter("/*")
public class CodificacionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }
        chain.doFilter(request, response);
    }
}
