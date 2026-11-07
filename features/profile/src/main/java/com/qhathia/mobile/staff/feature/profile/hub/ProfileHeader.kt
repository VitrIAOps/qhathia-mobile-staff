package com.qhathia.mobile.staff.feature.profile.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaInitialsAvatar
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.profile.R
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.initialsOf
import com.qhathia.mobile.staff.feature.profile.common.labelRes
import com.qhathia.mobile.staff.core.ui.R as CoreUiR

/** Gradient header of Mi perfil: title, the bell to Solicitudes and the identity card. */
@Composable
internal fun ProfileHeader(
    uiState: ProfileHubUiState,
    onEvent: (ProfileHubUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(QhathiaColors.headerGradient)
            .statusBarsPadding()
            .padding(horizontal = Dimens.SpaceMd)
            .padding(top = Dimens.SpaceMd, bottom = Dimens.SpaceLg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = QhathiaColors.onHeaderGradient,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            IconButton(onClick = { onEvent(ProfileHubUiEvent.Open(ProfileDestination.ORDER_REQUESTS)) }) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ic_bell),
                    contentDescription = stringResource(R.string.profile_bell_cd),
                    tint = QhathiaColors.onHeaderGradient,
                    modifier = Modifier.size(Dimens.SpaceLg),
                )
            }
        }
        Spacer(Modifier.size(Dimens.SpaceMd))
        IdentityCard(
            uiState = uiState,
            onEdit = { onEvent(ProfileHubUiEvent.Open(ProfileDestination.PERSONAL_DATA)) },
            onRetry = { onEvent(ProfileHubUiEvent.Retry) },
        )
    }
}

@Composable
private fun IdentityCard(
    uiState: ProfileHubUiState,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, QhathiaColors.onHeaderGradient.copy(alpha = 0.7f), RoundedCornerShape(Dimens.CardRadius))
            .padding(Dimens.SpaceMd),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            QhathiaInitialsAvatar(initials = initialsOf(uiState.displayName))
            Spacer(Modifier.width(Dimens.SpaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = QhathiaColors.onHeaderGradient,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = uiState.displayEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = QhathiaColors.onHeaderGradient,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val roleLabel = stringResource(uiState.role.labelRes())
                Text(
                    text = uiState.storeCount?.let {
                        pluralStringResource(R.plurals.profile_role_stores, it, roleLabel, it)
                    } ?: roleLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = QhathiaColors.onHeaderGradient,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.profile_edit_cd),
                    tint = QhathiaColors.onHeaderGradient,
                )
            }
        }
        if (uiState.profile is LoadState.Error) {
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.profile_load_error_retry),
                    color = QhathiaColors.onHeaderGradient,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(name = "Profile header — light", showBackground = true)
@Preview(name = "Profile header — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProfileHeaderPreview() {
    QhathiaTheme {
        ProfileHeader(uiState = ProfileHubPreviewData.owner, onEvent = {})
    }
}
