package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.Device

/** Port for reading the store's paired devices and pairing a new one. */
interface DeviceRepository {

    /** Lists the tablets paired to the current store. */
    suspend fun getDevices(): Result<List<Device>>

    /**
     * Pairs (approves) a device by its pairing [code]. The device metadata (name, platform, app
     * version) is supplied by the data layer. Failures carry a `PairDeviceException`.
     */
    suspend fun pairDevice(code: String): Result<Unit>
}
