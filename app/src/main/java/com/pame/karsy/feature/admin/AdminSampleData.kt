package com.pame.karsy.feature.admin

import androidx.compose.ui.graphics.Color

// Datos de ejemplo copiados del mockup (WebAdminDashboardView y vistas de administración).
// TODO: reemplazar por consultas reales a Supabase (public.cuentas, lotes, publicaciones, reportes, solicitudes_destacado).

data class AdminUser(
    val name: String,
    val email: String,
    val type: String,
    val date: String,
    val status: String,
    val posts: Int = 12,
)

data class AdminLot(
    val name: String,
    val responsible: String,
    val city: String,
    val vehicles: Int,
    val status: String,
)

data class AdminVehicle(
    val name: String,
    val seller: String,
    val year: Int,
    val price: String,
    val status: String,
)

data class AdminReport(
    val id: Int,
    val type: String,
    val reporter: String,
    val target: String,
    val date: String,
    val status: String,
)

enum class FeaturedStatus { Pendiente, Aprobada, Rechazada }

data class FeaturedRequest(
    val id: Int,
    val vehicle: String,
    val year: Int,
    val price: String,
    val image: String,
    val user: String,
    val time: String,
    val status: FeaturedStatus,
)

data class AdminKpi(
    val label: String,
    val value: String,
    val change: String,
    val positive: Boolean,
    val color: Color,
    val bg: Color,
)

data class AdminAlert(val text: String, val bg: Color, val color: Color)

object AdminSampleData {

    val users = listOf(
        AdminUser("María González", "maria.gonzalez@email.com", "Particular", "15 sep 2026", "Activo"),
        AdminUser("Carlos Ramírez", "carlos.ramirez@email.com", "Particular", "14 sep 2026", "Activo"),
        AdminUser("Auto Premium", "contacto@autopremium.com", "Lote", "13 sep 2026", "Activo"),
        AdminUser("Laura Martínez", "laura.martinez@email.com", "Particular", "11 sep 2026", "Suspendido"),
        AdminUser("Seminuevos del Pacífico", "ventas@pacifico.com", "Lote", "10 sep 2026", "Activo"),
        AdminUser("Jorge Hernández", "jorge.hernandez@email.com", "Particular", "09 sep 2026", "Activo"),
    )

    val lots = listOf(
        AdminLot("Auto Premium", "Carlos Ramírez", "Manzanillo", 38, "Activo"),
        AdminLot("Seminuevos del Pacífico", "Laura Torres", "Colima", 26, "Activo"),
        AdminLot("Autos del Valle", "Miguel Sánchez", "Tecomán", 19, "Activo"),
        AdminLot("Grupo Motor", "Andrea López", "Manzanillo", 14, "Pendiente"),
        AdminLot("Karsy Motors", "Daniel Pérez", "Villa de Álvarez", 9, "Activo"),
    )

    val vehicles = listOf(
        AdminVehicle("BMW Serie 3 320i", "Auto Premium", 2023, "$685,000", "Activo"),
        AdminVehicle("Toyota Corolla LE", "María González", 2022, "$325,000", "Activo"),
        AdminVehicle("Nissan Versa Advance", "Seminuevos del Pacífico", 2022, "$245,000", "Activo"),
        AdminVehicle("Volkswagen Jetta Trendline", "Carlos Ramírez", 2020, "$280,000", "Deshabilitado"),
        AdminVehicle("Mazda 3 Sedán i Sport", "Autos del Valle", 2023, "$365,000", "Pendiente"),
        AdminVehicle("Honda Civic Sport", "Laura Martínez", 2021, "$298,000", "Activo"),
    )

    val reports = listOf(
        AdminReport(1, "Publicación", "María González", "BMW Serie 3 320i", "17 sep 2026", "Pendiente"),
        AdminReport(2, "Usuario", "Jorge Hernández", "Laura Martínez", "16 sep 2026", "En revisión"),
        AdminReport(3, "Publicación", "Carlos Ramírez", "Jetta Trendline", "15 sep 2026", "Resuelto"),
        AdminReport(4, "Usuario", "Andrea López", "Cuenta sospechosa", "14 sep 2026", "Pendiente"),
    )

