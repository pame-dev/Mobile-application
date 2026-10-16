package com.pame.karsy.feature.dashboard

import com.pame.karsy.R
import com.pame.karsy.core.util.Xlsx
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.repository.DestacadoPublicacion
import com.pame.karsy.data.repository.MercadoPublicacion
import com.pame.karsy.data.repository.RendimientoPublicacion
import com.pame.karsy.data.repository.ReporteExtra
import com.pame.karsy.data.repository.SellerStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/** Pruebas del libro de Excel (.xlsx) de Mi panel y de la lógica nueva del reporte. */
class ExportarPanelLibroTest {

    // ---------- utilidades ----------

    private fun car(
        id: Long,
        brand: String = "Marca$id",
        model: String = "Modelo$id",
        year: Int = 2020,
        priceValue: Double = 100.0,
        currency: String = "MXN",
        status: String = "Activo",
        approved: Boolean = true,
        publishedAt: String = "2026-05-01T10:20:30+00:00",
        featured: Boolean = false,
        featuredUntil: String? = null,
        soldVia: String? = null,
        rejectedReason: String? = null,
        disabledReason: String? = null,
    ) = Car(
        id = id, brand = brand, model = model, year = year, price = priceValue.toString(), priceValue = priceValue, currency = currency,
        description = "", imageUrl = null, status = status, approved = approved, publishedAt = publishedAt, featured = featured,
        featuredUntil = featuredUntil, soldVia = soldVia, rejectedReason = rejectedReason, disabledReason = disabledReason,
    )

    private fun stats(vararg r: RendimientoPublicacion, porMes: List<Float> = emptyList()) = SellerStats(
        publicadas = 0, activas = 2, vendidas = 1, pendientes = 1, vistas = 0, contactos = 0,
        vistasMes = 0, vistasMesAnterior = 0, vistasPorMes = porMes, favoritosSemana = emptyList(),
        diasSemana = emptyList(), embudoVistas = 88, embudoFavoritos = 3, embudoContactos = 6,
        rendimiento = r.toList(),
    )

    /** id de recurso → nombre (p. ej. "profile_dashboard_xls_sheet_summary"). */
    private val nombres: Map<Int, String> = R.string::class.java.fields.associate { it.getInt(null) to it.name }

    /** Textos en español de values/strings.xml (las pruebas corren desde el módulo app/). */
    private val es: Map<String, String> by lazy {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/values/strings.xml"))
        val l = doc.getElementsByTagName("string")
        (0 until l.length).associate {
            val n = l.item(it)
            n.attributes.getNamedItem("name").nodeValue to n.textContent.replace("\\'", "'").replace("\\\"", "\"")
        }
    }

    /** Traductor "por nombre": deja ver qué recurso se usó y sus argumentos. */
    private val tNombre: (Int, Array<out Any>) -> String = { id, a ->
        nombres.getValue(id) + if (a.isEmpty()) "" else a.joinToString(",", "(", ")")
    }

    /** Traductor real en español (como hace Android con getString). */
    private val tEs: (Int, Array<out Any>) -> String = { id, a -> String.format(es.getValue(nombres.getValue(id)), *a) }

    private val info = ExportarPanel.Info("Pame", "16/10/2026", 1, (1..12).map { "m$it" })

    private val E = Xlsx.Estilo

    // ---------- datos de ejemplo (los mismos que gen.mjs de la web) ----------

