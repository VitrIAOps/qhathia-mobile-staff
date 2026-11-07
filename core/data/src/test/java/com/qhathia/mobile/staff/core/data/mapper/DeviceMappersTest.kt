package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.DeviceCommandStatus
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import com.qhathia.mobile.staff.core.network.dto.AuthResponseDto
import com.qhathia.mobile.staff.core.network.dto.DeviceCommandDto
import com.qhathia.mobile.staff.core.network.dto.DeviceDto
import com.qhathia.mobile.staff.core.network.dto.UserDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class DeviceMappersTest {

    @Test
    fun `device keeps telemetry and drops impossible or blank values`() {
        val device = DeviceDto(
            id = "d1",
            name = "  ",
            storeName = "Stand 1",
            status = "ACTIVE",
            batteryLevel = 150,
            networkStatus = "WIFI",
            lastPingAt = "2026-09-13T09:41:55Z",
            lastSyncAt = "not-a-date",
        ).toDomain()

        assertNull(device.name)
        assertNull(device.batteryLevel)
        assertEquals("WIFI", device.networkStatus)
        assertEquals("Stand 1", device.storeName)
        assertEquals(Instant.parse("2026-09-13T09:41:55Z"), device.lastPingAt)
        assertNull(device.lastSyncAt)
    }

    @Test
    fun `command maps type, status and trimmed detail`() {
        val command = DeviceCommandDto(
            id = "c1",
            command = "RESTART_APP",
            status = "FAILED",
            requestedAt = "2026-09-13T09:41:55Z",
            resultDetail = " sin conexión ",
        ).toDomain()!!

        assertEquals(DeviceCommandType.RESTART_APP, command.type)
        assertEquals(DeviceCommandStatus.FAILED, command.status)
        assertEquals("sin conexión", command.resultDetail)
    }

    @Test
    fun `command maps expiresAt, and leaves it null when absent`() {
        val withExpiry = DeviceCommandDto(
            id = "c1",
            command = "REFRESH_CATALOG",
            status = "PENDING",
            requestedAt = "2026-09-13T09:41:55Z",
            expiresAt = "2026-09-13T09:51:55Z",
        ).toDomain()!!
        val withoutExpiry = DeviceCommandDto(id = "c2", command = "REFRESH_CATALOG", status = "PENDING").toDomain()!!

        assertEquals(Instant.parse("2026-09-13T09:51:55Z"), withExpiry.expiresAt)
        assertNull(withoutExpiry.expiresAt)
    }

    @Test
    fun `command of an unknown type is dropped and an unknown status degrades`() {
        assertNull(DeviceCommandDto(id = "c1", command = "FORMAT_DISK", status = "PENDING").toDomain())
        assertEquals(
            DeviceCommandStatus.UNKNOWN,
            DeviceCommandDto(id = "c2", command = "REFRESH_CATALOG", status = "QUEUED").toDomain()?.status,
        )
    }

    @Test
    fun `login response carries the session id into the session`() {
        val session = AuthResponseDto(
            accessToken = "a",
            refreshToken = "r",
            user = UserDto("u1", "owner@tienda.com", "Juan", "OWNER"),
            sessionId = "3f0c",
        ).toSession()

        assertEquals("3f0c", session.sessionId)
    }

    @Test
    fun `command maps deliveredAt and ackedAt, and drops unparseable ones`() {
        val command = DeviceCommandDto(
            id = "c1",
            command = "RESTART_APP",
            status = "ACKED",
            deliveredAt = "2026-09-13T09:42:10Z",
            ackedAt = "2026-09-13T09:42:30Z",
        ).toDomain()!!
        val broken = DeviceCommandDto(id = "c2", command = "RESTART_APP", status = "ACKED", ackedAt = "ayer").toDomain()!!

        assertEquals(Instant.parse("2026-09-13T09:42:10Z"), command.deliveredAt)
        assertEquals(Instant.parse("2026-09-13T09:42:30Z"), command.ackedAt)
        assertNull(broken.ackedAt)
    }
}
