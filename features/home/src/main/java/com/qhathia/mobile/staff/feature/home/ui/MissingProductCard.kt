package com.qhathia.mobile.staff.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.MissingProductSuggestion
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.home.R

/** Card width from the design; the row scrolls, so it does not stretch to the viewport. */
internal val MissingCardWidth = 209.dp

/**
 * A product kiosk shoppers searched for and did not find: how many distinct people asked for it
 * ("~N búsquedas" when the backend could only count searches), its canonical term, how they most
 * often asked for it (when that says more than the term), the other terms they used, and an
 * `Agregar` action.
 *
 * [onAdd] opens Registro with the term as the product name. A null [onAdd] draws no button, for
 * "También te piden", where there is nothing for the store to add.
 */
@Composable
internal fun MissingProductCard(
    suggestion: MissingProductSuggestion,
    onAdd: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(MissingCardWidth)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.SpaceSm))
            .padding(Dimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersonCountBlock(personCount = suggestion.personCount, estimated = suggestion.personCountEstimated)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = suggestion.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                suggestion.example?.let { example ->
                    Text(
                        text = stringResource(R.string.home_missing_example, example),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (suggestion.variants.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.home_missing_variants, suggestion.variants.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (onAdd != null) {
            SuggestionActionButton(text = stringResource(R.string.home_add_product), onClick = onAdd)
        }
    }
}

/**
 * The "9 / Personas" block — the whole reason the suggestion is worth acting on. It counts people,
 * not searches: one shopper retrying nine times is one person of demand. When [estimated], the
 * backend had no sessions to tell people apart, so it reads "~9 / Búsquedas" and says so aloud.
 */
@Composable
private fun PersonCountBlock(
    personCount: Int,
    estimated: Boolean,
    modifier: Modifier = Modifier,
) {
    val description = if (estimated) {
        pluralStringResource(R.plurals.home_searches_estimated_cd, personCount, personCount)
    } else {
        "$personCount ${pluralStringResource(R.plurals.home_people_label, personCount)}"
    }
    Column(
        modifier = modifier
            .background(QhathiaColors.searchCountContainer, RoundedCornerShape(Dimens.SpaceSm))
            .clearAndSetSemantics { contentDescription = description }
            .padding(Dimens.SpaceSm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = if (estimated) "~$personCount" else personCount.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = QhathiaColors.onSearchCountContainer,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = pluralStringResource(
                if (estimated) R.plurals.home_searches_label else R.plurals.home_people_label,
                personCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = QhathiaColors.onSearchCountContainer,
            maxLines = 1,
        )
    }
}

@Preview(name = "Missing product card — light", showBackground = true)
@Preview(name = "Missing product card — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun MissingProductCardPreview() {
    QhathiaTheme {
        MissingProductCard(
            suggestion = HomePreviewData.missingProducts.first(),
            onAdd = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Other request card — light", showBackground = true)
@Preview(name = "Other request card — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun OtherRequestCardPreview() {
    QhathiaTheme {
        MissingProductCard(
            suggestion = HomePreviewData.otherRequests.first(),
            onAdd = null,
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
