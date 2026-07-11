package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceStatus
import com.qhathia.mobile.staff.core.network.dto.DeviceDto

/** Maps a transport [DeviceDto] to the domain [Device]. */
fun DeviceDto.toDomain(): Device = Device(
    id = id,
    serialNumber = serialNumber,
    status = status.toDeviceStatus(),
    appVersion = appVersion,
    linkedAt = linkedAt,
)

/** Unknown or absent status strings degrade to [DeviceStatus.UNKNOWN]. */
private fun String?.toDeviceStatus(): DeviceStatus = when (this) {
    "PENDING_LINK" -> DeviceStatus.PENDING_LINK
    "ACTIVE" -> DeviceStatus.ACTIVE
    "INACTIVE" -> DeviceStatus.INACTIVE
    "SUSPENDED" -> DeviceStatus.SUSPENDED
    else -> DeviceStatus.UNKNOWN
}
