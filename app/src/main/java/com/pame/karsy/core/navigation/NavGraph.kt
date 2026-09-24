package com.pame.karsy.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.session.UserMode
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.repository.AuthRepository
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
import kotlinx.coroutines.launch

@Composable
fun KarsyNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val userMode = SessionManager.userMode
    val scope = rememberCoroutineScope()

    // Al abrir la app se restaura la sesión guardada por Supabase Auth.
    var restoring by remember { mutableStateOf(true) }
    var restored by remember { mutableStateOf<SessionAccount?>(null) }
    LaunchedEffect(Unit) {
        restored = safeCall { AuthRepository.restoreSession() }.getOrNull()
        restored?.let(SessionManager::login)
        restoring = false
    }
    if (restoring) {
        Box(Modifier.fillMaxSize().background(KarsyBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KarsyTeal)
        }
        return
    }

    /**
     * Entra a la app según el rol de la cuenta y borra el historial de onboarding:
     * admin → panel de administración, lote → su panel de vendedor,
     * particular → inicio. El inicio queda debajo para regresar con "atrás".
     */
    fun enterAs(account: SessionAccount) {
        SessionManager.login(account)
        navController.navigate(Routes.Home.route) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
        when (account.mode) {
            UserMode.ADMIN -> navController.navigate(Routes.AdminDashboard.route)
            UserMode.LOTE -> navController.navigate(Routes.Dashboard.route)
            else -> Unit
        }
    }

    fun enterAsGuest() {
        SessionManager.enterAsGuest()
        navController.navigate(Routes.Home.route) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    fun logout() {
        scope.launch {
            AuthRepository.signOut()
            SessionManager.logout()
            navController.navigate(Routes.Welcome.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    fun openCar(id: Long) = navController.navigate(Routes.CarDetail.createRoute(id))
    fun goRegister() = navController.navigate(Routes.RegisterType.route)
    fun back() { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = if (restored != null) Routes.Home.route else Routes.Welcome.route
    ) {
        // ── Onboarding ───────────────────────────────────────────────
        composable(Routes.Welcome.route) {
            WelcomeScreen(
                onLogin = { navController.navigate(Routes.Login.route) },
                onRegister = ::goRegister,
                onGuest = ::enterAsGuest
            )
        }
        composable(Routes.Login.route) {
            LoginScreen(
                onBack = ::back,
                onLogin = ::enterAs,
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
            RegisterParticularScreen(onBack = ::back, onCreated = ::enterAs)
        }
        composable(Routes.RegisterLote.route) {
            RegisterLoteScreen(onBack = ::back, onCreated = ::enterAs)
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
            arguments = listOf(navArgument(Routes.CarDetail.ARG) { type = NavType.LongType })
        ) { entry ->
            CarDetailScreen(
                carId = entry.arguments?.getLong(Routes.CarDetail.ARG) ?: 0L,
                userMode = userMode,
                onBack = ::back,
                onRegister = ::goRegister,
                onCarClick = ::openCar
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
            SettingsScreen(onBack = ::back, onLogout = ::logout)
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
                onNewPublication = { navController.navigate(Routes.PublishFlow.route) },
                onCarClick = ::openCar
            )
        }

        // ── Administración ───────────────────────────────────────────
        composable(Routes.AdminDashboard.route) {
            AdminDashboardScreen(onBack = ::back, onCarClick = ::openCar, onLogout = ::logout)
        }
    }
}
