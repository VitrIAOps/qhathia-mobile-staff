package com.qhathia.mobile.staff.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qhathia.mobile.staff.feature.auth.navigation.LOGIN_ROUTE
import com.qhathia.mobile.staff.feature.auth.navigation.authGraph
import com.qhathia.mobile.staff.ui.home.HomePlaceholderScreen

const val HOME_ROUTE = "home"

/**
 * App navigation graph. Starts at login or home depending on [startAuthenticated]; a successful
 * login replaces login with home so back does not return to the login screen.
 */
@Composable
fun QhathiaNavHost(
    startAuthenticated: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = if (startAuthenticated) HOME_ROUTE else LOGIN_ROUTE,
    ) {
        authGraph(
            onNavigateToHome = {
                navController.navigate(HOME_ROUTE) {
                    popUpTo(LOGIN_ROUTE) { inclusive = true }
                }
            },
        )
        composable(route = HOME_ROUTE) {
            HomePlaceholderScreen()
        }
    }
}
