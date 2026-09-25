package com.pame.karsy.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.repository.AuthRepository
import com.pame.karsy.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * Verificación del correo con el código que genera y valida Supabase Auth.
 * Desde el login se pide un código nuevo al abrir la pantalla; desde el registro
 * Supabase ya lo mandó al crear la cuenta.
 */
class VerifyEmailViewModel : ViewModel() {
    val code = mutableStateListOf(*Array(EMAIL_CODE_LENGTH) { "" })
    val countdown = ResendCountdown(viewModelScope)

    var verifying by mutableStateOf(false)
        private set
    var sending by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var started = false

    fun start(email: String, sendCode: Boolean) {
        if (started) return
        started = true
        if (sendCode) resend(email) else countdown.start()
    }

    fun resend(email: String) {
        if (sending || countdown.seconds > 0) return
        sending = true
        error = null
        viewModelScope.launch {
            safeCall { AuthRepository.sendVerificationCode(email) }
                .onSuccess {
                    for (i in code.indices) code[i] = ""
                    countdown.start()
                }
                .onFailure { error = it.mensajeUsuario() }
            sending = false
        }
    }

    fun verify(email: String, onVerified: (SessionAccount) -> Unit) {
        if (verifying) return
        val token = code.joinToString("")
        if (token.length < EMAIL_CODE_LENGTH) {
            error = "Escribe los $EMAIL_CODE_LENGTH dígitos del código."
            return
        }
        verifying = true
        error = null
        viewModelScope.launch {
            safeCall { AuthRepository.verifyEmail(email, token) }
                .onSuccess { cuenta ->
                    // Ya hay sesión: se sube el logo que se eligió en el registro (si hubo).
                    SessionManager.login(cuenta)
                    safeCall { UserRepository.uploadPendingAvatar(email) }
                    onVerified(cuenta)
                }
                .onFailure { error = it.mensajeUsuario() }
            verifying = false
        }
    }
}
