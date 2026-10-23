package com.pame.karsy.feature.dashboard

import androidx.compose.runtime.getValue
import com.pame.karsy.core.session.PublicacionesOcultas
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.esDuplicado
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.repository.CarRepository
import com.pame.karsy.data.repository.SellerStats
import com.pame.karsy.data.repository.StatsRepository
import com.pame.karsy.data.repository.UserRepository
import kotlinx.coroutines.launch

/**
 * Panel del vendedor en sesión: métricas (vendedor_estadisticas), sus publicaciones
 * y solicitudes para destacar (solicitudes_destacado).
 */
class DashboardViewModel : ViewModel() {

    var stats by mutableStateOf<SellerStats?>(null)
        private set
    var myListings by mutableStateOf<List<Car>>(emptyList())
        private set
    /** Cuántos usuarios guardaron en favoritos cada publicación (por id). */
    var favoriteCounts by mutableStateOf<Map<Long, Int>>(emptyMap())
        private set
    var avatarUrl by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val displayName: String get() = SessionManager.account?.nombre.orEmpty()

    /** Auto para el que se muestra el diálogo "¿Quieres destacar tu vehículo?". */
    var destacarCar by mutableStateOf<Car?>(null)
        private set

    /** Auto para el que se muestra "Solicitud enviada". */
    var solicitudCar by mutableStateOf<Car?>(null)
        private set

    /** [onDone] se llama cuando todo se actualizó (para quitar el pull-to-refresh). */
    fun load(onDone: () -> Unit = {}) {
        val uid = SessionManager.userId ?: run { loading = false; onDone(); return }
        viewModelScope.launch {
            safeCall { StatsRepository.seller() }
                .onSuccess { stats = it; error = null }
                .onFailure { error = it.mensajeUsuario() }
            myListings = safeCall { CarRepository.ownerCars(uid) }.getOrDefault(myListings)
            // Las ocultas se comparten con la web: se vuelven a leer junto con el panel.
            safeCall { PublicacionesOcultas.sincronizar() }
            favoriteCounts = safeCall { CarRepository.favoriteCounts() }.getOrDefault(favoriteCounts)
            if (avatarUrl == null) avatarUrl = safeCall { UserRepository.currentUser()?.avatarUrl }.getOrNull()
            loading = false
            onDone()
        }
    }

    /** Destacar pide confirmación; renovar un destacado que está por terminar va directo (un toque). */
    fun askDestacar(car: Car) {
        destacarCar = car
        if (car.featured && car.featuredEndsSoon) confirmDestacar()
    }

    fun cancelDestacar() {
        destacarCar = null
    }

    fun confirmDestacar() {
        val car = destacarCar ?: return
        destacarCar = null
        viewModelScope.launch {
            safeCall { CarRepository.requestFeatured(car.id) }
                .onSuccess {
                    solicitudCar = car
                    myListings = myListings.map { if (it.id == car.id) it.copy(featuredPending = true) else it }
                }
                .onFailure {
                    error = if (it.esDuplicado()) texto(R.string.profile_dashboard_request_already_pending) else it.mensajeUsuario()
                }
        }
    }

    fun closeSolicitud() {
        solicitudCar = null
    }
}
