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
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.Imagenes
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

    val stats: List<Pair<String, String>>
        get() = listOf(
            cars.size.toString() to texto(R.string.profile_stat_posts),
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

    fun save(updated: User, onDone: () -> Unit) {
        val original = user ?: return
        if (saving) return
        saving = true
        editError = null
        viewModelScope.launch {
            safeCall { UserRepository.updateProfile(original, updated) }
                .onSuccess {
                    user = updated
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
            new.length < 6 -> texto(R.string.core_error_weak_password)
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

    fun uploadAvatar(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        saving = true
        viewModelScope.launch {
            safeCall { UserRepository.uploadAvatar(Imagenes.jpegBytes(appContext, uri, maxLado = 800)) }
                .onSuccess { url -> user = user?.copy(avatarUrl = url); message = texto(R.string.profile_photo_updated) }
                .onFailure { message = it.mensajeUsuario() }
            saving = false
        }
    }
}
