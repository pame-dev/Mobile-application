package com.pame.karsy.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * "¿Olvidaste tu contraseña?": correo → código → contraseña nueva.
 * Supabase Auth genera, manda y valida el código; al validarlo abre una sesión
 * temporal que solo sirve para cambiar la contraseña y después se cierra.
 */
class PasswordRecoveryViewModel : ViewModel() {
    /** 0 = formulario de login, 1 = correo, 2 = código, 3 = contraseña nueva. */
    var step by mutableIntStateOf(0)
        private set

    var email by mutableStateOf("")
    val code = mutableStateListOf(*Array(EMAIL_CODE_LENGTH) { "" })
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    val countdown = ResendCountdown(viewModelScope)

    var sending by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    /** La contraseña ya se cambió: se muestra el aviso para iniciar sesión. */
    var success by mutableStateOf(false)
        private set

    val passwordMismatch: Boolean
        get() = confirmPassword.isNotEmpty() && newPassword != confirmPassword

    /** Correo al que se mandó el último código. */
    private var sentTo: String? = null
    /** El código ya se validó y la sesión temporal sigue abierta. */
    private var recoverySession = false

    /** Desde el login; usa el correo que ya se había escrito ahí. */
    fun start(loginEmail: String) {
        if (loginEmail.isNotBlank()) email = loginEmail.trim()
        clearCode()
        newPassword = ""
        confirmPassword = ""
        error = null
        success = false
        step = 1
    }

    /** Paso 1: manda el código y pasa a escribirlo. */
    fun sendCode() {
        if (!email.contains("@")) {
            error = "Escribe el correo de tu cuenta."
            return
        }
        // Si regresó al paso 1 sin cambiar el correo, el código de hace unos segundos sigue sirviendo.
        if (countdown.seconds > 0 && sentTo == email.trim()) {
            error = null
            step = 2
            return
        }
        send { step = 2 }
    }

    fun resend() {
        if (countdown.seconds == 0) send {}
    }

    /** Paso 2: valida el código y pasa a la contraseña nueva. */
    fun verifyCode() {
        if (loading) return
        val token = code.joinToString("")
        if (token.length < EMAIL_CODE_LENGTH) {
            error = "Escribe los $EMAIL_CODE_LENGTH dígitos del código."
            return
        }
        loading = true
        error = null
        viewModelScope.launch {
            safeCall { AuthRepository.verifyPasswordResetCode(email, token) }
                .onSuccess {
                    recoverySession = true
                    step = 3
                }
                .onFailure { error = it.mensajeUsuario() }
            loading = false
        }
    }

    /** Paso 3: guarda la contraseña nueva si coincide con la confirmación. */
    fun resetPassword() {
        if (loading) return
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            error = "Escribe tu nueva contraseña y confírmala."
            return
        }
        if (newPassword != confirmPassword) return // el aviso "no coinciden" ya está en pantalla
        loading = true
        error = null
        viewModelScope.launch {
            safeCall { AuthRepository.resetPassword(newPassword) }
                .onSuccess {
                    recoverySession = false
                    success = true
                }
                .onFailure { error = it.mensajeUsuario() }
            loading = false
        }
    }

    /**
     * Flecha atrás. Desde la contraseña nueva se cancela todo: el código ya se usó,
     * así que se cierra la sesión temporal y se vuelve al login.
     */
    fun back() {
        error = null
        if (step == 3) {
            closeRecoverySession()
            step = 0
        } else {
            step -= 1
        }
    }

    /** Botón "Iniciar Sesión" del aviso de éxito. */
    fun finish() {
        success = false
        newPassword = ""
        confirmPassword = ""
        step = 0
    }

    private fun send(onSent: () -> Unit) {
        if (sending) return
        sending = true
        error = null
        viewModelScope.launch {
            safeCall { AuthRepository.sendPasswordResetCode(email) }
                .onSuccess {
                    sentTo = email.trim()
                    clearCode()
                    countdown.start()
                    onSent()
                }
                .onFailure { error = it.mensajeUsuario() }
            sending = false
        }
    }

    private fun closeRecoverySession() {
        if (!recoverySession) return
        recoverySession = false
        viewModelScope.launch { AuthRepository.signOut() }
    }

    private fun clearCode() {
        for (i in code.indices) code[i] = ""
    }
}
