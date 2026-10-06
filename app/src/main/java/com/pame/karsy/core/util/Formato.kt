package com.pame.karsy.core.util

import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.locale.textoPlural
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Formatos de texto para mostrar datos de la BD. */
object Formato {

    // Nombres de meses en el idioma actual (lista separada por comas en strings.xml).
    private val MESES: List<String> get() = texto(R.string.core_months_short).split(",")
    private val MESES_LARGOS: List<String> get() = texto(R.string.core_months_long).split(",")

    private val numeros = NumberFormat.getIntegerInstance(Locale.US)

    /** 229000.0 -> "$229,000" */
    fun precio(valor: Double?): String = if (valor == null) "—" else "$" + numeros.format(valor)

    /** 48000 -> "48,000 km" */
    fun km(valor: Int?): String = if (valor == null) "—" else numeros.format(valor) + " km"

    fun entero(valor: Int): String = numeros.format(valor)

    /** "jahir@gmail.com" -> "ja*****@gmail.com"; null si no parece un correo. */
    fun correoOculto(correo: String): String? {
        val partes = correo.trim().split("@")
        if (partes.size < 2 || partes[1].isEmpty()) return null
        val nombre = partes[0]
        val visible = nombre.take(2)
        return visible + "*".repeat(maxOf(5, nombre.length - visible.length)) + "@" + partes[1]
    }

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
            minutos < 1 -> texto(R.string.core_time_just_now)
            minutos < 60 -> texto(R.string.core_time_minutes_ago, minutos.toInt())
            minutos < 60 * 24 -> (minutos / 60).toInt().let { textoPlural(R.plurals.core_time_hours_ago, it, it) }
            minutos < 60 * 48 -> texto(R.string.core_time_yesterday)
            minutos < 60 * 24 * 7 -> (minutos / (60 * 24)).toInt().let { textoPlural(R.plurals.core_time_days_ago, it, it) }
            minutos < 60 * 24 * 14 -> textoPlural(R.plurals.core_time_weeks_ago, 1, 1)
            minutos < 60 * 24 * 30 -> (minutos / (60 * 24 * 7)).toInt().let { textoPlural(R.plurals.core_time_weeks_ago, it, it) }
            else -> fechaCorta(iso)
        }
    }

    /** true si [iso] es dentro de [dias] días o menos (y aún no pasa). */
    @OptIn(ExperimentalTime::class)
    fun terminaEnDias(iso: String?, dias: Int): Boolean {
        val fin = instante(iso) ?: return false
        val restante = fin - Clock.System.now()
        return restante.isPositive() && restante.inWholeHours <= dias * 24L
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
