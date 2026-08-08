package com.qhathia.mobile.staff.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** The DS header sweep, as authored in Figma: orange at 35%, navy at 83%, along -69.67°. */
private const val HEADER_GRADIENT_ANGLE = -69.668f
private const val HEADER_GRADIENT_ORANGE_STOP = 0.3498f
private const val HEADER_GRADIENT_NAVY_STOP = 0.83446f

/**
 * A CSS-style angled linear gradient: [angleDegrees] is measured clockwise from straight up, and
 * the sweep is resolved against the box it paints, so the header keeps its designed diagonal at any
 * width or height instead of degenerating into a corner-to-corner sweep.
 */
private class AngledGradient(
    private val angleDegrees: Float,
    private val colors: List<Color>,
    private val stops: List<Float>,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(radians).toFloat()
        val dy = -cos(radians).toFloat()
        // Length of the CSS "gradient line": the box projected onto the sweep direction.
        val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val reach = Offset(dx * half, dy * half)
        return LinearGradientShader(
            from = center - reach,
            to = center + reach,
            colors = colors,
            colorStops = stops,
        )
    }
}

private val LightHeaderGradient = AngledGradient(
    angleDegrees = HEADER_GRADIENT_ANGLE,
    colors = listOf(QhathiaPalette.HeaderGradientOrange, QhathiaPalette.HeaderGradientNavy),
    stops = listOf(HEADER_GRADIENT_ORANGE_STOP, HEADER_GRADIENT_NAVY_STOP),
)

private val DarkHeaderGradient = AngledGradient(
    angleDegrees = HEADER_GRADIENT_ANGLE,
    colors = listOf(QhathiaPalette.DarkHeaderGradientOrange, QhathiaPalette.DarkHeaderGradientNavy),
    stops = listOf(HEADER_GRADIENT_ORANGE_STOP, HEADER_GRADIENT_NAVY_STOP),
)

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

    /** Fill of the "Filtros" pill — one step lighter than the brand accent it is outlined with. */
    val brandAccentSoft: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.BrandAccentSoft

    /** Secondary glyph tint inside a list card, e.g. the per-row overflow "⋮". */
    val iconMuted: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkIconMuted
        } else {
            QhathiaPalette.IconMuted
        }

    /** Hairline separating consecutive rows inside a list card. */
    val rowDivider: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkBorderDefault
        } else {
            QhathiaPalette.RowDivider
        }

    val onSuccess: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.OnSuccess

    /** Brand orange in the lower-right sweeping diagonally up into navy. */
    val headerGradient: Brush
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) DarkHeaderGradient else LightHeaderGradient

    /** Text and icons drawn on top of [headerGradient]. */
    val onHeaderGradient: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.OnPrimary
}
