package com.pame.karsy.core.session

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto

/**
 * Tipo de usuario con el que se navega la app.
 * VISITANTE: sin sesión, solo puede ver anuncios.
 * PARTICULAR / LOTE: cuenta registrada (cuentas.tipo_cuenta en Supabase).
 *
 * Ser administrador NO es un modo: es un rol extra (cuentas_roles) encima de una
 * cuenta particular o lote, que conserva todo lo suyo y además ve el panel de
 * administración. Se consulta con [SessionManager.isAdmin].
 */
enum class UserMode(@StringRes val labelRes: Int) {
    VISITANTE(R.string.core_mode_guest),
    PARTICULAR(R.string.core_mode_particular),
    LOTE(R.string.core_mode_lote);

    val label: String get() = texto(labelRes)

    val isLoggedIn: Boolean get() = this != VISITANTE

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
    /** El modo sale solo del tipo de cuenta; el rol de admin se suma aparte. */
    val mode: UserMode
        get() = if (tipoCuenta == "lote") UserMode.LOTE else UserMode.PARTICULAR
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

    /** La cuenta en sesión tiene además el rol "administrador". */
    val isAdmin: Boolean get() = account?.esAdmin == true

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
