package com.qhathia.mobile.staff.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = QhathiaPalette.TextPrimary,
    onPrimary = QhathiaPalette.OnPrimary,
    error = QhathiaPalette.StatusError,
    onError = QhathiaPalette.OnPrimary,
    background = QhathiaPalette.Surface,
    onBackground = QhathiaPalette.TextPrimary,
    surface = QhathiaPalette.Surface,
    onSurface = QhathiaPalette.TextPrimary,
    onSurfaceVariant = QhathiaPalette.TextSecondary,
    outline = QhathiaPalette.BorderDefault,
    outlineVariant = QhathiaPalette.BorderStrong,
)

private val DarkColors = darkColorScheme(
    primary = QhathiaPalette.DarkTextPrimary,
    onPrimary = QhathiaPalette.TextPrimary,
    error = QhathiaPalette.StatusError,
    onError = QhathiaPalette.OnPrimary,
    background = QhathiaPalette.DarkSurface,
    onBackground = QhathiaPalette.DarkTextPrimary,
    surface = QhathiaPalette.DarkSurface,
    onSurface = QhathiaPalette.DarkTextPrimary,
    onSurfaceVariant = QhathiaPalette.DarkTextSecondary,
    outline = QhathiaPalette.DarkBorderDefault,
    outlineVariant = QhathiaPalette.BorderStrong,
)

/** Root Material 3 theme. All composables and Compose tests must be wrapped in this theme. */
@Composable
fun QhathiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = QhathiaTypography,
        content = content,
    )
}
