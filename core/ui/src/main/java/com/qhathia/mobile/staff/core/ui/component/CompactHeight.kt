package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * Whether the window is short: a phone in landscape (or any window under 480 dp tall). Headers then
 * collapse to one row and the bottom bar drops its labels, so lists keep most of the height (F-84 a).
 */
@Composable
@ReadOnlyComposable
fun isCompactHeight(): Boolean {
    val heightPx = LocalWindowInfo.current.containerSize.height
    return with(LocalDensity.current) { heightPx.toDp() } < CompactHeightLimit
}

private val CompactHeightLimit = 480.dp
