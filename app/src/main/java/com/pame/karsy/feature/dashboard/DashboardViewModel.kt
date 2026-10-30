package com.pame.karsy.feature.dashboard

import android.content.Context
import androidx.compose.runtime.getValue
import com.pame.karsy.core.session.PublicacionesOcultas
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.PagoDestacado
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

    /** Auto para el que se muestra la ventana "Destacar tu auto". */
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

    /** Solo los lotes pueden destacar o renovar (cuentas.tipo_cuenta). */
    val puedeDestacar: Boolean get() = PagoDestacado.puedeDestacar(SessionManager.account?.tipoCuenta)

    /** Datos de pago, comprobante y casilla de la ventana "Destacar tu auto". */
    val pago = DestacarPago()

    /** Destacar y renovar abren la misma ventana: los dos se pagan con transferencia y comprobante. */
    fun askDestacar(car: Car) {
        if (!puedeDestacar) return
        destacarCar = car
        pago.abrir(this)
    }

    fun cancelDestacar() {
        if (pago.enviando) return
        destacarCar = null
    }

    fun retryDatosPago() = pago.cargar(this)

    /** Sube el comprobante y pide el destacado; al aceptarse se ve "Solicitud enviada". */
    fun confirmDestacar(context: Context) {
        val car = destacarCar ?: return
        pago.enviar(this, context, car.id) {
            destacarCar = null
            solicitudCar = car
            myListings = myListings.map { if (it.id == car.id) it.copy(featuredPending = true) else it }
        }
    }

    fun closeSolicitud() {
        solicitudCar = null
    }
}
