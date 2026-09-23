package com.pame.karsy.data.model

/**
 * Información completa de un anuncio para la pantalla de detalle.
 * TODO: construir desde publicaciones (ficha técnica) + cuentas/telefonos_contacto (vendedor).
 */
data class CarDetail(
    val car: Car,
    val gallery: List<String>,
    val transmision: String,
    val kilometraje: String,
    val cilindros: String,
    val caballos: String,
    val tipoCarro: String,
    val color: String,
    val cantDuenos: String,
    val contactoNombre: String,
    val contactoTelefono: String,
    val contactoCorreo: String,
    val detalles: String,
    val descripcionLarga: String,
)
