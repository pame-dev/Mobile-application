package com.pame.karsy.data.model

/**
 * Vehículo tal como se muestra en listas y tarjetas.
 * TODO: mapear desde public.publicaciones + fotos_publicacion + catálogos (marcas, modelos...).
 */
data class Car(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val price: String,
    val description: String,
    val imageUrl: String,
    val badge: String? = null,
    val condition: String = "Seminuevo",
    val rating: Double = 4.7,
) {
    val title: String get() = "$brand $model"
}
