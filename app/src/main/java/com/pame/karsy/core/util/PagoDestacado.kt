package com.pame.karsy.core.util

import com.pame.karsy.R
import java.text.NumberFormat
import java.util.Locale

/**
 * Reglas del pago de un destacado por transferencia (config_pagos y solicitar_destacado):
 * solo lotes, referencia KAR-<id_publicacion> y cuenta con banco y CLABE de 18 dígitos.
 */
object PagoDestacado {

    /** cuentas.tipo_cuenta de quien puede pedir (y renovar) un destacado. */
    const val TIPO_CUENTA = "lote"

    const val LARGO_CLABE = 18
    const val MAX_BANCO = 60
    const val MAX_PRECIO = 100_000.0
    const val MAX_DIAS = 365

    private val CLABE = Regex("^[0-9]{18}$")
    private val PESOS = intArrayOf(3, 7, 1)
    private val BANCOS = mapOf(
        "002" to "BANAMEX", "012" to "BBVA", "014" to "SANTANDER", "021" to "HSBC", "030" to "BAJÍO",
        "036" to "INBURSA", "042" to "MIFEL", "044" to "SCOTIABANK", "058" to "BANREGIO", "062" to "AFIRME",
        "072" to "BANORTE", "127" to "AZTECA", "130" to "COMPARTAMOS", "137" to "BANCOPPEL", "138" to "ABC CAPITAL",
        "143" to "CIBANCO", "152" to "BANCREA", "156" to "SABADELL", "166" to "BANCO DEL BIENESTAR",
        "638" to "NU MÉXICO", "646" to "STP", "722" to "MERCADO PAGO", "728" to "SPIN BY OXXO",
    )

    /** Solo los lotes pueden destacar; los destacados vigentes de particulares siguen hasta terminar. */
    fun puedeDestacar(tipoCuenta: String?): Boolean = tipoCuenta == TIPO_CUENTA

    /** Lo que se escribe en el concepto de la transferencia (igual que en la BD). */
    fun referencia(idPublicacion: Long): String = "KAR-$idPublicacion"

    /** 300.0 -> "$300.00": el monto exacto a transferir, con centavos. */
    fun monto(valor: Double): String =
        "$" + NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(valor)

    /** Sin banco o sin CLABE nadie puede pedir destacados (no se paga a una cuenta vacía). */
    fun disponible(banco: String?, clabe: String?): Boolean = !banco.isNullOrBlank() && !clabe.isNullOrBlank()

    /** Deja solo los dígitos de la CLABE (máx. 18) mientras el admin la escribe. */
    fun limpiarClabe(texto: String): String = texto.filter { it in '0'..'9' }.take(LARGO_CLABE)

    /** 18 dígitos y dígito de control correcto. */
    fun clabeValida(clabe: String): Boolean = CLABE.matches(clabe) && clabeDigitoOk(clabe)

    /**
     * Dígito de control de la CLABE: sobre los primeros 17 dígitos, con pesos 3, 7, 1 repetidos,
     * se suma (dígito * peso) % 10; el control es (10 - suma % 10) % 10 y debe ser el dígito 18.
     */
    fun clabeDigitoOk(clabe: String): Boolean {
        if (!CLABE.matches(clabe)) return false
        val suma = (0 until LARGO_CLABE - 1).sumOf { i -> (clabe[i].digitToInt() * PESOS[i % 3]) % 10 }
        return (10 - suma % 10) % 10 == clabe[LARGO_CLABE - 1].digitToInt()
    }

    /** Banco según los 3 primeros dígitos de la CLABE; null si no se conoce. */
    fun bancoDeClabe(clabe: String): String? = if (clabe.length >= 3) BANCOS[clabe.take(3)] else null

    /** "032180000118359719" -> "032 180 00011835971 9"; si no tiene 18 dígitos, tal cual. */
    fun clabeAgrupada(clabe: String): String =
        if (CLABE.matches(clabe)) "${clabe.substring(0, 3)} ${clabe.substring(3, 6)} ${clabe.substring(6, 17)} ${clabe.substring(17)}"
        else clabe

    /** "300", "300.5" o "1,250.00" -> número; null si no es un precio. */
    fun leerPrecio(texto: String): Double? = texto.replace(",", "").trim().toDoubleOrNull()

    /**
     * Id del texto de error del formulario "Datos para recibir pagos", o null si se puede guardar.
     * Mismas reglas que admin_guardar_config_pagos (más el dígito de control); banco y CLABE pueden quedar vacíos.
     */
    fun errorConfig(precio: String, dias: String, banco: String, clabe: String): Int? {
        val p = leerPrecio(precio)
        val d = dias.trim().toIntOrNull()
        return when {
            p == null || p <= 0 || p > MAX_PRECIO -> R.string.admin2_pay_error_price
            d == null || d < 1 || d > MAX_DIAS -> R.string.admin2_pay_error_days
            banco.trim().length > MAX_BANCO -> R.string.admin2_pay_error_bank
            clabe.isNotEmpty() && !CLABE.matches(clabe) -> R.string.admin2_pay_error_clabe
            clabe.isNotEmpty() && !clabeDigitoOk(clabe) -> R.string.admin2_pay_error_clabe_digit
            else -> null
        }
    }
}
