package com.pame.karsy.data.model

/**
 * Vehículo tal como se muestra en listas y tarjetas.
 * Se construye desde la vista public.v_anuncios (ver CarRepository).
 */
data class Car(
    val id: Long,
    val brand: String,
    val model: String,
    val year: Int,
    val price: String,
    val currency: String,
    val description: String,
    val imageUrl: String?,
    val badge: String? = null,
    val condition: String = "Seminuevo",
    val kilometraje: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    /** Estado para el dueño / admin: Activo, Pendiente, Rechazado, Vendido, Deshabilitado. */
    val status: String = "Activo",
    // Campos numéricos para filtrar y ordenar en la app.
    val priceValue: Double = 0.0,
    val brandId: Long? = null,
    val bodyTypeId: Long? = null,
    val bodyType: String = "",
    val publishedAt: String = "",
    val featured: Boolean = false,
    val featuredPending: Boolean = false,
) {
    val title: String get() = "$brand $model"
}
