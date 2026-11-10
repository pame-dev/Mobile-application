package com.pame.karsy.core.push

import com.pame.karsy.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotificationTextsTest {
    @Test
    fun publicacionPorRevisar_usaSuTexto() {
        assertEquals(R.string.notif_listing_to_review, notificationTextRes("publicacion_por_revisar"))
    }

    @Test
    fun edicionPorRevisar_usaSuTexto() {
        assertEquals(R.string.notif_edit_to_review, notificationTextRes("edicion_por_revisar"))
    }

    @Test
    fun reporteNuevo_usaSuTexto() {
        assertEquals(R.string.notif_new_report, notificationTextRes("reporte_nuevo"))
    }

    @Test
    fun tiposNuevos_noCaenEnGenerico() {
        listOf("publicacion_por_revisar", "edicion_por_revisar", "reporte_nuevo").forEach {
            assertNotEquals(R.string.notif_generic, notificationTextRes(it))
        }
    }

    @Test
    fun tipoDesconocido_caeEnGenerico() {
        assertEquals(R.string.notif_generic, notificationTextRes("tipo_inexistente"))
    }
}
