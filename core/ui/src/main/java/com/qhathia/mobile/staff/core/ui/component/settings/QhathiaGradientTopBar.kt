package com.qhathia.mobile.staff.core.ui.component.settings

import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.component.isCompactHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

/**
 * DS screen header for pushed screens: the brand gradient behind the status bar, a back chevron and
 * a bold title announced as a heading. Pass a null [onBack] for a root screen without back.
 */
@Composable
fun QhathiaGradientTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    compact: Boolean = isCompactHeight(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(QhathiaColors.headerGradient)
            .statusBarsPadding()
            .padding(horizontal = Dimens.SpaceXs, vertical = if (compact) 0.dp else Dimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.navigate_back_cd),
                    tint = QhathiaColors.onHeaderGradient,
                )
            }
        } else {
            Spacer(Modifier.width(Dimens.SpaceMd))
        }
        Text(
            text = title,
            // A short window keeps the bar to one slim row, as Inicio and Inventario do (S17-07).
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = QhathiaColors.onHeaderGradient,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        actions()
    }
}

@Preview(name = "Gradient top bar — light", showBackground = true)
@Preview(name = "Gradient top bar — dark", showBackground = true, uiMode = 0x20)
@Preview(name = "Gradient top bar — landscape", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun QhathiaGradientTopBarPreview() {
    QhathiaTheme {
        QhathiaGradientTopBar(title = "Datos personales", onBack = {})
    }
}