    val featuredRequests = listOf(
        FeaturedRequest(
            1, "Honda Civic Sport", 2021, "$298,000 MXN",
            "https://images.unsplash.com/photo-1590362891991-f776e747a588?w=900&h=560&fit=crop&auto=format",
            "Pamela Rodríguez", "Hace 2 horas", FeaturedStatus.Pendiente,
        ),
        FeaturedRequest(
            2, "BMW Serie 3", 2022, "$625,000 MXN",
            "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=900&h=560&fit=crop&auto=format",
            "Jorge Hernández", "Hace 5 horas", FeaturedStatus.Pendiente,
        ),
        FeaturedRequest(
            3, "Mazda 3 i Grand Touring", 2020, "$315,000 MXN",
            "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=900&h=560&fit=crop&auto=format",
            "Laura Martínez", "Ayer", FeaturedStatus.Aprobada,
        ),
    )

    const val REQUESTER_AVATAR =
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=120&h=120&fit=crop&auto=format"

    val rejectOptions = listOf(
        "Pago no verificado" to "El comprobante de pago no coincide o no ha sido acreditado.",
        "Calidad de imágenes" to "Las fotos del vehículo no cumplen con la calidad o visibilidad requerida.",
        "Información incompleta" to "La publicación carece de detalles obligatorios o precisos.",
        "Vehículo no permitido" to "El vehículo no cumple con las políticas de publicación de Karsy.",
        "Otro motivo" to "Especifica el motivo en los comentarios adicionales.",
    )

    val kpis = listOf(
        AdminKpi("Usuarios registrados", "1,248", "+12%", true, Color(0xFF3B82F6), Color(0xFFEFF6FF)),
        AdminKpi("Lotes registrados", "86", "+8%", true, Color(0xFFA855F7), Color(0xFFF5F3FF)),
        AdminKpi("Vehículos publicados", "3,562", "+15%", true, Color(0xFF14B8A6), Color(0xFFF0FDFA)),
        AdminKpi("Publicaciones activas", "3,214", "+14%", true, Color(0xFF22C55E), Color(0xFFF0FDF4)),
        AdminKpi("Publicaciones deshabilitadas", "348", "-6%", false, Color(0xFFF59E0B), Color(0xFFFFFBEB)),
    )

    val alerts = listOf(
        AdminAlert("5 publicaciones deshabilitadas recientemente", Color(0xFFFEF2F2), Color(0xFFEF4444)),
        AdminAlert("3 cuentas desactivadas", Color(0xFFFFFBEB), Color(0xFFF59E0B)),
        AdminAlert("2 nuevos lotes registrados", Color(0xFFEFF6FF), Color(0xFF3B82F6)),
        AdminAlert("7 publicaciones pendientes de revisión", Color(0xFFF5F3FF), Color(0xFF8B5CF6)),
        AdminAlert("4 reportes de usuarios", Color(0xFFFEF2F2), Color(0xFFEF4444)),
    )

    // Crecimiento de la plataforma (usuarios, lotes, publicaciones por día)
    val growthLabels = listOf("3 sep", "4 sep", "5 sep", "6 sep", "7 sep", "8 sep", "9 sep")
    val growthUsuarios = listOf(180f, 195f, 210f, 225f, 240f, 255f, 268f)
    val growthLotes = listOf(60f, 68f, 72f, 78f, 82f, 85f, 88f)
    val growthPublicaciones = listOf(320f, 350f, 420f, 480f, 540f, 610f, 680f)

    // Comparación de ventas vía Karsy vs. fuera de la plataforma
    val salesLabels = listOf("Abr", "May", "Jun", "Jul", "Ago", "Sep")
    val salesViaPlataforma = listOf(82, 96, 118, 134, 156, 182)
    val salesFuera = listOf(58, 64, 72, 78, 84, 92)
}
