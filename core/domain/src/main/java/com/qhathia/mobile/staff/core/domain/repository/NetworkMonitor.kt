package com.qhathia.mobile.staff.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Whether the phone has a usable network right now, and every change after that. */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
