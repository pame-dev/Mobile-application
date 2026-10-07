package com.pame.karsy.feature.profile

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.session.PublicacionesOcultas
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.Imagenes
import com.pame.karsy.core.util.PasswordRules
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.model.User
import com.pame.karsy.data.repository.AuthRepository
import com.pame.karsy.data.repository.CarRepository
import com.pame.karsy.data.repository.UserRepository
import kotlinx.coroutines.launch

/** Perfil de la cuenta en sesión y sus publicaciones. */
class ProfileViewModel : ViewModel() {
    var user by mutableStateOf<User?>(null)
        private set
    var cars by mutableStateOf<List<Car>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var message by mutableStateOf<String?>(null)
    var saving by mutableStateOf(false)
        private set
    /** Error al guardar el perfil (se muestra dentro del diálogo). */
    var editError by mutableStateOf<String?>(null)
        private set
    /** Error al cambiar la contraseña (se muestra dentro del diálogo). */
    var passwordError by mutableStateOf<String?>(null)
        private set

    /** Las que el dueño ocultó (rechazadas / deshabilitadas) van aparte, en el desplegable de ocultas. */
    val visibleCars: List<Car> get() = cars.filterNot(PublicacionesOcultas::estaOculta)
    val hiddenCars: List<Car> get() = cars.filter(PublicacionesOcultas::estaOculta)

    val stats: List<Pair<String, String>>
        get() = listOf(
            visibleCars.size.toString() to texto(R.string.profile_stat_posts),
            cars.count { it.status == "Vendido" }.toString() to texto(R.string.profile_stat_sold),
            cars.count { it.status == "Activo" }.toString() to texto(R.string.profile_stat_active),
        )

    fun load() {
        val uid = SessionManager.userId ?: run { loading = false; return }
        viewModelScope.launch {
            safeCall { UserRepository.currentUser() }
                .onSuccess { user = it }
                .onFailure { message = it.mensajeUsuario() }
            cars = safeCall { CarRepository.ownerCars(uid) }.getOrDefault(cars)
            loading = false
        }
    }

    /**
     * Guarda los datos del perfil y, si se eligieron en el diálogo, sube la foto de perfil
     * y la portada (o quita la portada). Las fotos se suben hasta aquí para que "Descartar"
     * no deje nada guardado. La portada se comprime (lado mayor de 1600 px, JPEG).
     */
    fun save(
        context: Context,
        updated: User,
        newAvatar: Uri?,
        newCover: Uri?,
        removeCover: Boolean,
        onDone: () -> Unit,
    ) {
        val original = user ?: return
        if (saving) return
        val appContext = context.applicationContext
        saving = true
        editError = null
        viewModelScope.launch {
            safeCall {
                UserRepository.updateProfile(original, updated)
                var guardado = updated
                if (newAvatar != null) {
                    val url = UserRepository.uploadAvatar(Imagenes.jpegBytes(appContext, newAvatar, maxLado = 800))
                    guardado = guardado.copy(avatarUrl = url)
                }
                when {
                    newCover != null -> {
                        val url = UserRepository.uploadCover(Imagenes.jpegBytes(appContext, newCover, maxLado = 1600, calidad = 78))
                        guardado = guardado.copy(coverUrl = url)
                    }
                    removeCover -> {
                        UserRepository.removeCover()
                        guardado = guardado.copy(coverUrl = null)
                    }
                }
                guardado
            }
                .onSuccess {
                    user = it
                    message = texto(R.string.profile_saved)
                    onDone()
                }
                .onFailure { editError = it.mensajeUsuario() }
            saving = false
        }
    }

    /** Cambia la contraseña solo si [current] es la contraseña actual. */
    fun changePassword(current: String, new: String, confirm: String, onDone: () -> Unit) {
        if (saving) return
        passwordError = when {
            current.isEmpty() -> texto(R.string.profile_password_enter_current)
            !PasswordRules.esValida(new, SessionManager.account?.correo) -> PasswordRules.error(new, SessionManager.account?.correo)
            new != confirm -> texto(R.string.auth_passwords_mismatch)
            new == current -> texto(R.string.core_error_same_password)
            else -> null
        }
        if (passwordError != null) return
        saving = true
        viewModelScope.launch {
            safeCall { AuthRepository.changePassword(current, new) }
                .onSuccess {
                    message = texto(R.string.profile_password_changed)
                    onDone()
                }
                .onFailure { passwordError = it.mensajeUsuario() }
            saving = false
        }
    }

    fun clearDialogErrors() {
        editError = null
        passwordError = null
    }

}
