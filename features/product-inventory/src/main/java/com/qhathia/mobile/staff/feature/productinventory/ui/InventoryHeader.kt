package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Notifications
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
import com.qhathia.mobile.staff.feature.productinventory.R

/**
 * Inventory header (DS screen header, same shape as Solicitudes): a full-bleed gradient band with
 * rounded bottom corners holding an inert back chevron, the bold screen title, the notification
 * bell and the search box. The icon actions are visual-only for this slice.
 *
 * The band starts below the status bar rather than behind it: the shell's `Scaffold` consumes the
 * system-bar insets for every destination, and the DS header in Solicitudes sits the same way.
 */
@Composable
internal fun InventoryHeader(
    searchQuery: String,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = QhathiaColors.headerGradient,
                shape = RoundedCornerShape(
                    bottomStart = Dimens.SpaceSm,
                    bottomEnd = Dimens.SpaceSm,
                ),
            )
            .padding(horizontal = Dimens.SpaceMd)
            .padding(top = Dimens.SpaceSm, bottom = Dimens.SpaceMd),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.inventory_back_cd),
                    tint = QhathiaColors.onHeaderGradient,
                )
            }
            Text(
                text = stringResource(R.string.inventory_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = QhathiaColors.onHeaderGradient,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Dimens.SpaceXs),
            )
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.inventory_notifications_cd),
                    tint = QhathiaColors.onHeaderGradient,
                )
            }
        }

        Spacer(Modifier.height(Dimens.SpaceMd))

        InventorySearchField(query = searchQuery, onQueryChange = onSearch)
    }
}

/**
 * DS "Input Field" over the gradient: white, hairline border, 8dp corners, no icon — just a
 * "Buscar" placeholder, and a trailing "x" that clears the text once there is some. Built directly rather than with the form text field so it matches the DS
 * box height exactly.
 */
@Composable
internal fun InventorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Dimens.FieldRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .background(colors.surface, shape)
            .border(1.dp, colors.outline, shape)
            .padding(start = Dimens.SpaceMd, end = 12.dp),
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
                                text = stringResource(R.string.inventory_search_placeholder),
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
                            contentDescription = stringResource(R.string.inventory_search_clear_cd),
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
