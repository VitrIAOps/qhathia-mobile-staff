package com.qhathia.mobile.staff.feature.profile.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaComingSoonTag
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaGradientTopBar
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaInitialsAvatar
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSettingsSection
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.profile.R
import com.qhathia.mobile.staff.feature.profile.common.ErrorPane
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.LoadingPane
import com.qhathia.mobile.staff.feature.profile.common.formatPeruPhone
import com.qhathia.mobile.staff.feature.profile.common.initialsOf
import com.qhathia.mobile.staff.feature.profile.hub.ProfileHubPreviewData

/** Stateful Datos personales. */
@Composable
fun PersonalDataScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalDataViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PersonalDataContent(uiState = uiState, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}

/**
 * Stateless Datos personales. Every field is read-only by product decision, and "Cambiar foto" is
 * shown but not available: the backend has no avatar upload.
 */
@Composable
fun PersonalDataContent(
    uiState: PersonalDataUiState,
    onEvent: (PersonalDataUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        QhathiaGradientTopBar(title = stringResource(R.string.personal_title), onBack = onBack)
        when (val profile = uiState.profile) {
            LoadState.Loading -> LoadingPane()
            LoadState.Error -> ErrorPane(
                message = stringResource(R.string.personal_error),
                onRetry = { onEvent(PersonalDataUiEvent.Retry) },
            )
            is LoadState.Loaded -> PersonalDataBody(profile = profile.value)
        }
    }
}

@Composable
private fun PersonalDataBody(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        QhathiaSettingsSection(title = null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.SpaceMd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                QhathiaInitialsAvatar(initials = initialsOf(profile.fullName), size = 88.dp)
                val changePhotoCd = stringResource(R.string.personal_change_photo_cd)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = changePhotoCd
                        disabled()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.personal_change_photo),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                    )
                    QhathiaComingSoonTag()
                }
            }
        }
        QhathiaSettingsSection(title = null) {
            Column(
                modifier = Modifier.padding(vertical = Dimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            ) {
                ReadOnlyField(label = stringResource(R.string.personal_full_name), value = profile.fullName)
                ReadOnlyField(label = stringResource(R.string.personal_email), value = profile.email)
                ReadOnlyField(
                    label = stringResource(R.string.personal_phone),
                    value = profile.phone?.let(::formatPeruPhone) ?: stringResource(R.string.profile_missing_value),
                )
            }
        }
    }
}

/** A labelled value drawn like the DS input but never editable; announced as "label, value". */
@Composable
private fun ReadOnlyField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$label, $value" },
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.FieldHeight)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Dimens.FieldRadius))
                .padding(horizontal = Dimens.SpaceMd),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Datos personales — light", showBackground = true)
@Preview(name = "Datos personales — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun PersonalDataContentPreview() {
    QhathiaTheme {
        PersonalDataContent(
            uiState = PersonalDataUiState(LoadState.Loaded(ProfileHubPreviewData.profile)),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Datos personales, loading — light", showBackground = true)
@Composable
private fun PersonalDataLoadingPreview() {
    QhathiaTheme {
        PersonalDataContent(uiState = PersonalDataUiState(), onEvent = {}, onBack = {})
    }
}

@Preview(name = "Datos personales, error — light", showBackground = true)
@Composable
private fun PersonalDataErrorPreview() {
    QhathiaTheme {
        PersonalDataContent(uiState = PersonalDataUiState(LoadState.Error), onEvent = {}, onBack = {})
    }
}
