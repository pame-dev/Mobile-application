package com.pame.karsy.feature.publish

import org.junit.Assert.assertEquals
import org.junit.Test

/** Color escrito en "Otros": sin espacios a los lados y con la primera letra en mayúscula. */
class ColorOtroTest {

    @Test
    fun limpiar_quitaEspaciosYCapitaliza() {
        assertEquals("Gris oxford", PublishViewModel.limpiarOtro("  gris oxford "))
    }

    @Test
    fun limpiar_conservaElRestoDelTexto() {
        assertEquals("Azul eléctrico", PublishViewModel.limpiarOtro("azul eléctrico"))
    }

    @Test
    fun limpiar_respetaMayusculaConAcento() {
        assertEquals("Ámbar", PublishViewModel.limpiarOtro("Ámbar"))
    }

    @Test
    fun limpiar_soloEspacios_quedaVacio() {
        assertEquals("", PublishViewModel.limpiarOtro("   "))
    }

    @Test
    fun limpiar_vacio_quedaVacio() {
        assertEquals("", PublishViewModel.limpiarOtro(""))
    }

    @Test
    fun limpiar_transmision_semiautomatica() {
        assertEquals("Semiautomática", PublishViewModel.limpiarOtro("  semiautomática "))
    }

    @Test
    fun limpiar_tipoDeCarro_crossover() {
        assertEquals("Crossover", PublishViewModel.limpiarOtro("crossover"))
    }

    @Test
    fun limpiar_conservaMayusculasInternas() {
        assertEquals("Cvt doble", PublishViewModel.limpiarOtro(" cvt doble"))
        assertEquals("Mini van", PublishViewModel.limpiarOtro("mini van  "))
    }

    @Test
    fun limpiar_esIdempotente() {
        val una = PublishViewModel.limpiarOtro("  crossover ")
        assertEquals(una, PublishViewModel.limpiarOtro(una))
    }

    @Test
    fun limpiar_enieMinuscula_pasaAMayuscula() {
        assertEquals("Ñandú", PublishViewModel.limpiarOtro("ñandú"))
    }
}
