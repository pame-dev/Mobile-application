package com.pame.karsy.feature.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** Vehículo publicado por el vendedor, tal como aparece en "Mis Vehículos Publicados". */
data class MyListing(val name: String, val price: String, val imageUrl: String)

/**
 * Estado del panel del vendedor (datos estáticos del mockup).
 * TODO: obtener métricas y publicaciones del usuario en sesión desde Supabase
 *  (publicaciones del propietario, favoritos por día, contactos recibidos).
 */
class DashboardViewModel : ViewModel() {

    val totalSales = 163
    val salesTrend = "▲ +18% este mes"
    val publishedCount = 12
    val inquiries = 84
    val salesMonths = listOf(28f, 42f, 35f, 58f, 47f, 65f, 52f, 71f, 63f, 80f, 74f, 92f)
    val favWeekly = listOf(3f, 7f, 5f, 12f, 9f, 15f, 11f)
    val weekDays = listOf("L", "M", "X", "J", "V", "S", "D")
    val avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=80&h=80&fit=crop&auto=format"

    val myListings = listOf(
        MyListing("BMW 320i 2023", "$685,000", "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=300&h=200&fit=crop&auto=format"),
        MyListing("Mustang EcoBoost 2021", "$580,000", "https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=300&h=200&fit=crop&auto=format"),
        MyListing("Audi A4 2022", "$620,000", "https://images.unsplash.com/photo-1541443131876-44b03de101c5?w=300&h=200&fit=crop&auto=format"),
        MyListing("Mercedes C200 2022", "$710,000", "https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?w=300&h=200&fit=crop&auto=format"),
    )

    /** Auto para el que se muestra el diálogo "¿Quieres destacar tu vehículo?". */
    var destacarCar by mutableStateOf<String?>(null)
        private set

    /** Auto para el que se muestra "Solicitud enviada". */
    var solicitudCar by mutableStateOf<String?>(null)
        private set

    fun askDestacar(carName: String) {
        destacarCar = carName
    }

    fun cancelDestacar() {
        destacarCar = null
    }

    fun confirmDestacar() {
        // TODO: insertar en solicitudes_destacado (estado 'pendiente') para la publicación seleccionada
        solicitudCar = destacarCar
        destacarCar = null
    }

    fun closeSolicitud() {
        solicitudCar = null
    }
}
