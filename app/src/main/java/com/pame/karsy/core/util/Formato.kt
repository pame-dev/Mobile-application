package com.pame.karsy.core.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Formatos de texto para mostrar datos de la BD. */
object Formato {

    private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    private val MESES_LARGOS = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    private val numeros = NumberFormat.getIntegerInstance(Locale.US)

    /** 229000.0 -> "$229,000" */
    fun precio(valor: Double?): String = if (valor == null) "—" else "$" + numeros.format(valor)

    /** 48000 -> "48,000 km" */
    fun km(valor: Int?): String = if (valor == null) "—" else numeros.format(valor) + " km"

    fun entero(valor: Int): String = numeros.format(valor)

    /** "2026-09-23T20:38:05+00:00" -> "23 sep 2026" */
    fun fechaCorta(iso: String?): String {
        val fecha = local(iso) ?: return "—"
        return "${fecha.day} ${MESES[fecha.month.ordinal]} ${fecha.year}"
    }

    /** "2024-03-10T…" -> "Marzo 2024" */
    fun mesAnio(iso: String?): String {
        val fecha = local(iso) ?: return "—"
        return "${MESES_LARGOS[fecha.month.ordinal]} ${fecha.year}"
    }

    /** Tiempo transcurrido: "Hace 5 min", "Hace 2 horas", "Ayer", "Hace 3 días"… */
    @OptIn(ExperimentalTime::class)
    fun haceCuanto(iso: String?): String {
        val instante = instante(iso) ?: return ""
        val minutos = (Clock.System.now() - instante).inWholeMinutes
        return when {
            minutos < 1 -> "Justo ahora"
            minutos < 60 -> "Hace $minutos min"
            minutos < 60 * 24 -> (minutos / 60).let { if (it == 1L) "Hace 1 hora" else "Hace $it horas" }
            minutos < 60 * 48 -> "Ayer"
            minutos < 60 * 24 * 7 -> "Hace ${minutos / (60 * 24)} días"
            minutos < 60 * 24 * 14 -> "Hace 1 semana"
            minutos < 60 * 24 * 30 -> "Hace ${minutos / (60 * 24 * 7)} semanas"
            else -> fechaCorta(iso)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun instante(iso: String?): Instant? =
        iso?.let { runCatching { Instant.parse(it.replace(' ', 'T').let(::conZona)) }.getOrNull() }

    @OptIn(ExperimentalTime::class)
    private fun local(iso: String?) =
        instante(iso)?.toLocalDateTime(TimeZone.currentSystemDefault())

    // PostgREST devuelve "+00:00"; si viniera sin zona se asume UTC.
    private fun conZona(s: String): String =
        if (s.endsWith("Z") || Regex("[+-]\\d{2}(:?\\d{2})?$").containsMatchIn(s.substringAfter('T'))) s else s + "Z"
}
