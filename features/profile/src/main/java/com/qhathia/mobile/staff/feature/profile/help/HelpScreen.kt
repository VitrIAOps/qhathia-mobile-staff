package com.qhathia.mobile.staff.feature.profile.help

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaComingSoonTag
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaGradientTopBar
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaNavigationRow
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSettingsSection
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.profile.R

private data class Faq(@param:StringRes val question: Int, @param:StringRes val answer: Int)

private val Faqs = listOf(
    Faq(R.string.help_faq_link_tablet_q, R.string.help_faq_link_tablet_a),
    Faq(R.string.help_faq_ai_suggestions_q, R.string.help_faq_ai_suggestions_a),
    Faq(R.string.help_faq_offline_q, R.string.help_faq_offline_a),
    Faq(R.string.help_faq_prices_q, R.string.help_faq_prices_a),
)

private val Guides = listOf(
    R.string.help_guide_first_steps,
    R.string.help_guide_combos,
    R.string.help_guide_sales_report,
)

/**
 * Ayuda. Its content is local and static — the backend serves no FAQ, guides or terms — so it has no
 * ViewModel: the only state is which questions are expanded.
 */
@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val versionName = remember(context) { if (isPreview) "2.4.1" else context.appVersionName() }
    HelpContent(versionName = versionName, onBack = onBack, modifier = modifier)
}

@Composable
fun HelpContent(
    versionName: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        QhathiaGradientTopBar(title = stringResource(R.string.help_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
        ) {
            QhathiaSettingsSection(title = stringResource(R.string.help_faq_title)) {
                Faqs.forEachIndexed { index, faq ->
                    FaqItem(faq = faq, showDivider = index < Faqs.lastIndex)
                }
            }
            QhathiaSettingsSection(title = stringResource(R.string.help_guides_title)) {
                Guides.forEachIndexed { index, guide ->
                    QhathiaNavigationRow(
                        title = stringResource(guide),
                        onClick = null,
                        comingSoon = true,
                        showDivider = index < Guides.lastIndex,
                    )
                }
            }
            VersionFooter(versionName = versionName)
        }
    }
}

@Composable
private fun FaqItem(faq: Faq, showDivider: Boolean) {
    var expanded by rememberSaveable(faq.question) { mutableStateOf(false) }
    val stateLabel = stringResource(if (expanded) R.string.help_faq_expanded else R.string.help_faq_collapsed)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = expanded, role = Role.Button, onValueChange = { expanded = it })
                .semantics { stateDescription = stateLabel }
                .padding(vertical = Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(faq.question),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Spacer(Modifier.width(Dimens.SpaceSm))
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = stringResource(faq.answer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Dimens.SpaceMd),
            )
        }
        if (showDivider) HorizontalDivider(color = QhathiaColors.rowDivider)
    }
}

@Composable
private fun VersionFooter(versionName: String?) {
    val termsCd = stringResource(R.string.help_terms_cd)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        if (versionName != null) {
            Text(
                text = stringResource(R.string.help_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = termsCd
                disabled()
            },
        ) {
            Text(
                text = stringResource(R.string.help_terms),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            QhathiaComingSoonTag()
        }
    }
}

private fun Context.appVersionName(): String? = runCatching {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    info.versionName
}.getOrNull()

@Preview(name = "Ayuda — light", showBackground = true, heightDp = 850)
@Preview(name = "Ayuda — dark", showBackground = true, heightDp = 850, uiMode = 0x20)
@Composable
private fun HelpContentPreview() {
    QhathiaTheme {
        HelpContent(versionName = "2.4.1", onBack = {})
    }
}
