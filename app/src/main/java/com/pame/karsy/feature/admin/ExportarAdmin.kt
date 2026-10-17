package com.pame.karsy.feature.admin

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.core.util.Xlsx
import com.pame.karsy.data.repository.AdminStats
import com.pame.karsy.data.repository.ReporteApp
import com.pame.karsy.data.repository.StatsRepository
import com.pame.karsy.feature.dashboard.ExportarPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/**
 * Reporte del panel de administración (Excel y PDF): números de toda la app en el rango
 * elegido. Mismo contenido que la web (web/src/lib/exportarAdmin.ts).
 */
object ExportarAdmin {

    /** Fracción para Excel (0.25 = 25 %); 0 si no hay base. */
    fun fraccion(b: Int, a: Int): Double = if (a > 0) b.toDouble() / a else 0.0

    /** Una fila del Resumen: etiqueta, valor (número o texto) y % opcional. */
    data class Fila(val etiqueta: String, val valor: Any, val pct: Double? = null)

    /**
     * Filas del Resumen agrupadas por sección (título como recurso). [t] traduce un recurso
     * con sus argumentos; así no depende de Android.
     */
    fun secciones(r: ReporteApp, t: (Int, Array<out Any>) -> String): List<Pair<Int, List<Fila>>> {
        fun tx(id: Int, vararg a: Any) = t(id, a)
        val u = r.usuarios
        val p = r.publicaciones
        val vistas = r.n(r.interes, "vistas")
        val contactos = r.n(r.interes, "contactos")
        val dia = ExportarPanel.pico(r.porDia)
        val hora = ExportarPanel.pico(r.porHora)
        return listOf(
            R.string.admin3_users to listOf(
                Fila(tx(R.string.admin3_total), r.n(u, "total")),
                Fila(tx(R.string.admin3_new_in_range), r.n(u, "nuevos")),
                Fila(tx(R.string.admin3_individual), r.n(u, "particulares")),
                Fila(tx(R.string.admin3_lot), r.n(u, "lotes")),
                Fila(tx(R.string.admin3_suspended), r.n(u, "suspendidos")),
            ),
            R.string.admin3_listings to listOf(
                Fila(tx(R.string.admin3_published), r.n(p, "publicadas")),
                Fila(tx(R.string.admin3_new_in_range_f), r.n(p, "nuevas")),
                Fila(tx(R.string.admin3_active), r.n(p, "activas")),
                Fila(tx(R.string.admin3_paused), r.n(p, "pausadas")),
                Fila(tx(R.string.admin3_disabled), r.n(p, "deshabilitadas")),
                Fila(tx(R.string.admin3_sold), r.n(p, "vendidas")),
                Fila(tx(R.string.admin3_sold_inside), r.n(p, "vendidas_dentro")),
                Fila(tx(R.string.admin3_sold_outside), r.n(p, "vendidas_fuera")),
            ),
            R.string.admin3_interest to listOf(
                Fila(tx(R.string.admin3_views), vistas),
                Fila(tx(R.string.admin3_favorites), r.favoritos, fraccion(r.favoritos, vistas)),
                Fila(tx(R.string.admin3_contacts), contactos, fraccion(contactos, vistas)),
            ),
            R.string.admin3_review to listOf(
                Fila(tx(R.string.admin3_pending), r.n(r.revision, "pendientes")),
                Fila(tx(R.string.admin3_approved), r.n(r.revision, "aprobadas")),
                Fila(tx(R.string.admin3_rejected), r.n(r.revision, "rechazadas")),
                Fila(tx(R.string.admin3_avg_hours), r.horasRevision ?: "—"),
            ),
            R.string.admin3_featured to listOf(
                Fila(tx(R.string.admin3_pending), r.n(r.destacados, "pendientes")),
                Fila(tx(R.string.admin3_current), r.n(r.destacados, "vigentes")),
                Fila(tx(R.string.admin3_approved_m), r.n(r.destacados, "aprobados")),
                Fila(tx(R.string.admin3_rejected_m), r.n(r.destacados, "rechazados")),
            ),
            R.string.admin3_reports to listOf(
                Fila(tx(R.string.admin3_pending), r.n(r.reportes, "pendientes")),
                Fila(tx(R.string.admin3_received), r.n(r.reportes, "recibidos")),
                Fila(tx(R.string.admin3_resolved), r.n(r.reportes, "resueltos")),
            ),
            R.string.admin3_activity to listOf(
                Fila(tx(R.string.admin3_best_day), ExportarPanel.DIAS.getOrNull(dia)?.let { tx(it) } ?: "—"),
                Fila(tx(R.string.admin3_best_hour), if (hora >= 0) ExportarPanel.rangoHora(hora) else "—"),
            ),
        )
    }

