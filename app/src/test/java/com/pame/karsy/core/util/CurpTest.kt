package com.pame.karsy.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurpTest {
    @Test fun curp1900_digitoEnPosicion16() {
        assertEquals(Curp.Datos(1990, 3, 15, 'H', "JC"), Curp.leer("GOMC900315HJCRRN09"))
    }

    @Test fun curp2000_letraEnPosicion16() {
        assertEquals(Curp.Datos(2005, 6, 12, 'M', "DF"), Curp.leer("LOPE050612MDFPRAA1"))
    }

    @Test fun sexoX_yEstadoNacidoEnExtranjero() {
        assertEquals(Curp.Datos(2001, 1, 1, 'X', "NE"), Curp.leer("ABCD010101XNELMNA5"))
    }

    @Test fun minusculasYEspacios_seNormalizan() {
        assertEquals(Curp.Datos(1990, 3, 15, 'H', "JC"), Curp.leer("  gomc900315hjcrrn09 "))
    }

    @Test fun formatoInvalido_null() {
        assertNull(Curp.leer(""))
        assertNull(Curp.leer("ABC"))
        assertNull(Curp.leer("GOMC900315HJCRRN0"))     // 17
        assertNull(Curp.leer("GOMC900315HJCRRN099"))   // 19
        assertNull(Curp.leer("GOMC900315ZJCRRN09"))    // sexo Z
        assertNull(Curp.leer("GOMC90031HHJCRRN09"))    // letra en fecha
    }

    @Test fun mes13_null() {
        assertNull(Curp.leer("GOMC901315HJCRRN09"))
    }

    @Test fun mes00_null() {
        assertNull(Curp.leer("GOMC900015HJCRRN09"))
    }

    @Test fun dia00_null() {
        assertNull(Curp.leer("GOMC900300HJCRRN09"))
    }

    @Test fun estadoInexistente_null() {
        assertNull(Curp.leer("GOMC900315HXXRRN09"))
    }

    @Test fun diaInexistenteDelMes_null() {
        assertNull(Curp.leer("GOMC900431HJCRRN09"))  // 31 de abril
        assertNull(Curp.leer("GOMC900230HJCRRN09"))  // 30 de febrero
    }

    @Test fun veintinueveDeFebrero_soloEnBisiesto() {
        assertNull(Curp.leer("GOMC900229HJCRRN09"))  // 1990 no bisiesto
        assertEquals(Curp.Datos(1992, 2, 29, 'H', "JC"), Curp.leer("GOMC920229HJCRRN09"))
        assertEquals(Curp.Datos(2000, 2, 29, 'H', "JC"), Curp.leer("GOMC000229HJCRRNA1"))  // 2000 si es bisiesto
        assertNull(Curp.leer("GOMC000229HJCRRN09"))  // 1900 no es bisiesto
    }
}
