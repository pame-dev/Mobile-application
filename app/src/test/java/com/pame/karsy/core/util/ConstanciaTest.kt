package com.pame.karsy.core.util

import com.pame.karsy.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConstanciaTest {
    private val ok = 1024L

    @Test fun pdf_jpeg_png_seAceptan() {
        assertNull(Constancia.errorDe("application/pdf", ok))
        assertNull(Constancia.errorDe("image/jpeg", ok))
        assertNull(Constancia.errorDe("image/png", ok))
    }

    @Test fun imageJpg_seAceptaYSeNormalizaAJpeg() {
        assertNull(Constancia.errorDe("image/jpg", ok))
        assertEquals("image/jpeg", Constancia.mimeValido("image/jpg"))
        assertEquals("jpg", Constancia.extension("image/jpg"))
    }

    @Test fun mayusculasYEspacios_seToleran() {
        assertEquals("application/pdf", Constancia.mimeValido("  Application/PDF "))
    }

    @Test fun webpYHeic_seRechazanPorFormato() {
        assertEquals(R.string.form_error_constancia_format, Constancia.errorDe("image/webp", ok))
        assertEquals(R.string.form_error_constancia_format, Constancia.errorDe("image/heic", ok))
    }

    @Test fun tipoNuloOVacio_seRechazaPorFormato() {
        assertEquals(R.string.form_error_constancia_format, Constancia.errorDe(null, ok))
        assertEquals(R.string.form_error_constancia_format, Constancia.errorDe("", ok))
    }

    @Test fun exactamenteCincoMb_esValido() {
        assertEquals(5L * 1024 * 1024, Constancia.MAX_BYTES)
        assertNull(Constancia.errorDe("application/pdf", Constancia.MAX_BYTES))
    }

    @Test fun cincoMbMasUnByte_fallaPorTamano() {
        assertEquals(R.string.form_error_constancia_size, Constancia.errorDe("application/pdf", Constancia.MAX_BYTES + 1))
    }

    @Test fun ceroBytesONegativoONulo_noSePuedeLeer() {
        assertEquals(R.string.form_error_constancia_unreadable, Constancia.errorDe("image/png", 0))
        assertEquals(R.string.form_error_constancia_unreadable, Constancia.errorDe("image/png", -1))
        assertEquals(R.string.form_error_constancia_unreadable, Constancia.errorDe("image/png", null))
    }

    @Test fun formatoInvalido_tienePrioridadSobreTamano() {
        assertEquals(R.string.form_error_constancia_format, Constancia.errorDe("image/webp", 0))
    }
}
