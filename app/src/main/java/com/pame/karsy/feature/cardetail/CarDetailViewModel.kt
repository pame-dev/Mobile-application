package com.pame.karsy.feature.cardetail

import android.content.Context
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
import com.pame.karsy.core.util.PagoDestacado
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.model.CarDetail
import com.pame.karsy.data.model.SellerContact
import com.pame.karsy.data.repository.CarRepository
import com.pame.karsy.feature.dashboard.DestacarPago
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
    /** No se pudo leer el contacto (red): la hoja muestra el error con "Reintentar". */
    var contactError by mutableStateOf(false)
        private set
    /** El contacto ya se contó en esta visita (las estadísticas no se inflan al refrescar o reabrir). */
    private var contactoRegistrado = false
    var sellerCars by mutableStateOf<List<Car>>(emptyList())
        private set
    /** Hasta 4 autos visibles del mismo tipo de carrocería (como "Vehículos similares" de la web). */
    var similares by mutableStateOf<List<Car>>(emptyList())
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
            // Similares aparte: no retrasa el contador de vistas ni el favorito. Sin tipo de carrocería, no hay.
            detail?.car?.takeIf { it.bodyType.isNotBlank() }?.let { car ->
                launch {
                    similares = safeCall { CarRepository.visibleCars() }.getOrDefault(emptyList())
                        .filter { it.bodyType == car.bodyType && it.id != car.id }
                        .take(4)
                }
            }
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

    /** Solo los lotes pueden destacar o renovar (cuentas.tipo_cuenta). */
    val puedeDestacar: Boolean get() = PagoDestacado.puedeDestacar(SessionManager.account?.tipoCuenta)

    /** Datos de pago, comprobante y casilla de la ventana "Destacar tu auto". */
    val pago = DestacarPago()

    /** Al abrir la ventana de destacar (o renovar): vuelve a leer precio, días y cuenta. */
    fun openFeature() = pago.abrir(this)

    fun retryDatosPago() = pago.cargar(this)

    /** El dueño (lote) sube su comprobante y pide destacar su publicación (lo aprueba un admin). */
    fun requestFeatured(context: Context, onSent: () -> Unit) {
        val current = detail ?: return
        pago.enviar(this, context, current.car.id) {
            detail = current.copy(car = current.car.copy(featuredPending = true))
            onSent()
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

    /** Pide teléfono y correo del vendedor; con [registrar] (al abrir la hoja) cuenta el contacto una vez. */
    fun loadContact(registrar: Boolean = true) {
        val id = detail?.car?.id ?: return
        if (registrar && !contactoRegistrado) {
            contactoRegistrado = true
            viewModelScope.launch { CarRepository.registrarContacto(id) }
        }
        if (contact != null || contactLoading) return
        contactLoading = true
        contactError = false
        viewModelScope.launch {
            safeCall { CarRepository.contact(id) }
                .onSuccess { contact = it }
                .onFailure { contactError = true }
            contactLoading = false
        }
    }

    /** Publicaciones visibles del vendedor para su perfil. */
    fun loadSellerCars() {
        val sellerId = detail?.sellerId ?: return
        viewModelScope.launch {
            sellerCars = safeCall { CarRepository.ownerCars(sellerId, onlyVisible = true) }.getOrDefault(emptyList())
        }
        // Ver el perfil no es contactar: no se cuenta.
        loadContact(registrar = false)
    }

    fun report(motivo: String, onDone: (String?) -> Unit) {
        // Siempre se avisa a [onDone]: la hoja de reporte muestra el bloqueo de carga hasta entonces.
        val id = detail?.car?.id ?: return onDone(texto(R.string.core_error_generic))
        viewModelScope.launch {
            safeCall { CarRepository.report(id, motivo) }
                .onSuccess { onDone(null) }
                .onFailure {
                    onDone(if (it.esDuplicado()) texto(R.string.detail_already_reported) else it.mensajeUsuario())
                }
        }
    }
}
