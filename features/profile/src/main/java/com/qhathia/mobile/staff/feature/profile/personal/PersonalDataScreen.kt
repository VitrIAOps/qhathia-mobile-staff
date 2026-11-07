package com.qhathia.mobile.staff.feature.profile.personal

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.ui.component.QhathiaButton
import com.qhathia.mobile.staff.core.ui.component.QhathiaTextField
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
import com.qhathia.mobile.staff.feature.profile.common.initialsOf
import com.qhathia.mobile.staff.feature.profile.hub.ProfileHubPreviewData

/** Stateful Datos personales: binds the ViewModel and confirms a save with a snackbar. */
@Composable
fun PersonalDataScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PersonalDataViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                PersonalDataUiEffect.Saved ->
                    snackbarHostState.showSnackbar(resources.getString(R.string.personal_saved))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PersonalDataContent(uiState = uiState, onEvent = viewModel::onEvent, onBack = onBack)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

/**
 * Stateless Datos personales. Name and phone are editable; the email is shown disabled because it
 * is the login. "Cambiar foto" is shown but not available: the backend has no avatar upload.
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
            is LoadState.Loaded -> PersonalDataBody(profile = profile.value, uiState = uiState, onEvent = onEvent)
        }
    }
}

@Composable
private fun PersonalDataBody(
    profile: UserProfile,
    uiState: PersonalDataUiState,
    onEvent: (PersonalDataUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        QhathiaSettingsSection(title = null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.SpaceMd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                // The saved name, not the one being typed: the avatar changes once it is saved.
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
                modifier = Modifier.padding(vertical = Dimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            ) {
                LabelledField(
                    label = stringResource(R.string.personal_full_name),
                    value = uiState.fullName,
                    onValueChange = { onEvent(PersonalDataUiEvent.NameChanged(it)) },
                    enabled = !uiState.isSaving,
                    errorText = uiState.nameErrorRes?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
                LabelledField(
                    label = stringResource(R.string.personal_email),
                    value = profile.email,
                    onValueChange = {},
                    enabled = false,
                    helperText = stringResource(R.string.personal_email_note),
                )
                LabelledField(
                    label = stringResource(R.string.personal_phone),
                    value = uiState.phone,
                    onValueChange = { onEvent(PersonalDataUiEvent.PhoneChanged(it)) },
                    enabled = !uiState.isSaving,
                    errorText = uiState.phoneErrorRes?.let { stringResource(it) },
                    helperText = stringResource(R.string.personal_phone_hint),
                    placeholder = stringResource(R.string.personal_phone_placeholder),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (uiState.canSave) onEvent(PersonalDataUiEvent.Save) }),
                    visualTransformation = PeruPhonePrefix,
                )
            }
        }
        uiState.bannerErrorRes?.let { messageRes ->
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        QhathiaButton(
            text = stringResource(R.string.personal_save),
            onClick = { onEvent(PersonalDataUiEvent.Save) },
            enabled = uiState.canSave,
            loading = uiState.isSaving,
        )
    }
}

@Composable
private fun LabelledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    errorText: String? = null,
    helperText: String? = null,
    placeholder: String = label,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        QhathiaTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            enabled = enabled,
            isError = errorText != null,
            errorText = errorText,
            helperText = helperText,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
        )
    }
}

/**
 * Shows "+51 " before the typed digits without storing it: the phone is kept and sent as its 9
 * digits, and the prefix is the app's to draw. Nothing is drawn while the field is empty, so the
 * placeholder stays readable.
 */
private object PeruPhonePrefix : VisualTransformation {
    private const val PREFIX = "+51 "

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        return TransformedText(
            AnnotatedString(PREFIX) + text,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset + PREFIX.length
                override fun transformedToOriginal(offset: Int): Int =
                    (offset - PREFIX.length).coerceIn(0, text.length)
            },
        )
    }
}

@Preview(name = "Datos personales — light", showBackground = true, heightDp = 900)
@Preview(name = "Datos personales — dark", showBackground = true, uiMode = 0x20, heightDp = 900)
@Composable
private fun PersonalDataContentPreview() {
    val profile = ProfileHubPreviewData.profile
    QhathiaTheme {
        PersonalDataContent(
            uiState = PersonalDataUiState(
                profile = LoadState.Loaded(profile),
                fullName = profile.fullName,
                phone = profile.phone.orEmpty(),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Datos personales, invalid phone — light", showBackground = true, heightDp = 900)
@Composable
private fun PersonalDataInvalidPreview() {
    val profile = ProfileHubPreviewData.profile
    QhathiaTheme {
        PersonalDataContent(
            uiState = PersonalDataUiState(
                profile = LoadState.Loaded(profile),
                fullName = "Carla Mendoza Ruiz",
                phone = "88765432",
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Datos personales, saving — light", showBackground = true, heightDp = 900)
@Composable
private fun PersonalDataSavingPreview() {
    val profile = ProfileHubPreviewData.profile
    QhathiaTheme {
        PersonalDataContent(
            uiState = PersonalDataUiState(
                profile = LoadState.Loaded(profile),
                fullName = "Carla Mendoza Ruiz",
                phone = profile.phone.orEmpty(),
                isSaving = true,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Datos personales, save failed — light", showBackground = true, heightDp = 900)
@Composable
private fun PersonalDataSaveFailedPreview() {
    val profile = ProfileHubPreviewData.profile
    QhathiaTheme {
        PersonalDataContent(
            uiState = PersonalDataUiState(
                profile = LoadState.Loaded(profile),
                fullName = "Carla Mendoza Ruiz",
                phone = profile.phone.orEmpty(),
                bannerErrorRes = R.string.personal_error_save_network,
            ),
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
