package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow

/** [NetworkMonitor] whose state tests set with [online]. */
class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    override val isOnline = MutableStateFlow(online)

    fun online(value: Boolean) {
        isOnline.value = value
    }
}
