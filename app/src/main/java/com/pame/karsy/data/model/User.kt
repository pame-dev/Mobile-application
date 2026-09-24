package com.pame.karsy.data.model

/**
 * Perfil de la cuenta en sesión.
 * Se arma desde public.cuentas (+ perfiles_lote si es lote, + teléfono principal y correo).
 */
data class User(
    val id: String,
    val displayName: String,
    val email: String,
    val phone: String,
    val location: String,
    val bio: String,
    val avatarUrl: String?,
    val memberSince: String,
    val accountType: String, // "particular" | "lote"
)
