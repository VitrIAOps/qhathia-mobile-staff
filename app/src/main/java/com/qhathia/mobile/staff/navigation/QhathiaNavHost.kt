package com.qhathia.mobile.staff.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qhathia.mobile.staff.feature.auth.navigation.LOGIN_ROUTE
import com.qhathia.mobile.staff.feature.auth.navigation.authGraph

/** Route for the post-login app shell (bottom navigation + top-level destinations). */
const val MAIN_ROUTE = "main"

/**
 * App navigation graph. Starts at login or the main shell depending on [startAuthenticated]; a
 * successful login replaces login with the shell so back does not return to the login screen.
 */
@Composable
fun QhathiaNavHost(
    startAuthenticated: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = if (startAuthenticated) MAIN_ROUTE else LOGIN_ROUTE,
    ) {
        authGraph(
            onNavigateToHome = {
                navController.navigate(MAIN_ROUTE) {
                    popUpTo(LOGIN_ROUTE) { inclusive = true }
                }
            },
        )
        composable(route = MAIN_ROUTE) {
            MainShell()
        }
    }
}
