package com.qhathia.mobile.staff.feature.profile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaGradientTopBar
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSettingsSection
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.profile.R

/** Stateful Configuración. */
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AppSettingsContent(uiState = uiState, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}

/**
 * Stateless Configuración: the language pills in one card. Prices are not configured in the app —
 * they always include IGV — so there is no price or tax setting here.
 */
@Composable
fun AppSettingsContent(
    uiState: AppSettingsUiState,
    onEvent: (AppSettingsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        QhathiaGradientTopBar(title = stringResource(R.string.settings_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.SpaceMd),
        ) {
            QhathiaSettingsSection(title = null) {
                Column(
                    modifier = Modifier.padding(vertical = Dimens.SpaceSm),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    LanguagePills(
                        selected = uiState.language ?: AppLanguage.SPANISH,
                        onSelect = { onEvent(AppSettingsUiEvent.SelectLanguage(it)) },
                    )
                }
            }
        }
    }
}

/** Two outlined pills, the chosen one filled in the brand accent (DS "language-pill"). */
@Composable
private fun LanguagePills(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    Row(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        AppLanguage.entries.forEach { language ->
            val isSelected = language == selected
            val shape = RoundedCornerShape(Dimens.PillRadius)
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(shape)
                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                    .border(1.dp, MaterialTheme.colorScheme.secondary, shape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(language) })
                    .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(language.labelRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Each language is named in itself, so it stays recognisable whichever language is active. */
private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SPANISH -> R.string.settings_language_spanish
    AppLanguage.ENGLISH -> R.string.settings_language_english
}

@Preview(name = "Configuración — light", showBackground = true)
@Preview(name = "Configuración — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun AppSettingsContentPreview() {
    QhathiaTheme {
        AppSettingsContent(
            uiState = AppSettingsUiState(language = AppLanguage.SPANISH),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Configuración, English — light", showBackground = true)
@Composable
private fun AppSettingsEnglishPreview() {
    QhathiaTheme {
        AppSettingsContent(
            uiState = AppSettingsUiState(language = AppLanguage.ENGLISH),
            onEvent = {},
            onBack = {},
        )
    }
}
