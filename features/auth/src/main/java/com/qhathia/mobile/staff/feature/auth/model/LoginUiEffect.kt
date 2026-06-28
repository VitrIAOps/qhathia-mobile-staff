package com.qhathia.mobile.staff.feature.auth.model

import androidx.annotation.StringRes

/** One-shot side effects delivered via SharedFlow (never replayed). */
sealed interface LoginUiEffect {
    data object NavigateToHome : LoginUiEffect
    data class ShowError(@param:StringRes val messageRes: Int) : LoginUiEffect
}
