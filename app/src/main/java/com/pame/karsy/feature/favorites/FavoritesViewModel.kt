package com.pame.karsy.feature.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.repository.CarRepository
import kotlinx.coroutines.launch

/** Favoritos de la cuenta en sesión (public.favoritos). */
class FavoritesViewModel : ViewModel() {
    var cars by mutableStateOf<List<Car>>(emptyList())
        private set
    /** Ids marcados; al quitar uno la tarjeta se queda en la lista hasta salir de la pantalla. */
    var favoriteIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** 0 = guardados recientemente, 1 = menor precio, 2 = mayor precio. */
    var sortMode by mutableStateOf(0)
        private set
    val sortLabel: String get() = listOf("Recientes ↕", "Menor precio ↕", "Mayor precio ↕")[sortMode]

    fun sorted(list: List<Car>): List<Car> = when (sortMode) {
        1 -> list.sortedBy { it.priceValue }
        2 -> list.sortedByDescending { it.priceValue }
        else -> list
    }

    fun nextSort() {
        sortMode = (sortMode + 1) % 3
    }

    fun load() {
        viewModelScope.launch {
            loading = cars.isEmpty()
            safeCall { CarRepository.favoriteCars() }
                .onSuccess {
                    cars = it
                    favoriteIds = it.map { c -> c.id }.toSet()
                    error = null
                }
                .onFailure { error = it.mensajeUsuario() }
            loading = false
        }
    }

    fun toggle(id: Long, onError: (String) -> Unit) {
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