    /**
     * Libro de Excel del panel de administración con 6 hojas: Resumen, Crecimiento, Más vistas,
     * Marcas y estados, Reportes y Actividad.
     */
    fun libro(stats: AdminStats, r: ReporteApp, fecha: String, dias: Int, t: (Int, Array<out Any>) -> String): List<Xlsx.Hoja> {
        fun tx(id: Int, vararg a: Any) = t(id, a)
        fun c(v: Any?, s: Int) = Xlsx.Celda(v, s)
        fun cab(id: Int) = c(tx(id), Xlsx.Estilo.ENCABEZADO)
        val E = Xlsx.Estilo

        // 1. Resumen
        val filas = mutableListOf<List<Xlsx.Celda?>>(
            listOf(c(tx(R.string.admin3_report_title), E.TITULO)),
            listOf(c(tx(R.string.admin3_report_sub, dias, fecha), E.SUBTITULO)),
        )
        secciones(r, t).forEach { (titulo, items) ->
            filas += emptyList<Xlsx.Celda?>()
            filas += listOf(c(tx(titulo), E.SECCION))
            items.forEach { f ->
                val v = f.valor
                val estilo = when {
                    v is Int -> E.ENTERO
                    v is Double && v % 1.0 == 0.0 -> E.ENTERO
                    v is Double -> E.DECIMAL
                    else -> E.TEXTO
                }
                filas += listOfNotNull(c(f.etiqueta, E.ETIQUETA), c(v, estilo), f.pct?.let { c(it, E.PCT) })
            }
        }
        val resumen = Xlsx.Hoja(tx(R.string.admin3_sheet_summary), listOf(34, 16, 12), filas, unir = listOf("A1:C1", "A2:C2"))

        // 2. Crecimiento (acumulados) e interés (por periodo), lado a lado
        val crec = mutableListOf<List<Xlsx.Celda?>>(
            listOf(
                cab(R.string.admin3_date), cab(R.string.admin3_legend_users), cab(R.string.admin3_legend_lots), cab(R.string.admin3_legend_listings),
                null, cab(R.string.admin3_period), cab(R.string.admin3_views), cab(R.string.admin3_contacts),
            )
        )
        val ce = stats.crecimientoEtiquetas
        val ie = stats.interaccionesEtiquetas
        for (i in 0 until maxOf(ce.size, ie.size)) {
            val hayC = i < ce.size
            val hayI = i < ie.size
            crec += listOf(
                if (hayC) c(ce[i], E.TEXTO) else null,
                if (hayC) c(stats.crecimientoUsuarios.getOrElse(i) { 0f }.toInt(), E.ENTERO) else null,
                if (hayC) c(stats.crecimientoLotes.getOrElse(i) { 0f }.toInt(), E.ENTERO) else null,
                if (hayC) c(stats.crecimientoPublicaciones.getOrElse(i) { 0f }.toInt(), E.ENTERO) else null,
                null,
                if (hayI) c(ie[i], E.TEXTO) else null,
                if (hayI) c(stats.vistasPorMes.getOrElse(i) { 0 }, E.ENTERO) else null,
                if (hayI) c(stats.contactosPorMes.getOrElse(i) { 0 }, E.ENTERO) else null,
            )
        }
        val crecimiento = Xlsx.Hoja(tx(R.string.admin3_sheet_growth), listOf(12, 11, 11, 13, 3, 12, 10, 11), crec, fijas = 1)

        // 3. Más vistas
        val mv = mutableListOf<List<Xlsx.Celda?>>(
            listOf(cab(R.string.admin3_listing), cab(R.string.admin3_seller), cab(R.string.admin3_views), cab(R.string.admin3_contacts), cab(R.string.admin3_conversion))
        )
        r.masVistas.forEach { m ->
            mv += listOf(c(m.titulo, E.NEGRITA), c(m.vendedor, E.TEXTO), c(m.vistas, E.ENTERO), c(m.contactos, E.ENTERO), c(fraccion(m.contactos, m.vistas), E.PCT))
        }
        if (mv.size == 1) mv += listOf(c(tx(R.string.admin3_no_data), E.TEXTO))
        val masVistas = Xlsx.Hoja(tx(R.string.admin3_sheet_top), listOf(32, 26, 10, 11, 12), mv, fijas = 1)

        // 4. Marcas y estados (publicaciones activas)
        val me = mutableListOf<List<Xlsx.Celda?>>(
            listOf(cab(R.string.admin3_brand), cab(R.string.admin3_active_listings), null, cab(R.string.admin3_state), cab(R.string.admin3_active_listings))
        )
        for (i in 0 until maxOf(r.marcas.size, r.estados.size)) {
            val m = r.marcas.getOrNull(i)
            val e = r.estados.getOrNull(i)
            me += listOf(m?.let { c(it.first, E.TEXTO) }, m?.let { c(it.second, E.ENTERO) }, null, e?.let { c(it.first, E.TEXTO) }, e?.let { c(it.second, E.ENTERO) })
        }
        val marcas = Xlsx.Hoja(tx(R.string.admin3_sheet_brands), listOf(22, 14, 3, 22, 14), me, fijas = 1)

        // 5. Reportes (motivos más comunes del rango)
        val rep = mutableListOf<List<Xlsx.Celda?>>(listOf(cab(R.string.admin3_reason), cab(R.string.admin3_reports)))
        r.motivos.forEach { (motivo, n) -> rep += listOf(c(motivo, E.TEXTO), c(n, E.ENTERO)) }
        if (rep.size == 1) rep += listOf(c(tx(R.string.admin3_no_data), E.TEXTO))
        val reportes = Xlsx.Hoja(tx(R.string.admin3_reports), listOf(60, 10), rep, fijas = 1)

        // 6. Actividad (vistas por día y por hora)
        val act = mutableListOf<List<Xlsx.Celda?>>(
            listOf(cab(R.string.admin3_day), cab(R.string.admin3_views), null, cab(R.string.admin3_hour), cab(R.string.admin3_views))
        )
        for (i in 0 until 24) {
            act += listOf(
                if (i < 7) c(tx(ExportarPanel.DIAS[i]), E.TEXTO) else null,
                if (i < 7) c(r.porDia.getOrElse(i) { 0 }, E.ENTERO) else null,
                null,
                c(ExportarPanel.rangoHora(i), E.TEXTO),
                c(r.porHora.getOrElse(i) { 0 }, E.ENTERO),
            )
        }
        val actividad = Xlsx.Hoja(tx(R.string.admin3_activity), listOf(14, 10, 3, 14, 10), act, fijas = 1)

        return listOf(resumen, crecimiento, masVistas, marcas, reportes, actividad)
    }

