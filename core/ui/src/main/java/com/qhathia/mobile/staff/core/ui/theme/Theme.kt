package com.qhathia.mobile.staff.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = QhathiaPalette.TextPrimary,
    onPrimary = QhathiaPalette.OnPrimary,
    secondary = QhathiaPalette.BrandAccent,
    onSecondary = QhathiaPalette.OnBrandAccent,
    tertiary = QhathiaPalette.NavInactive,
    onTertiary = QhathiaPalette.OnBrandAccent,
    error = QhathiaPalette.StatusError,
    onError = QhathiaPalette.OnPrimary,
    errorContainer = QhathiaPalette.ErrorContainer,
    onErrorContainer = QhathiaPalette.OnErrorContainer,
    background = QhathiaPalette.ScreenBackground,
    onBackground = QhathiaPalette.TextPrimary,
    surface = QhathiaPalette.Surface,
    onSurface = QhathiaPalette.TextPrimary,
    surfaceVariant = QhathiaPalette.SegmentTrack,
    onSurfaceVariant = QhathiaPalette.TextSecondary,
    outline = QhathiaPalette.BorderDefault,
    outlineVariant = QhathiaPalette.BorderStrong,
)

private val DarkColors = darkColorScheme(
    primary = QhathiaPalette.DarkTextPrimary,
    onPrimary = QhathiaPalette.TextPrimary,
    secondary = QhathiaPalette.BrandAccent,
    onSecondary = QhathiaPalette.OnBrandAccent,
    tertiary = QhathiaPalette.DarkNavInactive,
    onTertiary = QhathiaPalette.OnBrandAccent,
    error = QhathiaPalette.StatusError,
    onError = QhathiaPalette.OnPrimary,
    errorContainer = QhathiaPalette.DarkErrorContainer,
    onErrorContainer = QhathiaPalette.DarkOnErrorContainer,
    background = QhathiaPalette.DarkSurface,
    onBackground = QhathiaPalette.DarkTextPrimary,
    surface = QhathiaPalette.DarkSurface,
    onSurface = QhathiaPalette.DarkTextPrimary,
    surfaceVariant = QhathiaPalette.DarkBorderDefault,
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
