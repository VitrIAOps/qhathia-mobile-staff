package com.qhathia.mobile.staff.core.ui.component.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

/**
 * Round avatar showing the user's initials — there is no photo upload, so this is the only avatar.
 * Decorative: the name next to it carries the meaning, so it is hidden from screen readers by the
 * caller's merged semantics.
 */
@Composable
fun QhathiaInitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val textStyle: TextStyle = if (size >= 72.dp) {
        MaterialTheme.typography.headlineMedium
    } else {
        MaterialTheme.typography.titleMedium
    }
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.onBackground, CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.background,
            maxLines = 1,
        )
    }
}

@Preview(name = "Initials avatar — light", showBackground = true)
@Preview(name = "Initials avatar — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaInitialsAvatarPreview() {
    QhathiaTheme {
        Row(modifier = Modifier.padding(Dimens.SpaceMd), verticalAlignment = Alignment.CenterVertically) {
            QhathiaInitialsAvatar(initials = "CM")
            QhathiaInitialsAvatar(initials = "CM", size = 88.dp, modifier = Modifier.padding(start = Dimens.SpaceMd))
        }
    }
}
