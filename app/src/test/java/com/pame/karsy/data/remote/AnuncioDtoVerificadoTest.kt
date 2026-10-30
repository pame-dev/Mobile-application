package com.pame.karsy.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnuncioDtoVerificadoTest {
    // Misma configuración que core/supabase/Supabase.kt
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun fila(extra: String) = """
        {"id_publicacion":1,"id_propietario":"u1","propietario_nombre":"Ana","propietario_tipo":"particular",
         "estado_publicacion":"publicada","estado_administrativo":"aprobada","fecha_creacion":"2026-01-01T00:00:00Z"$extra}
    """.trimIndent()

    @Test
    fun sinLaClaveEsFalse() {
        assertFalse(json.decodeFromString<AnuncioDto>(fila("")).propietarioVerificado)
    }

    @Test
    fun conLaClaveEnTrue() {
        assertTrue(json.decodeFromString<AnuncioDto>(fila(""","propietario_verificado":true""")).propietarioVerificado)
    }

    @Test
    fun conLaClaveNulaEsFalse() {
        assertFalse(json.decodeFromString<AnuncioDto>(fila(""","propietario_verificado":null""")).propietarioVerificado)
    }
}
