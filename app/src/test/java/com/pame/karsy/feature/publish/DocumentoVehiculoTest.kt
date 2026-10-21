package com.pame.karsy.feature.publish

import com.pame.karsy.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Documento del vehículo del paso "Datos": VIN, placa, tarjeta de circulación o baja. */
class DocumentoVehiculoTest {

    private val vinValido = "1HGCM82633A004352"

    // ── normalizarDocumento ──────────────────────────────────────────────────

    @Test
    fun normalizar_quitaEspaciosYPasaAMayusculas() {
        assertEquals("1HGCM82633A004352", PublishViewModel.normalizarDocumento(" 1hgcm 82633a 004352 "))
    }

    @Test
    fun normalizar_quitaTabuladoresYSaltosDeLinea() {
        assertEquals("ABC123", PublishViewModel.normalizarDocumento("abc\t12\n3"))
    }

    @Test
    fun normalizar_conservaElGuionDeLaPlaca() {
        assertEquals("ABC-123", PublishViewModel.normalizarDocumento("abc - 123"))
    }

    @Test
    fun normalizar_textoVacioOSoloEspacios_quedaVacio() {
        assertEquals("", PublishViewModel.normalizarDocumento(""))
        assertEquals("", PublishViewModel.normalizarDocumento("   "))
    }

    // ── constantes ───────────────────────────────────────────────────────────

    @Test
    fun constantes_coincidenConLaBaseDeDatos() {
        assertEquals(17, PublishViewModel.LARGO_VIN)
        assertEquals(10, PublishViewModel.MAX_PLACA)
        assertEquals(listOf("tarjeta_circulacion", "baja"), PublishViewModel.TIPOS_DOCUMENTO_FOTO)
    }

    // ── errorDocumento: tipo ─────────────────────────────────────────────────

    @Test
    fun tipoVacio_pideElegirDocumento_aunqueHayaTextoOFoto() {
        assertEquals(R.string.publish_error_document_type, PublishViewModel.errorDocumento("", "", false))
        assertEquals(R.string.publish_error_document_type, PublishViewModel.errorDocumento("", vinValido, true))
    }

    // ── errorDocumento: VIN ──────────────────────────────────────────────────

    @Test
    fun vinValido_noTieneError() {
        assertNull(PublishViewModel.errorDocumento("vin", vinValido, false))
    }

    @Test
    fun vinEnMinusculasYConEspacios_esValidoTrasNormalizar() {
        assertNull(PublishViewModel.errorDocumento("vin", " 1hgcm 82633a 004352 ", false))
    }

    @Test
    fun vinDe16Caracteres_esInvalido() {
        assertEquals(R.string.publish_error_vin, PublishViewModel.errorDocumento("vin", vinValido.dropLast(1), false))
    }

    @Test
    fun vinDe18Caracteres_esInvalido() {
        assertEquals(R.string.publish_error_vin, PublishViewModel.errorDocumento("vin", vinValido + "1", false))
    }

    @Test
    fun vinConLetrasIOQ_esInvalido() {
        for (letra in listOf('I', 'O', 'Q', 'i', 'o', 'q')) {
            val vin = vinValido.replaceRange(5, 6, letra.toString())
            assertEquals("VIN con $letra", R.string.publish_error_vin, PublishViewModel.errorDocumento("vin", vin, false))
        }
    }

    @Test
    fun vinConSimbolos_esInvalido() {
        assertEquals(R.string.publish_error_vin, PublishViewModel.errorDocumento("vin", "1HGCM82633A00435-", false))
    }

    @Test
    fun vinVacio_esInvalido() {
        assertEquals(R.string.publish_error_vin, PublishViewModel.errorDocumento("vin", "", false))
    }

    @Test
    fun vin_unaFotoNoSustituyeAlTexto() {
        assertEquals(R.string.publish_error_vin, PublishViewModel.errorDocumento("vin", "", true))
    }

    // ── errorDocumento: placa ────────────────────────────────────────────────

    @Test
    fun placaValida_noTieneError() {
        assertNull(PublishViewModel.errorDocumento("placa", "ABC-123", false))
        assertNull(PublishViewModel.errorDocumento("placa", " abc 123 ", false))
    }

    @Test
    fun placaEnLosLimites_4y10Caracteres_esValida() {
        assertNull(PublishViewModel.errorDocumento("placa", "AB12", false))
        assertNull(PublishViewModel.errorDocumento("placa", "ABCDE-1234", false))
    }

    @Test
    fun placaFueraDeLimites_3y11Caracteres_esInvalida() {
        assertEquals(R.string.publish_error_plate, PublishViewModel.errorDocumento("placa", "AB1", false))
        assertEquals(R.string.publish_error_plate, PublishViewModel.errorDocumento("placa", "ABCDE-12345", false))
    }

    @Test
    fun placaVaciaOConSimbolos_esInvalida() {
        assertEquals(R.string.publish_error_plate, PublishViewModel.errorDocumento("placa", "", false))
        assertEquals(R.string.publish_error_plate, PublishViewModel.errorDocumento("placa", "ABC_123", false))
        assertEquals(R.string.publish_error_plate, PublishViewModel.errorDocumento("placa", "ABC.123", false))
    }

    // ── errorDocumento: fotos ────────────────────────────────────────────────

    @Test
    fun tarjetaYBaja_sinFoto_piden_laFoto() {
        for (tipo in PublishViewModel.TIPOS_DOCUMENTO_FOTO) {
            assertEquals(tipo, R.string.publish_error_document_photo, PublishViewModel.errorDocumento(tipo, "", false))
        }
    }

    @Test
    fun tarjetaYBaja_conFoto_noTienenError() {
        for (tipo in PublishViewModel.TIPOS_DOCUMENTO_FOTO) {
            assertNull(tipo, PublishViewModel.errorDocumento(tipo, "", true))
        }
    }

    @Test
    fun tarjeta_unTextoNoSustituyeALaFoto() {
        assertEquals(R.string.publish_error_document_photo, PublishViewModel.errorDocumento("tarjeta_circulacion", vinValido, false))
    }

    @Test
    fun vinYPlaca_noPidenFoto() {
        assertNull(PublishViewModel.errorDocumento("vin", vinValido, false))
        assertNull(PublishViewModel.errorDocumento("placa", "ABC-123", false))
    }
}
