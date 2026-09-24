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

/** Cuenta con sesión iniciada en Supabase Auth (fila de public.cuentas + rol). */
data class SessionAccount(
    val id: String,
    val nombre: String,
    val tipoCuenta: String,
    val esAdmin: Boolean,
    val correo: String,
) {
    val mode: UserMode
        get() = when {
            esAdmin -> UserMode.ADMIN
            tipoCuenta == "lote" -> UserMode.LOTE
            else -> UserMode.PARTICULAR
        }
}

/**
 * Sesión en memoria. El token lo guarda Supabase Auth (se restaura al abrir la app);
 * aquí solo se guarda la cuenta y el modo que decide qué ve el usuario.
 */
object SessionManager {
    var userMode by mutableStateOf(UserMode.VISITANTE)
        private set
    var account by mutableStateOf<SessionAccount?>(null)
        private set

    val userId: String? get() = account?.id

    fun login(account: SessionAccount) {
        this.account = account
        userMode = account.mode
    }

    fun enterAsGuest() {
        account = null
        userMode = UserMode.VISITANTE
    }

    fun updateName(nombre: String) {
        account = account?.copy(nombre = nombre)
    }

    fun logout() {
        account = null
        userMode = UserMode.VISITANTE
    }
}
