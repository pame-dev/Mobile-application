package com.pame.karsy.core.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun KarsyNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Welcome.route
    ) {
        composable(Routes.Welcome.route) {
            Text("Welcome") // TODO: reemplazar por WelcomeScreen()
        }
        composable(Routes.Login.route) {
            Text("Login") // TODO: reemplazar por LoginScreen()
        }
        composable(Routes.RegisterType.route) {
            Text("RegisterType") // TODO
        }
        composable(Routes.RegisterParticular.route) {
            Text("RegisterParticular") // TODO
        }
        composable(Routes.RegisterLote.route) {
            Text("RegisterLote") // TODO
        }
        composable(Routes.Home.route) {
            Text("Home") // TODO
        }
        composable(Routes.CarDetail.route) { backStackEntry ->
            val carId = backStackEntry.arguments?.getString("carId") ?: ""
            Text("CarDetail: $carId") // TODO
        }
        composable(Routes.Favorites.route) {
            Text("Favorites") // TODO
        }
        composable(Routes.Profile.route) {
            Text("Profile") // TODO
        }
        composable(Routes.History.route) {
            Text("History") // TODO
        }
        composable(Routes.Settings.route) {
            Text("Settings") // TODO
        }
        composable(Routes.PublishFlow.route) {
            Text("PublishFlow") // TODO
        }
        composable(Routes.Dashboard.route) {
            Text("Dashboard") // TODO
        }
        composable(Routes.AdminDashboard.route) {
            Text("AdminDashboard") // TODO
        }
    }
}