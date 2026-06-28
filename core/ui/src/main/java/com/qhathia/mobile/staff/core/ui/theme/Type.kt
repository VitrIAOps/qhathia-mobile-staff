package com.qhathia.mobile.staff.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// TODO(design): swap FontFamily.Default for the Urbanist family once the font is bundled
//  (res/font or downloadable Google Fonts). Sizes/line-heights below already match the DS.
private val UrbanistFamily = FontFamily.Default

/**
 * Typography matching the Qhathia DS scale. `bodyMedium` = Urbanist Body/MD (14/20),
 * `bodySmall` = Urbanist Body/SM (12/18). `displaySmall` is used for the large brand wordmark.
 */
val QhathiaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = UrbanistFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = UrbanistFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = UrbanistFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = UrbanistFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = UrbanistFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = UrbanistFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
