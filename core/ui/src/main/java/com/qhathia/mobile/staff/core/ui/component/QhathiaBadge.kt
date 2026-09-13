package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

/** Diameter of the bubble holding a single digit; wider counts grow it horizontally. */
private val BadgeMinSize = 16.dp

/** Above this the exact number stops being actionable information, so it is capped. */
private const val BADGE_MAX_COUNT = 99

/**
 * The classic count bubble: a brand-accent circle with the count in white DS Caption digits.
 *
 * Counts above 99 render as `99+`. The badge carries no semantics of its own — callers merge it
 * into the control it decorates, so a screen reader announces "Notificaciones, 2 pendientes" as one
 * node instead of reading a bare number.
 */
@Composable
fun QhathiaBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = BadgeMinSize, minHeight = BadgeMinSize)
            .background(MaterialTheme.colorScheme.secondary, CircleShape)
            .padding(horizontal = Dimens.SpaceXs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = badgeLabel(count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondary,
            maxLines = 1,
        )
    }
}

/** `2` up to 99, `99+` beyond it. */
internal fun badgeLabel(count: Int): String =
    if (count > BADGE_MAX_COUNT) "$BADGE_MAX_COUNT+" else count.toString()

@Preview(name = "Badge — light", showBackground = true)
@Preview(name = "Badge — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaBadgePreview() {
    QhathiaTheme {
        Box(modifier = Modifier.padding(Dimens.SpaceMd)) {
            QhathiaBadge(count = 2)
        }
    }
}

@Preview(name = "Badge capped — light", showBackground = true)
@Preview(name = "Badge capped — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaBadgeCappedPreview() {
    QhathiaTheme {
        Box(modifier = Modifier.padding(Dimens.SpaceMd)) {
            QhathiaBadge(count = 128)
        }
    }
}
