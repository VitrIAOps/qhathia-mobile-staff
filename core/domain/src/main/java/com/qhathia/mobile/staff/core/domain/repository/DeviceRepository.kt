package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceCommand
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import com.qhathia.mobile.staff.core.domain.model.DeviceSettings
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsChange

/**
 * Port for reading the store's paired devices, pairing a new one, sending remote commands and
 * managing each tablet's remote settings.
 */
interface DeviceRepository {

    /** Lists the tablets paired to the current store. */
    suspend fun getDevices(): Result<List<Device>>

    /**
     * Pairs (approves) a device by its pairing [code]. The device metadata (name, platform, app
     * version) is supplied by the data layer. Failures carry a `PairDeviceException`.
     */
    suspend fun pairDevice(code: String): Result<Unit>

    /** How many active products the store's catalog exposes. */
    suspend fun getCatalogCount(): Result<Int>

    /** Queues [type] for [deviceId]. Sending again while one is in flight returns that same command. */
    suspend fun sendCommand(deviceId: String, type: DeviceCommandType): Result<DeviceCommand>

    /** The device's most recent commands, newest first. */
    suspend fun getCommands(deviceId: String): Result<List<DeviceCommand>>

    /** The tablet's remote settings. Failures carry a `DeviceSettingsException`. */
    suspend fun getSettings(deviceId: String): Result<DeviceSettings>

    /**
     * Applies one [change] (a partial update) and returns the full settings as the server now has
     * them. Failures carry a `DeviceSettingsException`.
     */
    suspend fun updateSettings(deviceId: String, change: DeviceSettingsChange): Result<DeviceSettings>
}
