package com.pame.karsy.core.util

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Precio sugerido al publicar: rango y precio típico de autos similares que ya están en
 * Karsy (mismo modelo y años parecidos). Igual que web/src/lib/precioSugerido.ts.
 */
object PrecioSugerido {
    /** Mínimo de autos para sugerir algo; con menos, la sugerencia no sería confiable. */
    const val MINIMO_SIMILARES = 2

    /** [tipico] es la mediana: el precio "del medio", sin que lo muevan los muy caros o baratos. */
    data class Rango(val n: Int, val min: Long, val max: Long, val tipico: Long)

    enum class Comparacion { ABAJO, DENTRO, ARRIBA }

    /**
     * Rango de precios de autos similares, redondeado a miles. Con 5 o más se usa del 20 % al
     * 80 % (para que un precio raro no estire el rango); con menos, del más barato al más caro.
     * null si hay menos de [MINIMO_SIMILARES].
     */
    fun calcular(precios: List<Double>): Rango? {
        val ordenados = precios.filter { it.isFinite() && it > 0 }.sorted()
        if (ordenados.size < MINIMO_SIMILARES) return null
        val conPercentiles = ordenados.size >= 5
        return Rango(
            n = ordenados.size,
            min = redondear(if (conPercentiles) percentil(ordenados, 0.2) else ordenados.first()),
            max = redondear(if (conPercentiles) percentil(ordenados, 0.8) else ordenados.last()),
            tipico = redondear(percentil(ordenados, 0.5)),
        )
    }

    /** Dónde queda el precio escrito respecto al rango. */
    fun comparar(precio: Long, r: Rango): Comparacion = when {
        precio < r.min -> Comparacion.ABAJO
        precio > r.max -> Comparacion.ARRIBA
        else -> Comparacion.DENTRO
    }

    private fun redondear(n: Double): Long = (n / 1000).roundToLong() * 1000

    /** Valor en la posición p (0–1) de una lista ordenada, interpolando entre vecinos. */
    private fun percentil(ordenados: List<Double>, p: Double): Double {
        val i = (ordenados.size - 1) * p
        val abajo = floor(i).toInt()
        val arriba = ceil(i).toInt()
        return ordenados[abajo] + (ordenados[arriba] - ordenados[abajo]) * (i - abajo)
    }
}
