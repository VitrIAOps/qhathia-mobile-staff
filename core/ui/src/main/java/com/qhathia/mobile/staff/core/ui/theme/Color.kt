package com.qhathia.mobile.staff.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw Qhathia Design System color tokens (Figma "Qhatia DS").
 * Prefer consuming these through `MaterialTheme.colorScheme` roles (see [QhathiaTheme]).
 */
internal object QhathiaPalette {
    val TextPrimary = Color(0xFF0A2540)
    val TextSecondary = Color(0xFF616161)
    val TextDisabled = Color(0xFF9E9E9E)
    val BorderDefault = Color(0xFFE0E0E0)
    val BorderStrong = Color(0xFF9E9E9E)
    val BorderFocus = Color(0xFF0A2540)
    val StatusError = Color(0xFFD93B00)
    val Surface = Color(0xFFFFFFFF)
    val OnPrimary = Color(0xFFFFFFFF)

    // Screen background behind cards (DS primitive-neutral-50).
    val ScreenBackground = Color(0xFFF8F8F8)

    // Brand accent (DS primitive-orange-500) — registration FAB and the active bottom-nav item.
    val BrandAccent = Color(0xFFFF4500)
    val OnBrandAccent = Color(0xFFFFFFFF)

    // DS primitive-orange-400 — fill of the "Filtros" pill, outlined in [BrandAccent].
    val BrandAccentSoft = Color(0xFFFF6738)

    // DS primitive-blue-500 — inactive bottom-nav items.
    val NavInactive = Color(0xFF1C4466)

    // DS primitive-blue-400 — the row overflow ("⋮") glyph inside a list card.
    val IconMuted = Color(0xFF3B6080)

    // DS primitive-blue-50 — hairline between consecutive rows inside a list card.
    val RowDivider = Color(0xFFE8EDF2)

    // Segmented-control track behind the category tabs (DS neutral).
    val SegmentTrack = Color(0xFFEDEDF2)

    // Soft error surface — the expiry countdown pill in its final minutes.
    val ErrorContainer = Color(0xFFFFE3DC)
    val OnErrorContainer = Color(0xFFB33000)

    // Warning surface — the "Quedan pocos disponibles" band inside an order item. Material 3 has
    // no warning role, so the DS defines its own pair.
    val WarningContainer = Color(0xFFFDF3D0)
    val OnWarningContainer = Color(0xFF6B4E00)

    // Success surface — the green snackbar shown when an order is confirmed.
    val Success = Color(0xFF1E8E3E)
    val OnSuccess = Color(0xFFFFFFFF)

    // Soft success surface — rank badges and positive delta pills on the sales statistics screen.
    val SuccessContainer = Color(0xFFDDF7E5)
    val OnSuccessContainer = Color(0xFF1E8E3E)

    // Light blue track behind the period pills inside the "Filtros" panel (DS primitive-blue-50).
    val FilterTrack = Color(0xFFEBF3F9)

    // Inicio summary card (DS "week-summary-card"). The card is dark navy in both themes — it is a
    // branded surface, not a theme surface, so its tokens barely move between light and dark.
    val SummaryCardSurface = Color(0xFF10283C) // DS primitive-blue-700
    val SummaryBarFill = Color(0xFF1C4466) // DS primitive-blue-500
    val InsightStripSurface = Color(0xFF043762) // DS primitive-green-900 (a blue, despite the name)
    val OnSummaryCard = Color(0xFFFFFFFF)
    // "Esta semana" — the design draws it at 65% of the card's on-colour.
    val OnSummaryCardMuted = Color(0xA6FFFFFF)

    // Period selector inside the summary card: a light track carrying a brand-accent pill. The
    // track is light on both themes (it sits on the dark card), so its on-colour is fixed too.
    val PeriodTabTrack = Color(0xFFEBF3F9) // DS primitive-blue-50
    val OnPeriodTab = Color(0xFF050D14) // DS color-text-primary

    // "N / Búsquedas" block on a "producto no encontrado" card.
    val SearchCountContainer = Color(0xFFEBF3F9)
    val OnSearchCountContainer = Color(0xFF0979D6)

    // "Solo queda N" chip on a restock card — a stock alert, distinct from the amber
    // [WarningContainer] band used inside an order item.
    val StockAlertContainer = Color(0xFFFCCFD9)
    val OnStockAlertContainer = Color(0xFF78081F)

    // Header gradient (DS screen header): brand orange in the lower-right sweeping up into navy.
    val HeaderGradientOrange = Color(0xFFFF4500)
    val HeaderGradientNavy = Color(0xFF0D1F38)

    // Dark-mode surfaces (DS does not define a dark palette yet; derived for parity).
    val DarkSurface = Color(0xFF0E1721)
    val DarkTextPrimary = Color(0xFFE8EDF2)
    val DarkTextSecondary = Color(0xFFB0B8C1)
    val DarkBorderDefault = Color(0xFF2A3744)
    val DarkIconMuted = Color(0xFF7D93A8)

    /** Inactive tabs and "Ver más" on the dark surface: ~8:1, where [NavInactive] is ~1.6:1. */
    val DarkNavInactive = Color(0xFF8FB3D9)
    val DarkErrorContainer = Color(0xFF4A1A0B)
    val DarkOnErrorContainer = Color(0xFFFFB4A1)
    val DarkWarningContainer = Color(0xFF3A2E05)
    val DarkOnWarningContainer = Color(0xFFF2D98B)
    val DarkSuccess = Color(0xFF2FA653)
    val DarkSuccessContainer = Color(0xFF10351D)
    val DarkOnSuccessContainer = Color(0xFF8FE0A8)
    val DarkSegmentTrack = Color(0xFF1B2733)
    val DarkFilterTrack = Color(0xFF13283A)
    val DarkSummaryCardSurface = Color(0xFF0F2334)
    val DarkSummaryBarFill = Color(0xFF1A3D5C)
    val DarkInsightStripSurface = Color(0xFF06304F)
    val DarkSearchCountContainer = Color(0xFF13283A)
    val DarkOnSearchCountContainer = Color(0xFF6FB8F2)
    val DarkStockAlertContainer = Color(0xFF3E0A18)
    val DarkOnStockAlertContainer = Color(0xFFF5B9C6)
    val DarkHeaderGradientOrange = Color(0xFF7A2707)
    val DarkHeaderGradientNavy = Color(0xFF06121D)

    // Mi suscripción card (DS "subscription-card"): a branded gold surface in both themes, with the
    // yellow plan badge and the blue period-progress bar drawn on it.
    val SubscriptionCardSurface = Color(0xFF946B00)
    val DarkSubscriptionCardSurface = Color(0xFF6E5000)
    val OnSubscriptionCard = Color(0xFFFFFFFF)
    val PlanBadge = Color(0xFFFFC53D)
    val OnPlanBadge = Color(0xFF050D14)
    val PlanProgressTrack = Color(0xFFE8EDF2)
    val PlanProgressFill = Color(0xFF2F6BA8)

    // Checked settings switch (DS "toggle-on").
    val SwitchChecked = Color(0xFF34A853)
    val DarkSwitchChecked = Color(0xFF2FA653)
}