    private fun hoy(): String = java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG).format(java.util.Date())

    private fun archivo(context: Context, extension: String): File {
        val dir = File(context.cacheDir, "exportes").apply { mkdirs() }
        val fecha = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        return File(dir, "karsy-admin-$fecha.$extension")
    }

    /** Genera el Excel (.xlsx) en caché (llamar fuera del hilo principal). */
    fun generarExcel(context: Context, stats: AdminStats, r: ReporteApp, dias: Int): File {
        val hojas = libro(stats, r, hoy(), dias) { id, a -> texto(id, *a) }
        val f = archivo(context, "xlsx")
        f.outputStream().use { Xlsx.escribir(hojas, it) }
        return f
    }

    /** Genera el reporte en PDF (hojas tamaño carta) en caché (llamar fuera del hilo principal). */
    fun generarPdf(context: Context, stats: AdminStats, r: ReporteApp, dias: Int): File {
        val ancho = 612
        val alto = 792
        val margen = 40f
        val tinta = Color.rgb(13, 43, 69)
        val gris = Color.rgb(91, 107, 122)
        val linea = Color.rgb(215, 222, 229)
        val teal = Color.rgb(44, 154, 160)
        fun paint(size: Float, color: Int = tinta, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        fun relleno(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        fun entero(n: Int) = "%,d".format(n)
        fun recortar(texto: String, p: Paint, w: Float): String {
            if (p.measureText(texto) <= w) return texto
            var t = texto
            while (t.length > 1 && p.measureText("$t…") > w) t = t.dropLast(1)
            return "$t…"
        }
        // El PNG es grande (1234 px): se lee reducido para no gastar memoria (se dibuja de 40 pt).
        val logo = BitmapFactory.decodeResource(
            context.resources, R.drawable.karsy_logo,
            BitmapFactory.Options().apply { inScaled = false; inSampleSize = 8 }
        )

        val doc = PdfDocument()
        try {
            var numero = 1
            var pagina = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numero).create())
            var c = pagina.canvas
            var y = margen
            fun pie() {
                c.drawText(texto(R.string.admin3_report_footer), margen, alto - 20f, paint(8f, gris))
                val n = numero.toString()
                c.drawText(n, ancho - margen - paint(8f).measureText(n), alto - 20f, paint(8f, gris))
            }
            fun nuevaPagina() {
                pie()
                doc.finishPage(pagina)
                numero++
                pagina = doc.startPage(PdfDocument.PageInfo.Builder(ancho, alto, numero).create())
                c = pagina.canvas
                y = margen
            }
            fun asegurar(espacio: Float) {
                if (y > alto - margen - 30 - espacio) nuevaPagina()
            }
            fun titulo(id: Int) {
                asegurar(60f)
                y += 8
                c.drawText(texto(id), margen, y + 12, paint(13f, bold = true))
                y += 22
            }

            // Encabezado con logo
            logo?.let { c.drawBitmap(it, null, RectF(margen, y, margen + 40, y + 40), Paint(Paint.FILTER_BITMAP_FLAG)) }
            c.drawText(texto(R.string.admin3_report_title), margen + 52, y + 18, paint(17f, bold = true))
            c.drawText(texto(R.string.admin3_report_sub, dias, hoy()), margen + 52, y + 34, paint(10.5f, gris))
            c.drawText("Karsy", ancho - margen - 52, y + 26, paint(18f, bold = true))
            y += 50
            c.drawLine(margen, y, ancho - margen, y, paint(1f, tinta).apply { strokeWidth = 2f })
            y += 14

            // Secciones del resumen en cajas de 3 columnas
            val gap = 8f
            val w = (ancho - margen * 2 - gap * 2) / 3
            val secciones = secciones(r) { id, a -> texto(id, *a) }
            secciones.chunked(3).forEach { fila ->
                val altoFila = 22f + fila.maxOf { it.second.size } * 13f
                asegurar(altoFila)
                fila.forEachIndexed { i, (tituloId, items) ->
                    val x = margen + i * (w + gap)
                    c.drawRoundRect(RectF(x, y, x + w, y + altoFila), 8f, 8f, paint(1f, linea).apply { style = Paint.Style.STROKE })
                    c.drawText(recortar(texto(tituloId), paint(10f, teal, true), w - 16), x + 8, y + 15, paint(10f, teal, true))
                    items.forEachIndexed { k, f ->
                        val yy = y + 30 + k * 13
                        val valor = when (val v = f.valor) {
                            is Int -> entero(v)
                            is Double -> if (v % 1.0 == 0.0) entero(v.toInt()) else "%.1f".format(v)
                            else -> v.toString()
                        } + (f.pct?.let { " (${(it * 100).roundToInt()}%)" } ?: "")
                        val pv = paint(9f, bold = true)
                        val anchoValor = pv.measureText(valor)
                        c.drawText(recortar(f.etiqueta, paint(8.5f, gris), w - 20 - anchoValor), x + 8, yy, paint(8.5f, gris))
                        c.drawText(valor, x + w - 8 - anchoValor, yy, pv)
                    }
                }
                y += altoFila + gap
            }

            // Crecimiento (acumulados)
            titulo(R.string.admin3_growth)
            val et = stats.crecimientoEtiquetas
            val colW = (ancho - margen * 2 - 90) / et.size.coerceAtLeast(1)
            c.drawText(texto(R.string.admin3_date), margen, y + 10, paint(8f, gris, true))
            et.forEachIndexed { i, e -> c.drawText(e, margen + 90 + i * colW, y + 10, paint(8f, gris, true)) }
            y += 16
            listOf(
                R.string.admin3_legend_users to stats.crecimientoUsuarios,
                R.string.admin3_legend_lots to stats.crecimientoLotes,
                R.string.admin3_legend_listings to stats.crecimientoPublicaciones,
            ).forEach { (id, serie) ->
                c.drawText(texto(id), margen, y + 10, paint(9.5f, bold = true))
                serie.forEachIndexed { i, v -> c.drawText(entero(v.toInt()), margen + 90 + i * colW, y + 10, paint(9.5f)) }
                y += 15
            }

            // Más vistas del rango
            if (r.masVistas.isNotEmpty()) {
                titulo(R.string.admin3_sheet_top)
                val max = (r.masVistas.maxOfOrNull { it.vistas } ?: 0).coerceAtLeast(1)
                r.masVistas.forEach { m ->
                    asegurar(26f)
                    c.drawText(recortar(m.titulo, paint(10f, bold = true), 230f), margen, y + 11, paint(10f, bold = true))
                    c.drawText(recortar(m.vendedor, paint(9f, gris), 150f), margen + 240, y + 11, paint(9f, gris))
                    c.drawText(texto(R.string.admin3_views_contacts, m.vistas, m.contactos), margen + 400, y + 11, paint(9f))
                    c.drawRoundRect(RectF(margen, y + 15, margen + 230, y + 19), 2f, 2f, relleno(Color.rgb(238, 242, 245)))
                    c.drawRoundRect(RectF(margen, y + 15, margen + maxOf(4f, 230f * m.vistas / max), y + 19), 2f, 2f, relleno(teal))
                    y += 26
                }
            }

            // Vistas por día de la semana (barras)
            // Título y barras juntos (no se separan entre hojas).
            asegurar(110f)
            titulo(R.string.admin3_by_day)
            val maxDia = (r.porDia.maxOrNull() ?: 0).coerceAtLeast(1)
            val paso = (ancho - margen * 2) / 7
            val base = y + 56
            r.porDia.take(7).forEachIndexed { i, v ->
                val h = maxOf(2f, 50f * v / maxDia)
                val x = margen + i * paso
                c.drawRoundRect(RectF(x + paso * 0.25f, base - h, x + paso * 0.75f, base), 2f, 2f, relleno(teal))
                c.drawText(texto(ExportarPanel.DIAS[i]).take(3), x + paso * 0.3f, base + 11, paint(8f, gris))
            }
            y = base + 20

            // Marcas y estados con más autos
            val listas = listOf(R.string.admin3_top_brands to r.marcas, R.string.admin3_top_states to r.estados)
            val altoListas = 22f + (listas.maxOf { it.second.size }.coerceAtLeast(1)) * 13f
            asegurar(altoListas + 10)
            y += 8
            val w2 = (ancho - margen * 2 - gap) / 2
            listas.forEachIndexed { i, (tituloId, items) ->
                val x = margen + i * (w2 + gap)
                c.drawRoundRect(RectF(x, y, x + w2, y + altoListas), 8f, 8f, paint(1f, linea).apply { style = Paint.Style.STROKE })
                c.drawText(texto(tituloId), x + 8, y + 15, paint(10f, teal, true))
                if (items.isEmpty()) c.drawText(texto(R.string.admin3_no_data), x + 8, y + 30, paint(8.5f, gris))
                items.forEachIndexed { k, (nombre, n) ->
                    val yy = y + 30 + k * 13
                    val v = entero(n)
                    val pv = paint(9f, bold = true)
                    c.drawText(recortar(nombre, paint(8.5f, gris), w2 - 30 - pv.measureText(v)), x + 8, yy, paint(8.5f, gris))
                    c.drawText(v, x + w2 - 8 - pv.measureText(v), yy, pv)
                }
            }
            y += altoListas + 6

            // Motivos de reporte más comunes
            if (r.motivos.isNotEmpty()) {
                titulo(R.string.admin3_report_reasons)
                r.motivos.forEach { (motivo, n) ->
                    asegurar(16f)
                    c.drawText(recortar("• ${entero(n)} · $motivo", paint(9.5f), ancho - margen * 2), margen, y + 10, paint(9.5f))
                    y += 16
                }
            }
            pie()
            doc.finishPage(pagina)

            val f = archivo(context, "pdf")
            f.outputStream().use { doc.writeTo(it) }
            return f
        } finally {
            doc.close()
        }
    }
}

