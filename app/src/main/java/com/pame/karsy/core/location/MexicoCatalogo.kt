package com.pame.karsy.core.location

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Estados y municipios de México, del Marco Geoestadístico del INEGI (oct. 2026), en
 * assets/mexico_estados_municipios.json. Va dentro de la app: funciona sin internet.
 * Algunos estados usan su nombre común: Coahuila, Michoacán, Veracruz, Estado de México.
 */
object MexicoCatalogo {
    @Serializable
    private data class Estado(val estado: String, val municipios: List<String>)

    @Volatile private var cache: List<Estado>? = null

    private fun datos(context: Context): List<Estado> = cache ?: synchronized(this) {
        cache ?: context.applicationContext.assets.open("mexico_estados_municipios.json")
            .bufferedReader().use { Json.decodeFromString<List<Estado>>(it.readText()) }
            .also { cache = it }
    }

    /** Los 32 estados en orden alfabético. */
    fun estados(context: Context): List<String> = datos(context).map { it.estado }

    /**
     * Municipios del estado (ignora acentos y mayúsculas, y acepta el nombre oficial,
     * p. ej. "Coahuila de Zaragoza"). Lista vacía si el estado no está en el catálogo.
     */
    fun municipios(context: Context, estado: String): List<String> =
        datos(context).firstOrNull { UbicacionActual.mismoLugar(it.estado, estado) }?.municipios.orEmpty()

    /** Nombre del catálogo para un estado escrito de otra forma ("coahuila" → "Coahuila"), o null. */
    fun estadoDelCatalogo(context: Context, estado: String): String? =
        datos(context).firstOrNull { UbicacionActual.mismoLugar(it.estado, estado) }?.estado
}
