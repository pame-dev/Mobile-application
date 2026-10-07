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
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.pame.karsy.feature.admin.AdminVehicleReviewBar
import com.pame.karsy.feature.admin.AdminViewModel
import com.pame.karsy.feature.admin.ReportReviewBar
import com.pame.karsy.feature.admin.ReportReviewNotice
import com.pame.karsy.feature.admin.UserReportReviewScreen
import com.pame.karsy.feature.cardetail.CarDetailScreen
import com.pame.karsy.feature.dashboard.DashboardScreen
import com.pame.karsy.feature.favorites.FavoritesScreen
import com.pame.karsy.feature.home.HomeScreen
import com.pame.karsy.feature.onboarding.LoginScreen
import com.pame.karsy.feature.onboarding.VerifyEmailScreen
import com.pame.karsy.feature.onboarding.WelcomeScreen
import com.pame.karsy.feature.profile.HistoryScreen
import com.pame.karsy.feature.profile.NotificationsScreen
import com.pame.karsy.feature.profile.ProfileScreen
import com.pame.karsy.feature.profile.SettingsScreen
import com.pame.karsy.feature.profile.PrivacyNoticeScreen
import com.pame.karsy.feature.profile.PrivacyScreen
import com.pame.karsy.feature.profile.TermsScreen
import com.pame.karsy.feature.publish.PublishFlowScreen
import com.pame.karsy.feature.register.RegisterLoteScreen
import com.pame.karsy.feature.register.RegisterParticularScreen
import com.pame.karsy.feature.register.RegisterTypeScreen
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pame.karsy.core.push.PushTokens

