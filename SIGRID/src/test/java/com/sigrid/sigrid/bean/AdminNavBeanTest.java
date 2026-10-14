package com.sigrid.sigrid.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sigrid.sigrid.dto.GrupoMenu;
import com.sigrid.sigrid.dto.ItemMenu;
import com.sigrid.sigrid.servicio.DashboardServicio;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Menú lateral del administrador: los nombres de las secciones (CC-04) y a qué vista lleva cada una. */
@ExtendWith(MockitoExtension.class)
class AdminNavBeanTest {

    @Mock
    private DashboardServicio servicio;

    @InjectMocks
    private AdminNavBean menu;

    private List<ItemMenu> items() {
        List<ItemMenu> todos = new ArrayList<>();
        for (GrupoMenu g : menu.getGrupos()) {
            todos.addAll(g.getItems());
        }
        return todos;
    }

    @DisplayName("CP-41: el menú dice \"Grado de satisfacción\" y no \"Valoraciones\"")
    @Test
    void cp41_elMenuDiceGradoDeSatisfaccionYNoValoraciones() {
        List<String> textos = new ArrayList<>();
        for (ItemMenu i : items()) {
            textos.add(i.getTexto());
        }

        assertTrue(textos.contains("Grado de satisfacción"));
        assertFalse(textos.contains("Valoraciones"));
    }

    @Test
    void laSeccionRenombradaSigueEnLaMismaDireccion() {
        ItemMenu seccion = items().stream().filter(i -> i.getTexto().equals("Grado de satisfacción")).findFirst()
                .orElseThrow();

        assertEquals("/admin/valoraciones-dashboard-admin", seccion.getVista());
    }
}
