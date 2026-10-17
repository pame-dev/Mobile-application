package com.pame.karsy.feature.admin

import com.pame.karsy.R
import com.pame.karsy.core.util.Xlsx
import com.pame.karsy.data.repository.AdminStats
import com.pame.karsy.data.repository.ReporteApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/** Pruebas de la parte pura del reporte del panel de administración (ExportarAdmin). */
class ExportarAdminTest {

    // ---------- utilidades ----------

    /** id de recurso → nombre (p. ej. "admin3_users"). */
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

    private val E = Xlsx.Estilo

    private fun stats(
        ce: List<String> = listOf("10 oct", "11 oct", "12 oct"),
        ie: List<String> = listOf("10 oct", "11 oct"),
    ) = AdminStats(
        usuarios = 15, usuariosNuevos = 5, lotes = 3, lotesNuevos = 1, publicadas = 25, publicadasNuevas = 5, activas = 20,
        deshabilitadas = 1, deshabilitadasSinVer = 0, pausadasSinVer = 0, suspendidasSinVer = 0, suspendidas = 1,
        propuestasPendientes = 1, reportesPendientes = 2, destacadosPendientes = 1,
        crecimientoEtiquetas = ce,
        crecimientoUsuarios = listOf(10f, 12f, 15f),
        crecimientoLotes = listOf(2f, 2f, 3f),
        crecimientoPublicaciones = listOf(20f, 22f, 25f),
        interaccionesEtiquetas = ie,
        vistasPorMes = listOf(40, 55),
        contactosPorMes = listOf(3, 5),
    )

    // Mismos datos que gen.mjs de la web.
    private fun reporte(
        horas: Double? = 13.5,
        porDia: List<Int> = listOf(10, 5, 4, 6, 8, 20, 35),
        porHora: List<Int> = (0 until 24).map { if (it == 20) 30 else it % 5 },
        marcas: List<Pair<String, Int>> = listOf("Nissan" to 8, "Ford" to 5),
        estados: List<Pair<String, Int>> = listOf("Coahuila" to 12),
        masVistas: List<ReporteApp.MasVista> = listOf(
            ReporteApp.MasVista(1, "Ford Escape 2022", "Autos Norte", 59, 5),
            ReporteApp.MasVista(2, "Nissan Versa 2026", "Pame", 29, 1),
        ),
        motivos: List<Pair<String, Int>> = listOf("Publicación: Precio falso" to 3, "Fotos" to 2),
        porFecha: List<Pair<String, Int>> = listOf("10 oct" to 4, "11 oct" to 0, "12 oct" to 7),
    ) = ReporteApp(
        usuarios = mapOf("total" to 15, "nuevos" to 5, "particulares" to 12, "lotes" to 3, "suspendidos" to 1),
        publicaciones = mapOf(
            "publicadas" to 25, "nuevas" to 5, "activas" to 20, "pausadas" to 2, "deshabilitadas" to 1,
            "vendidas" to 3, "vendidas_dentro" to 2, "vendidas_fuera" to 1,
        ),
        interes = mapOf("vistas" to 95, "contactos" to 8),
        favoritos = 12,
        destacados = mapOf("pendientes" to 1, "vigentes" to 2, "aprobados" to 2, "rechazados" to 0),
        reportes = mapOf("pendientes" to 2, "recibidos" to 5, "resueltos" to 3),
        revision = mapOf("pendientes" to 1, "aprobadas" to 6, "rechazadas" to 2),
        horasRevision = horas,
        marcas = marcas, estados = estados, masVistas = masVistas,
        porFecha = porFecha, porDia = porDia, porHora = porHora, motivos = motivos,
    )

    private fun libro(r: ReporteApp = reporte(), s: AdminStats = stats(), t: (Int, Array<out Any>) -> String = tNombre) =
        ExportarAdmin.libro(s, r, "16/10/2026", 30, t)

    // ---------- fraccion ----------

    @Test
    fun fraccion_divideYCeroSinBase() {
        assertEquals(0.25, ExportarAdmin.fraccion(1, 4), 1e-9)
        assertEquals(0.0, ExportarAdmin.fraccion(5, 0), 0.0)
        assertEquals(0.0, ExportarAdmin.fraccion(0, 10), 0.0)
        assertEquals(1.5, ExportarAdmin.fraccion(3, 2), 1e-9)
    }

    // ---------- secciones ----------

