package com.pame.karsy.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.session.UserMode
import com.pame.karsy.feature.admin.AdminDashboardScreen
import com.pame.karsy.feature.cardetail.CarDetailScreen
import com.pame.karsy.feature.dashboard.DashboardScreen
import com.pame.karsy.feature.favorites.FavoritesScreen
import com.pame.karsy.feature.home.HomeScreen
import com.pame.karsy.feature.onboarding.LoginScreen
import com.pame.karsy.feature.onboarding.WelcomeScreen
import com.pame.karsy.feature.profile.HistoryScreen
import com.pame.karsy.feature.profile.ProfileScreen
import com.pame.karsy.feature.profile.SettingsScreen
import com.pame.karsy.feature.publish.PublishFlowScreen
import com.pame.karsy.feature.register.RegisterLoteScreen
import com.pame.karsy.feature.register.RegisterParticularScreen
import com.pame.karsy.feature.register.RegisterTypeScreen

@Composable
fun KarsyNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val userMode = SessionManager.userMode

    /** Entra al Home con el modo indicado y borra el historial de onboarding. */
    fun enterHome(mode: UserMode) {
        SessionManager.login(mode)
        navController.navigate(Routes.Home.route) {
            popUpTo(Routes.Welcome.route) { inclusive = true }
        }
    }

    fun openCar(id: Int) = navController.navigate(Routes.CarDetail.createRoute(id))
    fun goRegister() = navController.navigate(Routes.RegisterType.route)
    fun back() { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = Routes.Welcome.route
    ) {
        // ── Onboarding ───────────────────────────────────────────────
        composable(Routes.Welcome.route) {
            WelcomeScreen(
                onLogin = { navController.navigate(Routes.Login.route) },
                onRegister = ::goRegister,
                onGuest = { enterHome(UserMode.VISITANTE) }
            )
        }
        composable(Routes.Login.route) {
            LoginScreen(
                onBack = ::back,
                onLogin = { mode -> enterHome(mode) },
                onRegister = ::goRegister
            )
        }

        // ── Registro ─────────────────────────────────────────────────
        composable(Routes.RegisterType.route) {
            RegisterTypeScreen(
                onBack = ::back,
                onParticular = { navController.navigate(Routes.RegisterParticular.route) },
                onLote = { navController.navigate(Routes.RegisterLote.route) }
            )
        }
        composable(Routes.RegisterParticular.route) {
            RegisterParticularScreen(
                onBack = ::back,
                onCreated = { enterHome(UserMode.PARTICULAR) }
            )
        }
        composable(Routes.RegisterLote.route) {
            RegisterLoteScreen(
                onBack = ::back,
                onCreated = { enterHome(UserMode.LOTE) }
            )
        }

        // ── Marketplace ──────────────────────────────────────────────
        composable(Routes.Home.route) {
            HomeScreen(
                userMode = userMode,
                onCarClick = ::openCar,
                onFavorites = { navController.navigate(Routes.Favorites.route) },
                onProfile = { navController.navigate(Routes.Profile.route) },
                onAdminPanel = { navController.navigate(Routes.AdminDashboard.route) },
                onPublish = { navController.navigate(Routes.PublishFlow.route) },
                onRegister = ::goRegister
            )
        }
        composable(
            Routes.CarDetail.route,
            arguments = listOf(navArgument(Routes.CarDetail.ARG) { type = NavType.IntType })
        ) { entry ->
            CarDetailScreen(
                carId = entry.arguments?.getInt(Routes.CarDetail.ARG) ?: 0,
                userMode = userMode,
                onBack = ::back,
                onRegister = ::goRegister
            )
        }
        composable(Routes.Favorites.route) {
            FavoritesScreen(onBack = ::back, onCarClick = ::openCar)
        }

        // ── Perfil ───────────────────────────────────────────────────
        composable(Routes.Profile.route) {
            ProfileScreen(
                userMode = userMode,
                onBack = ::back,
                onPanel = {
                    navController.navigate(
                        if (userMode.isAdmin) Routes.AdminDashboard.route else Routes.Dashboard.route
                    )
                },
                onHistory = { navController.navigate(Routes.History.route) },
                onSettings = { navController.navigate(Routes.Settings.route) },
                onCarClick = ::openCar
            )
        }
        composable(Routes.History.route) {
            HistoryScreen(onBack = ::back, onCarClick = ::openCar)
        }
        composable(Routes.Settings.route) {
            SettingsScreen(
                onBack = ::back,
                onLogout = {
                    SessionManager.logout()
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Publicar y panel del vendedor ────────────────────────────
        composable(Routes.PublishFlow.route) {
            PublishFlowScreen(
                onBack = ::back,
                onFinished = {
                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.PublishFlow.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Dashboard.route) {
            DashboardScreen(
                onBack = ::back,
                onNewPublication = { navController.navigate(Routes.PublishFlow.route) }
            )
        }

        // ── Administración ───────────────────────────────────────────
        composable(Routes.AdminDashboard.route) {
            AdminDashboardScreen(onBack = ::back)
        }
    }
}
