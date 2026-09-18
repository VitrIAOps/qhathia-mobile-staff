package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.DeviceStatus
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.testing.FakeAccountSecurityRepository
import com.qhathia.mobile.staff.core.testing.FakeAppPreferencesRepository
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.FakeStoreSettingsRepository
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration

class ProfileUseCasesTest {

    private val prefs = FakeAppPreferencesRepository(language = AppLanguage.SPANISH)
    private val store = FakeStoreSettingsRepository()

    private fun setLanguage(role: UserRole) = SetAppLanguageUseCase(
        appPreferencesRepository = prefs,
        storeSettingsRepository = store,
        sessionRepository = FakeSessionRepository(TestFixtures.session(user = TestFixtures.user(role = role))),
    )

    @Test
    fun `owner language change is stored locally and mirrored to the store`() = runTest {
        val result = setLanguage(UserRole.OWNER)(AppLanguage.ENGLISH)

        assertTrue(result.isSuccess)
        assertEquals(AppLanguage.ENGLISH, prefs.language)
        assertEquals(listOf(AppLanguage.ENGLISH), store.languageUpdates)
    }

    @Test
    fun `seller language change stays on the device`() = runTest {
        setLanguage(UserRole.SELLER)(AppLanguage.ENGLISH)

        assertEquals(AppLanguage.ENGLISH, prefs.language)
        assertTrue(store.languageUpdates.isEmpty())
    }

    @Test
    fun `a failed store mirror never reverts the local language`() = runTest {
        store.updateFailure = IOException("offline")

        val result = setLanguage(UserRole.OWNER)(AppLanguage.ENGLISH)

        assertTrue(result.isSuccess)
        assertEquals(AppLanguage.ENGLISH, prefs.language)
    }

    @Test
    fun `a failed local write fails and does not touch the store`() = runTest {
        prefs.setLanguageFailure = IOException("disk")

        val result = setLanguage(UserRole.OWNER)(AppLanguage.ENGLISH)

        assertTrue(result.isFailure)
        assertEquals(AppLanguage.SPANISH, prefs.language)
        assertTrue(store.languageUpdates.isEmpty())
    }

    @Test
    fun `this device is resolved by stored id and listed first, the rest by recent use`() = runTest {
        val security = FakeAccountSecurityRepository().apply {
            sessionsResult = Result.success(
                listOf(
                    TestFixtures.activeSession(sessionId = "old", lastUsedAt = TestFixtures.NOW.minusSeconds(7_200)),
                    TestFixtures.activeSession(sessionId = "recent", lastUsedAt = TestFixtures.NOW.minusSeconds(60)),
                    TestFixtures.activeSession(sessionId = "mine", lastUsedAt = TestFixtures.NOW.minusSeconds(86_400)),
                ),
            )
        }
        val useCase = GetActiveSessionsUseCase(security, FakeSessionRepository(TestFixtures.session(sessionId = "mine")))

        val sessions = useCase().getOrThrow()

        assertEquals(listOf("mine", "recent", "old"), sessions.map { it.sessionId })
        assertTrue(sessions.first().isCurrent)
        assertFalse(sessions[1].isCurrent)
    }

    @Test
    fun `a tablet is connected only while active and recently seen`() {
        val now = TestFixtures.NOW
        val recent = TestFixtures.device(lastPingAt = now.minus(Duration.ofMinutes(9)))

        assertTrue(recent.isConnected(now))
        assertFalse(recent.copy(lastPingAt = now.minus(Duration.ofMinutes(11))).isConnected(now))
        assertFalse(recent.copy(status = DeviceStatus.INACTIVE).isConnected(now))
        assertFalse(recent.copy(lastPingAt = null).isConnected(now))
    }

    @Test
    fun `language tags resolve by primary subtag`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es_PE"))
        assertNull(AppLanguage.fromTag("pt-BR"))
        assertNull(AppLanguage.fromTag(""))
    }
}
