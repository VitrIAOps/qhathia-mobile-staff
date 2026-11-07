package com.qhathia.mobile.staff.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.component.QhathiaButton
import com.qhathia.mobile.staff.core.ui.component.QhathiaTextField
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.auth.R
import com.qhathia.mobile.staff.feature.auth.model.LoginUiEvent
import com.qhathia.mobile.staff.feature.auth.model.LoginUiState

/**
 * Stateless login UI — a pure function of [uiState]. Phone-first single column: large brand
 * wordmark, email + password fields, and the submit button which becomes a spinner while
 * submitting (inputs are disabled during submission).
 */
@Composable
fun LoginContent(
    uiState: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val inputsEnabled = !uiState.isSubmitting

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Dimens.SpaceLg, vertical = Dimens.SpaceXl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.login_brand),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceXl))

            QhathiaTextField(
                value = uiState.email,
                onValueChange = { onEvent(LoginUiEvent.EmailChanged(it)) },
                placeholder = stringResource(R.string.login_email_placeholder),
                leadingIcon = Icons.Filled.Email,
                leadingIconDescription = stringResource(R.string.login_email_icon_cd),
                enabled = inputsEnabled,
                isError = uiState.emailError != null,
                errorText = uiState.emailError?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceMd))

            QhathiaTextField(
                value = uiState.password,
                onValueChange = { onEvent(LoginUiEvent.PasswordChanged(it)) },
                placeholder = stringResource(R.string.login_password_placeholder),
                leadingIcon = Icons.Filled.Lock,
                leadingIconDescription = stringResource(R.string.login_password_icon_cd),
                enabled = inputsEnabled,
                isPassword = true,
                isError = uiState.passwordError != null,
                errorText = uiState.passwordError?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                // Done signs in, as the button does; the view model ignores repeats while submitting.
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboard?.hide()
                        onEvent(LoginUiEvent.Submit)
                    },
                ),
            )

            Spacer(modifier = Modifier.height(Dimens.SpaceXl))

            QhathiaButton(
                text = stringResource(R.string.login_submit),
                onClick = {
                    keyboard?.hide()
                    onEvent(LoginUiEvent.Submit)
                },
                loading = uiState.isSubmitting,
            )
        }
    }
}

@Preview(name = "Login — light", showBackground = true)
@Preview(name = "Login — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun LoginContentPreview() {
    QhathiaTheme {
        LoginContent(
            uiState = LoginUiState(email = "owner@tienda.com", password = "secret"),
            onEvent = {},
        )
    }
}

@Preview(name = "Login — error", showBackground = true)
@Composable
private fun LoginContentErrorPreview() {
    QhathiaTheme {
        LoginContent(
            uiState = LoginUiState(
                email = "owner",
                password = "",
                emailError = R.string.login_error_invalid_email,
                passwordError = R.string.login_error_empty_password,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Login — submitting", showBackground = true)
@Composable
private fun LoginContentSubmittingPreview() {
    QhathiaTheme {
        LoginContent(
            uiState = LoginUiState(
                email = "owner@tienda.com",
                password = "secret",
                isSubmitting = true,
            ),
            onEvent = {},
        )
    }
}
