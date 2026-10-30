package com.pame.karsy.core.components

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvisoKarsyTest {
    @After
    fun limpiar() = AvisoKarsy.cerrar()

    @Test
    fun mostrarGuardaTextoYNoEsErrorPorDefecto() {
        AvisoKarsy.mostrar("Hola")
        val a = AvisoKarsy.actual!!
        assertEquals("Hola", a.texto)
        assertFalse(a.error)
    }

    @Test
    fun mostrarConErrorMarcaError() {
        AvisoKarsy.mostrar("Falló", error = true)
        assertTrue(AvisoKarsy.actual!!.error)
    }

    @Test
    fun cadaLlamadaTieneIdMayor() {
        AvisoKarsy.mostrar("Igual")
        val id1 = AvisoKarsy.actual!!.id
        AvisoKarsy.mostrar("Igual")
        assertTrue(AvisoKarsy.actual!!.id > id1)
    }

    @Test
    fun cerrarDejaNulo() {
        AvisoKarsy.mostrar("x")
        AvisoKarsy.cerrar()
        assertNull(AvisoKarsy.actual)
    }
}
