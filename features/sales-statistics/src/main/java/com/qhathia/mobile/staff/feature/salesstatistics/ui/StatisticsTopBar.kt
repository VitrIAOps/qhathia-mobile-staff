package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.salesstatistics.R

private const val DISABLED_ALPHA = 0.38f

/**
 * Gradient top bar: back, the "Resumen de ventas" title and share. Share is disabled while
 * [shareEnabled] is false, that is, while the tab on screen has nothing loaded.
 */
@Composable
internal fun StatisticsTopBar(
    onBack: () -> Unit,
    onShare: () -> Unit,
    shareEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(QhathiaColors.headerGradient)
            .statusBarsPadding()
            .padding(horizontal = Dimens.SpaceXs, vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.statistics_back_cd),
                tint = QhathiaColors.onHeaderGradient,
            )
        }
        Text(
            text = stringResource(R.string.statistics_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = QhathiaColors.onHeaderGradient,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(onClick = onShare, enabled = shareEnabled) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.statistics_share_cd),
                tint = QhathiaColors.onHeaderGradient.copy(alpha = if (shareEnabled) 1f else DISABLED_ALPHA),
            )
        }
    }
}

@Preview(name = "Statistics top bar — light", showBackground = true)
@Preview(name = "Statistics top bar — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun StatisticsTopBarPreview() {
    QhathiaTheme {
        StatisticsTopBar(onBack = {}, onShare = {}, shareEnabled = true)
    }
}
