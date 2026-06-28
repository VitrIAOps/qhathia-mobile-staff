package com.qhathia.mobile.staff.feature.productinventory.model

import androidx.annotation.StringRes

/** One-shot side effects delivered via SharedFlow (never replayed). */
sealed interface InventoryUiEffect {
    data class ShowError(@param:StringRes val messageRes: Int) : InventoryUiEffect
}
