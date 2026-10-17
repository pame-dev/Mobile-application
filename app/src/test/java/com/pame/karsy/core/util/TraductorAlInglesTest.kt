package com.pame.karsy.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLDecoder
import java.net.URLStreamHandler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Traductor.alIngles con un MyMemory falso: se intercepta https en esta JVM, así que ninguna
 * prueba hace red real. Cada prueba usa textos distintos porque la caché es global.
 */
class TraductorAlInglesTest {

    companion object {
        /** Peticiones hechas por texto (q). */
        val peticiones = java.util.concurrent.ConcurrentHashMap<String, AtomicInteger>()

        /** Si no es null, las respuestas esperan a que se abra. */
        @Volatile var compuerta: CountDownLatch? = null

        @BeforeClass @JvmStatic
        fun instalarMyMemoryFalso() {
            URL.setURLStreamHandlerFactory { protocolo ->
                if (protocolo != "https") null else object : URLStreamHandler() {
                    override fun openConnection(u: URL): URLConnection = Conexion(u)
                }
            }
        }

        fun peticionesDe(q: String) = peticiones[q]?.get() ?: 0
    }

    private class Conexion(u: URL) : HttpURLConnection(u) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getInputStream(): InputStream {
            val q = URLDecoder.decode(url.query.substringAfter("q=").substringBefore("&"), "UTF-8")
            peticiones.getOrPut(q) { AtomicInteger() }.incrementAndGet()
            compuerta?.await(5, TimeUnit.SECONDS)
            val cuerpo = when {
                "CUOTA" in q -> """{"responseStatus":200,"responseData":{"translatedText":"MYMEMORY WARNING: YOU USED ALL AVAILABLE FREE TRANSLATIONS FOR TODAY."}}"""
                "LIMITE" in q -> """{"responseStatus":429,"responseData":{"translatedText":"x"}}"""
                else -> """{"responseStatus":200,"responseData":{"translatedText":"EN($q)"}}"""
            }
            return ByteArrayInputStream(cuerpo.toByteArray())
        }
    }

    /** Cada prueba empieza con el límite diario disponible (una prueba lo agota a propósito). */
    @Before
    fun cuotaDisponible() {
        Traductor.agotadoHasta = 0L
    }

    private fun esperarPeticion(q: String) {
        val fin = System.currentTimeMillis() + 5_000
        while (peticionesDe(q) == 0 && System.currentTimeMillis() < fin) Thread.sleep(10)
    }

    @Test
    fun exito_traduceYGuarda() = runBlocking {
        assertNull(Traductor.guardada("Auto rojo"))
        assertEquals("EN(Auto rojo)", Traductor.alIngles("  Auto rojo "))
        assertEquals("EN(Auto rojo)", Traductor.guardada("Auto rojo"))
        // La segunda vez sale de la caché, sin otra petición.
        assertEquals("EN(Auto rojo)", Traductor.alIngles("Auto rojo"))
        assertEquals(1, peticionesDe("Auto rojo"))
    }

    @Test
    fun cuotaAgotada_regresaNullYNoGuarda() = runBlocking {
        assertNull(Traductor.alIngles("Texto CUOTA"))
        assertNull(Traductor.guardada("Texto CUOTA"))
        assertNull(Traductor.alIngles("Texto LIMITE"))
        assertNull(Traductor.guardada("Texto LIMITE"))
    }

    @Test
    fun dosLlamadasSimultaneas_unaSolaPeticion() = runBlocking {
        val q = "Camioneta simultanea"
        val c = CountDownLatch(1).also { compuerta = it }
        try {
            val llamadas = List(2) { async(Dispatchers.Default) { Traductor.alIngles(q) } }
            esperarPeticion(q)
            delay(200) // la segunda ya está esperando la misma petición
            c.countDown()
            assertEquals(listOf("EN($q)", "EN($q)"), llamadas.awaitAll())
            assertEquals(1, peticionesDe(q))
        } finally {
            compuerta = null
        }
    }

    @Test
    fun siSeCancelaQuienPidio_laTraduccionSeGuardaIgual() = runBlocking {
        val q = "Sedan cancelado"
        val c = CountDownLatch(1).also { compuerta = it }
        try {
            val trabajo = launch(Dispatchers.Default) { Traductor.alIngles(q) }
            esperarPeticion(q)
            trabajo.cancel()
            c.countDown()
            val fin = System.currentTimeMillis() + 5_000
            while (Traductor.guardada(q) == null && System.currentTimeMillis() < fin) delay(10)
            assertEquals("EN($q)", Traductor.guardada(q))
        } finally {
            compuerta = null
        }
    }

    @Test
    fun corto_traduceSoloElInicioYAgregaPuntosSiSeCorto() = runBlocking {
        assertEquals("EN(Corto)", Traductor.alIngles("Corto", corto = true))
        val largo = List(30) { "carro" }.joinToString(" ")
        val inicio = List(16) { "carro" }.joinToString(" ")
        assertEquals("EN($inicio)…", Traductor.alIngles(largo, corto = true))
        assertEquals("EN($inicio)…", Traductor.guardada(largo, corto = true))
        assertNull(Traductor.guardada("Nunca pedido", corto = true))
    }

    @Test
    fun cuotaAgotada_noSeVuelveAPedirNadaHastaQuePaseUnaHora() = runBlocking {
        assertNull(Traductor.alIngles("Otra vez CUOTA"))
        assertTrue(Traductor.agotadoHasta > System.currentTimeMillis())
        // Mientras esté agotada, otro texto no hace petición y se queda en el original.
        assertNull(Traductor.alIngles("Pickup sin pedir"))
        assertEquals(0, peticionesDe("Pickup sin pedir"))
        // Lo que ya estaba guardado sí se sigue mostrando.
        Traductor.agotadoHasta = 0L
        assertEquals("EN(Hatchback guardado)", Traductor.alIngles("Hatchback guardado"))
        Traductor.agotadoHasta = System.currentTimeMillis() + 60_000
        assertEquals("EN(Hatchback guardado)", Traductor.alIngles("Hatchback guardado"))
    }

    @Test
    fun corto_conCuotaAgotada_noPideNadaYRegresaNull() = runBlocking {
        Traductor.agotadoHasta = System.currentTimeMillis() + 60_000
        val largo = List(30) { "sedan" }.joinToString(" ")
        val inicio = List(16) { "sedan" }.joinToString(" ")
        assertNull(Traductor.alIngles(largo, corto = true))
        assertNull(Traductor.guardada(largo, corto = true))
        assertEquals(0, peticionesDe(inicio))
        assertEquals(0, peticionesDe(largo))
    }

    @Test
    fun corto_conTextoQueCabe_noAgregaPuntos() = runBlocking {
        assertEquals("EN(Versa 2020 impecable)", Traductor.alIngles("  Versa 2020 impecable ", corto = true))
        assertEquals("EN(Versa 2020 impecable)", Traductor.guardada("Versa 2020 impecable", corto = true))
    }
}
