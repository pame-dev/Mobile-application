package com.pame.karsy.feature.admin

import com.pame.karsy.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPorRevisarTest {
    @Test fun porRevisarTieneEtiqueta() {
        assertEquals(R.string.admin2_value_to_review, adminValueLabelRes(POR_REVISAR))
    }

    @Test fun todosSigueTeniendoEtiqueta() {
        assertNotNull(adminValueLabelRes("Todos"))
    }
}

class AdminFiltrosVehiculoTest {
    @Test fun porRevisarIncluyePendienteYCambios() {
        assertTrue(enFiltroVehiculo(POR_REVISAR, "Pendiente"))
        assertTrue(enFiltroVehiculo(POR_REVISAR, "Cambios pendientes"))
        assertFalse(enFiltroVehiculo(POR_REVISAR, "Activo"))
        assertFalse(enFiltroVehiculo(POR_REVISAR, "Rechazado"))
    }

    @Test fun publicadasSoloActivo() {
        assertTrue(enFiltroVehiculo(PUBLICADAS, "Activo"))
        for (e in listOf("Pendiente", "Cambios pendientes", "Rechazado", "Deshabilitado", "Pausado", "Vendido"))
            assertFalse(e, enFiltroVehiculo(PUBLICADAS, e))
    }

    @Test fun bloqueadasIncluyeRechazadoYDeshabilitado() {
        assertTrue(enFiltroVehiculo(BLOQUEADAS, "Rechazado"))
        assertTrue(enFiltroVehiculo(BLOQUEADAS, "Deshabilitado"))
        assertFalse(enFiltroVehiculo(BLOQUEADAS, "Pausado"))
        assertFalse(enFiltroVehiculo(BLOQUEADAS, "Activo"))
    }

    @Test fun inactivasIncluyePausadoYVendido() {
        assertTrue(enFiltroVehiculo(INACTIVAS, "Pausado"))
        assertTrue(enFiltroVehiculo(INACTIVAS, "Vendido"))
        assertFalse(enFiltroVehiculo(INACTIVAS, "Deshabilitado"))
        assertFalse(enFiltroVehiculo(INACTIVAS, "Activo"))
    }

    @Test fun todasIncluyeTodo() {
        for (e in listOf("Pendiente", "Cambios pendientes", "Activo", "Rechazado", "Deshabilitado", "Pausado", "Vendido"))
            assertTrue(e, enFiltroVehiculo(TODAS, e))
    }

    @Test fun filtroDesconocidoIncluyeTodo() {
        assertTrue(enFiltroVehiculo("Inventado", "Activo"))
        assertTrue(enFiltroVehiculo("Deshabilitado", "Pendiente"))
    }

    @Test fun cadaEstadoCaeEnExactamenteUnGrupo() {
        val grupos = listOf(POR_REVISAR, PUBLICADAS, BLOQUEADAS, INACTIVAS)
        for (e in listOf("Pendiente", "Cambios pendientes", "Activo", "Rechazado", "Deshabilitado", "Pausado", "Vendido"))
            assertEquals(e, 1, grupos.count { enFiltroVehiculo(it, e) })
    }

    @Test fun etiquetasNuevas() {
        assertEquals(R.string.admin2_value_published, adminValueLabelRes(PUBLICADAS))
        assertEquals(R.string.admin2_value_blocked, adminValueLabelRes(BLOQUEADAS))
        assertEquals(R.string.admin2_value_inactive, adminValueLabelRes(INACTIVAS))
        assertEquals(R.string.admin2_value_all_f, adminValueLabelRes(TODAS))
    }
}
