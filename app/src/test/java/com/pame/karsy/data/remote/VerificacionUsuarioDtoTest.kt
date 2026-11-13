package com.pame.karsy.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerificacionUsuarioDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun constancia_traeRutaYNoCurp() {
        val dto = json.decodeFromString<VerificacionUsuarioDto>(
            """{"estado":"pendiente","tipo_documento":"constancia_situacion_fiscal","archivo_path":"uid/csf.pdf"}"""
        )
        assertEquals("constancia_situacion_fiscal", dto.tipoDocumento)
        assertEquals("uid/csf.pdf", dto.archivoPath)
        assertNull(dto.curp)
    }

    @Test
    fun curp_traeCurpYNoRuta() {
        val dto = json.decodeFromString<VerificacionUsuarioDto>(
            """{"estado":"aprobado","tipo_documento":"curp","archivo_path":null,"curp":"ABCD010203HDFXYZ09"}"""
        )
        assertEquals("curp", dto.tipoDocumento)
        assertNull(dto.archivoPath)
        assertEquals("ABCD010203HDFXYZ09", dto.curp)
    }

    @Test
    fun sinCamposNuevos_quedanNulos() {
        val dto = json.decodeFromString<VerificacionUsuarioDto>("""{"estado":"rechazado"}""")
        assertNull(dto.tipoDocumento)
        assertNull(dto.archivoPath)
        assertNull(dto.curp)
    }
}
