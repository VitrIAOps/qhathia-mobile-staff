package com.qhathia.mobile.staff.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.component.QhathiaBadge
import com.qhathia.mobile.staff.core.ui.component.time.rememberShortTimeFormatter
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.home.R
import com.qhathia.mobile.staff.core.ui.R as CoreUiR
import java.time.Instant

/** Pushes the badge just past the bell's top-end corner. */
private val BellBadgeOffset = 6.dp

/**
 * Gradient header of the Inicio tab: the greeting, the freshness line, and the notification bell.
 *
 * The freshness line only appears once a load has succeeded — before that there is nothing truthful
 * to say about how current the figures are. The bell and its badge are announced as one node, so a
 * screen reader says "Notificaciones, 2 pendientes" rather than reading a loose number.
 */
@Composable
internal fun HomeHeader(
    merchantFirstName: String?,
    lastUpdatedAt: Instant?,
    pendingOrderCount: Int,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bellDescription = if (pendingOrderCount > 0) {
        pluralStringResource(
            R.plurals.home_notifications_pending_cd,
            pendingOrderCount,
            pendingOrderCount,
        )
    } else {
        stringResource(R.string.home_notifications_cd)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = QhathiaColors.headerGradient,
                shape = RoundedCornerShape(
                    bottomStart = Dimens.SpaceLg,
                    bottomEnd = Dimens.SpaceLg,
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = Dimens.SpaceMd)
            .padding(top = Dimens.SpaceMd, bottom = Dimens.SpaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = merchantFirstName
                    ?.let { stringResource(R.string.home_greeting_named, it) }
                    ?: stringResource(R.string.home_greeting_anonymous),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = QhathiaColors.onHeaderGradient,
                maxLines = 1,
            )
            if (lastUpdatedAt != null) {
                val timeFormatter = rememberShortTimeFormatter()
                Text(
                    text = stringResource(
                        R.string.home_last_updated,
                        timeFormatter.format(lastUpdatedAt),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = QhathiaColors.onHeaderGradient,
                )
            }
        }

        IconButton(
            onClick = onOpenNotifications,
            // Stated in full: clearAndSetSemantics sits before the button's own click and role and
            // would otherwise wipe them, leaving a node a screen reader cannot activate.
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = bellDescription
                role = Role.Button
                onClick {
                    onOpenNotifications()
                    true
                }
            },
        ) {
            Box {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ic_bell),
                    contentDescription = null,
                    tint = QhathiaColors.onHeaderGradient,
                    modifier = Modifier.size(Dimens.SpaceLg),
                )
                if (pendingOrderCount > 0) {
                    QhathiaBadge(
                        count = pendingOrderCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = BellBadgeOffset, y = -BellBadgeOffset),
                    )
                }
            }
        }
    }
}

@Preview(name = "Home header — light", showBackground = true)
@Preview(name = "Home header — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun HomeHeaderPreview() {
    QhathiaTheme {
        HomeHeader(
            merchantFirstName = "Maria",
            lastUpdatedAt = HomePreviewData.generatedAt,
            pendingOrderCount = 2,
            onOpenNotifications = {},
        )
    }
}

@Preview(name = "Home header, no data yet — light", showBackground = true)
@Preview(name = "Home header, no data yet — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun HomeHeaderPristinePreview() {
    QhathiaTheme {
        HomeHeader(
            merchantFirstName = null,
            lastUpdatedAt = null,
            pendingOrderCount = 0,
            onOpenNotifications = {},
        )
    }
}
