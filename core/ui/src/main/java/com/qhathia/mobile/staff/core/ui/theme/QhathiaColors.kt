package com.qhathia.mobile.staff.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design-system colors that have no Material 3 role, so they cannot live in
 * `MaterialTheme.colorScheme`: the warning pair behind "quedan pocos disponibles", the success pair
 * behind a confirmation snackbar, and the brand header gradient.
 *
 * Each resolves against the active light/dark theme exactly like a `colorScheme` role would —
 * feature modules read these instead of hardcoding hex values.
 */
object QhathiaColors {

    val warningContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkWarningContainer
        } else {
            QhathiaPalette.WarningContainer
        }

    val onWarningContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkOnWarningContainer
        } else {
            QhathiaPalette.OnWarningContainer
        }

    val success: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) QhathiaPalette.DarkSuccess else QhathiaPalette.Success

    val onSuccess: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.OnSuccess

    /** Brand navy sweeping into burnt orange, left to right. */
    val headerGradient: Brush
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            Brush.horizontalGradient(
                listOf(
                    QhathiaPalette.DarkHeaderGradientStart,
                    QhathiaPalette.DarkHeaderGradientEnd,
                ),
            )
        } else {
            Brush.horizontalGradient(
                listOf(QhathiaPalette.HeaderGradientStart, QhathiaPalette.HeaderGradientEnd),
            )
        }

    /** Text and icons drawn on top of [headerGradient]. */
    val onHeaderGradient: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.OnPrimary
}
