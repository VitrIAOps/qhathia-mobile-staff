package com.qhathia.mobile.staff.feature.salesstatistics.model

import androidx.compose.runtime.Immutable

/**
 * The load state of one tab. [data] belongs to the filters currently selected: it is cleared when
 * the filters change and kept, next to [isError], when a retry for the same filters fails.
 */
@Immutable
data class TabContent<out T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
)
