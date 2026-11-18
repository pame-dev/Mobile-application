package com.pame.karsy.feature.publish

import org.junit.Assert.assertEquals
import org.junit.Test

/** Color escrito en "Otros": sin espacios a los lados y con la primera letra en mayúscula. */
class ColorOtroTest {

    @Test
    fun limpiar_quitaEspaciosYCapitaliza() {
        assertEquals("Gris oxford", PublishViewModel.limpiarColorOtro("  gris oxford "))
    }

    @Test
    fun limpiar_conservaElRestoDelTexto() {
        assertEquals("Azul eléctrico", PublishViewModel.limpiarColorOtro("azul eléctrico"))
    }

    @Test
    fun limpiar_respetaMayusculaConAcento() {
        assertEquals("Ámbar", PublishViewModel.limpiarColorOtro("Ámbar"))
    }

    @Test
    fun limpiar_soloEspacios_quedaVacio() {
        assertEquals("", PublishViewModel.limpiarColorOtro("   "))
    }

    @Test
    fun limpiar_vacio_quedaVacio() {
        assertEquals("", PublishViewModel.limpiarColorOtro(""))
    }

    @Test
    fun limpiar_enieMinuscula_pasaAMayuscula() {
        assertEquals("Ñandú", PublishViewModel.limpiarColorOtro("ñandú"))
    }
}
