package com.qhathia.mobile.staff.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.feature.auth.ui.LoginScreen

/** Stable route id for the login destination. */
const val LOGIN_ROUTE = "login"

/** Registers the auth graph. [onNavigateToHome] is invoked after a successful login. */
fun NavGraphBuilder.authGraph(onNavigateToHome: () -> Unit) {
    composable(route = LOGIN_ROUTE) {
        LoginScreen(onNavigateToHome = onNavigateToHome)
    }
}

/** Convenience navigation to the login destination. */
fun NavController.navigateToLogin() = navigate(LOGIN_ROUTE)
