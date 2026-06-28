package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.feature.productinventory.R

/** String resource for the availability option shown in the sub-filter menu. */
@StringRes
internal fun Availability.filterLabelRes(): Int = when (this) {
    Availability.AVAILABLE -> R.string.inventory_availability_available
    Availability.LOW_STOCK -> R.string.inventory_availability_low_stock
    Availability.CONSULT -> R.string.inventory_availability_consult
}