/**
 * Botón "Exportar" del Inicio del panel de administración: Excel (.xlsx) o PDF con los
 * números de toda la app en el rango elegido.
 */
@Composable
fun ExportarAdminBoton(vm: AdminViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialogo by remember { mutableStateOf(false) }
    var exportando by remember { mutableStateOf(false) }

    // Compacto y de la misma altura que los chips de 7/30/90 días, para que quepa en la misma fila.
    OutlinedButton(
        onClick = { dialogo = true },
        enabled = vm.stats != null && !exportando,
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.height(42.dp)
    ) {
        if (exportando) CircularProgressIndicator(color = KarsyTeal, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
        else Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = KarsyInk, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(if (exportando) R.string.admin3_exporting else R.string.admin3_export),
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarsyInk
        )
    }

    if (dialogo) {
        val dias = vm.range.toInt()
        fun elegir(pdf: Boolean) {
            dialogo = false
            exportando = true
            // El archivo se arma fuera del hilo principal; el menú de compartir se abre de vuelta en él.
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        // Todo del mismo rango: las estadísticas en pantalla pueden ser del rango anterior.
                        val st = StatsRepository.admin(dias)
                        val r = StatsRepository.adminReport(dias)
                        if (pdf) ExportarAdmin.generarPdf(context, st, r, dias) else ExportarAdmin.generarExcel(context, st, r, dias)
                    }
                }.onSuccess { ExportarPanel.compartir(context, it, R.string.admin3_export_title) }
                    .onFailure {
                        if (it is CancellationException) throw it // se salió del panel: sin aviso de error
                        Toast.makeText(context, texto(R.string.admin3_export_error), Toast.LENGTH_LONG).show()
                    }
                exportando = false
            }
        }
        AlertDialog(
            onDismissRequest = { dialogo = false },
            containerColor = KarsySurface,
            title = {
                Text(stringResource(R.string.admin3_export_title), fontFamily = Outfit, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.admin3_export_sub), fontFamily = DmSans, fontSize = 13.sp, color = KarsyMid)
                    OutlinedButton(onClick = { elegir(false) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.admin3_export_excel), fontFamily = DmSans, color = KarsyInk)
                    }
                    Button(
                        onClick = { elegir(true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy)
                    ) {
                        Text(stringResource(R.string.admin3_export_pdf), fontFamily = DmSans, color = KarsyWhite)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { dialogo = false }) {
                    Text(stringResource(R.string.admin3_cancel), fontFamily = DmSans, color = KarsyMid)
                }
            }
        )
    }
}

