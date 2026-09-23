package com.pame.karsy.data.model

/**
 * Perfil público de una cuenta.
 * TODO: mapear desde public.cuentas (+ perfiles_lote cuando tipo_cuenta = 'lote').
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
