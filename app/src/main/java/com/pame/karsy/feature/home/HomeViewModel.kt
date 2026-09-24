package com.pame.karsy.feature.home

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
import com.pame.karsy.data.repository.CatalogRepository
import com.pame.karsy.data.repository.Catalogs
import kotlinx.coroutines.launch

internal const val TODAS = "Todas"
internal const val TODOS = "Todos"
internal const val CUALQUIERA = "Cualquiera"

/** Filtros del panel lateral + orden. Se aplican sobre los anuncios ya cargados. */
data class HomeFilters(
    val marca: String = TODAS,
    val modelo: String = TODOS,
    val anio: String = CUALQUIERA,
    val tipo: String = TODOS,
    val precioMin: String = "",
    val precioMax: String = "",
    val orden: String = ORDEN_OPCIONES.first(),
)

/** Opciones de los selectores del panel de filtros (salen de la BD). */
data class FilterOptions(
    val marcas: List<String> = listOf(TODAS),
    val modelosPorMarca: Map<String, List<String>> = emptyMap(),
    val anios: List<String> = listOf(CUALQUIERA),
    val tipos: List<String> = listOf(TODOS),
) {
    fun modelos(marca: String): List<String> = listOf(TODOS) + modelosPorMarca[marca].orEmpty()
}

class HomeViewModel : ViewModel() {
    var cars by mutableStateOf<List<Car>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var favoriteIds by mutableStateOf<Set<Long>>(emptySet())
        private set
    var catalogs by mutableStateOf<Catalogs?>(null)
        private set

    var filters by mutableStateOf(HomeFilters())
    var query by mutableStateOf("")

    val featured: List<Car> get() = cars.filter { it.featured }

    /** Anuncios con búsqueda, filtros y orden aplicados. */
    val visibleCars: List<Car>
        get() {
            val f = filters
            val texto = query.trim().lowercase()
            val min = f.precioMin.filter(Char::isDigit).toDoubleOrNull()
            val max = f.precioMax.filter(Char::isDigit).toDoubleOrNull()
            val lista = cars.filter { car ->
                (texto.isEmpty() || "${car.brand} ${car.model} ${car.year}".lowercase().contains(texto)) &&
                    (f.marca == TODAS || car.brand == f.marca) &&
                    (f.modelo == TODOS || car.model == f.modelo) &&
                    (f.anio == CUALQUIERA || car.year.toString() == f.anio) &&
                    (f.tipo == TODOS || car.bodyType == f.tipo) &&
                    (min == null || car.priceValue >= min) &&
                    (max == null || car.priceValue <= max)
            }
            return when (f.orden) {
                "Menor precio" -> lista.sortedBy { it.priceValue }
                "Mayor precio" -> lista.sortedByDescending { it.priceValue }
                else -> lista.sortedByDescending { it.publishedAt }
            }
        }

    val filterOptions: FilterOptions
        get() {
            val c = catalogs ?: return FilterOptions()
            val marcas = c.marcas.filterNot { it.esOtro }
            return FilterOptions(
                marcas = listOf(TODAS) + marcas.map { it.nombre },
                modelosPorMarca = marcas.associate { m ->
                    m.nombre to c.modelosDe(m.id).filterNot { it.esOtro }.map { it.nombre }
                },
                anios = listOf(CUALQUIERA) + cars.map { it.year }.filter { it > 0 }
                    .distinct().sortedDescending().map { it.toString() },
                tipos = listOf(TODOS) + c.carrocerias.filterNot { it.esOtro }.map { it.nombre },
            )
        }

    /** Cantidades reales para el encabezado (vehículos, marcas y modelos). */
    val heroStats: List<Pair<String, String>>
        get() = listOf(
            cars.size.toString() to "Vehículos",
            (catalogs?.marcas?.count { !it.esOtro } ?: 0).toString() to "Marcas",
            (catalogs?.modelos?.count { !it.esOtro } ?: 0).toString() to "Modelos",
        )

    /** Se llama cada vez que la pantalla aparece (p. ej. al volver del detalle). */
    fun refresh() {
        viewModelScope.launch {
            if (cars.isEmpty()) loading = true
            safeCall { CarRepository.visibleCars() }
                .onSuccess { cars = it; error = null }
                .onFailure { error = it.mensajeUsuario() }
            loading = false

            if (catalogs == null) catalogs = safeCall { CatalogRepository.get() }.getOrNull()
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
