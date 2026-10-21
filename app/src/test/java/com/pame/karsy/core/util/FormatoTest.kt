package com.pame.karsy.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatoTest {
    // Mediodía UTC: en cualquier zona horaria real cae el mismo día.
    @Test fun fechaNumerica_formatoDiaMesAnio() {
        assertEquals("23/09/2026", Formato.fechaNumerica("2026-09-23T12:00:00+00:00"))
    }

    @Test fun fechaNumerica_rellenaConCeros() {
        assertEquals("03/09/2026", Formato.fechaNumerica("2026-09-03T12:00:00+00:00"))
        assertEquals("05/01/2027", Formato.fechaNumerica("2027-01-05T12:00:00Z"))
    }

    @Test fun fechaNumerica_sinZonaSeAsumeUtc() {
        assertEquals("03/09/2026", Formato.fechaNumerica("2026-09-03 12:00:00"))
    }

    @Test fun fechaNumerica_nuloOIlegible_devuelveNull() {
        assertNull(Formato.fechaNumerica(null))
        assertNull(Formato.fechaNumerica(""))
        assertNull(Formato.fechaNumerica("no es fecha"))
    }

    @Test fun fechaNumerica_fechaInexistente_devuelveNull() {
        assertNull(Formato.fechaNumerica("2026-13-45T12:00:00Z"))
        assertNull(Formato.fechaNumerica("   "))
    }
}
