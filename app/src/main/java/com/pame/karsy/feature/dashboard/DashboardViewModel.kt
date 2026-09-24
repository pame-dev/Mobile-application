package com.pame.karsy.feature.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.repository.CarRepository
import com.pame.karsy.data.repository.SellerStats
import com.pame.karsy.data.repository.StatsRepository
import com.pame.karsy.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Panel del vendedor en sesión: métricas (vendedor_estadisticas), sus publicaciones
 * y solicitudes para destacar (solicitudes_destacado).
 */
class DashboardViewModel : ViewModel() {

    var stats by mutableStateOf<SellerStats?>(null)
        private set
    var myListings by mutableStateOf<List<Car>>(emptyList())
        private set
    var avatarUrl by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    val displayName: String get() = SessionManager.account?.nombre.orEmpty()

    /** "▲ +20% este mes" comparando las vistas del mes contra el anterior. */
    val viewsTrend: String
        get() {
            val s = stats ?: return ""
            if (s.vistasMesAnterior == 0) return "${s.vistasMes} este mes"
            val pct = ((s.vistasMes - s.vistasMesAnterior) * 100f / s.vistasMesAnterior).roundToInt()
            return if (pct >= 0) "▲ +$pct% este mes" else "▼ $pct% este mes"
        }

    /** Auto para el que se muestra el diálogo "¿Quieres destacar tu vehículo?". */
    var destacarCar by mutableStateOf<Car?>(null)
        private set

    /** Auto para el que se muestra "Solicitud enviada". */
    var solicitudCar by mutableStateOf<Car?>(null)
        private set

    fun load() {
        val uid = SessionManager.userId ?: run { loading = false; return }
        viewModelScope.launch {
            safeCall { StatsRepository.seller() }
                .onSuccess { stats = it; error = null }
                .onFailure { error = it.mensajeUsuario() }
            myListings = safeCall { CarRepository.ownerCars(uid) }.getOrDefault(myListings)
            if (avatarUrl == null) avatarUrl = safeCall { UserRepository.currentUser()?.avatarUrl }.getOrNull()
            loading = false
        }
    }

    fun askDestacar(car: Car) {
        destacarCar = car
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
                    val msg = it.mensajeUsuario()
                    error = if (msg.contains("uq_solicitudes_pendiente")) "Ya hay una solicitud pendiente para este vehículo." else msg
                }
        }
    }

    fun closeSolicitud() {
        solicitudCar = null
    }
}
