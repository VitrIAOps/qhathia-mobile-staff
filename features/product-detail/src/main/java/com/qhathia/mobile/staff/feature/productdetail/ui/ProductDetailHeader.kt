package com.qhathia.mobile.staff.feature.productdetail.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.productdetail.R

/** Detail header: a back chevron, the bold screen title and an inert notification bell. */
@Composable
internal fun ProductDetailHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.detail_back_cd),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.detail_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Dimens.SpaceXs),
        )
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = stringResource(R.string.detail_notifications_cd),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