    private val ejemploCars = listOf(
        car(1, brand = "Ford", model = "Escape", year = 2022, priceValue = 787800.0, publishedAt = "2026-10-01T10:00:00Z"),
        car(2, brand = "Nissan", model = "Versa", year = 2026, priceValue = 250000.0, publishedAt = "2026-10-01T10:00:00Z",
            featured = true, featuredUntil = "14 nov 2026"),
        car(3, brand = "Mazda", model = "3", year = 2023, priceValue = 787800.0, publishedAt = "2026-10-01T10:00:00Z",
            status = "Vendido", soldVia = "dentro_app"),
        car(4, brand = "Kia", model = "Rio, Sedán", year = 2022, priceValue = 787800.0, publishedAt = "2026-10-01T10:00:00Z",
            status = "Rechazado", approved = false, rejectedReason = "Falta la foto del tablero"),
    )
    private val ejemploStats = stats(
        RendimientoPublicacion(1, 59, 2, 5), RendimientoPublicacion(2, 29, 1, 0),
        porMes = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 3f, 8f, 12f, 30f, 35f),
    )
    private val ejemploExtra = ReporteExtra(
        mercado = listOf(MercadoPublicacion(1, 700000.0, 3)),
        porDia = listOf(10, 5, 4, 6, 8, 20, 35),
        porHora = (0 until 24).map { if (it == 20) 30 else it % 5 },
        destacados = listOf(DestacadoPublicacion(2, "2026-10-11T10:00:00Z", "2026-11-14T00:00:00Z", 5, 5.0, 5.0, 4, 20)),
    )

    private fun libroEjemplo(t: (Int, Array<out Any>) -> String = tNombre) =
        ExportarPanel.libro(ejemploStats, ejemploCars, ejemploExtra, info, t)

    // ---------- libro ----------

    @Test
    fun libro_cincoHojasConSusNombres() {
        val h = libroEjemplo()
        assertEquals(
            listOf(
                "profile_dashboard_xls_sheet_summary", "profile_dashboard_xls_sheet_listings", "profile_dashboard_xls_sheet_activity",
                "profile_dashboard_xls_sheet_featured", "profile_dashboard_report_tips",
            ),
            h.map { it.nombre },
        )
    }

    @Test
    fun libro_encabezadosDePublicaciones() {
        val cab = libroEjemplo()[1].filas.first()
        assertEquals(17, cab.size)
        assertTrue(cab.all { it!!.s == E.ENCABEZADO })
        assertEquals("profile_dashboard_export_col_listing", cab[0]!!.v)
        assertEquals("profile_dashboard_export_col_price", cab[3]!!.v)
        assertEquals("profile_dashboard_xls_market_avg", cab[5]!!.v)
        assertEquals("profile_dashboard_xls_vs_market", cab[6]!!.v)
        assertEquals("profile_dashboard_xls_save_rate", cab[9]!!.v)
        assertEquals("profile_dashboard_xls_featured_until", cab[14]!!.v)
        assertEquals("profile_dashboard_xls_sold_via", cab[15]!!.v)
        assertEquals("profile_dashboard_xls_reason", cab[16]!!.v)
    }

    @Test
    fun libro_unaFilaPorAprobadaMasPendientesNoAprobadas() {
        val cars = ejemploCars + car(5, status = "Pendiente", approved = false) + car(6, status = "Pausado", approved = false)
        val pubs = ExportarPanel.libro(ejemploStats, cars, ejemploExtra, info, tNombre)[1].filas
        // cabecera + 3 aprobadas + 2 pendientes no aprobadas (la pausada no aprobada no entra)
        assertEquals(1 + 3 + 2, pubs.size)
        assertEquals(listOf("Ford Escape 2022", "Nissan Versa 2026", "Mazda 3 2023", "Kia Rio, Sedán 2022", "Marca5 Modelo5 2020"),
            pubs.drop(1).map { it[0]!!.v })
        // La rechazada lleva su motivo y sin números.
        val kia = pubs[4]
        assertEquals("Falta la foto del tablero", kia[16]!!.v)
        assertNull(kia[7]!!.v)
    }

    @Test
    fun libro_precioNumeroDineroYDifMercadoFraccion() {
        val ford = libroEjemplo()[1].filas[1]
        assertEquals(787800.0, ford[3]!!.v)
        assertEquals(E.DINERO, ford[3]!!.s)
        assertEquals(700000.0, ford[5]!!.v)
        assertEquals(E.DINERO, ford[5]!!.s)
        assertEquals(0.1254, ford[6]!!.v as Double, 1e-4)
        assertEquals(E.PCT, ford[6]!!.s)
        // % guardado y tasa de contacto como fracción
        assertEquals(2.0 / 59, ford[9]!!.v as Double, 1e-9)
        assertEquals(5.0 / 59, ford[11]!!.v as Double, 1e-9)
    }

    @Test
    fun libro_fechasComoNumeroDeExcel() {
        val pubs = libroEjemplo()[1].filas
        val ford = pubs[1]
        assertEquals(Xlsx.fechaExcel("2026-10-01"), ford[13]!!.v)
        assertEquals(E.FECHA, ford[13]!!.s)
        // Nissan destacada: "hasta" sale del fin del destacado (número de fecha).
        val nissan = pubs[2]
        assertEquals(Xlsx.fechaExcel("2026-11-14"), nissan[14]!!.v)
        val des = libroEjemplo()[3].filas[1]
        assertEquals(Xlsx.fechaExcel("2026-10-11"), des[1]!!.v)
        assertEquals(Xlsx.fechaExcel("2026-11-14"), des[2]!!.v)
        assertEquals(0.8, des[4]!!.v)
        assertEquals(E.DECIMAL, des[4]!!.s)
        assertEquals(4.0, des[5]!!.v)
        assertEquals(E.DECIMAL, des[5]!!.s)
        assertEquals(4.0, des[6]!!.v as Double, 1e-9) // +400 %
    }

    @Test
    fun libro_destacadaSinFechaIsoQuedaVacia() {
        // Destacada pero sin datos del destacado (extra null): no cae al texto "14 nov 2026".
        val pubs = ExportarPanel.libro(ejemploStats, ejemploCars, null, info, tNombre)[1].filas
        val nissan = pubs.single { it[0]!!.v == "Nissan Versa 2026" }
        assertNull(nissan[14]!!.v)
        assertEquals(E.FECHA, nissan[14]!!.s)
    }

    @Test
    fun libro_picoFueraDeRangoMuestraGuion() {
        // 8 valores por día (uno de más) con el pico en el índice 7: no truena y sale "—".
        val ex = ejemploExtra.copy(porDia = listOf(0, 0, 0, 0, 0, 0, 0, 9))
        val r = ExportarPanel.libro(ejemploStats, ejemploCars, ex, info, tEs)[0].filas.filter { it.size >= 2 }.associate { it[0]!!.v to it[1]!!.v }
        assertEquals("—", r["Mejor día"])
    }

    @Test
    fun libro_filtroConRangoCorrecto() {
        val h = libroEjemplo()[1]
        assertEquals("A1:Q5", h.filtro) // 17 columnas (A..Q), cabecera + 4 filas
        assertEquals(1, h.fijas)
        val sinNada = ExportarPanel.libro(stats(), emptyList(), null, info, tNombre)[1]
        assertEquals("A1:Q1", sinNada.filtro)
    }

    @Test
    fun libro_resumenConVendidosMejorDiaYHora() {
        val r = libroEjemplo(tEs)[0].filas
        val porEtiqueta = r.filter { it.size >= 2 }.associate { it[0]!!.v to it[1]!!.v }
        val vendidas = r.single { f -> f.any { it?.v == "Vendidas por Karsy: 1 · fuera de Karsy: 0" } }
        assertEquals(1, vendidas[1]!!.v)
        assertEquals("Sábado", porEtiqueta["Mejor día"])
        assertEquals("20:00–21:00", porEtiqueta["Mejor hora"])
    }

    @Test
    fun libro_sinExtraNoFallaYUsaGuiones() {
        val h = ExportarPanel.libro(stats(), listOf(car(1)), null, info, tEs)
        val r = h[0].filas.filter { it.size >= 2 }.associate { it[0]!!.v to it[1]!!.v }
        assertEquals("—", r["Mejor día"])
        assertEquals("—", r["Mejor hora"])
        assertEquals("Todavía no has destacado ninguna publicación.", h[3].filas[1][0]!!.v)
        assertEquals(25, h[2].filas.size) // cabecera + 24 horas
    }

    @Test
    fun libro_recomendacionesIncluyenPendientesConMotivo() {
        val rec = libroEjemplo(tEs)[4].filas
        assertTrue(rec.any { it[0]!!.v == "Kia Rio, Sedán 2022" && (it[1]!!.v as String).endsWith(": Falta la foto del tablero") })
        val vacio = ExportarPanel.libro(stats(), emptyList(), null, info, tEs)[4].filas
        assertEquals("Todo en orden: no hay recomendaciones por ahora.", vacio[1][0]!!.v)
    }

    // ---------- lógica nueva ----------

    @Test
    fun difMercado_casos() {
        val ex = ReporteExtra(mercado = listOf(MercadoPublicacion(1, 100000.0, 3), MercadoPublicacion(2, 0.0, 1)))
        assertEquals(0.1, ExportarPanel.difMercado(car(1, priceValue = 110000.0), ex)!!, 1e-9)
        assertEquals(-0.2, ExportarPanel.difMercado(car(1, priceValue = 80000.0), ex)!!, 1e-9)
        assertNull(ExportarPanel.difMercado(car(2, priceValue = 1.0), ex)) // promedio 0
        assertNull(ExportarPanel.difMercado(car(1, priceValue = 0.0), ex)) // sin precio
        assertNull(ExportarPanel.difMercado(car(3), ex)) // sin similares
        assertNull(ExportarPanel.difMercado(car(1), null))
    }

    @Test
    fun pendientes_soloRechazadasPendientesYDeshabilitadas() {
        val cars = listOf("Activo", "Rechazado", "Pendiente", "Deshabilitado", "Vendido", "Pausado").mapIndexed { i, s -> car(i + 1L, status = s) }
        assertEquals(listOf(2L, 3L, 4L), ExportarPanel.pendientes(cars).map { it.id })
    }

    @Test
    fun vendidos_porKarsyYFuera() {
        val cars = listOf(
            car(1, status = "Vendido", soldVia = "dentro_app"), car(2, status = "Vendido", soldVia = "fuera_app"),
            car(3, status = "Vendido", soldVia = "fuera_app"), car(4, status = "Vendido"), car(5, soldVia = "dentro_app"),
        )
        assertEquals(ExportarPanel.Vendidos(4, 1, 2), ExportarPanel.vendidos(cars))
        assertEquals(ExportarPanel.Vendidos(0, 0, 0), ExportarPanel.vendidos(emptyList()))
    }

    @Test
    fun pico_casos() {
        assertEquals(6, ExportarPanel.pico(listOf(10, 5, 4, 6, 8, 20, 35)))
        assertEquals(1, ExportarPanel.pico(listOf(0, 7, 7))) // empate: el primero
        assertEquals(-1, ExportarPanel.pico(listOf(0, 0, 0)))
        assertEquals(-1, ExportarPanel.pico(emptyList()))
    }

    @Test
    fun rangoHora_casos() {
        assertEquals("00:00–01:00", ExportarPanel.rangoHora(0))
        assertEquals("09:00–10:00", ExportarPanel.rangoHora(9))
        assertEquals("19:00–20:00", ExportarPanel.rangoHora(19))
        assertEquals("23:00–00:00", ExportarPanel.rangoHora(23))
    }

    @Test
    fun efectoDestacado_casos() {
        val e = ExportarPanel.efectoDestacado(DestacadoPublicacion(1, "", "", 5, 5.0, 5.0, 10, 25))
        assertEquals(2.0, e.antes, 1e-9)
        assertEquals(5.0, e.durante, 1e-9)
        assertEquals(1.5, e.cambio!!, 1e-9)
        val baja = ExportarPanel.efectoDestacado(DestacadoPublicacion(1, "", "", 4, 4.0, 4.0, 8, 4))
        assertEquals(-0.5, baja.cambio!!, 1e-9)
    }

    @Test
    fun efectoDestacado_ventanasDeDistintoLargo() {
        // Publicado hace poco: la ventana "antes" es de 2 días y "durante" de 6.
        val e = ExportarPanel.efectoDestacado(DestacadoPublicacion(1, "", "", 6, 2.0, 6.0, 4, 30))
        assertEquals(2.0, e.antes, 1e-9)
        assertEquals(5.0, e.durante, 1e-9)
        assertEquals(1.5, e.cambio!!, 1e-9)
        // Días con fracción (0.5 días)
        val medio = ExportarPanel.efectoDestacado(DestacadoPublicacion(1, "", "", 1, 0.5, 0.5, 1, 3))
        assertEquals(2.0, medio.cambio!!, 1e-9)
    }

    @Test
    fun efectoDestacado_antesCeroDaCambioNull() {
        val sinVistas = ExportarPanel.efectoDestacado(DestacadoPublicacion(1, "", "", 3, 3.0, 3.0, 0, 9))
        assertEquals(0.0, sinVistas.antes, 1e-9)
        assertEquals(3.0, sinVistas.durante, 1e-9)
        assertNull(sinVistas.cambio)
        // Sin ventana "antes" (se destacó al publicarse): antes = 0 y cambio null, sin dividir entre 0.
        val sinVentana = ExportarPanel.efectoDestacado(DestacadoPublicacion(1, "", "", 3, 0.0, 3.0, 5, 9))
        assertEquals(0.0, sinVentana.antes, 1e-9)
        assertNull(sinVentana.cambio)
        val negativa = ExportarPanel.efectoDestacado(DestacadoPublicacion(1, "", "", 3, -1.0, 0.0, 5, 9))
        assertEquals(0.0, negativa.antes, 1e-9)
        assertEquals(0.0, negativa.durante, 1e-9)
        assertNull(negativa.cambio)
    }

    // ---------- consejo PrecioAlto ----------

    private val hoy: Long =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse("2026-10-16")!!.time + 12 * 3_600_000L

    private fun consejos(precio: Double, similares: Int, status: String = "Activo") = ExportarPanel.resumen(
        stats(RendimientoPublicacion(1, 10, 0, 1)),
        listOf(car(1, priceValue = precio, status = status, publishedAt = "2026-10-14")),
        hoy,
        ReporteExtra(mercado = listOf(MercadoPublicacion(1, 100000.0, similares))),
    ).consejos.map { it.first }

    @Test
    fun precioAlto_diezPorCientoYDosSimilares() {
        assertEquals(listOf(ExportarPanel.TipoConsejo.PrecioAlto), consejos(110000.0, 2))
        assertEquals(listOf(ExportarPanel.TipoConsejo.PrecioAlto), consejos(150000.0, 5))
    }

    @Test
    fun precioAlto_noSiMenosDeDiezPorCientoOUnSimilar() {
        assertTrue(consejos(109000.0, 5).isEmpty())
        assertTrue(consejos(150000.0, 1).isEmpty())
    }

    @Test
    fun precioAlto_soloActivas() {
        listOf("Pausado", "Vendido", "Deshabilitado").forEach { assertTrue(it, consejos(150000.0, 5, it).isEmpty()) }
    }

    @Test
    fun precioAlto_sinExtraNoHayConsejo() {
        val r = ExportarPanel.resumen(stats(RendimientoPublicacion(1, 10, 0, 1)), listOf(car(1, priceValue = 999999.0, publishedAt = "2026-10-14")), hoy, null)
        assertTrue(r.consejos.isEmpty())
    }

    // ---------- Xlsx ----------

    @Test
    fun fechaExcel_casos() {
        assertEquals(46311L, Xlsx.fechaExcel("2026-10-16"))
        assertEquals(46311L, Xlsx.fechaExcel("2026-10-16T23:59:59-06:00"))
        assertEquals(1L, Xlsx.fechaExcel("1899-12-31"))
        assertEquals(25569L, Xlsx.fechaExcel("1970-01-01"))
        assertEquals(Xlsx.fechaExcel("2024-02-28")!! + 1, Xlsx.fechaExcel("2024-02-29"))
        assertNull(Xlsx.fechaExcel("2026-02-29"))
        assertNull(Xlsx.fechaExcel("2026-13-01"))
        assertNull(Xlsx.fechaExcel("no-es-fecha"))
        assertNull(Xlsx.fechaExcel(""))
        assertNull(Xlsx.fechaExcel(null))
    }

    @Test
    fun columna_casos() {
        assertEquals("A", Xlsx.columna(0))
        assertEquals("Q", Xlsx.columna(16))
        assertEquals("Z", Xlsx.columna(25))
        assertEquals("AA", Xlsx.columna(26))
        assertEquals("AZ", Xlsx.columna(51))
        assertEquals("BA", Xlsx.columna(52))
    }

    @Test
    fun escribir_generaUnZipXlsxValido() {
        val hojas = libroEjemplo(tEs)
        val f = File.createTempFile("panel", ".xlsx")
        try {
            f.outputStream().use { Xlsx.escribir(hojas, it) }
            // Firma de ZIP "PK\u0003\u0004"
            val firma = f.inputStream().use { it.readNBytes(4) }
            assertEquals(listOf(0x50, 0x4B, 0x03, 0x04), firma.map { it.toInt() and 0xFF })
            ZipFile(f).use { zip ->
                val nombresZip = zip.entries().toList().map { it.name }.toSet()
                val esperados = setOf(
                    "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels", "xl/styles.xml",
                ) + (1..5).map { "xl/worksheets/sheet$it.xml" }
                assertEquals(esperados, nombresZip)
                // Cada XML está bien formado.
                val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                nombresZip.forEach { n -> zip.getInputStream(zip.getEntry(n)).use { db.parse(it) } }
                val wb = zip.getInputStream(zip.getEntry("xl/workbook.xml")).bufferedReader().readText()
                listOf("Resumen", "Publicaciones", "Actividad", "Destacados", "Recomendaciones").forEach { assertTrue(it, wb.contains("name=\"$it\"")) }
                val s2 = zip.getInputStream(zip.getEntry("xl/worksheets/sheet2.xml")).bufferedReader().readText()
                assertTrue(s2.contains("<autoFilter ref=\"A1:Q5\"/>"))
                assertTrue(s2.contains("Kia Rio, Sedán 2022"))
                val estilos = zip.getInputStream(zip.getEntry("xl/styles.xml")).bufferedReader().readText()
                assertTrue(estilos.contains("<cellXfs count=\"13\">"))
                assertEquals(13, Regex("<xf ").findAll(estilos.substringAfter("<cellXfs").substringBefore("</cellXfs>")).count())
                assertTrue(estilos.contains("#,##0.0"))
            }
            // Copia para revisarlo a mano (LibreOffice / comparación con la web).
            System.getProperty("xlsx.salida")?.let { f.copyTo(File(it), overwrite = true) }
                ?: f.copyTo(File("build/test-xlsx/panel-app.xlsx"), overwrite = true)
        } finally {
            f.delete()
        }
    }
}
