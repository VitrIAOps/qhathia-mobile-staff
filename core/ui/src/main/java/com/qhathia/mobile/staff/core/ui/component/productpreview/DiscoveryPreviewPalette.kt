package com.qhathia.mobile.staff.core.ui.component.productpreview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTypography

/**
 * Colors of the Qhathia Discovery screens the previews depict (Figma "Qhatia DS", nodes `644:26084`,
 * `644:26105` and `644:26093`).
 *
 * They are fixed on purpose and do not follow the staff app's light/dark theme: a preview shows how
 * the product looks in another app, so it must look the same whatever theme the merchant uses.
 */
internal object DiscoveryPreviewPalette {
    val Background = Color(0xFFF8F8F8) // primitive-neutral-50
    val Surface = Color(0xFFFFFFFF) // primitive-neutral-0
    val TextBody = Color(0xFF222222)
    val TextHeading = Color(0xFF0A2540)
    val TextSecondary = Color(0xFF616161)
    val TextStrong = Color(0xFF212121) // primitive-neutral-800
    val TextPlaceholder = Color(0xFFA8A8A8)
    val Divider = Color(0xFFE0E0E0)
    val InputBorder = Color(0xFFDBDBDB)
    val Navy = Color(0xFF1C4466) // primitive-blue-500
    val NavyDark = Color(0xFF0B1B28) // primitive-blue-800
    val ToolbarBorder = Color(0xFF73AAD8) // primitive-blue-200
    val Accent = Color(0xFFFF4500) // color-brand-accent
    val Success = Color(0xFF22C55E) // primitive-green-500
    val Danger = Color(0xFFFF3B30) // Colors/Red
    val OnFilled = Color(0xFFFFFFFF)

    /** Photo slot before its image arrives, and the blocks standing in for obscured content. */
    val Skeleton = Color(0xFFE6E6E6)
}

/** Discovery's type scale, built on the same Urbanist family the staff app ships. */
internal object DiscoveryPreviewType {
    /** Urbanist/Body/MD — 14/20 Regular. */
    val Body: TextStyle = QhathiaTypography.bodyMedium

    /** Urbanist/H3 — 20/28 SemiBold. */
    val H3: TextStyle = QhathiaTypography.titleLarge.copy(fontSize = 20.sp, lineHeight = 28.sp)

    /** Urbanist/H1 — 32/40 Bold, -0.5. */
    val H1: TextStyle = QhathiaTypography.headlineMedium

    /** Section headings on the detail screen — 18 SemiBold. */
    val SectionHeading: TextStyle = QhathiaTypography.titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp)

    /** Urbanist/Button/XL — 16/24 SemiBold, 0.3. */
    val Button: TextStyle = QhathiaTypography.bodyLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.3.sp,
    )

    /** Category tag on a horizontal item — 10/16 Regular, 0.8. */
    val CategoryTag: TextStyle = QhathiaTypography.labelSmall.copy(
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
    )
}
