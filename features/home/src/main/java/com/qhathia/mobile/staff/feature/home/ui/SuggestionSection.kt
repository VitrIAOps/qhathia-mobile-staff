package com.qhathia.mobile.staff.feature.home.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.MissingProductSuggestion
import com.qhathia.mobile.staff.core.domain.model.RestockSuggestion
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.home.R
import com.qhathia.mobile.staff.core.ui.R as CoreUiR

private val ActionButtonHeight = 32.dp
private val CardSpacing = 12.dp

/**
 * The "Productos no encontrados" row. Rendered only when it has cards — an empty row would be a
 * heading over nothing, which reads as a bug rather than as an absence.
 */
@Composable
internal fun MissingProductsSection(
    suggestions: List<MissingProductSuggestion>,
    onAdd: (MissingProductSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return
    SuggestionSection(
        titleRes = R.string.home_missing_products_title,
        iconRes = CoreUiR.drawable.ic_search,
        modifier = modifier,
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(CardSpacing),
            contentPadding = PaddingValues(horizontal = Dimens.SpaceMd),
        ) {
            items(suggestions, key = { it.id }) { suggestion ->
                MissingProductCard(suggestion = suggestion, onAdd = { onAdd(suggestion) })
            }
        }
    }
}

/** The "Productos para reponer" row, on the same omit-when-empty rule. */
@Composable
internal fun RestockProductsSection(
    suggestions: List<RestockSuggestion>,
    onRestock: (RestockSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) return
    SuggestionSection(
        titleRes = R.string.home_restock_products_title,
        iconRes = CoreUiR.drawable.ic_alert,
        modifier = modifier,
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(CardSpacing),
            contentPadding = PaddingValues(horizontal = Dimens.SpaceMd),
        ) {
            items(suggestions, key = { it.id }) { suggestion ->
                RestockProductCard(suggestion = suggestion, onRestock = { onRestock(suggestion) })
            }
        }
    }
}

/**
 * A titled row of suggestion cards. The heading is padded to the screen gutter while [row] runs
 * edge to edge, so a card can scroll past the gutter instead of stopping short of it.
 */
@Composable
private fun SuggestionSection(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    row: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMd),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                // The heading right beside it names the row.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(Dimens.IconSm),
            )
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        row()
    }
}

/**
 * The brand-accent action at the foot of a suggestion card. It is inert in this change — the mocked
 * suggestions carry no product id anything downstream could resolve — but it renders and reacts to
 * touch exactly as designed.
 */
@Composable
internal fun SuggestionActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ActionButtonHeight)
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(Dimens.SpaceSm))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondary,
            maxLines = 1,
        )
    }
}

@Preview(name = "Suggestion sections — light", showBackground = true)
@Preview(name = "Suggestion sections — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun SuggestionSectionsPreview() {
    QhathiaTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = Dimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
        ) {
            MissingProductsSection(
                suggestions = HomePreviewData.missingProducts,
                onAdd = {},
            )
            RestockProductsSection(
                suggestions = HomePreviewData.restockProducts,
                onRestock = {},
            )
        }
    }
}
