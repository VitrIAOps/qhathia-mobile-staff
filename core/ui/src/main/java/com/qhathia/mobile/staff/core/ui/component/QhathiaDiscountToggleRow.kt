package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.format.toPenPrice
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

private val ToggleRowMinHeight = 48.dp

/**
 * The "Descuento" line of a product form: the label and its switch, and below them the applied
 * discount's [summary] ("Descuento de 20%") when there is one, with the price it leaves the
 * product at ([finalPrice]) on the opposite edge. The whole label row is a single `Switch`-role
 * node; the summary line is its own tappable node that reopens the discount editor.
 *
 * While [isLoading] a small spinner sits next to the switch and nothing can be tapped. [warning],
 * when set, is shown below the summary in the error color and announced politely by TalkBack — for
 * example a fixed discount that no longer fits the price.
 */
@Composable
fun QhathiaDiscountToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    summary: String? = null,
    finalPrice: Double? = null,
    onSummaryClick: () -> Unit = {},
    warning: String? = null,
) {
    val interactive = enabled && !isLoading
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ToggleRowMinHeight)
                .toggleable(
                    value = checked,
                    enabled = interactive,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.discount_toggle_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(Dimens.IconSm),
                )
                Spacer(Modifier.width(Dimens.SpaceSm))
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = interactive,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = QhathiaColors.switchChecked,
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedBorderColor = QhathiaColors.switchChecked,
                ),
            )
        }
        if (summary != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ToggleRowMinHeight)
                    .clickable(
                        enabled = interactive,
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.discount_edit_cd),
                        onClick = onSummaryClick,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                if (finalPrice != null) {
                    val finalPriceCd = stringResource(R.string.discount_resulting_price_cd, finalPrice.toPenPrice())
                    Spacer(Modifier.width(Dimens.SpaceSm))
                    Text(
                        text = finalPrice.toPenPrice(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { contentDescription = finalPriceCd },
                    )
                }
            }
        }
        if (warning != null) {
            Text(
                text = warning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.SpaceSm)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Preview(name = "Discount toggle — light", showBackground = true)
@Preview(name = "Discount toggle — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaDiscountToggleRowPreview() {
    QhathiaTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(Dimens.SpaceMd),
        ) {
            QhathiaDiscountToggleRow(checked = false, onCheckedChange = {})
            QhathiaDiscountToggleRow(checked = false, onCheckedChange = {}, enabled = false)
            QhathiaDiscountToggleRow(
                checked = true,
                onCheckedChange = {},
                summary = stringResource(R.string.discount_summary_percentage, "20"),
                finalPrice = 151.2,
            )
            QhathiaDiscountToggleRow(
                checked = true,
                onCheckedChange = {},
                isLoading = true,
                summary = stringResource(R.string.discount_summary_amount, "15"),
            )
            QhathiaDiscountToggleRow(
                checked = true,
                onCheckedChange = {},
                enabled = false,
                summary = stringResource(R.string.discount_summary_amount, "900"),
                warning = "El descuento es mayor que el precio. Edítalo o quítalo.",
            )
        }
    }
}
