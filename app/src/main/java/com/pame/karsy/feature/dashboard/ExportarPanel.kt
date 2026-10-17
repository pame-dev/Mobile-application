package com.pame.karsy.feature.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.pame.karsy.R
import com.pame.karsy.core.locale.Idioma
import com.pame.karsy.data.repository.StatsRepository
import com.pame.karsy.data.repository.DestacadoPublicacion
import com.pame.karsy.data.repository.ReporteExtra
import com.pame.karsy.core.util.Xlsx
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.locale.textoPlural
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.repository.RendimientoPublicacion
import com.pame.karsy.data.repository.SellerStats
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/**
 * Exportar "Mi panel": Excel (.xlsx) para trabajar los números o PDF para presentarlos.
 * Los dos se comparten con el menú de Android (guardar en archivos, WhatsApp, correo…).
 * El contenido es el mismo que en la web (web/src/lib/exportarPanel.ts).
 */
object ExportarPanel {

    /**
     * Una fila por cada publicación ya publicada (aunque aún no tenga vistas), la más vista
     * primero. Los números vienen de la BD por id y, si no hay, van en 0.
     */
    fun filas(stats: SellerStats, listings: List<Car>): List<Pair<Car, RendimientoPublicacion>> {
        val datos = stats.rendimiento.associateBy { it.id }
        return listings
            .filter { it.approved }
            .map { it to (datos[it.id] ?: RendimientoPublicacion(it.id, 0, 0, 0)) }
            .sortedWith(compareByDescending<Pair<Car, RendimientoPublicacion>> { it.second.vistas }.thenByDescending { it.second.contactos })
    }

    /** % de [b] sobre [a], sin decimales y hasta 100 (se puede guardar en favoritos sin abrir el detalle). */
    fun porcentaje(b: Int, a: Int): Int = if (a > 0) (b * 100.0 / a).roundToInt().coerceAtMost(100) else 0

    /** Fracción para Excel (0.25 = 25 %); 0 si no hay base. */
    private fun fraccion(b: Int, a: Int): Double = if (a > 0) b.toDouble() / a else 0.0

    /** Precio del auto vs el promedio de similares: 0.08 = 8 % arriba; null si no hay con qué comparar. */
    fun difMercado(car: Car, extra: ReporteExtra?): Double? {
        val m = extra?.mercado?.firstOrNull { it.id == car.id } ?: return null
        return if (m.promedio > 0 && car.priceValue > 0) (car.priceValue - m.promedio) / m.promedio else null
    }

    /** Publicaciones que necesitan algo: rechazadas, en revisión o deshabilitadas (con su motivo). */
    fun pendientes(listings: List<Car>): List<Car> = listings.filter { it.status == "Rechazado" || it.status == "Pendiente" || it.status == "Deshabilitado" }

    data class Vendidos(val total: Int, val dentro: Int, val fuera: Int)

    fun vendidos(listings: List<Car>): Vendidos {
        val v = listings.filter { it.status == "Vendido" }
        return Vendidos(v.size, v.count { it.soldVia == "dentro_app" }, v.count { it.soldVia == "fuera_app" })
    }

    /** Índice del valor más alto (el primero si empatan); -1 si todo es 0. */
    fun pico(v: List<Int>): Int = if (v.any { it > 0 }) v.indexOf(v.max()) else -1

    /** "19:00–20:00" */
    fun rangoHora(h: Int): String = "%02d:00–%02d:00".format(h, (h + 1) % 24)

    data class Efecto(val antes: Double, val durante: Double, val cambio: Double?)

    /** Vistas por día antes y durante el destacado, y el cambio (0.5 = +50 %). */
    fun efectoDestacado(d: DestacadoPublicacion): Efecto {
        // Cada ventana con sus días exactos (la de "antes" puede ser más corta si el auto se publicó hace poco).
        val antes = if (d.diasAntes > 0) d.vistasAntes / d.diasAntes else 0.0
        val durante = if (d.diasDurante > 0) d.vistasDurante / d.diasDurante else 0.0
        return Efecto(antes, durante, if (antes > 0) (durante - antes) / antes else null)
    }

    private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /** Etiquetas de los últimos 12 meses (del más antiguo al actual), como vistas_por_mes. Mismos nombres que la web. */
    fun ultimosMeses(locale: java.util.Locale, hoy: java.util.Calendar = java.util.Calendar.getInstance()): List<String> {
        val nombres = if (locale.language == "en") MONTHS else MESES
        return (11 downTo 0).map { atras ->
            val c = hoy.clone() as java.util.Calendar
            c.set(java.util.Calendar.DAY_OF_MONTH, 1)
            c.add(java.util.Calendar.MONTH, -atras)
            "${nombres[c.get(java.util.Calendar.MONTH)]} ${c.get(java.util.Calendar.YEAR)}"
        }
    }

