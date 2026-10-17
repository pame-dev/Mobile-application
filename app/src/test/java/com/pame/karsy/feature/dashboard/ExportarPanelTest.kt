package com.pame.karsy.feature.dashboard

import com.pame.karsy.core.util.Xlsx
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.repository.RendimientoPublicacion
import com.pame.karsy.data.repository.SellerStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportarPanelTest {

    private fun car(
        id: Long,
        brand: String = "Marca$id",
        model: String = "Modelo$id",
        year: Int = 2020,
        price: String = "100",
        priceValue: Double = 100.0,
        currency: String = "MXN",
        status: String = "Activo",
        approved: Boolean = true,
        publishedAt: String = "2026-05-01T10:20:30+00:00",
    ) = Car(
        id = id, brand = brand, model = model, year = year, price = price, priceValue = priceValue, currency = currency,
        description = "", imageUrl = null, status = status, approved = approved, publishedAt = publishedAt,
    )

    private fun stats(vararg r: RendimientoPublicacion) = SellerStats(
        publicadas = 0, activas = 3, vendidas = 1, pendientes = 2, vistas = 0, contactos = 0,
        vistasMes = 0, vistasMesAnterior = 0, vistasPorMes = emptyList(), favoritosSemana = emptyList(),
        diasSemana = emptyList(), embudoVistas = 50, embudoFavoritos = 10, embudoContactos = 5,
        rendimiento = r.toList(),
    )

    /** Traductor de prueba: "#<id>" (con sus argumentos), para no depender de Android. */
    private val t: (Int, Array<out Any>) -> String = { id, a -> "#$id" + if (a.isEmpty()) "" else a.joinToString(",", ":") }

    private val info = ExportarPanel.Info("Autos Pame", "16 de octubre de 2026", 1, (1..12).map { "m$it" })

    /** Hoja "Publicaciones" del libro (la segunda). */
    private fun publicaciones(s: SellerStats, cars: List<Car>) = ExportarPanel.libro(s, cars, null, info, t)[1]

    /** Valor de la celda [col] de cada fila de datos (sin el encabezado). */
    private fun columna(h: Xlsx.Hoja, col: Int) = h.filas.drop(1).map { it.getOrNull(col)?.v }

    // ---------- filas ----------

    @Test
    fun filas_soloEntranAprobadas() {
        val r = ExportarPanel.filas(stats(), listOf(car(1), car(2, approved = false), car(3)))
        assertEquals(listOf(1L, 3L), r.map { it.first.id })
    }

    @Test
    fun filas_sinDatosVanEnCero() {
        val r = ExportarPanel.filas(stats(RendimientoPublicacion(99, 7, 7, 7)), listOf(car(1)))
        assertEquals(RendimientoPublicacion(1, 0, 0, 0), r.single().second)
    }

    @Test
    fun filas_ordenPorVistasYLuegoContactos() {
        val s = stats(
            RendimientoPublicacion(1, 5, 0, 1),
            RendimientoPublicacion(2, 10, 0, 0),
            RendimientoPublicacion(3, 5, 0, 4),
        )
        val r = ExportarPanel.filas(s, listOf(car(1), car(2), car(3), car(4)))
        assertEquals(listOf(2L, 3L, 1L, 4L), r.map { it.first.id })
    }

    @Test
    fun filas_listaVacia() {
        assertTrue(ExportarPanel.filas(stats(RendimientoPublicacion(1, 1, 1, 1)), emptyList()).isEmpty())
    }

    // ---------- porcentaje ----------

    @Test
    fun porcentaje_baseCeroDaCero() {
        assertEquals(0, ExportarPanel.porcentaje(5, 0))
    }

    @Test
    fun porcentaje_seLimitaA100() {
        assertEquals(100, ExportarPanel.porcentaje(15, 10))
    }

    @Test
    fun porcentaje_normal() {
        assertEquals(25, ExportarPanel.porcentaje(1, 4))
        assertEquals(100, ExportarPanel.porcentaje(4, 4))
        // Redondea igual que la web: 2 de 3 = 67 %.
        assertEquals(67, ExportarPanel.porcentaje(2, 3))
    }

    // ---------- libro de Excel (antes CSV) ----------

    private fun libroEjemplo(): List<Xlsx.Hoja> {
        val s = stats(
            RendimientoPublicacion(1, 4, 2, 1),
            RendimientoPublicacion(2, 10, 3, 5),
        )
        val cars = listOf(
            car(1, brand = "Nissan", model = "Versa", year = 2019, price = "\$250,000", priceValue = 250000.0, status = "Activo"),
            car(2, brand = "Ford", model = "Lobo \"XL\"", year = 0, price = "300000", priceValue = 300000.0, currency = "USD", status = "Vendido",
                publishedAt = "2026-03-15"),
            car(3, approved = false),
        )
        return ExportarPanel.libro(s, cars, null, info, t)
    }

    @Test
    fun libro_cincoHojas() {
        assertEquals(5, libroEjemplo().size)
    }

    @Test
    fun libro_xlsxEsZipConSusArchivos() {
        val bytes = java.io.ByteArrayOutputStream().also { Xlsx.escribir(libroEjemplo(), it) }.toByteArray()
        val nombres = mutableListOf<String>()
        java.util.zip.ZipInputStream(bytes.inputStream()).use { z -> generateSequence { z.nextEntry }.forEach { nombres += it.name } }
        assertTrue(nombres.containsAll(listOf("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet5.xml")))
    }

    @Test
    fun libro_estructuraPublicaciones() {
        val h = libroEjemplo()[1]
        // Encabezado + 2 aprobadas (la no aprobada que no está pendiente no entra).
        assertEquals(3, h.filas.size)
        assertEquals(Xlsx.Estilo.ENCABEZADO, h.filas[0][0]?.s)
        // Más vista primero; año 0 va vacío; las comillas se guardan tal cual.
        assertEquals(listOf("Ford Lobo \"XL\"", "Nissan Versa 2019"), columna(h, 0))
        assertEquals(listOf(null, 2019), columna(h, 1))
        // Precio como número (con formato de moneda) para que Excel lo sume y ordene.
        assertEquals(listOf(300000.0, 250000.0), columna(h, 3))
        assertEquals(Xlsx.Estilo.DINERO, h.filas[1][3]?.s)
        // Fecha de publicación como número de fecha de Excel.
        assertEquals(listOf(Xlsx.fechaExcel("2026-03-15"), Xlsx.fechaExcel("2026-05-01")), columna(h, 13))
        assertEquals("A1:Q3", h.filtro)
        assertEquals(1, h.fijas)
    }

    @Test
    fun libro_comillasSeEscapanEnElXml() {
        val xml = Xlsx.archivos(libroEjemplo())["xl/worksheets/sheet2.xml"]!!
        assertTrue(xml.contains("Ford Lobo &quot;XL&quot;"))
    }

    @Test
    fun libro_tituloConComaQuedaIgual() {
        val h = publicaciones(stats(), listOf(car(1, brand = "Kia", model = "Rio, Sedán")))
        assertEquals("Kia Rio, Sedán 2020", columna(h, 0).single())
    }

    @Test
    fun libro_fechaVaciaNoFalla() {
        val h = publicaciones(stats(), listOf(car(1, publishedAt = "")))
        assertEquals(null, columna(h, 13).single())
    }

    @Test
    fun libro_sinPublicacionesSoloEncabezado() {
        val h = publicaciones(stats(), emptyList())
        assertEquals(1, h.filas.size)
    }

    // ---------- diasDesde ----------

    /** "Hoy" fijo: 16 oct 2026 a mediodía (hora local, igual que el parseo de diasDesde). */
    private val hoy: Long =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse("2026-10-16")!!.time + 12 * 3_600_000L

    @Test
    fun diasDesde_isoLargo() {
        assertEquals(10, ExportarPanel.diasDesde("2026-10-06T23:59:59+00:00", hoy))
        assertEquals(10, ExportarPanel.diasDesde("2026-10-06", hoy))
        assertEquals(0, ExportarPanel.diasDesde("2026-10-16T08:00:00Z", hoy))
    }

    @Test
    fun diasDesde_vaciaOCortaDaCero() {
        assertEquals(0, ExportarPanel.diasDesde("", hoy))
        assertEquals(0, ExportarPanel.diasDesde("2026-10", hoy))
    }

    @Test
    fun diasDesde_fechaFuturaDaCero() {
        assertEquals(0, ExportarPanel.diasDesde("2027-01-01", hoy))
    }

    @Test
    fun diasDesde_fechaInvalidaDaCero() {
        assertEquals(0, ExportarPanel.diasDesde("no-es-fecha", hoy))
    }

    // ---------- resumen ----------

    private val vieja = "2026-09-01T00:00:00+00:00" // 45 días antes de hoy
    private val reciente = "2026-10-14" // 2 días antes de hoy

    @Test
    fun resumen_masVistaIgnoraCeroVistas() {
        val r = ExportarPanel.resumen(stats(RendimientoPublicacion(2, 3, 0, 0)), listOf(car(1), car(2)), hoy)
        assertEquals(2L, r.masVista?.id)
        val nadie = ExportarPanel.resumen(stats(), listOf(car(1), car(2)), hoy)
        assertEquals(null, nadie.masVista)
    }

    @Test
    fun resumen_masContactada() {
        val s = stats(
            RendimientoPublicacion(1, 50, 0, 1),
            RendimientoPublicacion(2, 10, 0, 4),
        )
        assertEquals(2L, ExportarPanel.resumen(s, listOf(car(1), car(2)), hoy).masContactada?.id)
    }

    @Test
    fun resumen_masContactadaNullSinContactos() {
        val s = stats(RendimientoPublicacion(1, 50, 3, 0))
        assertEquals(null, ExportarPanel.resumen(s, listOf(car(1), car(2)), hoy).masContactada)
    }

    @Test
    fun resumen_sinVistasCuentaSoloAprobadas() {
        val s = stats(RendimientoPublicacion(1, 5, 0, 1))
        val r = ExportarPanel.resumen(s, listOf(car(1), car(2), car(3), car(4, approved = false)), hoy)
        assertEquals(2, r.sinVistas)
    }

    @Test
    fun resumen_consejoSinContacto() {
        val s = stats(
            RendimientoPublicacion(1, 20, 2, 0), // justo 20: sí
            RendimientoPublicacion(2, 19, 0, 0), // 19: no (y es reciente)
            RendimientoPublicacion(3, 40, 0, 1), // tiene contacto: no
        )
        val cars = listOf(car(1, publishedAt = reciente), car(2, publishedAt = reciente), car(3, publishedAt = reciente))
        val r = ExportarPanel.resumen(s, cars, hoy)
        assertEquals(listOf(ExportarPanel.TipoConsejo.SinContacto to 1L), r.consejos.map { it.first to it.second.id })
    }

    @Test
    fun resumen_consejoSinContactoTambienParaNoActivas() {
        // La regla de 20+ vistas sin contacto no revisa el estado (igual que la web).
        val s = stats(RendimientoPublicacion(1, 30, 0, 0))
        val r = ExportarPanel.resumen(s, listOf(car(1, status = "Pausado")), hoy)
        assertEquals(ExportarPanel.TipoConsejo.SinContacto, r.consejos.single().first)
    }

    @Test
    fun resumen_consejoPocasVistas() {
        val s = stats(
            RendimientoPublicacion(1, 4, 0, 0), // vieja, 4 vistas: sí
            RendimientoPublicacion(2, 5, 0, 0), // 5 vistas: no
            RendimientoPublicacion(3, 0, 0, 0), // reciente: no
        )
        val cars = listOf(car(1, publishedAt = vieja), car(2, publishedAt = vieja), car(3, publishedAt = reciente))
        val r = ExportarPanel.resumen(s, cars, hoy)
        assertEquals(listOf(ExportarPanel.TipoConsejo.PocasVistas to 1L), r.consejos.map { it.first to it.second.id })
    }

    @Test
    fun resumen_pocasVistasJustoSieteDias() {
        val r = ExportarPanel.resumen(stats(), listOf(car(1, publishedAt = "2026-10-09"), car(2, publishedAt = "2026-10-10")), hoy)
        assertEquals(listOf(1L), r.consejos.map { it.second.id })
    }

    @Test
    fun resumen_pocasVistasSoloActivas() {
        val cars = listOf("Pausado", "Vendido", "Pendiente", "Deshabilitado", "Rechazado").mapIndexed { i, st ->
            car(i + 1L, status = st, publishedAt = vieja)
        }
        assertTrue(ExportarPanel.resumen(stats(), cars, hoy).consejos.isEmpty())
    }

    @Test
    fun resumen_sinFechaNoDaPocasVistas() {
        assertTrue(ExportarPanel.resumen(stats(), listOf(car(1, publishedAt = "")), hoy).consejos.isEmpty())
    }

    @Test
    fun resumen_unConsejoPorAutoYMaximoTres() {
        val s = stats(
            RendimientoPublicacion(1, 30, 0, 0),
            RendimientoPublicacion(2, 25, 0, 0),
            RendimientoPublicacion(3, 2, 0, 0),
            RendimientoPublicacion(4, 1, 0, 0),
        )
        val cars = (1L..5L).map { car(it, publishedAt = vieja) }
        val r = ExportarPanel.resumen(s, cars, hoy)
        assertEquals(3, r.consejos.size)
        assertEquals(r.consejos.map { it.second.id }.distinct().size, r.consejos.size)
        // En orden de la tabla (más vistas primero).
        assertEquals(listOf(1L, 2L, 3L), r.consejos.map { it.second.id })
        assertEquals(
            listOf(ExportarPanel.TipoConsejo.SinContacto, ExportarPanel.TipoConsejo.SinContacto, ExportarPanel.TipoConsejo.PocasVistas),
            r.consejos.map { it.first },
        )
    }

    @Test
    fun resumen_noAprobadasNoEntran() {
        val s = stats(RendimientoPublicacion(1, 100, 0, 0))
        val r = ExportarPanel.resumen(s, listOf(car(1, approved = false, publishedAt = vieja)), hoy)
        assertEquals(null, r.masVista)
        assertEquals(0, r.sinVistas)
        assertTrue(r.consejos.isEmpty())
    }

    @Test
    fun libro_precioDecimalYRetornoDeCarro() {
        val h = publicaciones(stats(), listOf(car(1, brand = "Mazda", model = "3\rHB", priceValue = 199999.5)))
        assertEquals("Mazda 3\rHB 2020", columna(h, 0).single())
        assertEquals(199999.5, columna(h, 3).single())
        assertTrue(Xlsx.archivos(listOf(h))["xl/worksheets/sheet1.xml"]!!.contains("<v>199999.5</v>"))
    }
}
