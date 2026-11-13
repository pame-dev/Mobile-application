package com.pame.karsy.data.repository

import com.pame.karsy.data.model.Car
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Favoritos muestra activos, vendidos, pausados y deshabilitados; no pendientes ni rechazados. */
class SeVeEnFavoritosTest {

    private fun car(id: Long, status: String) = Car(
        id = id, brand = "Mazda", model = "3", year = 2020, price = "$300,000", currency = "MXN",
        description = "", imageUrl = null, status = status,
    )

    @Test
    fun activoVendidoPausadoDeshabilitado_seVen() {
        for (s in listOf("Activo", "Vendido", "Pausado", "Deshabilitado")) {
            assertTrue("$s debería verse en favoritos", CarRepository.seVeEnFavoritos(car(1, s)))
        }
    }

    @Test
    fun pendienteYRechazado_noSeVen() {
        for (s in listOf("Pendiente", "Rechazado")) {
            assertFalse("$s no debería verse en favoritos", CarRepository.seVeEnFavoritos(car(1, s)))
        }
    }

    @Test
    fun listaMixta_conservaOrden() {
        val lista = listOf(
            car(1, "Activo"), car(2, "Pendiente"), car(3, "Vendido"),
            car(4, "Rechazado"), car(5, "Pausado"), car(6, "Deshabilitado"),
        )
        assertEquals(listOf(1L, 3L, 5L, 6L), lista.filter(CarRepository::seVeEnFavoritos).map { it.id })
    }

    @Test
    fun listaVacia_quedaVacia() {
        assertEquals(emptyList<Car>(), emptyList<Car>().filter(CarRepository::seVeEnFavoritos))
    }
}
