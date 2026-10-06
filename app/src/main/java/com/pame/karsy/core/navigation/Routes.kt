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
    object Terms : Routes("terms")
    object Privacy : Routes("privacy")
    object PrivacyNotice : Routes("privacy_notice")
    object Notifications : Routes("notifications")

    /** Publicar; con editId corrige y reenvía una publicación rechazada. */
    object PublishFlow : Routes("publish_flow?editId={editId}") {
        const val ARG_EDIT = "editId"
        fun createRoute(editId: Long? = null) = if (editId == null) "publish_flow" else "publish_flow?editId=$editId"
    }
    object Dashboard : Routes("dashboard")
    object AdminDashboard : Routes("admin_dashboard")

    /** Publicación reportada abierta desde el panel, con las acciones de moderación abajo. */
    /** Publicación abierta desde el panel: acciones de admin abajo, nunca las del dueño. */
    object AdminVehicleReview : Routes("admin_vehicle_review/{carId}") {
        const val ARG = "carId"
        fun createRoute(carId: Long) = "admin_vehicle_review/$carId"
    }

    object AdminReportReview : Routes("admin_report_review/{reportId}") {
        const val ARG = "reportId"
        fun createRoute(reportId: Long) = "admin_report_review/$reportId"
    }
}

