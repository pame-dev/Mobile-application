package com.pame.karsy.core.navigation

import android.net.Uri

sealed class Routes(val route: String) {
    object Welcome : Routes("welcome")
    object Login : Routes("login")

    /** Código de verificación del correo. send = true genera uno nuevo al entrar. */
    object VerifyEmail : Routes("verify_email/{email}?send={send}") {
        const val ARG_EMAIL = "email"
        const val ARG_SEND = "send"
        fun createRoute(email: String, sendCode: Boolean) = "verify_email/${Uri.encode(email)}?send=$sendCode"
    }

    object RegisterType : Routes("register_type")
    object RegisterParticular : Routes("register_particular")
    object RegisterLote : Routes("register_lote")

    object Home : Routes("home")

    object CarDetail : Routes("car_detail/{carId}") {
        const val ARG = "carId"
        fun createRoute(carId: Long) = "car_detail/$carId"
    }

    object Favorites : Routes("favorites")
    object Profile : Routes("profile")
    object History : Routes("history")
    object Settings : Routes("settings")

    object PublishFlow : Routes("publish_flow")
    object Dashboard : Routes("dashboard")
    object AdminDashboard : Routes("admin_dashboard")
}
