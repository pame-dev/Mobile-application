package com.pame.karsy.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.repository.AuthRepository
import com.pame.karsy.data.repository.EmailNotVerifiedException
import kotlinx.coroutines.launch

/**
 * Inicio de sesión con Supabase Auth. El destino lo decide el rol de la cuenta.
 * Si el correo no está verificado se manda a la pantalla del código ([onVerifyEmail]).
 */
class LoginViewModel : ViewModel() {
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun signIn(
        email: String,
        password: String,
        onSuccess: (SessionAccount) -> Unit,
        onVerifyEmail: (String) -> Unit,
    ) {
        if (loading) return
        if (email.isBlank() || password.isEmpty()) {
            error = "Escribe tu correo y tu contraseña."
            return
        }
        loading = true
        error = null
        viewModelScope.launch {
            safeCall { AuthRepository.signIn(email, password) }
                .onSuccess(onSuccess)
                .onFailure {
                    if (it is EmailNotVerifiedException) onVerifyEmail(it.email)
                    else error = it.mensajeUsuario()
                }
            loading = false
        }
    }
}
