package com.qhathia.mobile.staff.feature.devicelinking.ui

import com.qhathia.mobile.staff.core.domain.model.PairDeviceError
import com.qhathia.mobile.staff.core.domain.model.PairDeviceException
import com.qhathia.mobile.staff.core.domain.usecase.GetDevicesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.PairDeviceUseCase
import com.qhathia.mobile.staff.core.testing.FakeDeviceRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.devicelinking.R
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiEvent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

class DeviceLinkingViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var repository: FakeDeviceRepository

    @BeforeEach
    fun setUp() {
        repository = FakeDeviceRepository()
    }

    private fun createViewModel() = DeviceLinkingViewModel(
        getDevicesUseCase = GetDevicesUseCase(repository),
        pairDeviceUseCase = PairDeviceUseCase(repository),
    )

    @Test
    fun `on init loads devices`() = runTest {
        repository.devicesResult = Result.success(
            listOf(TestFixtures.device(id = "d1"), TestFixtures.device(id = "d2")),
        )

        val state = createViewModel().uiState.value

        assertEquals(2, state.devices.size)
        assertTrue(state.hasDevices)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
    }

    @Test
    fun `given failure then isError is set`() = runTest {
        repository.devicesResult = Result.failure(IOException("offline"))

        assertTrue(createViewModel().uiState.value.isError)
    }

    @Test
    fun `open pair dialog resets code and shows it`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)

        val state = viewModel.uiState.value
        assertTrue(state.isPairDialogVisible)
        assertEquals("", state.pairCode)
        assertNull(state.pairErrorRes)
    }

    @Test
    fun `submit pair blank code is a no-op`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)

        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

        assertEquals(0, repository.pairCallCount)
        assertTrue(viewModel.uiState.value.isPairDialogVisible)
    }

    @Test
    fun `successful pairing closes dialog and reloads devices`() = runTest {
        val viewModel = createViewModel()
        val loadsAfterInit = repository.getDevicesCallCount
        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)
        viewModel.onEvent(DeviceLinkingUiEvent.PairCodeChanged("AB3CD"))

        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

        val state = viewModel.uiState.value
        assertFalse(state.isPairDialogVisible)
        assertFalse(state.isPairing)
        assertEquals("AB3CD", repository.lastPairedCode)
        assertEquals(loadsAfterInit + 1, repository.getDevicesCallCount) // reloaded after pairing
    }

    @Test
    fun `invalid code pairing keeps dialog open with invalid-code error`() = runTest {
        repository.pairResult = Result.failure(PairDeviceException(PairDeviceError.InvalidCode))
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)
        viewModel.onEvent(DeviceLinkingUiEvent.PairCodeChanged("BADXX"))

        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

        val state = viewModel.uiState.value
        assertTrue(state.isPairDialogVisible)
        assertFalse(state.isPairing)
        assertEquals(R.string.device_pair_error_invalid_code, state.pairErrorRes)
    }

    @Test
    fun `generic failure maps to generic error message`() = runTest {
        repository.pairResult = Result.failure(PairDeviceException(PairDeviceError.Network))
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)
        viewModel.onEvent(DeviceLinkingUiEvent.PairCodeChanged("AB3CD"))

        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

        assertEquals(R.string.device_pair_error_generic, viewModel.uiState.value.pairErrorRes)
    }

    @Test
    fun `dismiss pair dialog clears transient state`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)
        viewModel.onEvent(DeviceLinkingUiEvent.PairCodeChanged("AB3CD"))

        viewModel.onEvent(DeviceLinkingUiEvent.DismissPairDialog)

        val state = viewModel.uiState.value
        assertFalse(state.isPairDialogVisible)
        assertEquals("", state.pairCode)
        assertNull(state.pairErrorRes)
    }
}
