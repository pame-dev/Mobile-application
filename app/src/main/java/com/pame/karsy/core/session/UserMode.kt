package com.pame.karsy.core.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Tipo de usuario con el que se navega la app.
 * VISITANTE: sin sesión, solo puede ver anuncios.
 * PARTICULAR / LOTE: cuenta registrada (cuentas.tipo_cuenta en Supabase).
 * ADMIN: cuenta con el rol "administrador" (public.es_administrador()).
 */
enum class UserMode(val label: String) {
    VISITANTE("Visitante"),
    PARTICULAR("Particular"),
    LOTE("Lote"),
    ADMIN("Administrador");

    val isLoggedIn: Boolean get() = this != VISITANTE
    val isAdmin: Boolean get() = this == ADMIN

    /** Particulares y lotes pueden publicar, guardar favoritos y ver su panel. */
    val isSeller: Boolean get() = this == PARTICULAR || this == LOTE
}

/**
 * Sesión en memoria. De momento solo guarda el modo elegido en el login.
 * TODO: reemplazar por la sesión de Supabase Auth (supabase.auth.currentSessionOrNull())
 *  y leer tipo_cuenta / rol desde las tablas cuentas y cuentas_roles.
 */
object SessionManager {
    var userMode by mutableStateOf(UserMode.VISITANTE)

    fun login(mode: UserMode) {
        userMode = mode
    }

    fun logout() {
        // TODO: supabase.auth.signOut()
        userMode = UserMode.VISITANTE
    }
}
