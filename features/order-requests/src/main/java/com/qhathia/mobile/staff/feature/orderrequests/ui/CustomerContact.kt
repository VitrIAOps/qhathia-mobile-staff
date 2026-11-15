package com.qhathia.mobile.staff.feature.orderrequests.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.orderrequests.R
import java.time.Instant

private val PERU_CELL = Regex("^9\\d{8}$")
private const val PERU_PREFIX = "51"
private const val PREFIXED_LENGTH = 11

/**
 * The customer's Peruvian cell-phone as 9 digits, or null when it is not one: spaces, dashes and a
 * leading +51 are tolerated, anything else gets no call or WhatsApp button.
 */
internal fun peruvianCellOrNull(phone: String?): String? {
    val digits = phone?.filter(Char::isDigit) ?: return null
    val local = if (digits.length == PREFIXED_LENGTH && digits.startsWith(PERU_PREFIX)) {
        digits.drop(PERU_PREFIX.length)
    } else {
        digits
    }
    return local.takeIf { PERU_CELL.matches(it) }
}

/**
 * Under the detail's title: who asked and how long ago, with "Llamar" and "WhatsApp" when the
 * customer left a valid cell-phone. WhatsApp goes through `wa.me`, which opens the app when it is
 * installed and the browser otherwise.
 */
@Composable
internal fun CustomerContactRow(
    customerName: String?,
    customerPhone: String?,
    createdAt: Instant?,
    minuteClock: Instant,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cell = peruvianCellOrNull(customerPhone)
    val name = customerName?.trim()?.takeIf { it.isNotEmpty() }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        val parts = listOfNotNull(name, createdAt?.let { arrivedAgoLabel(it, minuteClock) })
        if (parts.isNotEmpty()) {
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (cell != null) {
            val who = name ?: stringResource(R.string.order_contact_customer)
            val callCd = stringResource(R.string.order_contact_call_cd, who)
            val whatsappCd = stringResource(R.string.order_contact_whatsapp_cd, who)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                ContactButton(
                    text = stringResource(R.string.order_contact_call),
                    description = callCd,
                    icon = { Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = { context.openSafely(Intent(Intent.ACTION_DIAL, "tel:$cell".toUri())) },
                    modifier = Modifier.weight(1f),
                )
                ContactButton(
                    text = stringResource(R.string.order_contact_whatsapp),
                    description = whatsappCd,
                    icon = {
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        context.openSafely(Intent(Intent.ACTION_VIEW, "https://wa.me/$PERU_PREFIX$cell".toUri()))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ContactButton(
    text: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.ButtonRadius),
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = description },
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = Dimens.SpaceSm),
        )
    }
}

/** A phone without a dialer or a browser simply does nothing. */
private fun Context.openSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Nothing can handle it; the number is still on screen in the dialer-less case.
    }
}

@Preview(name = "Customer contact — light", showBackground = true)
@Preview(name = "Customer contact — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun CustomerContactRowPreview() {
    QhathiaTheme {
        CustomerContactRow(
            customerName = "Maria Murillo",
            customerPhone = "987654321",
            createdAt = PREVIEW_NOW.minusSeconds(5 * 60L),
            minuteClock = PREVIEW_NOW,
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
