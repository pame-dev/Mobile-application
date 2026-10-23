package com.pame.karsy.core.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConditionTagTest {

    @Test fun seminuevo_capitalizado() = assertEquals("seminuevo", normalizarCondicion("Seminuevo"))

    @Test fun usado_mayusculas() = assertEquals("usado", normalizarCondicion("USADO"))

    @Test fun usado_con_espacios() = assertEquals("usado", normalizarCondicion(" usado "))

    @Test fun usado_con_acento() = assertEquals("usado", normalizarCondicion("Usádo"))

    @Test fun seminuevo_con_acento() = assertEquals("seminuevo", normalizarCondicion("Semínuevo"))

    @Test fun nulo_devuelve_null() = assertNull(normalizarCondicion(null))

    @Test fun vacio_devuelve_null() = assertNull(normalizarCondicion(""))

    @Test fun solo_espacios_devuelve_null() = assertNull(normalizarCondicion("   "))

    @Test fun nuevo_devuelve_null() = assertNull(normalizarCondicion("nuevo"))

    @Test fun plural_devuelve_null() = assertNull(normalizarCondicion("seminuevos"))
}
