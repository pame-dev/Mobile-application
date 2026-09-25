package com.pame.karsy.feature.profile

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.Imagenes
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.model.User
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

    val stats: List<Pair<String, String>>
        get() = listOf(
            cars.size.toString() to "Publicaciones",
            cars.count { it.status == "Vendido" }.toString() to "Vendidos",
            cars.count { it.status == "Activo" }.toString() to "Activas",
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
        saving = true
        viewModelScope.launch {
            safeCall { UserRepository.updateProfile(original, updated) }
                .onSuccess {
                    user = updated
                    message = if (updated.email.trim() != original.email.trim())
                        "Perfil guardado. Revisa tu correo nuevo para confirmar el cambio."
                    else "Perfil guardado."
                    onDone()
                }
                .onFailure { message = it.mensajeUsuario() }
            saving = false
        }
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        saving = true
        viewModelScope.launch {
            safeCall { UserRepository.uploadAvatar(Imagenes.jpegBytes(appContext, uri, maxLado = 800)) }
                .onSuccess { url -> user = user?.copy(avatarUrl = url); message = "Foto actualizada." }
                .onFailure { message = it.mensajeUsuario() }
            saving = false
        }
    }
}