    @Test
    fun secciones_sieteConSusTitulos() {
        val s = ExportarAdmin.secciones(reporte(), tNombre)
        assertEquals(
            listOf("admin3_users", "admin3_listings", "admin3_interest", "admin3_review", "admin3_featured", "admin3_reports", "admin3_activity"),
            s.map { nombres.getValue(it.first) },
        )
        assertEquals(listOf(5, 8, 3, 4, 4, 3, 2), s.map { it.second.size })
    }

    @Test
    fun secciones_valoresDesdeLosMapas() {
        val s = ExportarAdmin.secciones(reporte(), tNombre).associate { nombres.getValue(it.first) to it.second }
        assertEquals(listOf(15, 5, 12, 3, 1), s.getValue("admin3_users").map { it.valor })
        assertEquals(listOf(25, 5, 20, 2, 1, 3, 2, 1), s.getValue("admin3_listings").map { it.valor })
        val interes = s.getValue("admin3_interest")
        assertEquals(listOf(95, 12, 8), interes.map { it.valor })
        assertNull(interes[0].pct)
        assertEquals(12.0 / 95, interes[1].pct!!, 1e-9)
        assertEquals(8.0 / 95, interes[2].pct!!, 1e-9)
        assertEquals(listOf<Any>(1, 6, 2, 13.5), s.getValue("admin3_review").map { it.valor })
        assertEquals(listOf(1, 2, 2, 0), s.getValue("admin3_featured").map { it.valor })
        assertEquals(listOf(2, 5, 3), s.getValue("admin3_reports").map { it.valor })
        // Pico: sábado (35) y 20:00–21:00 (30).
        assertEquals(listOf("profile_dashboard_day_sat", "20:00–21:00"), s.getValue("admin3_activity").map { it.valor })
        assertEquals("admin3_sold_inside", s.getValue("admin3_listings")[6].etiqueta)
    }

    @Test
    fun secciones_claveFaltanteEsCero() {
        val r = reporte().copy(usuarios = emptyMap(), interes = emptyMap())
        val s = ExportarAdmin.secciones(r, tNombre)
        assertEquals(listOf(0, 0, 0, 0, 0), s[0].second.map { it.valor })
        // Sin vistas: los % quedan en 0 (sin dividir entre cero).
        assertEquals(0.0, s[2].second[1].pct!!, 0.0)
        assertEquals(0.0, s[2].second[2].pct!!, 0.0)
    }

    @Test
    fun secciones_guionSinHorasNiPico() {
        val r = reporte(horas = null, porDia = List(7) { 0 }, porHora = List(24) { 0 })
        val s = ExportarAdmin.secciones(r, tNombre)
        assertEquals("—", s[3].second[3].valor)
        assertEquals(listOf("—", "—"), s[6].second.map { it.valor })
        // Listas vacías tampoco fallan.
        val vacias = ExportarAdmin.secciones(reporte(porDia = emptyList(), porHora = emptyList()), tNombre)
        assertEquals(listOf("—", "—"), vacias[6].second.map { it.valor })
    }

    // ---------- libro ----------

    @Test
    fun libro_seisHojasConSusNombres() {
        assertEquals(
            listOf("admin3_sheet_summary", "admin3_sheet_growth", "admin3_sheet_top", "admin3_sheet_brands", "admin3_reports", "admin3_activity"),
            libro().map { it.nombre },
        )
        assertEquals(
            listOf("Resumen", "Crecimiento", "Más vistas", "Marcas y estados", "Reportes", "Actividad"),
            libro(t = tEs).map { it.nombre },
        )
    }

