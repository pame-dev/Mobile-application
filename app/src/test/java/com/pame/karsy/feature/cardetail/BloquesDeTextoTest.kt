package com.pame.karsy.feature.cardetail

import org.junit.Assert.assertEquals
import org.junit.Test

/** Partes del texto del vendedor (descripción y detalles) para mostrarlo justificado. */
class BloquesDeTextoTest {

    @Test
    fun unaLinea_daUnParrafo() {
        assertEquals(listOf(BloqueTexto.Parrafo("Auto muy cuidado")), bloquesDeTexto("Auto muy cuidado"))
    }

    @Test
    fun lineasSeguidas_seUnenEnUnParrafoConSalto() {
        assertEquals(listOf(BloqueTexto.Parrafo("a\nb")), bloquesDeTexto("a\nb"))
    }

    @Test
    fun lineaEnBlanco_cierraElParrafo() {
        assertEquals(
            listOf(BloqueTexto.Parrafo("Desc larga"), BloqueTexto.Vacia, BloqueTexto.Parrafo("Potencia: 188 hp")),
            bloquesDeTexto("Desc larga\n\nPotencia: 188 hp"),
        )
    }

    @Test
    fun lineaSoloConEspacios_cuentaComoVacia() {
        assertEquals(
            listOf(BloqueTexto.Parrafo("a"), BloqueTexto.Vacia, BloqueTexto.Parrafo("b")),
            bloquesDeTexto("a\n   \nb"),
        )
    }

    @Test
    fun vinetas_conAsteriscoGuionYPunto() {
        assertEquals(
            listOf(BloqueTexto.Vineta("uno"), BloqueTexto.Vineta("dos"), BloqueTexto.Vineta("tres")),
            bloquesDeTexto("* uno\n- dos\n• tres"),
        )
    }

    @Test
    fun vineta_conEspaciosAlInicio() {
        assertEquals(listOf(BloqueTexto.Vineta("Aire acondicionado")), bloquesDeTexto("   * Aire acondicionado"))
    }

    @Test
    fun guionPegadoAlNumero_noEsVineta() {
        assertEquals(listOf(BloqueTexto.Parrafo("-5%")), bloquesDeTexto("-5%"))
    }

    @Test
    fun guionSoloConEspacio_noEsVineta() {
        assertEquals(listOf(BloqueTexto.Parrafo("- ")), bloquesDeTexto("- "))
    }

    @Test
    fun textoMezclado_parrafoVinetasYParrafo() {
        assertEquals(
            listOf(
                BloqueTexto.Parrafo("Intro"),
                BloqueTexto.Vineta("A"),
                BloqueTexto.Vineta("B"),
                BloqueTexto.Parrafo("Cierre"),
            ),
            bloquesDeTexto("Intro\n* A\n* B\nCierre"),
        )
    }

    @Test
    fun textoVacio_daUnaSolaVacia() {
        // "".lines() devuelve [""], que es una línea en blanco.
        assertEquals(listOf(BloqueTexto.Vacia), bloquesDeTexto(""))
    }
}
