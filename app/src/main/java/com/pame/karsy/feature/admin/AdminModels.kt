package com.pame.karsy.feature.admin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto

// Modelos que muestran las secciones del panel de administración.
// Se llenan desde Supabase en AdminViewModel.

data class AdminUser(
    val id: String,
    val name: String,
    val email: String,
    val type: String,
    val date: String,
    val status: String,
    val posts: Int,
    val isAdmin: Boolean = false,
)

data class AdminLot(
    val id: String,
    val name: String,
    val responsible: String,
    val city: String,
    val vehicles: Int,
    val status: String,
)

data class AdminVehicle(
    val id: Long,
    /** Propuesta pendiente de revisión (null si no hay nada que moderar). */
    val pendingProposalId: Long?,
    val name: String,
    val seller: String,
    val year: Int,
    val price: String,
    val status: String,
    val transmision: String,
    val kilometraje: String,
    val tipo: String,
    val color: String,
    val imageUrl: String?,
    val enabledByAdmin: Boolean,
)

data class AdminReport(
    val id: Long,
    val publicationId: Long,
    val type: String,
    val reporter: String,
    val target: String,
    val imageUrl: String?,
    val reason: String,
    val resolution: String?,
    val date: String,
    val status: String,
)

enum class FeaturedStatus { Pendiente, Aprobada, Rechazada }

data class FeaturedRequest(
    val id: Long,
    val publicationId: Long,
    val vehicle: String,
    val year: Int,
    val price: String,
    val image: String?,
    val user: String,
    val userAvatar: String?,
    val time: String,
    val status: FeaturedStatus,
    val transmision: String,
    val kilometraje: String,
    val color: String,
    val combustible: String,
    val description: String,
)

/** [change] es lo nuevo en el rango elegido; null cuando no aplica un cambio. */
data class AdminKpi(
    val label: String,
    val value: String,
    val change: String?,
    val icon: ImageVector,
    val color: Color,
    val bg: Color,
)

data class AdminAlert(
    val text: String,
    val icon: ImageVector,
    val bg: Color,
    val color: Color,
    val target: AdminSection,
)

object AdminOptions {
    // Getter: se resuelve en el idioma actual cada vez que se lee.
    val rejectOptions: List<Pair<String, String>>
        get() = listOf(
            texto(R.string.admin1_reject_payment_title) to texto(R.string.admin1_reject_payment_desc),
            texto(R.string.admin1_reject_images_title) to texto(R.string.admin1_reject_images_desc),
            texto(R.string.admin1_reject_incomplete_title) to texto(R.string.admin1_reject_incomplete_desc),
            texto(R.string.admin1_reject_not_allowed_title) to texto(R.string.admin1_reject_not_allowed_desc),
            texto(R.string.admin1_reject_other_title) to texto(R.string.admin1_reject_other_desc),
        )
}

/** Valores del eje Y (5 líneas) que cubren [max] con números redondos. */
internal fun niceTicks(max: Float): List<Int> {
    val top = maxOf(4f, max * 1.1f)
    val rawStep = top / 4f
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(rawStep.toDouble()))).toFloat()
    val step = listOf(1f, 2f, 5f, 10f).first { it * magnitude >= rawStep } * magnitude
    return (0..4).map { Math.round(it * step) }
}