    @Test
    fun libro_resumenTituloSeccionesYEstilos() {
        val h = libro()[0]
        assertEquals(listOf("A1:C1", "A2:C2"), h.unir)
        assertEquals("admin3_report_title", h.filas[0][0]!!.v)
        assertEquals(E.TITULO, h.filas[0][0]!!.s)
        assertEquals("admin3_report_sub(30,16/10/2026)", h.filas[1][0]!!.v)
        // 2 de título + 7 × (vacía + sección) + 29 filas de datos
        assertEquals(2 + 7 * 2 + 29, h.filas.size)
        val secciones = h.filas.filter { it.size == 1 && it[0]!!.s == E.SECCION }.map { it[0]!!.v }
        assertEquals(7, secciones.size)
        val porEtiqueta = h.filas.filter { it.size >= 2 }.associate { it[0]!!.v to it }
        val fav = porEtiqueta.getValue("admin3_favorites")
        assertEquals(listOf<Any?>(12, 12.0 / 95), listOf(fav[1]!!.v, fav[2]!!.v))
        assertEquals(listOf(E.ETIQUETA, E.ENTERO, E.PCT), fav.map { it!!.s })
        val horas = porEtiqueta.getValue("admin3_avg_hours")
        assertEquals(13.5, horas[1]!!.v)
        assertEquals(E.DECIMAL, horas[1]!!.s)
        assertEquals(2, horas.size)
        assertEquals(E.TEXTO, porEtiqueta.getValue("admin3_best_hour")[1]!!.s)
        // Horas enteras van como entero; sin horas, texto "—".
        val enteras = libro(reporte(horas = 4.0))[0].filas.first { it.getOrNull(0)?.v == "admin3_avg_hours" }
        assertEquals(E.ENTERO, enteras[1]!!.s)
        val sin = libro(reporte(horas = null))[0].filas.first { it.getOrNull(0)?.v == "admin3_avg_hours" }
        assertEquals("—", sin[1]!!.v)
        assertEquals(E.TEXTO, sin[1]!!.s)
    }

    @Test
    fun libro_encabezadosDeCadaHoja() {
        val h = libro()
        fun cab(i: Int) = h[i].filas.first().map { it?.v }
        assertEquals(
            listOf("admin3_date", "admin3_legend_users", "admin3_legend_lots", "admin3_legend_listings", null, "admin3_period", "admin3_views", "admin3_contacts"),
            cab(1),
        )
        assertEquals(listOf("admin3_listing", "admin3_seller", "admin3_views", "admin3_contacts", "admin3_conversion"), cab(2))
        assertEquals(listOf("admin3_brand", "admin3_active_listings", null, "admin3_state", "admin3_active_listings"), cab(3))
        assertEquals(listOf("admin3_reason", "admin3_reports"), cab(4))
        assertEquals(listOf("admin3_date", "admin3_views", null, "admin3_day", "admin3_views", null, "admin3_hour", "admin3_views"), cab(5))
        (1..5).forEach { i ->
            assertTrue(h[i].filas.first().filterNotNull().all { it.s == E.ENCABEZADO })
            assertEquals(1, h[i].fijas)
        }
    }

    @Test
    fun libro_crecimientoLadoALadoConSeriesDeDistintoLargo() {
        val f = libro()[1].filas
        assertEquals(1 + 3, f.size)
        assertEquals(listOf<Any?>("10 oct", 10, 2, 20, null, "10 oct", 40, 3), f[1].map { it?.v })
        assertEquals(listOf<Any?>("11 oct", 12, 2, 22, null, "11 oct", 55, 5), f[2].map { it?.v })
        // La serie de interés es más corta: su lado queda vacío.
        assertEquals(listOf<Any?>("12 oct", 15, 3, 25, null, null, null, null), f[3].map { it?.v })
        // Al revés: crecimiento más corto que interés.
        val g = libro(s = stats(ce = listOf("10 oct"), ie = listOf("10 oct", "11 oct")))[1].filas
        assertEquals(1 + 2, g.size)
        assertEquals(listOf<Any?>(null, null, null, null, null, "11 oct", 55, 5), g[2].map { it?.v })
        // Sin datos: solo encabezado.
        assertEquals(1, libro(s = stats(ce = emptyList(), ie = emptyList()))[1].filas.size)
    }

    @Test
    fun libro_masVistasConConversionComoFraccion() {
        val f = libro()[2].filas
        assertEquals(3, f.size)
        assertEquals(listOf<Any?>("Ford Escape 2022", "Autos Norte", 59, 5, 5.0 / 59), f[1].map { it?.v })
        assertEquals(listOf(E.NEGRITA, E.TEXTO, E.ENTERO, E.ENTERO, E.PCT), f[1].map { it!!.s })
        // Sin vistas: conversión 0 (no divide entre cero).
        val cero = libro(reporte(masVistas = listOf(ReporteApp.MasVista(9, "X", "Y", 0, 2))))[2].filas
        assertEquals(0.0, cero[1][4]!!.v)
    }

    @Test
    fun libro_marcasYEstadosDeDistintoLargo() {
        val f = libro()[3].filas
        assertEquals(1 + 2, f.size)
        assertEquals(listOf<Any?>("Nissan", 8, null, "Coahuila", 12), f[1].map { it?.v })
        assertEquals(listOf<Any?>("Ford", 5, null, null, null), f[2].map { it?.v })
        val g = libro(reporte(marcas = listOf("Kia" to 1), estados = listOf("Jalisco" to 4, "Nuevo León" to 2)))[3].filas
        assertEquals(listOf<Any?>(null, null, null, "Nuevo León", 2), g[2].map { it?.v })
    }

