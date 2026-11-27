package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens

/**
 * Buttons side by side in equal shares when every one fits its share on one line, or stacked at the
 * full width otherwise. Decided by measuring the buttons at the current text size, width and
 * language rather than by font scale alone, which cut labels on normal text (F-66, F-84).
 *
 * The children are laid out in the given order in both arrangements.
 */
@Composable
fun AdaptiveButtonRow(
    modifier: Modifier = Modifier,
    spacing: Dp = Dimens.SpaceSm,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        if (measurables.isEmpty()) return@Layout layout(0, 0) {}
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        val count = measurables.size
        val share = (width - gap * (count - 1)) / count
        val sideBySide = constraints.hasBoundedWidth &&
            measurables.all { it.maxIntrinsicWidth(Constraints.Infinity) <= share }
        if (sideBySide) {
            val placeables = measurables.map { it.measure(Constraints.fixedWidth(share).copy(minHeight = 0)) }
            val height = placeables.maxOf { it.height }
            layout(width, height) {
                var x = 0
                placeables.forEach { p ->
                    p.placeRelative(x, (height - p.height) / 2)
                    x += share + gap
                }
            }
        } else {
            val full = if (constraints.hasBoundedWidth) width else measurables.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
            val placeables = measurables.map { it.measure(Constraints.fixedWidth(full).copy(minHeight = 0)) }
            val height = placeables.sumOf { it.height } + gap * (count - 1)
            layout(full, height) {
                var y = 0
                placeables.forEach { p ->
                    p.placeRelative(0, y)
                    y += p.height + gap
                }
            }
        }
    }
}

