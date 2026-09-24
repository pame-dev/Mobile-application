package com.pame.karsy.feature.cardetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                    onDone(if (msg.contains("uq_reportes")) "Ya habías reportado esta publicación." else msg)
                }
        }
    }
}
