package com.pame.karsy.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Extensión que recibe el archivo en el bucket (documento del vehículo y constancia fiscal).
 * Debe coincidir con `<uuid>.(jpg|png|pdf)` de la política y el CHECK de la BD. ConstanciaTest ya
 * cubre image/jpg y las mayúsculas; aquí lo que faltaba.
 */
class ConstanciaExtensionTest {
    @Test fun extension_pdfYPng() {
        assertEquals("pdf", Constancia.extension("application/pdf"))
        assertEquals("png", Constancia.extension("image/png"))
    }

    @Test fun extension_jpeg_esJpgNoJpeg() {
        assertEquals("jpg", Constancia.extension("image/jpeg"))
        assertEquals("jpg", Constancia.extension("image/pjpeg"))
    }

    @Test fun extension_toleraMayusculasYEspacios() {
        assertEquals("pdf", Constancia.extension(" APPLICATION/PDF "))
        assertEquals("png", Constancia.extension("Image/PNG"))
    }

    @Test fun pjpeg_seNormalizaAJpeg() {
        assertEquals("image/jpeg", Constancia.mimeValido("image/pjpeg"))
    }

    @Test fun mimeValido_rechazaWebpGifHeicVacioYNulo() {
        assertNull(Constancia.mimeValido("image/webp"))
        assertNull(Constancia.mimeValido("image/gif"))
        assertNull(Constancia.mimeValido("image/heic"))
        assertNull(Constancia.mimeValido(""))
        assertNull(Constancia.mimeValido(null))
    }

    @Test fun extensionesPosibles_sonSoloLasQueAceptaLaBd() {
        val permitidas = setOf("jpg", "png", "pdf")
        val tipos = listOf("application/pdf", "image/jpeg", "image/jpg", "image/pjpeg", "image/png")
        assertEquals(permitidas, tipos.map { Constancia.extension(it) }.toSet())
    }
}