    /** Días desde [iso] (AAAA-MM-DD…) hasta [hoy] en milisegundos; 0 si no hay fecha. */
    fun diasDesde(iso: String, hoy: Long = System.currentTimeMillis()): Int {
        if (iso.length < 10) return 0
        // Días de calendario (solo AAAA-MM-DD, a medianoche local), igual que la web.
        val fecha = runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { isLenient = false }.parse(iso.take(10))
        }.getOrNull() ?: return 0
        return ((hoy - fecha.time) / 86_400_000L).toInt().coerceAtLeast(0)
    }

    enum class TipoConsejo { SinContacto, PocasVistas, PrecioAlto }

    fun textoConsejo(tipo: TipoConsejo): Int = when (tipo) {
        TipoConsejo.SinContacto -> R.string.profile_dashboard_report_tip_no_contact
        TipoConsejo.PocasVistas -> R.string.profile_dashboard_report_tip_few_views
        TipoConsejo.PrecioAlto -> R.string.profile_dashboard_report_tip_high_price
    }

    data class Resumen(
        val masVista: Car?,
        val masContactada: Car?,
        val sinVistas: Int,
        val consejos: List<Pair<TipoConsejo, Car>>,
    )

    /**
     * Lo más destacado del reporte y hasta 3 recomendaciones (igual que la web):
     * - muchas vistas (20+) y ningún contacto: revisar precio o descripción;
     * - activa desde hace 7+ días con menos de 5 vistas: mejores fotos o destacarla;
     * - activa con precio 10 %+ arriba del promedio de 2+ autos similares.
     */
    fun resumen(stats: SellerStats, listings: List<Car>, hoy: Long = System.currentTimeMillis(), extra: ReporteExtra? = null): Resumen {
        val todas = filas(stats, listings)
        val consejos = todas.mapNotNull { (car, r) ->
            val similares = extra?.mercado?.firstOrNull { it.id == car.id }?.similares ?: 0
            val dif = difMercado(car, extra)
            when {
                r.vistas >= 20 && r.contactos == 0 -> TipoConsejo.SinContacto to car
                car.status == "Activo" && r.vistas < 5 && diasDesde(car.publishedAt, hoy) >= 7 -> TipoConsejo.PocasVistas to car
                car.status == "Activo" && dif != null && dif >= 0.1 && similares >= 2 -> TipoConsejo.PrecioAlto to car
                else -> null
            }
        }
        return Resumen(
            masVista = todas.firstOrNull { it.second.vistas > 0 }?.first,
            masContactada = todas.maxByOrNull { it.second.contactos }?.takeIf { it.second.contactos > 0 }?.first,
            sinVistas = todas.count { it.second.vistas == 0 },
            consejos = consejos.take(3),
        )
    }

    /** Nombres de los días (domingo = 0); también los usa el reporte del administrador. */
    internal val DIAS = listOf(
        R.string.profile_dashboard_day_sun, R.string.profile_dashboard_day_mon, R.string.profile_dashboard_day_tue,
        R.string.profile_dashboard_day_wed, R.string.profile_dashboard_day_thu, R.string.profile_dashboard_day_fri,
        R.string.profile_dashboard_day_sat,
    )

    /** Datos del encabezado del libro y del PDF. */
    data class Info(val nombre: String, val fecha: String, val destacadas: Int, val meses: List<String>)

    /**
     * Libro de Excel de Mi panel con 5 hojas: Resumen, Publicaciones, Actividad, Destacados y
     * Recomendaciones. [t] traduce un recurso (con sus argumentos); así no depende de Android.
     */
    fun libro(stats: SellerStats, listings: List<Car>, extra: ReporteExtra?, info: Info, t: (Int, Array<out Any>) -> String): List<Xlsx.Hoja> {
        fun tx(id: Int, vararg a: Any) = t(id, a)
        /** Los recuadros de la app usan MAYÚSCULAS; en el Excel van como en la web ("Activas"). */
        fun etiqueta(id: Int) = tx(id).lowercase().replaceFirstChar { it.uppercase() }
        fun c(v: Any?, s: Int) = Xlsx.Celda(v, s)
        val E = Xlsx.Estilo
        val todas = filas(stats, listings)
        val res = resumen(stats, listings, System.currentTimeMillis(), extra)
        val ven = vendidos(listings)
        fun titulo(car: Car) = "${car.brand} ${car.model} ${if (car.year > 0) car.year else ""}".trim()
        fun estado(car: Car) = estadoRes(car.status)?.let { tx(it) } ?: car.status
        val porDia = extra?.porDia.orEmpty()
        val porHora = extra?.porHora.orEmpty()
        val dia = pico(porDia)
        val hora = pico(porHora)

        // 1. Resumen
        val resumenHoja = Xlsx.Hoja(
            nombre = tx(R.string.profile_dashboard_xls_sheet_summary),
            anchos = listOf(34, 18, 36),
            unir = listOf("A1:C1", "A2:C2"),
            filas = listOf(
                listOf(c(tx(R.string.profile_dashboard_report_title), E.TITULO)),
                listOf(c("${info.nombre} · ${info.fecha}", E.SUBTITULO)),
                emptyList(),
                listOf(c(tx(R.string.profile_dashboard_xls_listings), E.SECCION)),
                listOf(c(etiqueta(R.string.profile_dashboard_kpi_active), E.ETIQUETA), c(stats.activas, E.ENTERO)),
                listOf(c(etiqueta(R.string.profile_dashboard_kpi_sold), E.ETIQUETA), c(ven.total, E.ENTERO), c(tx(R.string.profile_dashboard_xls_sold_split, ven.dentro, ven.fuera), E.TEXTO)),
                listOf(c(etiqueta(R.string.profile_dashboard_kpi_in_review), E.ETIQUETA), c(stats.pendientes, E.ENTERO)),
                listOf(c(etiqueta(R.string.profile_dashboard_kpi_featured), E.ETIQUETA), c(info.destacadas, E.ENTERO)),
                emptyList(),
                listOf(c(tx(R.string.profile_dashboard_performance_title), E.SECCION)),
                listOf(c(tx(R.string.profile_dashboard_funnel_views), E.ETIQUETA), c(stats.embudoVistas, E.ENTERO)),
                listOf(c(tx(R.string.profile_dashboard_funnel_favorites), E.ETIQUETA), c(stats.embudoFavoritos, E.ENTERO), c(fraccion(stats.embudoFavoritos, stats.embudoVistas), E.PCT)),
                listOf(c(tx(R.string.profile_dashboard_funnel_contacts), E.ETIQUETA), c(stats.embudoContactos, E.ENTERO), c(fraccion(stats.embudoContactos, stats.embudoVistas), E.PCT)),
                emptyList(),
                listOf(c(tx(R.string.profile_dashboard_xls_highlights), E.SECCION)),
                listOf(c(tx(R.string.profile_dashboard_report_top_viewed), E.ETIQUETA), c(res.masVista?.let(::titulo) ?: "—", E.TEXTO)),
                listOf(c(tx(R.string.profile_dashboard_report_top_contacted), E.ETIQUETA), c(res.masContactada?.let(::titulo) ?: "—", E.TEXTO)),
                listOf(c(tx(R.string.profile_dashboard_report_no_views), E.ETIQUETA), c(res.sinVistas, E.ENTERO)),
                listOf(c(tx(R.string.profile_dashboard_report_best_day), E.ETIQUETA), c(DIAS.getOrNull(dia)?.let { tx(it) } ?: "—", E.TEXTO)),
                listOf(c(tx(R.string.profile_dashboard_report_best_hour), E.ETIQUETA), c(if (hora >= 0) rangoHora(hora) else "—", E.TEXTO)),
            ),
        )

        // 2. Publicaciones
        val cab = listOf(
            R.string.profile_dashboard_export_col_listing, R.string.profile_dashboard_export_col_year, R.string.profile_dashboard_export_col_status,
            R.string.profile_dashboard_export_col_price, R.string.profile_dashboard_export_col_currency, R.string.profile_dashboard_xls_market_avg,
            R.string.profile_dashboard_xls_vs_market, R.string.profile_dashboard_funnel_views, R.string.profile_dashboard_export_col_favorites,
            R.string.profile_dashboard_xls_save_rate, R.string.profile_dashboard_export_col_contacts, R.string.profile_dashboard_export_col_rate,
            R.string.profile_dashboard_xls_days_listed, R.string.profile_dashboard_export_col_published, R.string.profile_dashboard_xls_featured_until,
            R.string.profile_dashboard_xls_sold_via, R.string.profile_dashboard_xls_reason,
        )
        val pubs = mutableListOf<List<Xlsx.Celda?>>(cab.map { c(tx(it), E.ENCABEZADO) })
        fun vendidaPor(car: Car) = when (car.soldVia) {
            "dentro_app" -> tx(R.string.profile_dashboard_xls_sold_inside)
            "fuera_app" -> tx(R.string.profile_dashboard_xls_sold_outside)
            else -> ""
        }
        todas.forEach { (car, r) ->
            val m = extra?.mercado?.firstOrNull { it.id == car.id }
            val finDestacado = extra?.destacados?.firstOrNull { it.id == car.id }?.fin
            pubs += listOf(
                c(titulo(car), E.NEGRITA), c(if (car.year > 0) car.year else null, E.TEXTO), c(estado(car), E.TEXTO),
                c(car.priceValue, E.DINERO), c(car.currency, E.TEXTO), c(m?.promedio, E.DINERO), c(difMercado(car, extra), E.PCT),
                c(r.vistas, E.ENTERO), c(r.favoritos, E.ENTERO), c(fraccion(r.favoritos, r.vistas), E.PCT),
                c(r.contactos, E.ENTERO), c(fraccion(r.contactos, r.vistas), E.PCT), c(diasDesde(car.publishedAt), E.ENTERO),
                c(Xlsx.fechaExcel(car.publishedAt), E.FECHA),
                c(if (car.featured) Xlsx.fechaExcel(finDestacado) else null, E.FECHA),
                c(vendidaPor(car), E.TEXTO), c(car.rejectedReason ?: car.disabledReason ?: "", E.TEXTO),
            )
        }
        // Las que aún no se publican (en revisión o rechazadas) también, para ver su motivo.
        pendientes(listings).filter { !it.approved }.forEach { car ->
            pubs += listOf(
                c(titulo(car), E.NEGRITA), c(if (car.year > 0) car.year else null, E.TEXTO), c(estado(car), E.TEXTO),
                c(car.priceValue, E.DINERO), c(car.currency, E.TEXTO), c(null, E.DINERO), c(null, E.PCT), c(null, E.ENTERO),
                c(null, E.ENTERO), c(null, E.PCT), c(null, E.ENTERO), c(null, E.PCT), c(null, E.ENTERO), c(null, E.FECHA),
                c(null, E.FECHA), c("", E.TEXTO), c(car.rejectedReason ?: car.disabledReason ?: "", E.TEXTO),
            )
        }
        val publicaciones = Xlsx.Hoja(
            nombre = tx(R.string.profile_dashboard_xls_sheet_listings),
            anchos = listOf(30, 8, 14, 14, 9, 16, 12, 9, 11, 11, 11, 12, 10, 13, 14, 16, 40),
            fijas = 1,
            filtro = "A1:${Xlsx.columna(cab.size - 1)}${pubs.size}",
            filas = pubs,
        )

        // 3. Actividad
        val vistasCol = tx(R.string.profile_dashboard_funnel_views)
        val act = mutableListOf<List<Xlsx.Celda?>>(
            listOf(
                c(tx(R.string.profile_dashboard_xls_month), E.ENCABEZADO), c(vistasCol, E.ENCABEZADO), null,
                c(tx(R.string.profile_dashboard_xls_day), E.ENCABEZADO), c(vistasCol, E.ENCABEZADO), null,
                c(tx(R.string.profile_dashboard_xls_hour), E.ENCABEZADO), c(vistasCol, E.ENCABEZADO),
            )
        )
        for (i in 0 until 24) {
            act += listOf(
                if (i < 12) c(info.meses.getOrElse(i) { "" }, E.TEXTO) else null,
                if (i < 12) c(stats.vistasPorMes.getOrElse(i) { 0f }.toInt(), E.ENTERO) else null,
                null,
                if (i < 7) c(tx(DIAS[i]), E.TEXTO) else null,
                if (i < 7) c(porDia.getOrElse(i) { 0 }, E.ENTERO) else null,
                null,
                c(rangoHora(i), E.TEXTO),
                c(porHora.getOrElse(i) { 0 }, E.ENTERO),
            )
        }
        val actividad = Xlsx.Hoja(tx(R.string.profile_dashboard_xls_sheet_activity), listOf(14, 10, 3, 14, 10, 3, 14, 10), act, fijas = 1)

        // 4. Destacados
        val porId = listings.associateBy { it.id }
        val des = mutableListOf<List<Xlsx.Celda?>>(
            listOf(
                R.string.profile_dashboard_export_col_listing, R.string.profile_dashboard_xls_start, R.string.profile_dashboard_xls_end,
                R.string.profile_dashboard_xls_days, R.string.profile_dashboard_xls_views_before, R.string.profile_dashboard_xls_views_during,
                R.string.profile_dashboard_xls_change,
            ).map { c(tx(it), E.ENCABEZADO) }
        )
        extra?.destacados.orEmpty().forEach { d ->
            val car = porId[d.id] ?: return@forEach
            val ef = efectoDestacado(d)
            des += listOf(
                c(titulo(car), E.NEGRITA), c(Xlsx.fechaExcel(d.inicio), E.FECHA), c(Xlsx.fechaExcel(d.fin), E.FECHA), c(d.dias, E.ENTERO),
                c((ef.antes * 10).roundToInt() / 10.0, E.DECIMAL), c((ef.durante * 10).roundToInt() / 10.0, E.DECIMAL), c(ef.cambio, E.PCT),
            )
        }
        if (des.size == 1) des += listOf(c(tx(R.string.profile_dashboard_xls_no_featured), E.TEXTO))
        val destacados = Xlsx.Hoja(tx(R.string.profile_dashboard_xls_sheet_featured), listOf(30, 13, 13, 8, 18, 18, 12), des, fijas = 1)

        // 5. Recomendaciones
        val rec = mutableListOf<List<Xlsx.Celda?>>(
            listOf(c(tx(R.string.profile_dashboard_export_col_listing), E.ENCABEZADO), c(tx(R.string.profile_dashboard_report_tips), E.ENCABEZADO))
        )
        res.consejos.forEach { (tipo, car) -> rec += listOf(c(titulo(car), E.NEGRITA), c(tx(textoConsejo(tipo)), E.TEXTO)) }
        pendientes(listings).forEach { car ->
            val motivo = car.rejectedReason ?: car.disabledReason
            rec += listOf(c(titulo(car), E.NEGRITA), c(estado(car) + (motivo?.let { ": $it" } ?: ""), E.TEXTO))
        }
        if (rec.size == 1) rec += listOf(c(tx(R.string.profile_dashboard_xls_all_good), E.TEXTO))
        val recomendaciones = Xlsx.Hoja(tx(R.string.profile_dashboard_report_tips), listOf(30, 90), rec, fijas = 1)

        return listOf(resumenHoja, publicaciones, actividad, destacados, recomendaciones)
    }

    private fun estadoRes(status: String): Int? = when (status) {
        "Activo" -> R.string.profile_status_active
        "Pendiente" -> R.string.profile_status_pending
        "Rechazado" -> R.string.profile_status_rejected
        "Deshabilitado" -> R.string.profile_status_disabled
        "Vendido" -> R.string.profile_status_sold
        "Pausado" -> R.string.profile_status_paused
        else -> null
    }

    private fun estado(car: Car): String = estadoRes(car.status)?.let { texto(it) } ?: car.status

    private fun resumenEstados(stats: SellerStats, destacadas: Int) = listOf(
        texto(R.string.profile_dashboard_kpi_active) to stats.activas,
        texto(R.string.profile_dashboard_kpi_sold) to stats.vendidas,
        texto(R.string.profile_dashboard_kpi_in_review) to stats.pendientes,
        texto(R.string.profile_dashboard_kpi_featured) to destacadas,
    )

    private fun hoy(): String = java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG).format(java.util.Date())

    private fun archivo(context: Context, extension: String): File {
        val dir = File(context.cacheDir, "exportes").apply { mkdirs() }
        val fecha = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        return File(dir, "karsy-mi-panel-$fecha.$extension")
    }

    /** Datos extra del reporte; si fallan, el reporte sale igual (sin mercado, actividad ni destacados). */
    private suspend fun extra(): ReporteExtra? = try {
        StatsRepository.sellerReport()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun idioma(): java.util.Locale = Idioma.contextoActual().resources.configuration.locales[0]

    /** Genera el Excel (.xlsx) en caché (llamar fuera del hilo principal). */
    suspend fun generarExcel(context: Context, nombre: String, stats: SellerStats, listings: List<Car>, destacadas: Int): File {
        val hojas = libro(stats, listings, extra(), Info(nombre, hoy(), destacadas, ultimosMeses(idioma()))) { id, a -> texto(id, *a) }
        val f = archivo(context, "xlsx")
        f.outputStream().use { Xlsx.escribir(hojas, it) }
        return f
    }

    /** Foto del auto como Bitmap (con Coil); null si no hay o falla la descarga. */
    private suspend fun foto(context: Context, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        val pedido = ImageRequest.Builder(context).data(url).size(240, 170).allowHardware(false).build()
        return try {
            context.imageLoader.execute(pedido).image?.toBitmap()
        } catch (e: CancellationException) {
            throw e // si se sale de Mi panel, se deja de generar el PDF
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Genera el reporte en PDF (hojas tamaño carta) en caché: logo, recuadros, embudo, lo
     * más destacado, tabla con foto de cada auto y recomendaciones. Llamar fuera del hilo principal.
     */
    suspend fun generarPdf(context: Context, nombre: String, stats: SellerStats, listings: List<Car>, destacadas: Int): File {
        val ancho = 612
        val alto = 792
        val margen = 40f
        val tinta = Color.rgb(13, 43, 69)
        val gris = Color.rgb(91, 107, 122)
        val linea = Color.rgb(215, 222, 229)
        val suave = Color.rgb(243, 247, 250)
        val teal = Color.rgb(44, 154, 160)
        fun paint(size: Float, color: Int = tinta, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        fun relleno(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        fun titulo(car: Car) = "${car.brand} ${car.model} ${if (car.year > 0) car.year else ""}".trim()

        // Primero se descargan las fotos (fuera del dibujo).
        val todas = filas(stats, listings)
        val fotos = todas.associate { (car, _) -> car.id to foto(context, car.imageUrl) }
        // El PNG es grande (1234 px): se lee reducido para no gastar memoria (se dibuja de 40 pt).
        val logo = BitmapFactory.decodeResource(
            context.resources, R.drawable.karsy_logo,
            BitmapFactory.Options().apply { inScaled = false; inSampleSize = 8 }
        )
        val extra = extra()
        val res = resumen(stats, listings, System.currentTimeMillis(), extra)
        val ven = vendidos(listings)

        val doc = PdfDocument()
        try {
            var numero = 1
            var pagina = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numero).create())
            var c = pagina.canvas
            var y = margen

            // Encabezado con logo
            logo?.let { c.drawBitmap(it, null, RectF(margen, y, margen + 40, y + 40), Paint(Paint.FILTER_BITMAP_FLAG)) }
            c.drawText(texto(R.string.profile_dashboard_report_title), margen + 52, y + 18, paint(17f, bold = true))
            c.drawText("$nombre · ${hoy()}", margen + 52, y + 34, paint(10.5f, gris))
            c.drawText("Karsy", ancho - margen - 52, y + 26, paint(18f, bold = true))
            y += 50
            c.drawLine(margen, y, ancho - margen, y, paint(1f, tinta).apply { strokeWidth = 2f })
            y += 14

            fun cajas(items: List<Triple<String, String, String>>, fondo: Int? = null, valor: Float = 16f) {
                val gap = 8f
                val w = (ancho - margen * 2 - gap * (items.size - 1)) / items.size
                items.forEachIndexed { i, (k, n, sub) ->
                    val x = margen + i * (w + gap)
                    val r = RectF(x, y, x + w, y + 54)
                    if (fondo != null) c.drawRoundRect(r, 8f, 8f, relleno(fondo))
                    c.drawRoundRect(r, 8f, 8f, paint(1f, linea).apply { style = Paint.Style.STROKE })
                    c.drawText(k, x + 10, y + 15, paint(9f, gris))
                    val p = paint(valor, bold = true)
                    var v = n
                    while (p.measureText(v) > w - 20 && v.length > 4) v = v.dropLast(2) + "…"
                    c.drawText(v, x + 10, y + 34, p)
                    if (sub.isNotEmpty()) c.drawText(sub, x + 10, y + 47, paint(8f, gris))
                }
                y += 64
            }
            cajas(resumenEstados(stats, destacadas).map { (k, n) -> Triple(k, n.toString(), "") })
            if (ven.total > 0) {
                c.drawText(texto(R.string.profile_dashboard_xls_sold_split, ven.dentro, ven.fuera), margen, y - 2, paint(9f, gris))
                y += 10
            }

            // Embudo
            c.drawText(texto(R.string.profile_dashboard_performance_title), margen, y + 12, paint(13f, bold = true))
            c.drawText(texto(R.string.profile_dashboard_performance_sub), margen, y + 26, paint(9.5f, gris))
            y += 34
            cajas(
                listOf(
                    Triple(texto(R.string.profile_dashboard_funnel_views), stats.embudoVistas.toString(), ""),
                    Triple(
                        texto(R.string.profile_dashboard_funnel_favorites), stats.embudoFavoritos.toString(),
                        texto(R.string.profile_dashboard_funnel_of_views, porcentaje(stats.embudoFavoritos, stats.embudoVistas))
                    ),
                    Triple(
                        texto(R.string.profile_dashboard_funnel_contacts), stats.embudoContactos.toString(),
                        texto(R.string.profile_dashboard_funnel_of_views, porcentaje(stats.embudoContactos, stats.embudoVistas))
                    ),
                )
            )
            // Lo más destacado
            cajas(
                listOfNotNull(
                    Triple(texto(R.string.profile_dashboard_report_top_viewed), res.masVista?.let(::titulo) ?: "—", ""),
                    Triple(texto(R.string.profile_dashboard_report_top_contacted), res.masContactada?.let(::titulo) ?: "—", ""),
                    // Solo si algún auto no tiene vistas: es un aviso para revisarlo.
                    Triple(texto(R.string.profile_dashboard_report_no_views), res.sinVistas.toString(), "").takeIf { res.sinVistas > 0 },
                ),
                fondo = suave,
                valor = 12f
            )

            // Tabla con foto de cada auto (con salto de página)
            val cols = floatArrayOf(margen, margen + 64, 316f, 380f, 434f, 492f, 546f)
            /** Pie con número de hoja (en todas las hojas). */
            fun pie() {
                c.drawText(texto(R.string.profile_dashboard_report_footer), margen, alto - 20f, paint(8f, gris))
                val n = numero.toString()
                c.drawText(n, ancho - margen - paint(8f).measureText(n), alto - 20f, paint(8f, gris))
            }
            /** Recorta [texto] para que quepa en [w] puntos, con "…" si se corta. */
            fun recortar(texto: String, p: Paint, w: Float): String {
                if (p.measureText(texto) <= w) return texto
                var t = texto
                while (t.length > 1 && p.measureText("$t…") > w) t = t.dropLast(1)
                return "$t…"
            }
            fun nuevaPagina() {
                pie()
                doc.finishPage(pagina)
                numero++
                pagina = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numero).create())
                c = pagina.canvas
                y = margen
            }
            fun encabezadoTabla() {
                val p = paint(8f, gris, bold = true)
                listOf(
                    cols[0] to R.string.profile_dashboard_export_col_listing, cols[2] to R.string.profile_dashboard_export_col_status,
                    cols[3] to R.string.profile_dashboard_funnel_views, cols[4] to R.string.profile_dashboard_export_col_favorites,
                    cols[5] to R.string.profile_dashboard_export_col_contacts,
                ).forEachIndexed { i, (x, id) ->
                    val siguiente = if (i == 0) cols[2] else cols[i + 2]
                    c.drawText(recortar(texto(id).uppercase(), p, siguiente - x - 6), x, y + 10, p)
                }
                // La última columna es angosta: solo "%" (de contacto).
                c.drawText("%", cols[6], y + 10, p)
                y += 15
                c.drawLine(margen, y, ancho - margen, y, paint(1f, linea))
                y += 6
            }
            encabezadoTabla()
            val max = (todas.maxOfOrNull { it.second.vistas } ?: 0).coerceAtLeast(1)
            todas.forEach { (car, f) ->
                if (y > alto - margen - 52) {
                    nuevaPagina()
                    encabezadoTabla()
                }
                val marco = RectF(cols[0], y, cols[0] + 56, y + 40)
                val bmp = fotos[car.id]
                if (bmp != null) {
                    // Recorte al centro con la proporción del marco, para que la foto no se estire.
                    val prop = marco.width() / marco.height()
                    val src = if (bmp.width.toFloat() / bmp.height > prop) {
                        val w = (bmp.height * prop).toInt()
                        android.graphics.Rect((bmp.width - w) / 2, 0, (bmp.width + w) / 2, bmp.height)
                    } else {
                        val h = (bmp.width / prop).toInt()
                        android.graphics.Rect(0, (bmp.height - h) / 2, bmp.width, (bmp.height + h) / 2)
                    }
                    c.drawBitmap(bmp, src, marco, Paint(Paint.FILTER_BITMAP_FLAG))
                }
                else c.drawRoundRect(marco, 6f, 6f, relleno(Color.rgb(238, 242, 245)))
                c.drawText(recortar(titulo(car), paint(10.5f, bold = true), cols[2] - cols[1] - 8), cols[1], y + 12, paint(10.5f, bold = true))
                val dias = diasDesde(car.publishedAt)
                val dif = difMercado(car, extra)
                val detalle = "${car.price} ${car.currency} · " + textoPlural(R.plurals.profile_dashboard_report_days, dias, dias) +
                    (dif?.let { " · " + texto(R.string.profile_dashboard_report_vs_market_short, (if (it > 0) "+" else "") + (it * 100).roundToInt()) } ?: "") +
                    (car.featuredUntil?.takeIf { car.featured }?.let { " · " + texto(R.string.profile_dashboard_report_featured_until, it) } ?: "")
                c.drawText(recortar(detalle, paint(8.5f), cols[2] - cols[1] - 8), cols[1], y + 25, paint(8.5f, gris))
                val barra = cols[2] - cols[1] - 16
                c.drawRoundRect(RectF(cols[1], y + 31, cols[1] + barra, y + 35), 2f, 2f, relleno(Color.rgb(238, 242, 245)))
                c.drawRoundRect(RectF(cols[1], y + 31, cols[1] + maxOf(4f, barra * f.vistas / max), y + 35), 2f, 2f, relleno(teal))
                c.drawText(recortar(estado(car), paint(9.5f), cols[3] - cols[2] - 6), cols[2], y + 18, paint(9.5f))
                c.drawText(f.vistas.toString(), cols[3], y + 18, paint(10.5f))
                c.drawText(f.favoritos.toString(), cols[4], y + 18, paint(10.5f))
                c.drawText("(${porcentaje(f.favoritos, f.vistas)}%)", cols[4], y + 30, paint(8f, gris))
                c.drawText(f.contactos.toString(), cols[5], y + 18, paint(10.5f))
                c.drawText("${porcentaje(f.contactos, f.vistas)}%", cols[6], y + 18, paint(10.5f))
                y += 46
                c.drawLine(margen, y - 3, ancho - margen, y - 3, paint(1f, Color.rgb(238, 242, 245)))
            }

            /** Salta de hoja si no caben [alto] puntos más. */
            fun asegurar(espacio: Float) {
                if (y > alto - margen - 30 - espacio) nuevaPagina()
            }
            /** Título de sección y lista de viñetas (cada una en dos renglones como máximo). */
            fun lista(tituloId: Int, items: List<String>) {
                if (items.isEmpty()) return
                // El título no se queda solo al final de la hoja.
                asegurar(60f)
                y += 8
                c.drawText(texto(tituloId), margen, y + 12, paint(13f, bold = true))
                y += 22
                items.forEach { completo ->
                    asegurar(30f) // cada viñeta revisa que quepa en la hoja
                    val p = paint(9.5f)
                    val w = ancho - margen * 2
                    var corte = p.breakText(completo, true, w, null)
                    // Se corta en el último espacio para no partir palabras.
                    if (corte < completo.length) completo.lastIndexOf(' ', corte).takeIf { it > 0 }?.let { corte = it }
                    c.drawText(completo.take(corte), margen, y + 10, p)
                    if (corte < completo.length) c.drawText(recortar(completo.drop(corte).trim(), p, w - 8), margen + 8, y + 23, p)
                    y += 30
                }
            }

            // Actividad: vistas por mes (barras) y mejor día / mejor hora
            asegurar(130f)
            y += 8
            c.drawText(texto(R.string.profile_dashboard_report_activity), margen, y + 12, paint(13f, bold = true))
            y += 20
            val meses = ultimosMeses(idioma())
            val anchoBarras = (ancho - margen * 2) * 0.7f
            val maxMes = (stats.vistasPorMes.maxOrNull() ?: 0f).coerceAtLeast(1f)
            c.drawText(texto(R.string.profile_dashboard_report_by_month), margen, y + 10, paint(9f, gris))
            val baseY = y + 86
            val paso = anchoBarras / 12
            stats.vistasPorMes.take(12).forEachIndexed { i, v ->
                val h = maxOf(2f, 60f * v / maxMes)
                val x = margen + i * paso
                c.drawRoundRect(RectF(x + paso * 0.15f, baseY - h, x + paso * 0.85f, baseY), 2f, 2f, relleno(teal))
                c.drawText(meses.getOrElse(i) { "" }.substringBefore(' ').take(3), x + paso * 0.1f, baseY + 11, paint(7.5f, gris))
            }
            val xDer = margen + anchoBarras + 16
            val dia = pico(extra?.porDia.orEmpty())
            val hora = pico(extra?.porHora.orEmpty())
            c.drawText(texto(R.string.profile_dashboard_report_best_day), xDer, y + 20, paint(9f, gris))
            c.drawText(DIAS.getOrNull(dia)?.let { texto(it) } ?: "—", xDer, y + 36, paint(13f, bold = true))
            c.drawText(texto(R.string.profile_dashboard_report_best_hour), xDer, y + 58, paint(9f, gris))
            c.drawText(if (hora >= 0) rangoHora(hora) else "—", xDer, y + 74, paint(13f, bold = true))
            y = baseY + 20

            // Destacados: antes vs. durante
            val porId = listings.associateBy { it.id }
            lista(R.string.profile_dashboard_report_featured_effect, extra?.destacados.orEmpty().mapNotNull { d ->
                val car = porId[d.id] ?: return@mapNotNull null
                val ef = efectoDestacado(d)
                "• ${titulo(car)}: " + texto(R.string.profile_dashboard_report_featured_line, "%.1f".format(ef.antes), "%.1f".format(ef.durante)) +
                    (ef.cambio?.let { " (${if (it >= 0) "+" else ""}${(it * 100).roundToInt()}%)" } ?: "")
            })

            // Requieren atención: rechazadas, en revisión o deshabilitadas (con su motivo)
            lista(R.string.profile_dashboard_report_attention, pendientes(listings).map { car ->
                val motivo = car.rejectedReason ?: car.disabledReason
                "• ${titulo(car)}: ${estado(car)}" + (motivo?.let { " — $it" } ?: "")
            })

            // Recomendaciones
            lista(R.string.profile_dashboard_report_tips, res.consejos.map { (tipo, car) -> "• ${titulo(car)}: ${texto(textoConsejo(tipo))}" })
            pie()
            doc.finishPage(pagina)

            val archivo = archivo(context, "pdf")
            archivo.outputStream().use { doc.writeTo(it) }
            return archivo
        } finally {
            doc.close()
        }
    }

    /** Abre el menú de compartir de Android con [archivo] (Excel o PDF según la extensión). */
    fun compartir(context: Context, archivo: File, titulo: Int = R.string.profile_dashboard_export_title) {
        val tipo = if (archivo.extension == "pdf") "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
        val enviar = Intent(Intent.ACTION_SEND).apply {
            type = tipo
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(enviar, texto(titulo))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
