package com.pame.karsy.feature.home

import com.pame.karsy.data.model.Car
import com.pame.karsy.data.remote.CarroceriaDto
import com.pame.karsy.data.repository.Catalogs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Filtro por tipo: un tipo escrito a mano (conserva el id de "Otros") cae en "Otros". */
class FiltrarTipoOtrosTest {

    private val catalogs = Catalogs(
        marcas = emptyList(),
        modelos = emptyList(),
        colores = emptyList(),
        transmisiones = emptyList(),
        carrocerias = listOf(
            CarroceriaDto(1, "SUV", false),
            CarroceriaDto(2, "Sedán", false),
            CarroceriaDto(9, "Otros", true),
        ),
    )

    private fun car(id: Long, bodyType: String, bodyTypeId: Long?) = Car(
        id = id, brand = "Marca", model = "Modelo", year = 2020, price = "1", currency = "MXN",
        description = "", imageUrl = null, bodyType = bodyType, bodyTypeId = bodyTypeId,
    )

    private val crossover = car(1, "Crossover", 9)
    private val suv = car(2, "SUV", 1)
    private val otrosPorNombre = car(3, "Otros", null)
    private val todos = listOf(crossover, suv, otrosPorNombre)

    private fun ids(l: List<Car>) = l.map { it.id }

    @Test
    fun otros_conCatalogo_encuentraTipoEscritoAMano() {
        val r = todos.filtrar(HomeFilters(tipo = "Otros"), "", catalogs)
        assertTrue(crossover in r)
        assertEquals(listOf(1L, 3L), ids(r))
    }

    @Test
    fun suv_noEncuentraElTipoEscritoAMano() {
        val r = todos.filtrar(HomeFilters(tipo = "SUV"), "", catalogs)
        assertEquals(listOf(2L), ids(r))
    }

    @Test
    fun otros_sinCatalogo_soloPorNombre() {
        val r = todos.filtrar(HomeFilters(tipo = "Otros"), "")
        assertEquals(listOf(3L), ids(r))
    }

    @Test
    fun todos_encuentraTodo() {
        assertEquals(listOf(1L, 2L, 3L), ids(todos.filtrar(HomeFilters(), "", catalogs)))
    }

    @Test
    fun filterOptions_tiposIncluyeOtros() {
        val tipos = filterOptionsOf(catalogs, emptyList()).tipos
        assertEquals(listOf(TODOS, "SUV", "Sedán", "Otros"), tipos)
    }
}
