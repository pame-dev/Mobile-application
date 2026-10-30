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

    fun clabeValida(clabe: String): Boolean = CLABE.matches(clabe)

    /** "300", "300.5" o "1,250.00" -> número; null si no es un precio. */
    fun leerPrecio(texto: String): Double? = texto.replace(",", "").trim().toDoubleOrNull()

    /**
     * Id del texto de error del formulario "Datos para recibir pagos", o null si se puede guardar.
     * Mismas reglas que admin_guardar_config_pagos; banco y CLABE pueden quedar vacíos.
     */
    fun errorConfig(precio: String, dias: String, banco: String, clabe: String): Int? {
        val p = leerPrecio(precio)
        val d = dias.trim().toIntOrNull()
        return when {
            p == null || p <= 0 || p > MAX_PRECIO -> R.string.admin2_pay_error_price
            d == null || d < 1 || d > MAX_DIAS -> R.string.admin2_pay_error_days
            banco.trim().length > MAX_BANCO -> R.string.admin2_pay_error_bank
            clabe.isNotEmpty() && !clabeValida(clabe) -> R.string.admin2_pay_error_clabe
            else -> null
        }
    }
}
