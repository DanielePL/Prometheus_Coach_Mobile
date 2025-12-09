package com.prometheuscoach.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.prometheuscoach.mobile.ui.screens.auth.AuthScreen
import com.prometheuscoach.mobile.ui.screens.auth.AuthViewModel
import com.prometheuscoach.mobile.ui.screens.calendar.CalendarScreen
import com.prometheuscoach.mobile.ui.screens.clients.ClientDetailScreen
import com.prometheuscoach.mobile.ui.screens.clients.ClientsScreen
import com.prometheuscoach.mobile.ui.screens.dashboard.DashboardScreen
import com.prometheuscoach.mobile.ui.screens.settings.SettingsScreen

@Composable
fun PrometheusNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    val startDestination: NavRoute = if (authState.isAuthenticated) {
        NavRoute.Dashboard
    } else {
        NavRoute.Auth
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<NavRoute.Auth> {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoute.Dashboard) {
                        popUpTo(NavRoute.Auth) { inclusive = true }
                    }
                }
            )
        }

        composable<NavRoute.Dashboard> {
            DashboardScreen(
                onNavigateToClients = { navController.navigate(NavRoute.Clients) },
                onNavigateToCalendar = { navController.navigate(NavRoute.Calendar) },
                onNavigateToSettings = { navController.navigate(NavRoute.Settings) },
                onNavigateToClientDetail = { clientId ->
                    navController.navigate(NavRoute.ClientDetail(clientId))
                }
            )
        }

        composable<NavRoute.Clients> {
            ClientsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClientDetail = { clientId ->
                    navController.navigate(NavRoute.ClientDetail(clientId))
                }
            )
        }

        composable<NavRoute.ClientDetail> { backStackEntry ->
            val route: NavRoute.ClientDetail = backStackEntry.toRoute()
            ClientDetailScreen(
                clientId = route.clientId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.Calendar> {
            CalendarScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<NavRoute.Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(NavRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
