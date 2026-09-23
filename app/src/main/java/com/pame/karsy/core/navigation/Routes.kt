package com.pame.karsy.core.navigation

sealed class Routes(val route: String) {
    object Welcome : Routes("welcome")
    object Login : Routes("login")

    object RegisterType : Routes("register_type")
    object RegisterParticular : Routes("register_particular")
    object RegisterLote : Routes("register_lote")

    object Home : Routes("home")

    object CarDetail : Routes("car_detail/{carId}") {
        const val ARG = "carId"
        fun createRoute(carId: Int) = "car_detail/$carId"
    }

    object Favorites : Routes("favorites")
    object Profile : Routes("profile")
    object History : Routes("history")
    object Settings : Routes("settings")

    object PublishFlow : Routes("publish_flow")
    object Dashboard : Routes("dashboard")
    object AdminDashboard : Routes("admin_dashboard")
}
