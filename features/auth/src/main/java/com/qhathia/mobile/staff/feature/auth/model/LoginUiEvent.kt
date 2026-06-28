package com.qhathia.mobile.staff.feature.auth.model

/** User intents from the login screen. */
sealed interface LoginUiEvent {
    data class EmailChanged(val value: String) : LoginUiEvent
    data class PasswordChanged(val value: String) : LoginUiEvent
    data object Submit : LoginUiEvent
}
