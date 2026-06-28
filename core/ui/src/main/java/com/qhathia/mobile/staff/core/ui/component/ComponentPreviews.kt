package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

@Preview(name = "Components — light", showBackground = true)
@Preview(name = "Components — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaComponentsPreview() {
    QhathiaTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            ) {
                QhathiaTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = "Correo electrónico",
                    leadingIcon = Icons.Filled.Email,
                    helperText = "Empty state",
                )
                QhathiaTextField(
                    value = "owner@tienda.com",
                    onValueChange = {},
                    placeholder = "Correo electrónico",
                    leadingIcon = Icons.Filled.Email,
                )
                QhathiaTextField(
                    value = "secret",
                    onValueChange = {},
                    placeholder = "Contraseña",
                    leadingIcon = Icons.Filled.Lock,
                    isPassword = true,
                    isError = true,
                    errorText = "Credenciales incorrectas",
                )
                QhathiaButton(text = "Iniciar sesión", onClick = {})
                QhathiaButton(text = "Iniciar sesión", onClick = {}, loading = true)
            }
        }
    }
}
