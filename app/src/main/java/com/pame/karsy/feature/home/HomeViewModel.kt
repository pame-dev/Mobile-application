package com.pame.karsy.feature.home

import androidx.compose.runtime.getValue
import com.pame.karsy.core.location.UbicacionActual
import com.pame.karsy.core.location.Lugar
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
import com.pame.karsy.data.repository.CarRepository
import com.pame.karsy.data.repository.CatalogRepository
import com.pame.karsy.data.repository.Catalogs
import kotlinx.coroutines.launch
import java.text.Normalizer

// Valores internos de "sin filtro" (se comparan en visibleCars); su etiqueta visible está en rememberFilterLabel().
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

/** Minúsculas y sin acentos, para que "cafe" encuentre "Café". */
private fun String.normalizado(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(ACENTOS, "")

private val ACENTOS = Regex("\\p{Mn}+")

/**
 * Cada palabra de la búsqueda debe aparecer en marca, modelo, año, tipo, vendedor o
 * descripción, en cualquier orden: "versa 2020 nissan" encuentra "Nissan Versa 2020".
 */
private fun Car.coincide(palabras: List<String>): Boolean {
    if (palabras.isEmpty()) return true
    val texto = "$brand $model $year $bodyType $ownerName $description".normalizado()
    return palabras.all { it in texto }
}

/** Búsqueda y filtros del panel (sin ordenar). Lo usan el catálogo y Favoritos. */
internal fun List<Car>.filtrar(f: HomeFilters, query: String): List<Car> {
    val palabras = query.normalizado().split(' ').filter { it.isNotBlank() }
    val min = f.precioMin.filter(Char::isDigit).toDoubleOrNull()
    val max = f.precioMax.filter(Char::isDigit).toDoubleOrNull()
    return filter { car ->
        car.coincide(palabras) &&
            (f.marca == TODAS || car.brand == f.marca) &&
            (f.modelo == TODOS || car.model == f.modelo) &&
            (f.anio == CUALQUIERA || car.year.toString() == f.anio) &&
            (f.tipo == TODOS || car.bodyType == f.tipo) &&
            (min == null || car.priceValue >= min) &&
            (max == null || car.priceValue <= max)
    }
}

/** Opciones del panel: marcas, modelos y tipos del catálogo; años de los autos dados. */
internal fun filterOptionsOf(catalogs: Catalogs?, cars: List<Car>): FilterOptions {
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

    /** "Cerca de mí": lugar del teléfono; null si el filtro está apagado. */
    var cercaDe by mutableStateOf<Lugar?>(null)

    /** Anuncios con búsqueda, filtros y orden aplicados. */
    val visibleCars: List<Car>
        get() {
            val lugar = cercaDe
            // Cerca de mí: solo los del mismo estado.
            val lista = cars.filtrar(filters, query)
                .let { l -> if (lugar == null) l else l.filter { UbicacionActual.mismoLugar(it.estado, lugar.estado) } }
            val ordenada = when (filters.orden) {
                "Menor precio" -> lista.sortedBy { it.priceValue }
                "Mayor precio" -> lista.sortedByDescending { it.priceValue }
                else -> lista.sortedByDescending { it.publishedAt }
            }
            // ...y primero los del mismo municipio (sortedBy es estable: respeta el orden elegido).
            return if (lugar == null) ordenada
            else ordenada.sortedBy { if (UbicacionActual.mismoLugar(it.municipio, lugar.municipio)) 0 else 1 }
        }

    val filterOptions: FilterOptions get() = filterOptionsOf(catalogs, cars)

    /** Cantidades reales para el encabezado (vehículos, marcas y modelos). */
    val heroStats: List<Pair<String, String>>
        get() = listOf(
            cars.size.toString() to texto(R.string.home_stat_vehicles),
            (catalogs?.marcas?.count { !it.esOtro } ?: 0).toString() to texto(R.string.home_stat_brands),
            (catalogs?.modelos?.count { !it.esOtro } ?: 0).toString() to texto(R.string.home_stat_models),
        )

    /** Se llama cada vez que la pantalla aparece (p. ej. al volver del detalle). */
    /** [onDone] se llama cuando la lista ya se actualizó (para quitar el pull-to-refresh). */
    fun refresh(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (cars.isEmpty()) loading = true
            safeCall { CarRepository.visibleCars() }
                .onSuccess { cars = it; error = null }
                .onFailure { error = it.mensajeUsuario() }
            loading = false
            onDone()

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
