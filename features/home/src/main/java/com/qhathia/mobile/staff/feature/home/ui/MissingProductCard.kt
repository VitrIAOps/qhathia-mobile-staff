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
 * A product kiosk shoppers searched for and did not find: how many distinct people asked for it,
 * the way they most often named it, and an `Agregar` action.
 *
 * [onAdd] is deliberately a no-op today — there is no flow that creates a product from a search
 * yet, so the button renders as designed and goes nowhere.
 */
@Composable
internal fun MissingProductCard(
    suggestion: MissingProductSuggestion,
    onAdd: () -> Unit,
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
            PersonCountBlock(personCount = suggestion.personCount)
            Text(
                text = suggestion.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        SuggestionActionButton(text = stringResource(R.string.home_add_product), onClick = onAdd)
    }
}

/**
 * The "9 / Personas" block — the whole reason the suggestion is worth acting on. It counts people,
 * not searches: one shopper retrying nine times is one person of demand.
 */
@Composable
private fun PersonCountBlock(
    personCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(QhathiaColors.searchCountContainer, RoundedCornerShape(Dimens.SpaceSm))
            .padding(Dimens.SpaceSm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = personCount.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = QhathiaColors.onSearchCountContainer,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = pluralStringResource(R.plurals.home_people_label, personCount),
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
