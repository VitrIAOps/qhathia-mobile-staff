package com.qhathia.mobile.staff.core.ui.component.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

private val BarHeight = 8.dp

/**
 * The subscription period bar: how much of the paid period has elapsed, with the start and renewal
 * labels underneath. [fraction] is clamped to 0–1. Drawn on the gold subscription card.
 */
@Composable
fun QhathiaPlanProgressBar(
    fraction: Float,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .background(QhathiaColors.planProgressTrack, RoundedCornerShape(BarHeight))
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f) },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .background(QhathiaColors.planProgressFill, RoundedCornerShape(BarHeight)),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.labelSmall,
                color = QhathiaColors.onSubscriptionCard,
            )
            Text(
                text = endLabel,
                style = MaterialTheme.typography.labelSmall,
                color = QhathiaColors.onSubscriptionCard,
            )
        }
    }
}

@Preview(name = "Plan progress — light", showBackground = true)
@Preview(name = "Plan progress — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaPlanProgressBarPreview() {
    QhathiaTheme {
        Box(modifier = Modifier.background(QhathiaColors.subscriptionCard).padding(Dimens.SpaceMd)) {
            QhathiaPlanProgressBar(
                fraction = 0.55f,
                startLabel = "Inicio: 15 ago 2025",
                endLabel = "Renovación: 15 set 2026",
            )
        }
    }
}
