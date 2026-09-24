package com.pame.karsy.data.repository

import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.data.remote.CuentaDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Inicio de sesión y registro con Supabase Auth.
 * El rol (admin / lote / particular) se lee de public.cuentas y cuentas_roles,
 * no de lo que elija el usuario.
 */
object AuthRepository {

    private val auth get() = Supabase.client.auth

    suspend fun signIn(email: String, password: String): SessionAccount {
        auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        return loadAccountOrSignOut()
    }

    /** Sesión guardada de una apertura anterior de la app (o null). */
    suspend fun restoreSession(): SessionAccount? {
        auth.awaitInitialization()
        if (auth.currentSessionOrNull() == null) return null
        return runCatching { loadAccountOrSignOut() }.getOrNull()
    }

    /**
     * Crea el usuario en Supabase Auth. El trigger trg_auth_crear_cuenta crea la fila
     * de public.cuentas (y perfiles_lote / telefonos_contacto) con [data].
     */
    suspend fun signUp(email: String, password: String, data: JsonObject): SessionAccount {
        auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            this.data = data
        }
        if (auth.currentSessionOrNull() == null) {
            // El proyecto pide confirmar el correo: no hay sesión hasta confirmarlo.
            runCatching {
                auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
            }.onFailure {
                throw IllegalStateException(
                    "Tu cuenta se creó. Revisa tu correo para confirmarla y después inicia sesión."
                )
            }
        }
        return loadAccountOrSignOut()
    }

    suspend fun signOut() {
        runCatching { auth.signOut() }
    }

    private suspend fun loadAccountOrSignOut(): SessionAccount {
        val user = auth.currentUserOrNull()
            ?: throw IllegalStateException("No se pudo iniciar sesión.")

        val cuenta = Supabase.client.from("cuentas")
            .select { filter { eq("id_cuenta", user.id) } }
            .decodeSingleOrNull<CuentaDto>()

        if (cuenta == null) {
            signOut()
            throw IllegalStateException("Este usuario no tiene una cuenta de Karsy.")
        }
        if (cuenta.estadoCuenta == "suspendida") {
            signOut()
            throw IllegalStateException("Tu cuenta está suspendida. Contacta al administrador.")
        }

        val esAdmin = Supabase.client.postgrest
            .rpc("es_administrador", buildJsonObject { put("p_id_cuenta", user.id) })
            .decodeAs<Boolean>()

        return SessionAccount(
            id = cuenta.idCuenta,
            nombre = cuenta.nombreMostrar,
            tipoCuenta = cuenta.tipoCuenta,
            esAdmin = esAdmin,
            correo = user.email.orEmpty(),
        )
    }
}
