package com.pame.karsy.data.repository

import com.pame.karsy.data.model.Car
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Favoritos e historial ocultan las publicaciones pausadas por su dueño o deshabilitadas por el admin. */
class SigueDisponibleTest {

    private fun car(id: Long, status: String) = Car(
        id = id, brand = "Mazda", model = "3", year = 2020, price = "$300,000", currency = "MXN",
        description = "", imageUrl = null, status = status,
    )

    @Test
    fun activoVendidoPendienteYRechazado_siguenDisponibles() {
        for (s in listOf("Activo", "Vendido", "Pendiente", "Rechazado")) {
            assertTrue("$s debería seguir disponible", CarRepository.sigueDisponible(car(1, s)))
        }
    }

    @Test
    fun pausadoPorElDueno_noSigueDisponible() {
        assertFalse(CarRepository.sigueDisponible(car(1, "Pausado")))
    }

    @Test
    fun deshabilitadoPorElAdmin_noSigueDisponible() {
        assertFalse(CarRepository.sigueDisponible(car(1, "Deshabilitado")))
    }

    @Test
    fun estadoPorDefectoDeCar_esActivoYSigueDisponible() {
        val c = Car(id = 1, brand = "Kia", model = "Rio", year = 2019, price = "$200,000", currency = "MXN", description = "", imageUrl = null)
        assertTrue(CarRepository.sigueDisponible(c))
    }

    @Test
    fun filtrarUnaListaMixta_conservaElOrdenYSoloQuitaPausadasYDeshabilitadas() {
        val lista = listOf(
            car(1, "Activo"), car(2, "Pausado"), car(3, "Vendido"),
            car(4, "Deshabilitado"), car(5, "Pendiente"), car(6, "Rechazado"),
        )
        assertEquals(listOf(1L, 3L, 5L, 6L), lista.filter(CarRepository::sigueDisponible).map { it.id })
    }

    @Test
    fun listaVacia_quedaVacia() {
        assertEquals(emptyList<Car>(), emptyList<Car>().filter(CarRepository::sigueDisponible))
    }
}
