package com.qhathia.mobile.staff.feature.auth.model

import androidx.annotation.StringRes

/**
 * Immutable snapshot of the login screen. Field errors are string-resource ids so all user-facing
 * text stays externalized; the UI resolves them with `stringResource`.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    val isSubmitting: Boolean = false,
)