    @Test
    fun libro_sinDatosCuandoLasListasEstanVacias() {
        val h = libro(reporte(marcas = emptyList(), estados = emptyList(), masVistas = emptyList(), motivos = emptyList()))
        assertEquals(listOf<Any?>("admin3_no_data"), h[2].filas[1].map { it?.v })
        assertEquals(2, h[2].filas.size)
        assertEquals(listOf<Any?>("admin3_no_data"), h[4].filas[1].map { it?.v })
        assertEquals(2, h[4].filas.size)
        // Marcas y estados vacíos: solo el encabezado (igual que la web).
        assertEquals(1, h[3].filas.size)
    }

    @Test
    fun libro_reportesConMotivos() {
        val f = libro()[4].filas
        assertEquals(listOf<Any?>("Publicación: Precio falso", 3), f[1].map { it?.v })
        assertEquals(listOf<Any?>("Fotos", 2), f[2].map { it?.v })
    }

    @Test
    fun libro_actividadCon24Filas() {
        val f = libro()[5].filas
        assertEquals(1 + 24, f.size)
        // Columnas: fecha | vistas | · | día | vistas | · | hora | vistas
        assertEquals(listOf<Any?>("10 oct", 4, null, "profile_dashboard_day_sun", 10, null, "00:00–01:00", 0), f[1].map { it?.v })
        assertEquals(listOf<Any?>("12 oct", 7, null, "profile_dashboard_day_tue", 4, null, "02:00–03:00", 2), f[3].map { it?.v })
        assertEquals(listOf<Any?>(null, null, null, "profile_dashboard_day_sat", 35, null, "06:00–07:00", 1), f[7].map { it?.v })
        assertEquals(listOf<Any?>(null, null, null, null, null, null, "07:00–08:00", 2), f[8].map { it?.v })
        assertEquals(listOf<Any?>(null, null, null, null, null, null, "20:00–21:00", 30), f[21].map { it?.v })
        assertEquals(listOf<Any?>(null, null, null, null, null, null, "23:00–00:00", 3), f[24].map { it?.v })
        // Series cortas: lo que falta es 0.
        val g = libro(reporte(porDia = listOf(1), porHora = emptyList()))[5].filas
        assertEquals(0, g[2][4]!!.v)
        assertEquals(0, g[24][7]!!.v)
        // 30 días: una fila por fecha (más que las 24 horas).
        val treinta = libro(reporte(porFecha = (1..30).map { "$it sep" to it }))[5].filas
        assertEquals(1 + 30, treinta.size)
        assertEquals(listOf<Any?>("30 sep", 30), treinta[30].take(2).map { it?.v })
    }

    @Test
    fun escribir_generaXlsxValidoConSeisHojas() {
        val hojas = libro(t = tEs)
        val f = File.createTempFile("admin", ".xlsx")
        try {
            f.outputStream().use { Xlsx.escribir(hojas, it) }
            ZipFile(f).use { zip ->
                val nombresZip = zip.entries().toList().map { it.name }.toSet()
                val esperados = setOf(
                    "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels", "xl/styles.xml",
                ) + (1..6).map { "xl/worksheets/sheet$it.xml" }
                assertEquals(esperados, nombresZip)
                val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                nombresZip.forEach { n -> zip.getInputStream(zip.getEntry(n)).use { db.parse(it) } }
                val wb = zip.getInputStream(zip.getEntry("xl/workbook.xml")).bufferedReader().readText()
                listOf("Resumen", "Crecimiento", "Más vistas", "Marcas y estados", "Reportes", "Actividad")
                    .forEach { assertTrue(it, wb.contains("name=\"$it\"")) }
                val s1 = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).bufferedReader().readText()
                assertTrue(s1.contains("Reporte de Karsy · Administración"))
                assertTrue(s1.contains("Últimos 30 días · 16/10/2026"))
            }
            val salida = System.getProperty("xlsx.salida.admin")?.let { File(it) } ?: File("build/test-xlsx/admin-app.xlsx")
            salida.parentFile?.mkdirs()
            f.copyTo(salida, overwrite = true)
        } finally {
            f.delete()
        }
    }
}
