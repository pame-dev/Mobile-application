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
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.locale.textoPlural
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.repository.RendimientoPublicacion
import com.pame.karsy.data.repository.SellerStats
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/**
 * Exportar "Mi panel": Excel (CSV) para trabajar los números o PDF para presentarlos.
 * Los dos se comparten con el menú de Android (guardar en archivos, WhatsApp, correo…).
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

    /** Textos del CSV (ya traducidos), para que [csv] no dependa de Android. */
    data class TextosCsv(
        val titulo: String,
        val vendedor: String,
        val fecha: String,
        val resumen: List<Pair<String, Int>>,
        val columnas: List<String>,
        val estado: (Car) -> String,
    )

    /** Una celda de CSV: entre comillas si trae coma, comillas o salto de línea. */
    private fun celda(v: Any): String {
        val s = v.toString()
        return if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }

    /** CSV de Mi panel: resumen arriba y una fila por publicación (se abre en Excel o Google Sheets). */
    fun csv(stats: SellerStats, listings: List<Car>, tx: TextosCsv): String {
        val lineas = mutableListOf<List<Any>>(
            listOf(tx.titulo), listOf(tx.vendedor), listOf(tx.fecha), emptyList()
        )
        tx.resumen.forEach { (k, n) -> lineas += listOf(k, n) }
        lineas += emptyList<Any>()
        lineas += tx.columnas
        filas(stats, listings).forEach { (car, r) ->
            lineas += listOf(
                "${car.brand} ${car.model}".trim(),
                if (car.year > 0) car.year else "",
                // Número sin formato para que Excel lo pueda sumar y ordenar.
                if (car.priceValue % 1.0 == 0.0) car.priceValue.toLong() else car.priceValue,
                car.currency,
                tx.estado(car),
                r.vistas,
                r.favoritos,
                r.contactos,
                "${porcentaje(r.contactos, r.vistas)}%",
                car.publishedAt.take(10),
            )
        }
        // El BOM hace que Excel lea bien los acentos.
        return "\uFEFF" + lineas.joinToString("\r\n") { l -> l.joinToString(",") { celda(it) } }
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

    /** Genera el CSV en caché (llamar fuera del hilo principal). */
    fun generarCsv(context: Context, nombre: String, stats: SellerStats, listings: List<Car>, destacadas: Int): File {
        val tx = TextosCsv(
            titulo = texto(R.string.profile_dashboard_report_title),
            vendedor = nombre,
            fecha = hoy(),
            resumen = resumenEstados(stats, destacadas) + listOf(
                texto(R.string.profile_dashboard_funnel_views) to stats.embudoVistas,
                texto(R.string.profile_dashboard_funnel_favorites) to stats.embudoFavoritos,
                texto(R.string.profile_dashboard_funnel_contacts) to stats.embudoContactos,
            ),
            columnas = listOf(
                R.string.profile_dashboard_export_col_listing, R.string.profile_dashboard_export_col_year,
                R.string.profile_dashboard_export_col_price, R.string.profile_dashboard_export_col_currency,
                R.string.profile_dashboard_export_col_status, R.string.profile_dashboard_funnel_views,
                R.string.profile_dashboard_export_col_favorites, R.string.profile_dashboard_export_col_contacts,
                R.string.profile_dashboard_export_col_rate, R.string.profile_dashboard_export_col_published,
            ).map { texto(it) },
            estado = ::estado,
        )
        val f = archivo(context, "csv")
        f.writeText(csv(stats, listings, tx))
        return f
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

    enum class TipoConsejo { SinContacto, PocasVistas }

    data class Resumen(
        val masVista: Car?,
        val masContactada: Car?,
        val sinVistas: Int,
        val consejos: List<Pair<TipoConsejo, Car>>,
    )

    /**
     * Lo más destacado del reporte y hasta 3 recomendaciones (igual que la web):
     * - muchas vistas (20+) y ningún contacto: revisar precio o descripción;
     * - activa desde hace 7+ días con menos de 5 vistas: mejores fotos o destacarla.
     */
    fun resumen(stats: SellerStats, listings: List<Car>, hoy: Long = System.currentTimeMillis()): Resumen {
        val todas = filas(stats, listings)
        val consejos = todas.mapNotNull { (car, r) ->
            when {
                r.vistas >= 20 && r.contactos == 0 -> TipoConsejo.SinContacto to car
                car.status == "Activo" && r.vistas < 5 && diasDesde(car.publishedAt, hoy) >= 7 -> TipoConsejo.PocasVistas to car
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
        val res = resumen(stats, listings)

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
                listOf(
                    Triple(texto(R.string.profile_dashboard_report_top_viewed), res.masVista?.let(::titulo) ?: "—", ""),
                    Triple(texto(R.string.profile_dashboard_report_top_contacted), res.masContactada?.let(::titulo) ?: "—", ""),
                    Triple(texto(R.string.profile_dashboard_report_no_views), res.sinVistas.toString(), ""),
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
                val detalle = "${car.price} ${car.currency} · " + textoPlural(R.plurals.profile_dashboard_report_days, dias, dias)
                c.drawText(recortar(detalle, paint(8.5f), cols[2] - cols[1] - 8), cols[1], y + 25, paint(8.5f, gris))
                val barra = cols[2] - cols[1] - 16
                c.drawRoundRect(RectF(cols[1], y + 31, cols[1] + barra, y + 35), 2f, 2f, relleno(Color.rgb(238, 242, 245)))
                c.drawRoundRect(RectF(cols[1], y + 31, cols[1] + maxOf(4f, barra * f.vistas / max), y + 35), 2f, 2f, relleno(teal))
                c.drawText(recortar(estado(car), paint(9.5f), cols[3] - cols[2] - 6), cols[2], y + 18, paint(9.5f))
                c.drawText(f.vistas.toString(), cols[3], y + 18, paint(10.5f))
                c.drawText(f.favoritos.toString(), cols[4], y + 18, paint(10.5f))
                c.drawText(f.contactos.toString(), cols[5], y + 18, paint(10.5f))
                c.drawText("${porcentaje(f.contactos, f.vistas)}%", cols[6], y + 18, paint(10.5f))
                y += 46
                c.drawLine(margen, y - 3, ancho - margen, y - 3, paint(1f, Color.rgb(238, 242, 245)))
            }

            // Recomendaciones
            if (res.consejos.isNotEmpty()) {
                if (y > alto - margen - 40 - res.consejos.size * 30) nuevaPagina()
                y += 8
                c.drawText(texto(R.string.profile_dashboard_report_tips), margen, y + 12, paint(13f, bold = true))
                y += 22
                res.consejos.forEach { (tipo, car) ->
                    val consejo = texto(
                        if (tipo == TipoConsejo.SinContacto) R.string.profile_dashboard_report_tip_no_contact
                        else R.string.profile_dashboard_report_tip_few_views
                    )
                    // Texto en dos renglones como máximo.
                    val p = paint(9.5f)
                    val completo = "• ${titulo(car)}: $consejo"
                    val w = ancho - margen * 2
                    var corte = p.breakText(completo, true, w, null)
                    // Se corta en el último espacio para no partir palabras.
                    if (corte < completo.length) completo.lastIndexOf(' ', corte).takeIf { it > 0 }?.let { corte = it }
                    c.drawText(completo.take(corte), margen, y + 10, p)
                    if (corte < completo.length) c.drawText(recortar(completo.drop(corte).trim(), p, w - 8), margen + 8, y + 23, p)
                    y += 30
                }
            }
            pie()
            doc.finishPage(pagina)

            val archivo = archivo(context, "pdf")
            archivo.outputStream().use { doc.writeTo(it) }
            return archivo
        } finally {
            doc.close()
        }
    }

    /** Abre el menú de compartir de Android con [archivo] (CSV o PDF según la extensión). */
    fun compartir(context: Context, archivo: File) {
        val tipo = if (archivo.extension == "pdf") "application/pdf" else "text/csv"
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
        val enviar = Intent(Intent.ACTION_SEND).apply {
            type = tipo
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(enviar, texto(R.string.profile_dashboard_export_title))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
