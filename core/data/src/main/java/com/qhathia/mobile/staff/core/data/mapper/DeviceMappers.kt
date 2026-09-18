package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceCommand
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandStatus
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import com.qhathia.mobile.staff.core.domain.model.DeviceStatus
import com.qhathia.mobile.staff.core.network.dto.DeviceCommandDto
import com.qhathia.mobile.staff.core.network.dto.DeviceDto

/**
 * Maps a transport [DeviceDto] to the domain [Device]. A battery reading outside 0–100 is dropped
 * rather than shown: telemetry is best effort.
 */
fun DeviceDto.toDomain(): Device = Device(
    id = id,
    serialNumber = serialNumber,
    status = status.toDeviceStatus(),
    appVersion = appVersion,
    linkedAt = linkedAt,
    name = name?.trim()?.takeIf { it.isNotEmpty() },
    batteryLevel = batteryLevel?.takeIf { it in 0..100 },
    networkStatus = networkStatus?.trim()?.takeIf { it.isNotEmpty() },
    storeName = storeName?.trim()?.takeIf { it.isNotEmpty() },
    lastPingAt = lastPingAt.parseIsoInstant(),
    lastSyncAt = lastSyncAt.parseIsoInstant(),
)

/** Unknown or absent status strings degrade to [DeviceStatus.UNKNOWN]. */
private fun String?.toDeviceStatus(): DeviceStatus = when (this) {
    "PENDING_LINK" -> DeviceStatus.PENDING_LINK
    "ACTIVE" -> DeviceStatus.ACTIVE
    "INACTIVE" -> DeviceStatus.INACTIVE
    "SUSPENDED" -> DeviceStatus.SUSPENDED
    else -> DeviceStatus.UNKNOWN
}

/**
 * Maps a command, or `null` when its type is one this app does not know — such a command has no
 * button to report its state on.
 */
fun DeviceCommandDto.toDomain(): DeviceCommand? {
    val type = DeviceCommandType.entries.firstOrNull { it.name == command } ?: return null
    return DeviceCommand(
        id = id,
        type = type,
        status = DeviceCommandStatus.entries.firstOrNull { it.name == status }
            ?: DeviceCommandStatus.UNKNOWN,
        requestedAt = requestedAt.parseIsoInstant(),
        resultDetail = resultDetail?.trim()?.takeIf { it.isNotEmpty() },
    )
}
