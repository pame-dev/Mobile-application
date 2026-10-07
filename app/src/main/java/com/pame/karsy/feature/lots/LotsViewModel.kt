package com.pame.karsy.feature.lots

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.model.Lot
import com.pame.karsy.data.model.LotContact
import com.pame.karsy.data.repository.CarRepository
import com.pame.karsy.data.repository.CatalogRepository
import com.pame.karsy.data.repository.Catalogs
import com.pame.karsy.feature.home.FilterOptions
import com.pame.karsy.feature.home.HomeFilters
import com.pame.karsy.feature.home.ORDEN_OPCIONES
import com.pame.karsy.feature.home.filterOptionsOf
import com.pame.karsy.feature.home.filtrar
import com.pame.karsy.data.repository.LotRepository
import kotlinx.coroutines.launch

/** Directorio de lotes (pestaña "Lotes"). */
class LotsViewModel : ViewModel() {
    var lots by mutableStateOf<List<Lot>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var query by mutableStateOf("")

    /** Lotes que coinciden con la búsqueda por nombre o ciudad. */
    val visibleLots: List<Lot>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return lots
            return lots.filter { it.name.contains(q, ignoreCase = true) || it.city.contains(q, ignoreCase = true) }
        }

    /** [onDone] se llama al terminar (para quitar el pull-to-refresh). */
    fun load(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            loading = lots.isEmpty()
            safeCall { LotRepository.lots() }
                .onSuccess { lots = it; error = null }
                .onFailure { error = it.mensajeUsuario() }
            loading = false
            onDone()
        }
    }
}

/** Perfil de un lote: datos, contacto e inventario visible. */
class LotProfileViewModel : ViewModel() {
    var lot by mutableStateOf<Lot?>(null)
        private set
    var cars by mutableStateOf<List<Car>>(emptyList())
        private set
    var contact by mutableStateOf<LotContact?>(null)
        private set
    var favoriteIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var query by mutableStateOf("")
    /** Mismo panel de filtros que Inicio, aplicado solo al inventario de este lote. */
    var filters by mutableStateOf(HomeFilters())
    private var catalogs by mutableStateOf<Catalogs?>(null)

    /** Hay algún filtro distinto al de por defecto (para resaltar el botón). */
    val filtersActive: Boolean get() = filters != HomeFilters()

    /** Opciones del panel; los años salen del inventario del lote. */
    val filterOptions: FilterOptions get() = filterOptionsOf(catalogs, cars)

    /** Inventario con búsqueda, filtros y orden aplicados. */
    val visibleCars: List<Car>
        get() {
            val lista = cars.filtrar(filters, query)
            return when (filters.orden) {
                ORDEN_OPCIONES[1] -> lista.sortedBy { it.priceValue }
                ORDEN_OPCIONES[2] -> lista.sortedByDescending { it.priceValue }
                else -> lista.sortedByDescending { it.publishedAt }
            }
        }

    fun load(lotId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            loading = lot == null
            safeCall {
                val datos = LotRepository.lot(lotId)
                val inventario = CarRepository.ownerCars(lotId, onlyVisible = true)
                datos?.copy(vehicleCount = inventario.size) to inventario
            }
                .onSuccess { (datos, inventario) ->
                    lot = datos
                    cars = inventario
                    error = null
                }
                .onFailure { error = it.mensajeUsuario() }
            loading = false
            onDone()

            // Contacto, favoritos y catálogos no bloquean la pantalla; si fallan, solo no se muestran.
            if (catalogs == null) catalogs = safeCall { CatalogRepository.get() }.getOrNull()
            contact = safeCall { LotRepository.contact(lotId) }.getOrNull()
            favoriteIds = if (SessionManager.userId != null) {
                safeCall { CarRepository.favoriteIds() }.getOrDefault(favoriteIds)
            } else emptySet()
        }
    }

    /** Guarda o quita el favorito en la BD; si falla se revierte. */
    fun toggleFavorite(id: Long, onError: (String) -> Unit) {
        val agregar = id !in favoriteIds
        favoriteIds = if (agregar) favoriteIds + id else favoriteIds - id
        viewModelScope.launch {
            safeCall { CarRepository.setFavorite(id, agregar) }.onFailure {
                favoriteIds = if (agregar) favoriteIds - id else favoriteIds + id
                onError(it.mensajeUsuario())
            }
        }
    }
}
