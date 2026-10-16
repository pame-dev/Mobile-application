package com.pame.karsy.feature.dashboard

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

    private val tx = ExportarPanel.TextosCsv(
        titulo = "Mi panel",
        vendedor = "Autos Pame",
        fecha = "16 de octubre de 2026",
        resumen = listOf("Activas" to 3, "Vendidas" to 1),
        columnas = listOf("Publicación", "Año", "Precio", "Moneda", "Estado", "Vistas", "Favoritos", "Contactos", "Tasa", "Publicada"),
        estado = { "E-" + it.status },
    )

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

    // ---------- csv ----------

    private fun csvEjemplo(): String {
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
        return ExportarPanel.csv(s, cars, tx)
    }

    @Test
    fun csv_empiezaConBom() {
        assertTrue(csvEjemplo().startsWith("\uFEFF"))
    }

    @Test
    fun csv_usaCrlfSinSaltosSueltos() {
        val c = csvEjemplo()
        assertTrue(c.contains("\r\n"))
        assertFalse(c.replace("\r\n", "").contains("\n"))
    }

    @Test
    fun csv_estructuraCompleta() {
        val lineas = csvEjemplo().removePrefix("\uFEFF").split("\r\n")
        val esperado = listOf(
            "Mi panel",
            "Autos Pame",
            "16 de octubre de 2026",
            "",
            "Activas,3",
            "Vendidas,1",
            "",
            "Publicación,Año,Precio,Moneda,Estado,Vistas,Favoritos,Contactos,Tasa,Publicada",
            // Más vista primero; año 0 va vacío; comillas escapadas.
            "\"Ford Lobo \"\"XL\"\"\",,300000,USD,E-Vendido,10,3,5,50%,2026-03-15",
            // Precio como número (sin formato) para Excel; fecha recortada a 10 caracteres.
            "Nissan Versa,2019,250000,MXN,E-Activo,4,2,1,25%,2026-05-01",
        )
        assertEquals(esperado, lineas)
    }

    @Test
    fun csv_celdaConComaEnTitulo() {
        val c = ExportarPanel.csv(stats(), listOf(car(1, brand = "Kia", model = "Rio, Sedán")), tx)
        assertTrue(c.contains("\r\n\"Kia Rio, Sedán\",2020,"))
    }

    @Test
    fun csv_fechaCortaOVaciaNoFalla() {
        val c = ExportarPanel.csv(stats(), listOf(car(1, publishedAt = "")), tx)
        assertTrue(c.endsWith(",0%,"))
    }

    @Test
    fun csv_sinPublicacionesTerminaEnColumnas() {
        val c = ExportarPanel.csv(stats(), emptyList(), tx)
        assertTrue(c.endsWith("Tasa,Publicada"))
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
    fun csv_precioDecimalYRetornoDeCarro() {
        val c = ExportarPanel.csv(stats(), listOf(car(1, brand = "Mazda", model = "3\rHB", priceValue = 199999.5)), tx)
        assertTrue(c.contains("\r\n\"Mazda 3\rHB\",2020,199999.5,MXN,"))
    }
}
