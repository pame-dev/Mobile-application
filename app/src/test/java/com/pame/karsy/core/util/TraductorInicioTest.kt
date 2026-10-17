package com.pame.karsy.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Inicio de la descripción que se traduce en las tarjetas (igual que web/src/lib/traductor.ts). */
class TraductorInicioTest {

    @Test
    fun textoCorto_noSeCorta() {
        assertEquals("Hola mundo" to false, Traductor.inicio("Hola mundo"))
    }

    @Test
    fun espaciosYSaltos_seColapsan() {
        assertEquals("Hola mundo bonito" to false, Traductor.inicio("  Hola\n\n  mundo\t bonito  "))
    }

    @Test
    fun exactamenteMax_noSeCorta() {
        val t = "x".repeat(100)
        assertEquals(t to false, Traductor.inicio(t))
    }

    @Test
    fun largo_seCortaEnLaUltimaPalabraCompleta() {
        assertEquals("uno dos" to true, Traductor.inicio("uno dos tres cuatro", 10))
        val texto = List(30) { "carro" }.joinToString(" ")
        assertEquals(List(16) { "carro" }.joinToString(" ") to true, Traductor.inicio(texto))
    }

    @Test
    fun espacioJustoEnMax_seCortaAhi() {
        assertEquals("a".repeat(100) to true, Traductor.inicio("a".repeat(100) + " b"))
    }

    @Test
    fun palabraGiganteSinEspacios_seCortaEnMax() {
        assertEquals("x".repeat(100) to true, Traductor.inicio("x".repeat(200)))
        // El único espacio queda antes de max/2: también se corta en max.
        assertEquals("ab " + "x".repeat(97) to true, Traductor.inicio("ab " + "x".repeat(200)))
    }

    @Test
    fun vacioOSoloEspacios_quedaVacio() {
        assertEquals("" to false, Traductor.inicio(""))
        assertEquals("" to false, Traductor.inicio("   \n\t "))
    }
}
