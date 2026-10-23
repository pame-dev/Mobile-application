package com.pame.karsy.feature.cardetail

import androidx.compose.runtime.getValue
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
    /** El admin revisa la publicación: se muestran los cambios en revisión (CarRepository.detail). */
    private var cambiosPendientes = false

    /** Mientras se pausa o reactiva una publicación propia. */
    var updatingStatus by mutableStateOf(false)
        private set

    fun load(id: Long, cambiosPendientes: Boolean = false) {
        if (loadedId == id) return
        loadedId = id
        this.cambiosPendientes = cambiosPendientes
        viewModelScope.launch {
            loading = true
            safeCall { CarRepository.detail(id, cambiosPendientes) }
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
                CarRepository.detail(id, cambiosPendientes)
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
                CarRepository.detail(id, cambiosPendientes)
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
                    onDone(if (it.esDuplicado()) texto(R.string.profile_dashboard_request_already_pending) else it.mensajeUsuario())
                }
        }
    }

    /**
     * Jalar para recargar: vuelve a leer el auto (precio, estado, fotos), el favorito y,
     * si ya se habían cargado, el contacto y los autos del vendedor. Sin esqueleto y sin
     * contar otra vista.
     */
    fun refresh(onDone: () -> Unit) {
        val id = loadedId ?: return onDone()
        viewModelScope.launch {
            safeCall { CarRepository.detail(id, cambiosPendientes) }
                .onSuccess { if (it != null) detail = it; error = null }
            if (SessionManager.userId != null) {
                favorite = id in safeCall { CarRepository.favoriteIds() }.getOrDefault(emptySet())
            }
            if (contact != null) contact = safeCall { CarRepository.contact(id) }.getOrDefault(contact)
            val sellerId = detail?.sellerId
            if (sellerId != null && sellerCars.isNotEmpty()) {
                sellerCars = safeCall { CarRepository.ownerCars(sellerId, onlyVisible = true) }.getOrDefault(sellerCars)
            }
            onDone()
        }
    }

    fun retry() {
        loadedId?.let { loadedId = null; load(it, cambiosPendientes) }
    }

    /** "Deshacer" de quitar: lo vuelve a guardar (aunque mientras tanto se haya tocado el corazón). */
    fun restoreFavorite(onError: (String) -> Unit) {
        if (!favorite) toggleFavorite(onError)
    }

    /** [onRemoved]: se quitó de favoritos (para ofrecer "Deshacer"). */
    fun toggleFavorite(onError: (String) -> Unit, onRemoved: () -> Unit = {}) {
        val id = detail?.car?.id ?: return
        val nuevo = !favorite
        favorite = nuevo
        viewModelScope.launch {
            safeCall { CarRepository.setFavorite(id, nuevo) }
                .onSuccess { if (!nuevo) onRemoved() }
                .onFailure {
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
                    onDone(if (it.esDuplicado()) texto(R.string.detail_already_reported) else it.mensajeUsuario())
                }
        }
    }
}
