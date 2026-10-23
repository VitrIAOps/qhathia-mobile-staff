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
 *
 * Only the weights registered in [UrbanistFamily] render as such: asking for any other weight falls
 * back to the closest registered one. Light (300) is there for secondary text drawn lighter than
 * the body, such as the struck-through original price of a discounted product.
 */
@OptIn(ExperimentalTextApi::class)
private fun urbanist(weight: FontWeight) = Font(
    resId = R.font.urbanist_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val UrbanistFamily = FontFamily(
    urbanist(FontWeight.Light),
    urbanist(FontWeight.Normal),
    urbanist(FontWeight.Medium),
    urbanist(FontWeight.SemiBold),
    urbanist(FontWeight.Bold),
)

/**
 * Typography matching the Qhathia DS scale. `bodyMedium` = Urbanist Body/MD (14/20),
 * `bodySmall` = Urbanist Body/SM (12/18). `displaySmall` is used for the large brand wordmark,
 * `headlineMedium` for the DS H1 amount headline and `labelSmall` for the DS Caption scale.
 */
val QhathiaTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = UrbanistFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
    ),
    // DS Urbanist/H1 (32/40 Bold, -0.5) — the Inicio summary headline amount.
    headlineMedium = TextStyle(
        fontFamily = UrbanistFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
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
    // DS Urbanist/Caption (10/14 Medium, 0.4) — count badges and the stock-alert chip.
    labelSmall = TextStyle(
        fontFamily = UrbanistFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = UrbanistFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