@Composable
fun KarsyNavGraph(
    navController: NavHostController = rememberNavController(),
    /** Cambia cada vez que el usuario toca una notificación push. */
    openNotificationsRequest: Int = 0,
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

    // ── Notificaciones push ──────────────────────────────────────────────────
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val sessionUserId = SessionManager.userId
    LaunchedEffect(sessionUserId) {
        if (sessionUserId == null) return@LaunchedEffect
        // Este teléfono recibe los avisos de la cuenta en sesión.
        safeCall { PushTokens.register(context) }
        // Android 13+: se pide el permiso una sola vez (si lo niega, no se insiste).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            val prefs = context.getSharedPreferences("push", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("permiso_pedido", false)) {
                prefs.edit().putBoolean("permiso_pedido", true).apply()
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // Tocar una notificación push abre la bandeja de avisos (si hay sesión).
    LaunchedEffect(openNotificationsRequest, sessionUserId) {
        if (openNotificationsRequest > 0 && sessionUserId != null) {
            navController.navigate(Routes.Notifications.route) { launchSingleTop = true }
        }
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
            // Antes de cerrar sesión: este teléfono deja de recibir avisos de la cuenta.
            PushTokens.unregister(context)
            AuthRepository.signOut()
            SessionManager.logout()
            navController.navigate(Routes.Welcome.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    fun openCar(id: Long) = navController.navigate(Routes.CarDetail.createRoute(id))
    fun openAdminCar(id: Long) = navController.navigate(Routes.AdminVehicleReview.createRoute(id))
    fun goRegister() = navController.navigate(Routes.RegisterType.route)
    fun goVerifyEmail(email: String, sendCode: Boolean) =
        navController.navigate(Routes.VerifyEmail.createRoute(email, sendCode))
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
                onGuest = ::enterAsGuest,
                onTerms = { navController.navigate(Routes.Terms.route) },
                onPrivacyNotice = { navController.navigate(Routes.PrivacyNotice.route) }
            )
        }
        composable(Routes.Login.route) {
            LoginScreen(
                onBack = ::back,
                onLogin = ::enterAs,
                onVerifyEmail = { goVerifyEmail(it, sendCode = true) },
                onRegister = ::goRegister
            )
        }
        composable(
            Routes.VerifyEmail.route,
            arguments = listOf(
                navArgument(Routes.VerifyEmail.ARG_EMAIL) { type = NavType.StringType },
                navArgument(Routes.VerifyEmail.ARG_SEND) { type = NavType.BoolType; defaultValue = true },
            )
        ) { entry ->
            VerifyEmailScreen(
                email = entry.arguments?.getString(Routes.VerifyEmail.ARG_EMAIL).orEmpty(),
                sendCode = entry.arguments?.getBoolean(Routes.VerifyEmail.ARG_SEND) ?: true,
                onBack = ::back,
                onVerified = ::enterAs
            )
        }

        // ── Registro ─────────────────────────────────────────────────
        composable(Routes.RegisterType.route) {
            RegisterTypeScreen(
                onBack = ::back,
                onParticular = { navController.navigate(Routes.RegisterParticular.route) },
                onLote = { navController.navigate(Routes.RegisterLote.route) },
                onLogin = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Welcome.route)
                    }
                }
            )
        }
        // Supabase manda el código al crear la cuenta; no se pide otro al entrar.
        composable(Routes.RegisterParticular.route) {
            RegisterParticularScreen(
                onBack = ::back,
                onCreated = ::enterAs,
                onVerifyEmail = { goVerifyEmail(it, sendCode = false) },
                onTerms = { navController.navigate(Routes.Terms.route) },
                onLogin = { navController.navigate(Routes.Login.route) }
            )
        }
        composable(Routes.RegisterLote.route) {
            RegisterLoteScreen(
                onBack = ::back,
                onCreated = ::enterAs,
                onVerifyEmail = { goVerifyEmail(it, sendCode = false) },
                onTerms = { navController.navigate(Routes.Terms.route) },
                onLogin = { navController.navigate(Routes.Login.route) }
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
                onPublish = { navController.navigate(Routes.PublishFlow.createRoute()) },
                onRegister = ::goRegister,
                onLogin = { navController.navigate(Routes.Login.route) },
                onLogout = ::logout
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
                onCarClick = ::openCar,
                onEditPublication = { navController.navigate(Routes.PublishFlow.createRoute(it)) }
            )
        }
        composable(Routes.Favorites.route) {
            FavoritesScreen(userMode = userMode, onBack = ::back, onCarClick = ::openCar)
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
                onLogout = ::logout,
                onTerms = { navController.navigate(Routes.Terms.route) },
                onNotifications = { navController.navigate(Routes.Notifications.route) },
                onPrivacy = { navController.navigate(Routes.Privacy.route) }
            )
        }
        composable(Routes.Privacy.route) {
            PrivacyScreen(
                onBack = ::back,
                onPrivacyNotice = { navController.navigate(Routes.PrivacyNotice.route) },
                // Las sesiones ya se cerraron en Supabase; aquí solo se limpia la app.
                onSignedOutEverywhere = {
                    SessionManager.logout()
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.PrivacyNotice.route) {
            PrivacyNoticeScreen(onBack = ::back)
        }
        composable(Routes.Terms.route) {
            TermsScreen(onBack = ::back)
        }
        composable(Routes.Notifications.route) {
            NotificationsScreen(onBack = ::back, onCarClick = ::openCar)
        }

        // ── Publicar y panel del vendedor ────────────────────────────
        composable(
            Routes.PublishFlow.route,
            arguments = listOf(navArgument(Routes.PublishFlow.ARG_EDIT) { type = NavType.LongType; defaultValue = -1L })
        ) { entry ->
            val editando = (entry.arguments?.getLong(Routes.PublishFlow.ARG_EDIT) ?: -1L) > 0
            PublishFlowScreen(
                onBack = ::back,
                onFinished = {
                    // Al corregir se llegó desde el panel: basta con regresar a él.
                    if (editando) back()
                    else navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.PublishFlow.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Dashboard.route) {
            DashboardScreen(
                onBack = ::back,
                onNewPublication = { navController.navigate(Routes.PublishFlow.createRoute()) },
                onEditPublication = { navController.navigate(Routes.PublishFlow.createRoute(it)) },
                onCarClick = ::openCar
            )
        }

        // ── Administración ───────────────────────────────────────────
        composable(Routes.AdminDashboard.route) {
            AdminDashboardScreen(
                onBack = ::back,
                onCarClick = ::openAdminCar,
                onReviewReport = { navController.navigate(Routes.AdminReportReview.createRoute(it)) },
                onLogout = ::logout
            )
        }
        // Publicación abierta desde el panel: solo acciones de admin (aunque el admin sea el dueño).
        composable(
            Routes.AdminVehicleReview.route,
            arguments = listOf(navArgument(Routes.AdminVehicleReview.ARG) { type = NavType.LongType })
        ) { entry ->
            val panelEntry = remember(entry) { navController.getBackStackEntry(Routes.AdminDashboard.route) }
            val adminVm: AdminViewModel = viewModel(panelEntry)
            val carId = entry.arguments?.getLong(Routes.AdminVehicleReview.ARG) ?: 0L
            val vehicle = adminVm.vehicles.firstOrNull { it.id == carId }
            CarDetailScreen(
                carId = carId,
                userMode = userMode,
                onBack = ::back,
                onRegister = ::goRegister,
                onCarClick = ::openAdminCar,
                bottomBar = { vehicle?.let { AdminVehicleReviewBar(adminVm, it, onDone = ::back) } }
            )
        }
        // Comparte el AdminViewModel del panel: al resolver, la lista de reportes se recarga sola.
        composable(
            Routes.AdminReportReview.route,
            arguments = listOf(navArgument(Routes.AdminReportReview.ARG) { type = NavType.LongType })
        ) { entry ->
            val panelEntry = remember(entry) { navController.getBackStackEntry(Routes.AdminDashboard.route) }
            val adminVm: AdminViewModel = viewModel(panelEntry)
            val reportId = entry.arguments?.getLong(Routes.AdminReportReview.ARG) ?: 0L
            val report = adminVm.reports.firstOrNull { it.id == reportId }
            if (report == null) {
                // Ya resuelto o la lista aún no carga: regresa al panel.
                LaunchedEffect(adminVm.loading) { if (!adminVm.loading) back() }
            } else if (report.type == "Usuario") {
                // Reporte de cuenta: se revisa la cuenta del vendedor, no la publicación.
                UserReportReviewScreen(
                    vm = adminVm,
                    report = report,
                    onBack = ::back,
                    onCarClick = ::openAdminCar,
                    onDone = ::back
                )
            } else {
                CarDetailScreen(
                    carId = report.publicationId,
                    userMode = userMode,
                    onBack = ::back,
                    onRegister = ::goRegister,
                    onCarClick = ::openCar,
                    topNotice = { ReportReviewNotice(report) },
                    // Sin barra del dueño aunque el admin sea quien publicó.
                    bottomBar = {
                        if (report.status == "Pendiente") ReportReviewBar(adminVm, report, onDone = ::back)
                    }
                )
            }
        }
    }
}

