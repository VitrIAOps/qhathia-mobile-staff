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

    // DS primitive-blue-500 — inactive bottom-nav items.
    val NavInactive = Color(0xFF1C4466)

    // Segmented-control track behind the category tabs (DS neutral).
    val SegmentTrack = Color(0xFFEDEDF2)

    // Dark-mode surfaces (DS does not define a dark palette yet; derived for parity).
    val DarkSurface = Color(0xFF0E1721)
    val DarkTextPrimary = Color(0xFFE8EDF2)
    val DarkTextSecondary = Color(0xFFB0B8C1)
    val DarkBorderDefault = Color(0xFF2A3744)
}
