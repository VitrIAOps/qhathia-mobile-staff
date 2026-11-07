package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.CatalogCount
import com.qhathia.mobile.staff.core.domain.model.PairingSessionStatus
import com.qhathia.mobile.staff.core.domain.model.PairedDevice
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
     * Approves the pairing [code], naming the tablet [name] (the backend names it when null). The
     * tablet is not linked until it exchanges the code; follow it with [getPairingStatus]. Failures
     * carry a `PairDeviceException`.
     */
    suspend fun pairDevice(code: String, name: String?): Result<PairedDevice>

    /** Rejects the pairing [code]; the tablet shows that the store declined. Failures carry a `PairDeviceException`. */
    suspend fun rejectPairing(code: String): Result<Unit>

    /** Whether the tablet exchanged the code of the approved session [sessionId]. */
    suspend fun getPairingStatus(sessionId: String): Result<PairingSessionStatus>

    /**
     * Detaches [deviceId] from the store; it no longer appears in [getDevices]. A device that is
     * already gone fails with `DeviceSettingsException(NotFound)`.
     */
    suspend fun unlinkDevice(deviceId: String): Result<Unit>

    /** Renames [deviceId]. Failures carry a `DeviceSettingsException` (`NotFound`, `InvalidInput`). */
    suspend fun renameDevice(deviceId: String, name: String): Result<Device>

    /** How many active products the store's catalog has, and how many a tablet shows. */
    suspend fun getCatalogCount(): Result<CatalogCount>

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
