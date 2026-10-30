package com.pame.karsy.core.util

import com.pame.karsy.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PagoDestacadoTest {
    private val clabe = "012345678901234567"

    private fun error(precio: String = "300", dias: String = "30", banco: String = "BBVA", clabe: String = this.clabe) =
        PagoDestacado.errorConfig(precio, dias, banco, clabe)

    // Solo lotes
    @Test fun soloElLotePuedeDestacar() = assertTrue(PagoDestacado.puedeDestacar("lote"))
    @Test fun particularNoPuedeDestacar() = assertFalse(PagoDestacado.puedeDestacar("particular"))
    @Test fun sinTipoDeCuentaNoPuedeDestacar() = assertFalse(PagoDestacado.puedeDestacar(null))
    @Test fun mayusculasNoSonLote() = assertFalse(PagoDestacado.puedeDestacar("LOTE"))

    // Referencia
    @Test fun referencia_llevaElIdDeLaPublicacion() = assertEquals("KAR-42", PagoDestacado.referencia(42))

    // Monto
    @Test fun monto_dosDecimales() = assertEquals("$300.00", PagoDestacado.monto(300.0))
    @Test fun monto_completaCentavos() = assertEquals("$300.50", PagoDestacado.monto(300.5))
    @Test fun monto_separaMiles() = assertEquals("$1,250.00", PagoDestacado.monto(1250.0))
    @Test fun monto_redondea() = assertEquals("$10.13", PagoDestacado.monto(10.126))

    // Disponible
    @Test fun disponible_conBancoYClabe() = assertTrue(PagoDestacado.disponible("BBVA", clabe))
    @Test fun disponible_sinBanco() = assertFalse(PagoDestacado.disponible(null, clabe))
    @Test fun disponible_bancoEnBlanco() = assertFalse(PagoDestacado.disponible("  ", clabe))
    @Test fun disponible_sinClabe() = assertFalse(PagoDestacado.disponible("BBVA", null))
    @Test fun disponible_clabeVacia() = assertFalse(PagoDestacado.disponible("BBVA", ""))
    @Test fun disponible_sinNada() = assertFalse(PagoDestacado.disponible(null, null))

    // CLABE
    @Test fun limpiarClabe_quitaEspaciosYLetras() = assertEquals("0123", PagoDestacado.limpiarClabe("01 2a3-"))
    @Test fun limpiarClabe_recortaA18() = assertEquals(clabe, PagoDestacado.limpiarClabe(clabe + "999"))
    @Test fun limpiarClabe_vacio() = assertEquals("", PagoDestacado.limpiarClabe(""))
    @Test fun clabeValida_18Digitos() = assertTrue(PagoDestacado.clabeValida(clabe))
    @Test fun clabeValida_17NoSirve() = assertFalse(PagoDestacado.clabeValida(clabe.dropLast(1)))
    @Test fun clabeValida_19NoSirve() = assertFalse(PagoDestacado.clabeValida(clabe + "1"))
    @Test fun clabeValida_conLetrasNoSirve() = assertFalse(PagoDestacado.clabeValida("01234567890123456a"))
    @Test fun clabeValida_vaciaNoSirve() = assertFalse(PagoDestacado.clabeValida(""))

    // Precio
    @Test fun leerPrecio_entero() = assertEquals(300.0, PagoDestacado.leerPrecio("300")!!, 0.0)
    @Test fun leerPrecio_conComasYEspacios() = assertEquals(1250.0, PagoDestacado.leerPrecio(" 1,250.00 ")!!, 0.0)
    @Test fun leerPrecio_textoNoEsPrecio() = assertNull(PagoDestacado.leerPrecio("abc"))
    @Test fun leerPrecio_vacioNoEsPrecio() = assertNull(PagoDestacado.leerPrecio(""))

    // Formulario del admin (mismas reglas que admin_guardar_config_pagos)
    @Test fun config_valida() = assertNull(error())
    @Test fun config_bancoYClabeVaciosSePermiten() = assertNull(error(banco = "", clabe = ""))
    @Test fun config_precioCeroFalla() = assertEquals(R.string.admin2_pay_error_price, error(precio = "0"))
    @Test fun config_precioNegativoFalla() = assertEquals(R.string.admin2_pay_error_price, error(precio = "-5"))
    @Test fun config_precioVacioFalla() = assertEquals(R.string.admin2_pay_error_price, error(precio = ""))
    @Test fun config_precioMaximoSePermite() = assertNull(error(precio = "100000"))
    @Test fun config_precioSobreElMaximoFalla() = assertEquals(R.string.admin2_pay_error_price, error(precio = "100000.01"))
    @Test fun config_diasCeroFalla() = assertEquals(R.string.admin2_pay_error_days, error(dias = "0"))
    @Test fun config_diasUnoSePermite() = assertNull(error(dias = "1"))
    @Test fun config_dias365SePermite() = assertNull(error(dias = "365"))
    @Test fun config_dias366Falla() = assertEquals(R.string.admin2_pay_error_days, error(dias = "366"))
    @Test fun config_diasDecimalesFalla() = assertEquals(R.string.admin2_pay_error_days, error(dias = "1.5"))
    @Test fun config_diasVaciosFalla() = assertEquals(R.string.admin2_pay_error_days, error(dias = ""))
    @Test fun config_banco60SePermite() = assertNull(error(banco = "B".repeat(60)))
    @Test fun config_banco61Falla() = assertEquals(R.string.admin2_pay_error_bank, error(banco = "B".repeat(61)))
    @Test fun config_bancoCuentaSinEspaciosAlBorde() = assertNull(error(banco = " " + "B".repeat(60) + " "))
    @Test fun config_clabeCortaFalla() = assertEquals(R.string.admin2_pay_error_clabe, error(clabe = "1234"))
    @Test fun config_clabeConLetrasFalla() = assertEquals(R.string.admin2_pay_error_clabe, error(clabe = "01234567890123456x"))
    @Test fun config_primerErrorEsElPrecio() =
        assertEquals(R.string.admin2_pay_error_price, error(precio = "0", dias = "0", banco = "B".repeat(61), clabe = "1"))
}
