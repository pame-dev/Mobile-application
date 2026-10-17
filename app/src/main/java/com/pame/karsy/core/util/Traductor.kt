package com.pame.karsy.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Traducción automática español → inglés con MyMemory (gratis, sin cuenta ni clave). En
 * inglés la usan el detalle de un auto (CarRepository.traduccion) y los textos que escriben
 * los usuarios y el admin (rememberTraduccion). Igual que web/src/lib/traductor.ts.
 *
 * - MyMemory acepta hasta ~500 bytes por petición: el texto se parte por oraciones.
 * - Límite gratuito: unos 5,000 caracteres al día por conexión; si se acaba o falla,
 *   regresa null y se muestra el original. Si se acabó, no se vuelve a pedir en una hora.
 * - Las traducciones se guardan mientras la app está abierta para no pedirlas dos veces.
 */
object Traductor {
    private const val API = "https://api.mymemory.translated.net/get"
    private const val MAX_BYTES = 450
    private val cache = mutableMapOf<String, String>()
    private val enCurso = mutableMapOf<String, Deferred<String?>>()
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Hasta cuándo (ms) no se pide nada porque se acabó el límite diario; mientras, todo se
     * muestra en el original sin hacer peticiones que van a fallar. 0 = disponible.
     */
    @Volatile internal var agotadoHasta = 0L
    private const val ESPERA_SI_AGOTADO = 60 * 60 * 1000L
    private class CuotaAgotada : Exception()

    /**
     * Inicio del texto que cabe en una tarjeta (hasta [max] caracteres, cortado en la última
     * palabra completa) y si se cortó. Se traduce solo eso para gastar poco del límite diario.
     */
    fun inicio(texto: String, max: Int = 100): Pair<String, Boolean> {
        val limpio = texto.trim().replace(Regex("\\s+"), " ")
        if (limpio.length <= max) return limpio to false
        val corte = limpio.lastIndexOf(' ', max).takeIf { it > max / 2 } ?: max
        return limpio.substring(0, corte).trimEnd() to true
    }

    /**
     * El texto en inglés, o null si no se pudo traducir. Con [corto] solo se traduce su
     * [inicio] (listas y tarjetas) y se agrega "…" si se cortó.
     */
    suspend fun alIngles(texto: String, corto: Boolean = false): String? {
        if (corto) {
            val (ini, cortado) = inicio(texto)
            return alIngles(ini)?.let { if (cortado) "$it…" else it }
        }
        val limpio = texto.trim()
        if (limpio.isEmpty()) return null
        // Una sola petición por texto aunque la pidan varias tarjetas a la vez. Corre aparte de
        // quien la pidió: si esa tarjeta sale de pantalla, el resultado se guarda igual.
        val pedido = synchronized(cache) {
            cache[limpio]?.let { return it }
            if (System.currentTimeMillis() < agotadoHasta) return null
            enCurso.getOrPut(limpio) {
                alcance.async {
                    runCatching { partes(limpio).joinToString("") { traducirParte(it) }.trim() }
                        .onFailure { if (it is CuotaAgotada) agotadoHasta = System.currentTimeMillis() + ESPERA_SI_AGOTADO }
                        .getOrNull()
                        .also { r ->
                            synchronized(cache) {
                                if (r != null) cache[limpio] = r
                                enCurso.remove(limpio)
                            }
                        }
                }
            }
        }
        return pedido.await()
    }

    /** La traducción ya guardada (sin pedirla), o null; [corto] igual que en [alIngles]. */
    fun guardada(texto: String, corto: Boolean = false): String? {
        if (corto) {
            val (ini, cortado) = inicio(texto)
            return guardada(ini)?.let { if (cortado) "$it…" else it }
        }
        return synchronized(cache) { cache[texto.trim()] }
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
        if (estado == 429 || traducido?.startsWith("MYMEMORY WARNING") == true) throw CuotaAgotada()
        if (estado != 200 || traducido.isNullOrBlank()) error("sin traducción")
        // Conserva los espacios y saltos de línea de alrededor.
        return texto.replace(texto.trim(), traducido)
    }
}
