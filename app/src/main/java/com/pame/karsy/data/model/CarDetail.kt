package com.pame.karsy.data.model

/**
 * Información completa de un anuncio para la pantalla de detalle.
 * Ficha técnica desde v_anuncios; los datos de contacto (teléfono y correo) se piden
 * aparte con contacto_vendedor() cuando el usuario toca "Contactar".
 */
data class CarDetail(
    val car: Car,
    val gallery: List<String>,
    val transmision: String,
    val kilometraje: String,
    val cilindros: String,
    val motor: String,
    val tipoCarro: String,
    val color: String,
    val cantDuenos: String,
    val combustible: String,
    val ubicacion: String,
    val sellerId: String,
    val sellerType: String,
    val sellerAvatar: String?,
    val contactoNombre: String,
    val detalles: String,
    val descripcionLarga: String,
)

/** Datos de contacto del vendedor (función contacto_vendedor). */
data class SellerContact(
    val id: String,
    val nombre: String,
    val tipoCuenta: String,
    val descripcion: String,
    val ubicacion: String,
    val avatarUrl: String?,
    val telefono: String?,
    val whatsapp: Boolean,
    val correo: String?,
)
