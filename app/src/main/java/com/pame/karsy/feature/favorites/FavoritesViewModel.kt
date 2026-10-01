package com.pame.karsy.feature.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.repository.CarRepository
import com.pame.karsy.data.repository.CatalogRepository
import com.pame.karsy.data.repository.Catalogs
import com.pame.karsy.feature.home.FilterOptions
import com.pame.karsy.feature.home.HomeFilters
import com.pame.karsy.feature.home.ORDEN_OPCIONES
import com.pame.karsy.feature.home.filterOptionsOf
import com.pame.karsy.feature.home.filtrar
import kotlinx.coroutines.launch

/** Favoritos de la cuenta en sesión (public.favoritos). */
class FavoritesViewModel : ViewModel() {
    /** Guardados, del más reciente al más antiguo. Al quitar uno sale de aquí al instante. */
    var cars by mutableStateOf<List<Car>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var catalogs by mutableStateOf<Catalogs?>(null)

    /** Mismo panel de filtros que el catálogo, pero aplicado solo a los favoritos. */
    var filters by mutableStateOf(HomeFilters())
    var query by mutableStateOf("")

    val filterOptions: FilterOptions get() = filterOptionsOf(catalogs, cars)

    val visibleCars: List<Car>
        get() {
            val lista = cars.filtrar(filters, query)
            return when (filters.orden) {
                ORDEN_OPCIONES[1] -> lista.sortedBy { it.priceValue }
                ORDEN_OPCIONES[2] -> lista.sortedByDescending { it.priceValue }
                else -> lista // "Más recientes" = orden en que se guardaron
            }
        }

    val sortLabel: String
        get() = texto(
            listOf(R.string.home_fav_sort_recent, R.string.home_fav_sort_price_low, R.string.home_fav_sort_price_high)[
                ORDEN_OPCIONES.indexOf(filters.orden).coerceAtLeast(0)
            ]
        )

    fun nextSort() {
        val i = ORDEN_OPCIONES.indexOf(filters.orden).coerceAtLeast(0)
        filters = filters.copy(orden = ORDEN_OPCIONES[(i + 1) % ORDEN_OPCIONES.size])
    }

    fun load() {
        viewModelScope.launch {
            loading = cars.isEmpty()
            safeCall { CarRepository.favoriteCars() }
                .onSuccess { cars = it; error = null }
                .onFailure { error = it.mensajeUsuario() }
            loading = false

            if (catalogs == null) catalogs = safeCall { CatalogRepository.get() }.getOrNull()
        }
    }

    /** Quita el favorito de la lista al instante; si la BD falla, lo regresa a su lugar. */
    fun remove(id: Long, onError: (String) -> Unit) {
        val index = cars.indexOfFirst { it.id == id }
        if (index < 0) return
        val car = cars[index]
        cars = cars - car
        viewModelScope.launch {
            safeCall { CarRepository.setFavorite(id, false) }.onFailure {
                cars = cars.toMutableList().apply { add(index.coerceAtMost(size), car) }
                onError(it.mensajeUsuario())
            }
        }
    }
}
