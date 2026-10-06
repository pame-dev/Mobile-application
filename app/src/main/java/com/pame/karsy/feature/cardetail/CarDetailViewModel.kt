package com.pame.karsy.feature.cardetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.model.CarDetail
import com.pame.karsy.data.model.SellerContact
import com.pame.karsy.data.repository.CarRepository
import kotlinx.coroutines.launch

/** Detalle de un anuncio: ficha, favorito, contacto, perfil del vendedor y reportes. */
class CarDetailViewModel : ViewModel() {
    var detail by mutableStateOf<CarDetail?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var favorite by mutableStateOf(false)
        private set

    var contact by mutableStateOf<SellerContact?>(null)
        private set
    var contactLoading by mutableStateOf(false)
        private set
    var sellerCars by mutableStateOf<List<Car>>(emptyList())
        private set

    private var loadedId: Long? = null

    /** Mientras se pausa o reactiva una publicación propia. */
    var updatingStatus by mutableStateOf(false)
        private set

    fun load(id: Long) {
        if (loadedId == id) return
        loadedId = id
        viewModelScope.launch {
            loading = true
            safeCall { CarRepository.detail(id) }
                .onSuccess { detail = it; error = null }
                .onFailure { error = it.mensajeUsuario() }
            loading = false
            if (detail != null) {
                CarRepository.registerView(id)
                if (SessionManager.userId != null) {
                    favorite = id in safeCall { CarRepository.favoriteIds() }.getOrDefault(emptySet())
                }
            }
        }
    }

    /** El dueño deshabilita (pausa) o vuelve a habilitar su publicación. */
    fun setPaused(paused: Boolean, onDone: (error: String?) -> Unit) {
        val id = detail?.car?.id ?: return
        if (updatingStatus) return
        updatingStatus = true
        viewModelScope.launch {
            safeCall {
                CarRepository.setPaused(id, paused)
                CarRepository.detail(id)
            }
                .onSuccess { detail = it ?: detail; onDone(null) }
                .onFailure { onDone(it.mensajeUsuario()) }
            updatingStatus = false
        }
    }

    /**
     * El dueño marca su publicación como vendida ([via]: "dentro_app" o "fuera_app")
     * o le quita la marca para que vuelva a estar disponible.
     */
    fun setSold(sold: Boolean, via: String? = null, onDone: (error: String?) -> Unit) {
        val id = detail?.car?.id ?: return
        if (updatingStatus) return
        updatingStatus = true
        viewModelScope.launch {
            safeCall {
                CarRepository.setSold(id, sold, via)
                CarRepository.detail(id)
            }
                .onSuccess { detail = it ?: detail; onDone(null) }
                .onFailure { onDone(it.mensajeUsuario()) }
            updatingStatus = false
        }
    }

    /** El dueño pide destacar su publicación (lo aprueba un admin). */
    fun requestFeatured(onDone: (error: String?) -> Unit) {
        val current = detail ?: return
        viewModelScope.launch {
            safeCall { CarRepository.requestFeatured(current.car.id) }
                .onSuccess {
                    detail = current.copy(car = current.car.copy(featuredPending = true))
                    onDone(null)
                }
                .onFailure {
                    val msg = it.mensajeUsuario()
                    onDone(if (msg.contains("uq_solicitudes_pendiente")) texto(R.string.profile_dashboard_request_already_pending) else msg)
                }
        }
    }

    fun retry() {
        loadedId?.let { loadedId = null; load(it) }
    }

    fun toggleFavorite(onError: (String) -> Unit) {
        val id = detail?.car?.id ?: return
        val nuevo = !favorite
        favorite = nuevo
        viewModelScope.launch {
            safeCall { CarRepository.setFavorite(id, nuevo) }.onFailure {
                favorite = !nuevo
                onError(it.mensajeUsuario())
            }
        }
    }

    /** Pide teléfono y correo del vendedor (y registra el contacto). */
    fun loadContact() {
        val id = detail?.car?.id ?: return
        if (contact != null || contactLoading) return
        contactLoading = true
        viewModelScope.launch {
            contact = safeCall { CarRepository.contact(id) }.getOrNull()
            contactLoading = false
        }
    }

    /** Publicaciones visibles del vendedor para su perfil. */
    fun loadSellerCars() {
        val sellerId = detail?.sellerId ?: return
        viewModelScope.launch {
            sellerCars = safeCall { CarRepository.ownerCars(sellerId, onlyVisible = true) }.getOrDefault(emptyList())
        }
        loadContact()
    }

    fun report(motivo: String, onDone: (String?) -> Unit) {
        val id = detail?.car?.id ?: return
        viewModelScope.launch {
            safeCall { CarRepository.report(id, motivo) }
                .onSuccess { onDone(null) }
                .onFailure {
                    val msg = it.mensajeUsuario()
                    onDone(if (msg.contains("uq_reportes")) texto(R.string.detail_already_reported) else msg)
                }
        }
    }
}
