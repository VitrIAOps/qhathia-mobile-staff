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
 *
 * [pendingDestination] is a section requested from outside the app (a notification tap). It is
 * handed to the shell, which owns the inner NavHost that can reach it, and cleared through
 * [onPendingDestinationConsumed] once honoured. While unauthenticated the shell is not composed, so
 * the request simply waits for login.
 */
@Composable
fun QhathiaNavHost(
    startAuthenticated: Boolean,
    pendingDestination: String?,
    onPendingDestinationConsumed: () -> Unit,
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
            MainShell(
                pendingDestination = pendingDestination,
                onPendingDestinationConsumed = onPendingDestinationConsumed,
            )
        }
    }
}
