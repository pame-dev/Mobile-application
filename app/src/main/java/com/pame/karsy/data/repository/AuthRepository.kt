package com.pame.karsy.data.repository

import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.core.theme.Tema
import com.pame.karsy.core.util.UserFacingException
import com.pame.karsy.data.remote.CuentaDto
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** La cuenta existe pero su correo no está verificado: la app debe pedir el código. */
class EmailNotVerifiedException(val email: String) :
    UserFacingException(texto(R.string.core_error_verify_email))

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

    /**
     * Cambia la contraseña desde el perfil. Primero comprueba la actual iniciando sesión
     * con ella (Supabase no la pide para cambiarla); si no coincide no se cambia nada.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val email = auth.currentUserOrNull()?.email
            ?: throw UserFacingException(texto(R.string.core_error_login_first))
        try {
            auth.signInWith(Email) {
                this.email = email
                this.password = currentPassword
            }
        } catch (e: AuthRestException) {
            if (e.error == "invalid_credentials" || e.error == "invalid_grant") {
                throw UserFacingException(texto(R.string.profile_password_wrong_current))
            }
            throw e
        }
        auth.updateUser { password = newPassword }
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
                throw UserFacingException(texto(R.string.core_error_email_exists))
            }
            throw EmailNotVerifiedException(email.trim())
        }
        return loadAccountOrSignOut()
    }

    /** true si el número ya está registrado en otra cuenta (función telefono_en_uso). */
    suspend fun telefonoEnUso(numero: String): Boolean =
        Supabase.client.postgrest.rpc("telefono_en_uso", buildJsonObject { put("p_numero", numero) })
            .decodeAs<Boolean>()

    /**
     * Cierra la sesión solo en este teléfono. Si Supabase no responde (sin internet), la
     * sesión guardada se borra igual: si no, al volver a abrir la app se entraba sola.
     */
    suspend fun signOut() {
        runCatching { auth.signOut(SignOutScope.LOCAL) }
            .onFailure { runCatching { auth.clearSession() } }
    }

    /**
     * Cierra la sesión en todos los dispositivos de la cuenta, incluido este. La Edge
     * Function cerrar-sesiones invalida todas las sesiones y avisa por push a los otros
     * teléfonos para que se cierren al instante.
     */
    suspend fun signOutEverywhere() {
        closingEverywhere = true
        try {
            try {
                Supabase.client.functions.invoke("cerrar-sesiones")
            } catch (e: RestException) {
                // Si la sesión de este teléfono ya estaba cerrada desde otro lado, cuenta como
                // hecho. Si no, la función no está desplegada o falló: se cierran igual todas
                // las sesiones, sin el aviso inmediato (los teléfonos se enteran en un minuto).
                if (!sessionRevoked()) auth.signOut(SignOutScope.GLOBAL)
            }
        } catch (e: Exception) {
            closingEverywhere = false
            throw e
        }
        signOut()
    }

    /**
     * true desde que este teléfono pidió cerrar en todos lados hasta el siguiente inicio de
     * sesión: su propia sesión revocada no debe mostrarle "se cerró desde otro dispositivo".
     */
    @Volatile
    var closingEverywhere = false
        private set

    /**
     * Avisa a la pantalla que la sesión se cerró desde otro dispositivo (push
     * "sesion_cerrada"). Sin pantalla abierta el aviso se descarta: al abrir la app ya no
     * hay sesión.
     */
    val closedElsewhere = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Llegó el push "sesion_cerrada". Antes de cerrar se comprueba con Supabase que la
     * sesión ya no exista: un aviso atrasado no debe cerrar una sesión iniciada después.
     * Si Android termina el proceso antes de acabar, la app lo detecta al volver a ella.
     */
    suspend fun onSessionClosedPush() {
        if (closingEverywhere) return
        auth.awaitInitialization()
        if (!sessionRevoked()) return
        signOut()
        closedElsewhere.tryEmit(Unit)
    }

    /**
     * true si Supabase ya no reconoce la sesión de este teléfono: se cerró en todos los
     * dispositivos desde otro lado (o la cuenta ya no existe). Si no hay sesión guardada
     * también es true (Supabase la borra al fallar la renovación). Los errores de red se
     * lanzan: sin poder comprobarla no se cierra la sesión (si falló la renovación, es false).
     */
    suspend fun sessionRevoked(): Boolean {
        // Al volver de segundo plano Supabase recarga la sesión (estado Initializing):
        // hay que esperarla, si no parece cerrada.
        auth.awaitInitialization()
        // Falló la renovación (sin internet): no se puede comprobar, no se cierra.
        if (auth.sessionStatus.value is SessionStatus.RefreshFailure) return false
        if (auth.currentSessionOrNull() == null) return true
        return try {
            auth.retrieveUserForCurrentSession()
            false
        } catch (e: AuthRestException) {
            e.error == "session_not_found" || e.error == "user_not_found"
        }
    }

    private suspend fun loadAccountOrSignOut(): SessionAccount {
        closingEverywhere = false
        val user = auth.currentUserOrNull()
            ?: throw UserFacingException(texto(R.string.core_error_sign_in_failed))

        val cuenta = Supabase.client.from("cuentas")
            .select { filter { eq("id_cuenta", user.id) } }
            .decodeSingleOrNull<CuentaDto>()

        if (cuenta == null) {
            signOut()
            throw UserFacingException(texto(R.string.core_error_no_karsy_account))
        }
        if (cuenta.estadoCuenta == "suspendida") {
            signOut()
            throw UserFacingException(texto(R.string.core_error_account_suspended))
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
            tema = if (cuenta.tema == "sistema") Tema.SISTEMA else cuenta.tema,
        )
    }
}
