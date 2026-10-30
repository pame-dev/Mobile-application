package com.pame.karsy.core.session

import com.pame.karsy.feature.admin.AdminSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RolPanelTest {
    private fun cuenta(rol: String?) = SessionAccount("1", "n", "particular", rol, "a@b.c")

    @Test fun admin_ve_todas_las_secciones() {
        assertEquals(AdminSection.entries.toList(), AdminSection.delRol("administrador"))
    }

    @Test fun moderador_publicaciones() {
        assertEquals(
            setOf(AdminSection.Usuarios, AdminSection.Vehiculos, AdminSection.Reportes),
            AdminSection.delRol("moderador_publicaciones").toSet(),
        )
    }

    @Test fun moderador_verificaciones() {
        assertEquals(
            setOf(AdminSection.Destacados, AdminSection.Verificaciones),
            AdminSection.delRol("moderador_verificaciones").toSet(),
        )
    }

    @Test fun sin_rol_no_expone_nada_extra_a_un_moderador() {
        // null cae en entries (el panel se protege con tienePanel); se fija el comportamiento actual.
        assertEquals(AdminSection.entries.toList(), AdminSection.delRol(null))
    }

    @Test fun esAdmin_y_tienePanel() {
        assertTrue(cuenta("administrador").esAdmin)
        assertTrue(cuenta("administrador").tienePanel)
        assertFalse(cuenta("moderador_publicaciones").esAdmin)
        assertTrue(cuenta("moderador_publicaciones").tienePanel)
        assertFalse(cuenta("moderador_verificaciones").esAdmin)
        assertTrue(cuenta("moderador_verificaciones").tienePanel)
        assertFalse(cuenta(null).esAdmin)
        assertFalse(cuenta(null).tienePanel)
    }
}
