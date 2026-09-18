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

    /** Soft success surface: rank badges and positive delta pills. */
    val successContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkSuccessContainer
        } else {
            QhathiaPalette.SuccessContainer
        }

    val onSuccessContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkOnSuccessContainer
        } else {
            QhathiaPalette.OnSuccessContainer
        }

    /** Neutral track behind a segmented control drawn on the screen background. */
    val segmentTrack: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkSegmentTrack
        } else {
            QhathiaPalette.SegmentTrack
        }

    /** Light blue track behind a segmented control drawn inside a card, e.g. the "Filtros" panel. */
    val filterTrack: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkFilterTrack
        } else {
            QhathiaPalette.FilterTrack
        }

    /** Dark navy surface of the Inicio "Resumen" card. */
    val summaryCard: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkSummaryCardSurface
        } else {
            QhathiaPalette.SummaryCardSurface
        }

    /** Fill of an ordinary bar in the sales chart; the best bucket uses [brandAccentSoft]. */
    val summaryBar: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkSummaryBarFill
        } else {
            QhathiaPalette.SummaryBarFill
        }

    /** Surface of the insight strip sitting under the sales chart. */
    val insightStrip: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkInsightStripSurface
        } else {
            QhathiaPalette.InsightStripSurface
        }

    /** Text and glyphs drawn on [summaryCard]. */
    val onSummaryCard: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.OnSummaryCard

    /** Secondary text on [summaryCard], e.g. the period label above the headline amount. */
    val onSummaryCardMuted: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.OnSummaryCardMuted

    /**
     * Track behind the Mensual/Semanal/Diario/Por hora pills. It stays light in both themes because
     * it sits on the always-dark [summaryCard], so [onPeriodTab] is fixed against it.
     */
    val periodTabTrack: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.PeriodTabTrack

    /** Label of an unselected period pill; the selected one uses `onSecondary` over the accent. */
    val onPeriodTab: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.OnPeriodTab

    /** Surface of the "N / Búsquedas" block on a "producto no encontrado" card. */
    val searchCountContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkSearchCountContainer
        } else {
            QhathiaPalette.SearchCountContainer
        }

    val onSearchCountContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkOnSearchCountContainer
        } else {
            QhathiaPalette.OnSearchCountContainer
        }

    /** Surface of the "Solo queda N" chip on a restock card. */
    val stockAlertContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkStockAlertContainer
        } else {
            QhathiaPalette.StockAlertContainer
        }

    val onStockAlertContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkOnStockAlertContainer
        } else {
            QhathiaPalette.OnStockAlertContainer
        }

    /** Track of a checked settings switch. */
    val switchChecked: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkSwitchChecked
        } else {
            QhathiaPalette.SwitchChecked
        }

    /** Gold surface of the subscription card on Mi perfil and Mi suscripción. */
    val subscriptionCard: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            QhathiaPalette.DarkSubscriptionCardSurface
        } else {
            QhathiaPalette.SubscriptionCardSurface
        }

    /** Text and glyphs drawn on [subscriptionCard]. */
    val onSubscriptionCard: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.OnSubscriptionCard

    /** The "PLAN PRO" badge on [subscriptionCard]; fixed in both themes like the card it sits on. */
    val planBadge: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.PlanBadge

    val onPlanBadge: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.OnPlanBadge

    /** Track and fill of the subscription period bar. */
    val planProgressTrack: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.PlanProgressTrack

    val planProgressFill: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.PlanProgressFill

    /** Brand orange in the lower-right sweeping diagonally up into navy. */
    val headerGradient: Brush
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) DarkHeaderGradient else LightHeaderGradient

    /** Text and icons drawn on top of [headerGradient]. */
    val onHeaderGradient: Color
        @Composable @ReadOnlyComposable
        get() = QhathiaPalette.OnPrimary
}
