package com.qhathia.mobile.staff.core.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class LogRedactionTest {

    @Test
    fun `the login request and response lose their password and tokens`() {
        val request = """BODY START {"email":"carla@gmail.com","password":"Secreta123"} BODY END"""
        val response = """{"accessToken":"eyJhbGciOi.abc","refreshToken":"r-123","tokenType":"Bearer","expiresIn":900}"""

        assertEquals("""BODY START {"email":"***","password":"***"} BODY END""", redactSecrets(request))
        assertEquals(
            """{"accessToken":"***","refreshToken":"***","tokenType":"Bearer","expiresIn":900}""",
            redactSecrets(response),
        )
    }

    @Test
    fun `the change-password body loses both passwords and the refresh token`() {
        val body = """{ "currentPassword" : "Vieja123", "newPassword": "Nueva1234", "currentRefreshToken": "r-9" }"""

        assertEquals(
            """{ "currentPassword" : "***", "newPassword": "***", "currentRefreshToken": "***" }""",
            redactSecrets(body),
        )
    }

    @Test
    fun `push token, pairing token, exit PIN and phone are hidden`() {
        val body = """{"token":"fcm-abc","pairingToken":"p-1","exitPin":"4321","phone":"987654321"}"""

        assertEquals("""{"token":"***","pairingToken":"***","exitPin":"***","phone":"***"}""", redactSecrets(body))
    }

    @Test
    fun `fields that only look sensitive, and null values, are left as they are`() {
        val body = """{"passwordUpdatedAt":"2026-11-03T10:00:00Z","exitPinSet":true,"tokenType":"Bearer","phone":null}"""

        assertEquals(body, redactSecrets(body))
    }

    @Test
    fun `an escaped quote inside a value does not leak its tail`() {
        val body = """{"password":"ab\"cd-secret","name":"Taladro"}"""

        val redacted = redactSecrets(body)

        assertEquals("""{"password":"***","name":"Taladro"}""", redacted)
        assertFalse(redacted.contains("secret"))
    }

    @Test
    fun `keys match whatever their case`() {
        assertEquals("""{"Password":"***","ACCESSTOKEN":"***"}""", redactSecrets("""{"Password":"x","ACCESSTOKEN":"y"}"""))
    }
}
