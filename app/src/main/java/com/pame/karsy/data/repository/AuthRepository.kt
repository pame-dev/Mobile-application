package com.pame.karsy.data.repository

import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.core.util.UserFacingException
import com.pame.karsy.data.remote.CuentaDto
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** La cuenta existe pero su correo no está verificado: la app debe pedir el código. */
class EmailNotVerifiedException(val email: String) :
    UserFacingException("Verifica tu correo para continuar.")

/**
 * Inicio de sesión y registro con Supabase Auth.
 * El rol (admin / lote / particular) se lee de public.cuentas y cuentas_roles,
 * no de lo que elija el usuario.
 *
 * Verificación de correo: con "Confirm email" activo en Supabase, las cuentas nuevas
 * no tienen sesión hasta escribir el código de 6 dígitos que Supabase manda al correo
 * (plantilla "Confirm signup" con {{ .Token }}, ver supabase/templates).
 */
object AuthRepository {

    private val auth get() = Supabase.client.auth

    /** Lanza [EmailNotVerifiedException] si la contraseña es correcta pero falta verificar el correo. */
    suspend fun signIn(email: String, password: String): SessionAccount {
        try {
            auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        } catch (e: AuthRestException) {
            if (e.error == "email_not_confirmed") throw EmailNotVerifiedException(email.trim())
            throw e
        }
        return loadAccountOrSignOut()
    }

    /** Genera un código nuevo y lo manda al correo (el anterior deja de servir). */
    suspend fun sendVerificationCode(email: String) {
        auth.resendEmail(OtpType.Email.SIGNUP, email.trim())
    }

    /** Valida el código del correo; si es correcto Supabase abre la sesión. */
    suspend fun verifyEmail(email: String, code: String): SessionAccount {
        auth.verifyEmailOtp(OtpType.Email.SIGNUP, email.trim(), code)
        return loadAccountOrSignOut()
    }

    // ── Olvidé mi contraseña ─────────────────────────────────────────
    // 1. Código al correo (plantilla "Reset Password" con {{ .Token }}). Si el correo no
    //    tiene cuenta Supabase responde igual, para no revelar qué correos existen.
    // 2. El código abre una sesión temporal. 3. Con ella se cambia la contraseña.

    suspend fun sendPasswordResetCode(email: String) {
        auth.resetPasswordForEmail(email.trim())
    }

    suspend fun verifyPasswordResetCode(email: String, code: String) {
        auth.verifyEmailOtp(OtpType.Email.RECOVERY, email.trim(), code)
    }

    /** Guarda la contraseña nueva y cierra la sesión temporal para entrar con ella. */
    suspend fun resetPassword(newPassword: String) {
        auth.updateUser { password = newPassword }
        signOut()
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
     * Lanza [EmailNotVerifiedException] si hay que verificar el correo (el código ya se envió).
     */
    suspend fun signUp(email: String, password: String, data: JsonObject): SessionAccount {
        val user = auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            this.data = data
        }
        if (auth.currentSessionOrNull() == null) {
            // Si el correo ya tiene cuenta verificada, Supabase responde un usuario sin
            // identidades y no manda nada (para no revelar qué correos existen).
            if (user?.identities?.isEmpty() == true) {
                throw UserFacingException("Ya existe una cuenta con ese correo.")
            }
            throw EmailNotVerifiedException(email.trim())
        }
        return loadAccountOrSignOut()
    }

    suspend fun signOut() {
        runCatching { auth.signOut() }
    }

    private suspend fun loadAccountOrSignOut(): SessionAccount {
        val user = auth.currentUserOrNull()
            ?: throw UserFacingException("No se pudo iniciar sesión.")

        val cuenta = Supabase.client.from("cuentas")
            .select { filter { eq("id_cuenta", user.id) } }
            .decodeSingleOrNull<CuentaDto>()

        if (cuenta == null) {
            signOut()
            throw UserFacingException("Este usuario no tiene una cuenta de Karsy.")
        }
        if (cuenta.estadoCuenta == "suspendida") {
            signOut()
            throw UserFacingException("Tu cuenta está suspendida. Contacta al administrador.")
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
