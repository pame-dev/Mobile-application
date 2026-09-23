package com.pame.karsy.feature.admin.charts

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyMid

/** Serie de la gráfica de líneas. [fillArea] dibuja el degradado bajo la línea. */
data class ChartSeries(
    val values: List<Float>,
    val color: Color,
    val fillArea: Boolean = false,
)

/**
 * Gráfica de líneas "Crecimiento de la plataforma" (usuarios, lotes y publicaciones por día),
 * réplica del SVG del mockup dibujada con Canvas.
 */
@Composable
fun GrowthChart(
    labels: List<String>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    maxValue: Float = 800f,
    gridValues: List<Int> = listOf(0, 200, 400, 600, 800),
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = DmSans, fontSize = 10.sp, color = KarsyMid)

    Canvas(modifier) {
        val left = 34.dp.toPx()
        val right = size.width - 14.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val plotHeight = bottom - top

        fun yOf(v: Float) = bottom - (v / maxValue) * plotHeight
        fun xOf(i: Int, count: Int) =
            if (count <= 1) left else left + i.toFloat() / (count - 1) * (right - left)

        // Líneas guía y valores del eje Y
        gridValues.forEach { v ->
            val y = yOf(v.toFloat())
            drawLine(KarsyBg, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val layout = textMeasurer.measure(v.toString(), labelStyle)
            drawText(
                layout,
                topLeft = Offset(left - 8.dp.toPx() - layout.size.width, y - layout.size.height / 2f)
            )
        }

        // Etiquetas del eje X
        labels.forEachIndexed { i, label ->
            val layout = textMeasurer.measure(label, labelStyle)
            drawText(
                layout,
                topLeft = Offset(xOf(i, labels.size) - layout.size.width / 2f, bottom + 6.dp.toPx())
            )
        }

        // Áreas con degradado
        series.filter { it.fillArea && it.values.isNotEmpty() }.forEach { s ->
            val path = Path().apply {
                s.values.forEachIndexed { i, v ->
                    val x = xOf(i, s.values.size)
                    val y = yOf(v)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                lineTo(xOf(s.values.lastIndex, s.values.size), bottom)
                lineTo(left, bottom)
                close()
            }
            val minY = yOf(s.values.max())
            drawPath(
                path,
                Brush.verticalGradient(
                    listOf(s.color.copy(alpha = 0.22f), s.color.copy(alpha = 0f)),
                    startY = minY,
                    endY = bottom
                )
            )
        }

        // Líneas y puntos
        series.forEach { s ->
            val path = Path()
            s.values.forEachIndexed { i, v ->
                val x = xOf(i, s.values.size)
                val y = yOf(v)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path,
                s.color,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            s.values.forEachIndexed { i, v ->
                val center = Offset(xOf(i, s.values.size), yOf(v))
                drawCircle(Color.White, radius = 3.5.dp.toPx(), center = center)
                drawCircle(s.color, radius = 3.5.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
            }
        }
    }
}
