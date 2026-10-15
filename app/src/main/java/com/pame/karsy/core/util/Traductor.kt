package com.pame.karsy.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Traducción automática español → inglés con MyMemory (gratis, sin cuenta ni clave). La usa
 * el detalle de un auto en inglés para la descripción y las imperfecciones. Igual que
 * web/src/lib/traductor.ts.
 *
 * - MyMemory acepta hasta ~500 bytes por petición: el texto se parte por oraciones.
 * - Límite gratuito: unos 5,000 caracteres al día por conexión; si se acaba o falla,
 *   regresa null y se muestra el original.
 * - Las traducciones se guardan mientras la app está abierta para no pedirlas dos veces.
 */
object Traductor {
    private const val API = "https://api.mymemory.translated.net/get"
    private const val MAX_BYTES = 450
    private val cache = mutableMapOf<String, String>()
    private val json = Json { ignoreUnknownKeys = true }

    /** El texto en inglés, o null si no se pudo traducir. */
    suspend fun alIngles(texto: String): String? {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return null
        synchronized(cache) { cache[limpio] }?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { partes(limpio).joinToString("") { traducirParte(it) }.trim() }.getOrNull()
        }?.also { synchronized(cache) { cache[limpio] = it } }
    }

    private fun bytes(t: String) = t.toByteArray(Charsets.UTF_8).size

    /** Pedazos de máximo MAX_BYTES, por oraciones (y por palabras si una oración es muy larga). */
    private fun partes(texto: String): List<String> {
        val oraciones = Regex("[^.!?\\n]+[.!?]*\\s*|\\n+").findAll(texto).map { it.value }.toList().ifEmpty { listOf(texto) }
        val salida = mutableListOf<String>()
        var actual = ""
        fun agregar(pedazo: String) {
            if (bytes(actual + pedazo) <= MAX_BYTES) actual += pedazo
            else {
                if (actual.isNotEmpty()) salida += actual
                actual = pedazo
            }
        }
        for (o in oraciones) {
            if (bytes(o) <= MAX_BYTES) agregar(o)
            else Regex("(?<=\\s)").split(o).forEach(::agregar)
        }
        if (actual.isNotEmpty()) salida += actual
        return salida
    }

    private fun traducirParte(texto: String): String {
        if (texto.isBlank()) return texto
        val q = URLEncoder.encode(texto.trim(), "UTF-8")
        val conexion = URL("$API?q=$q&langpair=es%7Cen").openConnection() as HttpURLConnection
        conexion.connectTimeout = 8_000
        conexion.readTimeout = 8_000
        val cuerpo = conexion.inputStream.bufferedReader().use { it.readText() }
        val raiz = json.parseToJsonElement(cuerpo).jsonObject
        val estado = raiz["responseStatus"]?.jsonPrimitive?.let { it.intOrNull ?: it.content.toIntOrNull() }
        val traducido = raiz["responseData"]?.jsonObject?.get("translatedText")?.jsonPrimitive?.content
        // Con la cuota agotada responde 429 o un texto "MYMEMORY WARNING…".
        if (estado != 200 || traducido.isNullOrBlank() || traducido.startsWith("MYMEMORY WARNING")) error("sin traducción")
        // Conserva los espacios y saltos de línea de alrededor.
        return texto.replace(texto.trim(), traducido)
    }
}
