package com.pame.karsy.data.model

/** Lote o agencia del directorio (cuentas tipo "lote" + perfiles_lote). */
data class Lot(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val description: String,
    /** "Municipio, Estado". */
    val city: String,
    /** Calle, número, colonia, CP, municipio y estado; vacío si el lote no la registró. */
    val address: String,
    val hours: String?,
    /** Publicaciones visibles (aprobadas, activas y habilitadas). */
    val vehicleCount: Int,
)

/** Números del lote para "Llamar" y "WhatsApp" (solo se piden con sesión iniciada). */
data class LotContact(
    val phone: String?,
    val whatsapp: String?,
)
