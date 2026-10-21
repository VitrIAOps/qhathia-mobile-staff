package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.UpdateProfileError
import com.qhathia.mobile.staff.core.domain.model.UpdateProfileException
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.FakeUserProfileRepository
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateUserProfileUseCaseTest {

    private val profiles = FakeUserProfileRepository()

    @Test
    fun `a saved name is copied into the stored session`() = runTest {
        val sessions = FakeSessionRepository(TestFixtures.session(user = TestFixtures.user(fullName = "Carla Mendoza")))

        val result = UpdateUserProfileUseCase(profiles, sessions)(fullName = "Carla Ruiz", phone = null)

        assertTrue(result.isSuccess)
        assertEquals(FakeUserProfileRepository.Update("Carla Ruiz", null), profiles.updates.single())
        assertEquals("Carla Ruiz", sessions.observeSession().first()?.user?.fullName)
    }

    @Test
    fun `a failed update leaves the session untouched`() = runTest {
        val sessions = FakeSessionRepository(TestFixtures.session(user = TestFixtures.user(fullName = "Carla Mendoza")))
        profiles.updateResult = Result.failure(UpdateProfileException(UpdateProfileError.Network))

        val result = UpdateUserProfileUseCase(profiles, sessions)(fullName = "Carla Ruiz", phone = null)

        assertTrue(result.isFailure)
        assertEquals(0, sessions.saveCallCount)
        assertEquals("Carla Mendoza", sessions.observeSession().first()?.user?.fullName)
    }

    @Test
    fun `a phone-only change does not rewrite the session`() = runTest {
        val sessions = FakeSessionRepository(TestFixtures.session(user = TestFixtures.user(fullName = "Carla Mendoza")))
        profiles.profileResult = Result.success(TestFixtures.userProfile(fullName = "Carla Mendoza"))

        UpdateUserProfileUseCase(profiles, sessions)(fullName = null, phone = "987654321")

        assertEquals(0, sessions.saveCallCount)
    }

    @Test
    fun `without a session the update still succeeds`() = runTest {
        val result = UpdateUserProfileUseCase(profiles, FakeSessionRepository())(fullName = "Carla Ruiz", phone = null)

        assertTrue(result.isSuccess)
    }
}
