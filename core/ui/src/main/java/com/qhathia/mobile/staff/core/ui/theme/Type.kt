package com.qhathia.mobile.staff.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.staff.core.ui.R

/**
 * Urbanist ships as a single variable font (one `wght` axis), so every weight the DS uses is the
 * same file pinned to a different axis value — see `res/raw/license_urbanist_ofl.txt` for its SIL
 * Open Font License. Variable axes need API 26, which is the project's `minSdk`.
 */
@OptIn(ExperimentalTextApi::class)
private fun urbanist(weight: FontWeight) = Font(
    resId = R.font.urbanist_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val UrbanistFamily = FontFamily(
    urbanist(FontWeight.Normal),
    urbanist(FontWeight.Medium),
    urbanist(FontWeight.SemiBold),
    urbanist(FontWeight.Bold),
)

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
