package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.feature.orderrequests.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Gradient header of the Solicitudes screen: the screen title with the last-successful-load time
 * beneath it, a manual refresh action, and the search box.
 *
 * The "última actualización" line is the screen's freshness cue — it has no live updates, so the
 * merchant needs to see how stale the list is.
 */
@Composable
internal fun OrderRequestsHeader(
    lastUpdatedAt: Instant?,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = QhathiaColors.headerGradient,
                shape = RoundedCornerShape(
                    bottomStart = Dimens.SpaceLg,
                    bottomEnd = Dimens.SpaceLg,
                ),
            )
            .padding(horizontal = Dimens.SpaceMd)
            .padding(top = Dimens.SpaceSm, bottom = Dimens.SpaceLg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.order_requests_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = QhathiaColors.onHeaderGradient,
                )
                if (lastUpdatedAt != null) {
                    Text(
                        text = stringResource(
                            R.string.order_requests_last_updated,
                            lastUpdatedAt.toLocalTimeLabel(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = QhathiaColors.onHeaderGradient,
                    )
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.order_requests_refresh_cd),
                    tint = QhathiaColors.onHeaderGradient,
                )
            }
        }

        Spacer(Modifier.height(Dimens.SpaceMd))

        OrderSearchField(query = searchQuery, onQueryChange = onSearch)
    }
}

/**
 * Pill-shaped search field over the gradient (DS "Input Field"). Typing is debounced in the
 * ViewModel and resolved by the backend, so a match outside the loaded page is still found. A
 * trailing "x" clears the text once there is some.
 */
@Composable
private fun OrderSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .background(colors.surface, RoundedCornerShape(Dimens.SpaceSm))
            .padding(horizontal = Dimens.SpaceMd),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            textStyle = LocalTextStyle.current.merge(
                MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
            ),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.order_requests_search_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        // The glyph stays small to fit the DS field; `clickable` still gives it
                        // the 48dp minimum touch target.
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.order_requests_search_clear_cd),
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(role = Role.Button) { onQueryChange("") }
                                .padding(Dimens.SpaceXs)
                                .size(Dimens.IconSm),
                        )
                    }
                }
            },
        )
    }
}

/** The device-local wall-clock time of the load, formatted for the device locale. */
private fun Instant.toLocalTimeLabel(): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(this)
