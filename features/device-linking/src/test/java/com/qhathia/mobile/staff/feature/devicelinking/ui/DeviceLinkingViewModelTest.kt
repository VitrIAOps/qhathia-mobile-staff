package com.qhathia.mobile.staff.feature.devicelinking.ui

import com.qhathia.mobile.staff.core.domain.model.CatalogCount
import java.time.Instant
import com.qhathia.mobile.staff.feature.devicelinking.model.RestartPhase
import com.qhathia.mobile.staff.feature.devicelinking.model.RESTART_BACK_TIMEOUT
import org.junit.jupiter.api.Assertions.assertInstanceOf
import com.qhathia.mobile.staff.feature.devicelinking.model.PairingPhase
import com.qhathia.mobile.staff.core.domain.usecase.UnlinkDeviceUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RejectPairingUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetPairingStatusUseCase
import com.qhathia.mobile.staff.core.domain.model.PairingSessionStatus
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsException
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsError
import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandStatus
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import com.qhathia.mobile.staff.core.domain.model.PairDeviceError
import com.qhathia.mobile.staff.core.domain.model.PairDeviceException
import com.qhathia.mobile.staff.core.domain.usecase.GetCatalogCountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetDeviceCommandsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetDevicesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.PairDeviceUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SendDeviceCommandUseCase
import com.qhathia.mobile.staff.core.testing.FakeDeviceRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.MutableClock
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.devicelinking.R
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiEffect
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiEvent
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException
import java.time.Duration

class DeviceLinkingViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var repository: FakeDeviceRepository
    private val clock = MutableClock()

    @BeforeEach
    fun setUp() {
        repository = FakeDeviceRepository()
    }

    /** Every view model of the test, stopped at the end so their refresh loops do not run forever. */
    private val viewModels = mutableListOf<DeviceLinkingViewModel>()

    /**
     * `runTest` that stops every screen at the end: a visible Dispositivos refreshes forever, and
     * `runTest` would otherwise keep advancing virtual time to drain it.
     */
    private fun screenTest(body: suspend TestScope.() -> Unit): TestResult = runTest {
        try {
            body()
        } finally {
            viewModels.forEach { it.onEvent(DeviceLinkingUiEvent.ScreenStopped) }
        }
    }

    private fun createViewModel() = DeviceLinkingViewModel(
        getDevicesUseCase = GetDevicesUseCase(repository),
        pairDeviceUseCase = PairDeviceUseCase(repository),
        rejectPairingUseCase = RejectPairingUseCase(repository),
        getPairingStatusUseCase = GetPairingStatusUseCase(repository),
        unlinkDeviceUseCase = UnlinkDeviceUseCase(repository),
        getCatalogCountUseCase = GetCatalogCountUseCase(repository),
        sendDeviceCommandUseCase = SendDeviceCommandUseCase(repository),
        getDeviceCommandsUseCase = GetDeviceCommandsUseCase(repository),
        clock = clock,
    ).also { viewModels += it }

    private fun advancePoll() {
        mainDispatcher.dispatcher.scheduler.advanceTimeBy(DeviceLinkingViewModel.COMMAND_POLL_INTERVAL.toMillis())
        mainDispatcher.dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `on init loads devices and the catalog count`() = screenTest {
        repository.devicesResult = Result.success(
            listOf(TestFixtures.device(id = "d1"), TestFixtures.device(id = "d2")),
        )

        val state = createViewModel().uiState.value

        assertEquals(2, state.devices.size)
        assertTrue(state.hasDevices)
        assertEquals(CatalogCount(active = 12, visible = 12), state.catalogCount)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
    }

    @Test
    fun `given failure then isError is set`() = screenTest {
        repository.devicesResult = Result.failure(IOException("offline"))

        assertTrue(createViewModel().uiState.value.isError)
    }

    @Test
    fun `a failed catalog count leaves the devices visible without a count`() = screenTest {
        repository.catalogCountResult = Result.failure(IOException("offline"))

        val state = createViewModel().uiState.value

        assertTrue(state.hasDevices)
        assertNull(state.catalogCount)
    }

    @Test
    fun `refresh catalog is sent and the button stays busy until the tablet answers`() = screenTest {
        val viewModel = createViewModel()

        viewModel.onEvent(DeviceLinkingUiEvent.RefreshCatalog("device-1"))
        viewModel.onEvent(DeviceLinkingUiEvent.RefreshCatalog("device-1"))

        val state = viewModel.uiState.value
        assertEquals(listOf("device-1" to DeviceCommandType.REFRESH_CATALOG), repository.sentCommands)
        assertEquals(DeviceCommandStatus.PENDING, state.commandFor("device-1", DeviceCommandType.REFRESH_CATALOG)?.status)
        assertTrue(state.isCommandBusy("device-1", DeviceCommandType.REFRESH_CATALOG))
        assertFalse(state.isCommandBusy("device-1", DeviceCommandType.RESTART_APP))
    }

    @Test
    fun `a failed command is reported and the button is usable again`() = screenTest {
        repository.sendCommandResult = Result.failure(IOException("offline"))
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(DeviceLinkingUiEvent.RefreshCatalog("device-1"))
            assertEquals(DeviceLinkingUiEffect.ShowCommandError(DeviceCommandType.REFRESH_CATALOG), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isCommandBusy("device-1", DeviceCommandType.REFRESH_CATALOG))
    }

    @Test
    fun `a tablet unlinked from its settings leaves the list with a message`() = screenTest {
        repository.devicesResult = Result.success(
            listOf(TestFixtures.device(id = "d1"), TestFixtures.device(id = "d2")),
        )
        val viewModel = createViewModel()
        // The backend no longer lists it, so the refresh that follows agrees with the local removal.
        repository.devicesResult = Result.success(listOf(TestFixtures.device(id = "d2")))

        viewModel.uiEffect.test {
            viewModel.onEvent(DeviceLinkingUiEvent.DeviceUnlinked("d1"))
            assertEquals(DeviceLinkingUiEffect.ShowUnlinked(), awaitItem())
        }
        assertEquals(listOf("d2"), viewModel.uiState.value.devices.map { it.id })
    }

    @Test
    fun `a tablet already unlinked elsewhere says so`() = screenTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(DeviceLinkingUiEvent.DeviceUnlinked("device-1", alreadyGone = true))
            assertEquals(DeviceLinkingUiEffect.ShowUnlinked(alreadyGone = true), awaitItem())
        }
    }

    @Test
    fun `restart is sent only after confirmation`() = screenTest {
        val viewModel = createViewModel()

        viewModel.onEvent(DeviceLinkingUiEvent.RestartRequested("device-1"))
        assertEquals("device-1", viewModel.uiState.value.pendingRestartDeviceId)
        assertTrue(repository.sentCommands.isEmpty())

        viewModel.onEvent(DeviceLinkingUiEvent.RestartConfirmed)

        assertNull(viewModel.uiState.value.pendingRestartDeviceId)
        assertEquals(listOf("device-1" to DeviceCommandType.RESTART_APP), repository.sentCommands)
    }

    @Test
    fun `dismissing the restart confirmation sends nothing`() = screenTest {
        val viewModel = createViewModel()

        viewModel.onEvent(DeviceLinkingUiEvent.RestartRequested("device-1"))
        viewModel.onEvent(DeviceLinkingUiEvent.RestartDismissed)

        assertTrue(repository.sentCommands.isEmpty())
    }

    @Test
    fun `a visible screen polls an in-flight command until the tablet confirms`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        viewModel.onEvent(DeviceLinkingUiEvent.RefreshCatalog("device-1"))
        val callsBefore = repository.getCommandsCallCount
        repository.commandsResults["device-1"] = Result.success(
            listOf(TestFixtures.deviceCommand(id = "cmd-1", status = DeviceCommandStatus.ACKED)),
        )

        advancePoll()

        assertEquals(callsBefore + 1, repository.getCommandsCallCount)
        val state = viewModel.uiState.value
        assertEquals(DeviceCommandStatus.ACKED, state.commandFor("device-1", DeviceCommandType.REFRESH_CATALOG)?.status)
        assertFalse(state.isCommandBusy("device-1", DeviceCommandType.REFRESH_CATALOG))

        advancePoll()
        assertEquals(callsBefore + 1, repository.getCommandsCallCount)
    }

    @Test
    fun `an expired command frees the button for a retry`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        viewModel.onEvent(DeviceLinkingUiEvent.RefreshCatalog("device-1"))
        repository.commandsResults["device-1"] = Result.success(
            listOf(TestFixtures.deviceCommand(id = "cmd-1", status = DeviceCommandStatus.EXPIRED)),
        )

        advancePoll()

        assertEquals(
            DeviceCommandStatus.EXPIRED,
            viewModel.uiState.value.commandFor("device-1", DeviceCommandType.REFRESH_CATALOG)?.status,
        )
        assertFalse(viewModel.uiState.value.isCommandBusy("device-1", DeviceCommandType.REFRESH_CATALOG))
    }

    @Test
    fun `a stopped screen does not poll`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        viewModel.onEvent(DeviceLinkingUiEvent.RefreshCatalog("device-1"))
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStopped)
        val callsBefore = repository.getCommandsCallCount

        advancePoll()

        assertEquals(callsBefore, repository.getCommandsCallCount)
    }

    @Test
    fun `on open, a recent command is shown and an old finished one is not`() = screenTest {
        repository.devicesResult = Result.success(
            listOf(TestFixtures.device(id = "d1"), TestFixtures.device(id = "d2")),
        )
        repository.commandsResults["d1"] = Result.success(
            listOf(TestFixtures.deviceCommand(status = DeviceCommandStatus.ACKED, requestedAt = clock.now.minus(Duration.ofMinutes(2)))),
        )
        repository.commandsResults["d2"] = Result.success(
            listOf(TestFixtures.deviceCommand(status = DeviceCommandStatus.ACKED, requestedAt = clock.now.minus(Duration.ofMinutes(11)))),
        )

        val state = createViewModel().uiState.value

        assertEquals(DeviceCommandStatus.ACKED, state.commandFor("d1", DeviceCommandType.REFRESH_CATALOG)?.status)
        assertNull(state.commandFor("d2", DeviceCommandType.REFRESH_CATALOG))
    }

    @Test
    fun `open pair dialog resets code and shows it`() = screenTest {
        val viewModel = createViewModel()

        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)

        val state = viewModel.uiState.value
        assertTrue(state.isPairDialogVisible)
        assertEquals("", state.pairCode)
        assertNull(state.pairErrorRes)
    }

    @Test
    fun `submit pair blank code is a no-op`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)

        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

        assertEquals(0, repository.pairCallCount)
        assertTrue(viewModel.uiState.value.isPairDialogVisible)
    }

    @Test
    fun `a pairing the tablet exchanges closes the dialog, says so and reads the list twice`() = screenTest {
        repository.pairingStatusResult = Result.success(PairingSessionStatus("APPROVED", tokenExchanged = true))
        repository.pairResult = Result.success(TestFixtures.pairedDevice(name = "Mostrador"))
        val viewModel = createViewModel()
        val loadsAfterInit = repository.getDevicesCallCount
        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)
        viewModel.onEvent(DeviceLinkingUiEvent.PairCodeChanged("AB3CD"))

        viewModel.uiEffect.test {
            viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)
            assertEquals(DeviceLinkingUiEffect.ShowPaired("Mostrador"), awaitItem())
        }

        val state = viewModel.uiState.value
        assertFalse(state.isPairDialogVisible)
        assertFalse(state.isPairing)
        assertEquals("AB3CD", repository.lastPairedCode)
        assertEquals(loadsAfterInit + 1, repository.getDevicesCallCount)

        advanceBy(DeviceLinkingViewModel.FIRST_HEARTBEAT_GRACE)
        assertEquals(loadsAfterInit + 2, repository.getDevicesCallCount)
    }

    @Test
    fun `invalid code pairing keeps dialog open with invalid-code error`() = screenTest {
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
    fun `an unknown failure maps to the generic error message`() = screenTest {
        repository.pairResult = Result.failure(PairDeviceException(PairDeviceError.Unknown("boom")))
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)
        viewModel.onEvent(DeviceLinkingUiEvent.PairCodeChanged("AB3CD"))

        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

        assertEquals(R.string.device_pair_error_generic, viewModel.uiState.value.pairErrorRes)
    }

    @Test
    fun `dismiss pair dialog clears transient state`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)
        viewModel.onEvent(DeviceLinkingUiEvent.PairCodeChanged("AB3CD"))

        viewModel.onEvent(DeviceLinkingUiEvent.DismissPairDialog)

        val state = viewModel.uiState.value
        assertFalse(state.isPairDialogVisible)
        assertEquals("", state.pairCode)
        assertNull(state.pairErrorRes)
    }

    // ---- Code rules, name, waiting for the tablet, undo, reject ----

    private fun advanceBy(duration: Duration) {
        mainDispatcher.dispatcher.scheduler.advanceTimeBy(duration.toMillis())
        mainDispatcher.dispatcher.scheduler.runCurrent()
    }

    /** Opens the dialog and types a full code. */
    private fun DeviceLinkingViewModel.readyToPair(code: String = "AB3CD") {
        onEvent(DeviceLinkingUiEvent.OpenPairDialog)
        onEvent(DeviceLinkingUiEvent.PairCodeChanged(code))
    }

    @Test
    fun `the code is uppercased and ambiguous characters are dropped`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.OpenPairDialog)

        viewModel.onEvent(DeviceLinkingUiEvent.PairCodeChanged("rwn0za"))
        assertEquals("RWNZA", viewModel.uiState.value.pairCode)

        viewModel.onEvent(DeviceLinkingUiEvent.PairCodeChanged("o0i1l"))
        assertEquals("", viewModel.uiState.value.pairCode)

        viewModel.onEvent(DeviceLinkingUiEvent.PairCodeChanged("AB3CD99"))
        assertEquals("AB3CD", viewModel.uiState.value.pairCode)
    }

    @Test
    fun `four characters cannot be approved or rejected`() = screenTest {
        val viewModel = createViewModel()
        viewModel.readyToPair("AB3C")

        assertFalse(viewModel.uiState.value.canSubmitPair)
        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)
        viewModel.onEvent(DeviceLinkingUiEvent.RejectPair)

        assertEquals(0, repository.pairCallCount)
        assertTrue(repository.rejectedCodes.isEmpty())
    }

    @Test
    fun `the dialog suggests the first free tablet name and sends the typed one`() = screenTest {
        repository.devicesResult = Result.success(
            listOf(TestFixtures.device(id = "d1", name = "Tablet 1"), TestFixtures.device(id = "d3", name = "Tablet 3")),
        )
        val viewModel = createViewModel()
        viewModel.readyToPair()
        assertEquals("Tablet 2", viewModel.uiState.value.pairName)

        viewModel.onEvent(DeviceLinkingUiEvent.PairNameChanged("Mostrador"))
        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

        assertEquals("Mostrador", repository.lastPairName)
    }

    @Test
    fun `a blank name is sent as none`() = screenTest {
        val viewModel = createViewModel()
        viewModel.readyToPair()
        viewModel.onEvent(DeviceLinkingUiEvent.PairNameChanged("   "))

        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

        assertNull(repository.lastPairName)
    }

    @Test
    fun `back while waiting shows the options and stops polling`() = screenTest {
        val viewModel = createViewModel()
        viewModel.readyToPair()

        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)
        assertInstanceOf(PairingPhase.WaitingForTablet::class.java, viewModel.uiState.value.pairingPhase)

        // F-60: Back stops the wait and offers "Deshacer" / "Seguir esperando"; the dialog stays.
        viewModel.onEvent(DeviceLinkingUiEvent.DismissPairDialog)
        assertTrue(viewModel.uiState.value.isPairDialogVisible)
        assertInstanceOf(PairingPhase.TabletDidNotRespond::class.java, viewModel.uiState.value.pairingPhase)

        val reads = repository.statusCallCount
        advanceBy(DeviceLinkingViewModel.PAIR_EXCHANGE_TIMEOUT)
        assertEquals(reads, repository.statusCallCount)

        // On the notice, Back does nothing: the owner picks an option.
        viewModel.onEvent(DeviceLinkingUiEvent.DismissPairDialog)
        assertInstanceOf(PairingPhase.TabletDidNotRespond::class.java, viewModel.uiState.value.pairingPhase)
    }

    @Test
    fun `a tablet that never exchanges the code is reported after the wait`() = screenTest {
        val viewModel = createViewModel()
        viewModel.readyToPair()
        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

        advanceBy(DeviceLinkingViewModel.PAIR_EXCHANGE_TIMEOUT)

        assertInstanceOf(PairingPhase.TabletDidNotRespond::class.java, viewModel.uiState.value.pairingPhase)
        assertTrue(viewModel.uiState.value.isPairDialogVisible)
    }

    @Test
    fun `keeping on waiting polls again and closes when the tablet answers`() = screenTest {
        val viewModel = createViewModel()
        viewModel.readyToPair()
        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)
        advanceBy(DeviceLinkingViewModel.PAIR_EXCHANGE_TIMEOUT)
        repository.pairingStatusResult = Result.success(PairingSessionStatus("APPROVED", tokenExchanged = true))

        viewModel.onEvent(DeviceLinkingUiEvent.KeepWaiting)

        assertFalse(viewModel.uiState.value.isPairDialogVisible)
    }

    @Test
    fun `undoing unlinks the approved device and closes`() = screenTest {
        repository.pairResult = Result.success(TestFixtures.pairedDevice(deviceId = "device-new"))
        val viewModel = createViewModel()
        viewModel.readyToPair()
        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)
        advanceBy(DeviceLinkingViewModel.PAIR_EXCHANGE_TIMEOUT)

        viewModel.uiEffect.test {
            viewModel.onEvent(DeviceLinkingUiEvent.UndoPair)
            assertEquals(DeviceLinkingUiEffect.ShowPairUndone, awaitItem())
        }
        assertEquals(listOf("device-new"), repository.unlinkedDevices)
        assertFalse(viewModel.uiState.value.isPairDialogVisible)
    }

    @Test
    fun `undoing a device that is already gone counts as undone`() = screenTest {
        repository.unlinkResult = Result.failure(DeviceSettingsException(DeviceSettingsError.NotFound))
        val viewModel = createViewModel()
        viewModel.readyToPair()
        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)
        advanceBy(DeviceLinkingViewModel.PAIR_EXCHANGE_TIMEOUT)

        viewModel.onEvent(DeviceLinkingUiEvent.UndoPair)

        assertFalse(viewModel.uiState.value.isPairDialogVisible)
    }

    @Test
    fun `undo without connection keeps the dialog with the error`() = screenTest {
        repository.unlinkResult = Result.failure(DeviceSettingsException(DeviceSettingsError.Network))
        val viewModel = createViewModel()
        viewModel.readyToPair()
        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)
        advanceBy(DeviceLinkingViewModel.PAIR_EXCHANGE_TIMEOUT)

        viewModel.onEvent(DeviceLinkingUiEvent.UndoPair)

        val state = viewModel.uiState.value
        assertTrue(state.isPairDialogVisible)
        assertFalse(state.isUndoing)
        assertEquals(R.string.device_pair_error_network, state.pairErrorRes)
    }

    @Test
    fun `rejecting sends the code and closes`() = screenTest {
        val viewModel = createViewModel()
        viewModel.readyToPair("CXZC7")

        viewModel.uiEffect.test {
            viewModel.onEvent(DeviceLinkingUiEvent.RejectPair)
            assertEquals(DeviceLinkingUiEffect.ShowPairRejected, awaitItem())
        }
        assertEquals(listOf("CXZC7"), repository.rejectedCodes)
        assertFalse(viewModel.uiState.value.isPairDialogVisible)
    }

    @Test
    fun `each pairing error has its own message`() = screenTest {
        val cases = mapOf(
            PairDeviceError.ExpiredCode to R.string.device_pair_error_expired,
            PairDeviceError.AlreadyUsedCode to R.string.device_pair_error_used,
            PairDeviceError.CancelledCode to R.string.device_pair_error_cancelled,
            PairDeviceError.CatalogNotReady(null) to R.string.device_pair_error_catalog,
            PairDeviceError.CatalogCheckUnavailable to R.string.device_pair_error_catalog_unavailable,
            PairDeviceError.Network to R.string.device_pair_error_network,
        )
        val viewModel = createViewModel()
        viewModel.readyToPair()
        cases.forEach { (error, expected) ->
            repository.pairResult = Result.failure(PairDeviceException(error))

            viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

            assertEquals(expected, viewModel.uiState.value.pairErrorRes, error.toString())
            assertTrue(viewModel.uiState.value.isPairDialogVisible)
        }
    }

    @Test
    fun `coming back to the screen reads the list again`() = screenTest {
        val viewModel = createViewModel()
        val loadsAfterInit = repository.getDevicesCallCount

        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        assertEquals(loadsAfterInit, repository.getDevicesCallCount)

        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStopped)
        clock.advanceBy(Duration.ofSeconds(10))
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        assertEquals(loadsAfterInit + 1, repository.getDevicesCallCount)
    }

    @Test
    fun `coming back to the screen re-reads the catalog count`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        val countsBefore = repository.catalogCountCallCount
        repository.catalogCountResult = Result.success(CatalogCount(active = 51, visible = 47))

        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStopped)
        clock.advanceBy(Duration.ofSeconds(10))
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)

        assertEquals(countsBefore + 1, repository.catalogCountCallCount)
        assertEquals(CatalogCount(active = 51, visible = 47), viewModel.uiState.value.catalogCount)
    }

    @Test
    fun `a catalog refresh turning ACKED re-reads the count once`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        viewModel.onEvent(DeviceLinkingUiEvent.RefreshCatalog("device-1"))
        val countsBefore = repository.catalogCountCallCount
        repository.catalogCountResult = Result.success(CatalogCount(active = 13, visible = 13))
        repository.commandsResults["device-1"] = Result.success(
            listOf(TestFixtures.deviceCommand(id = "cmd-1", status = DeviceCommandStatus.ACKED)),
        )

        advancePoll()
        assertEquals(countsBefore + 1, repository.catalogCountCallCount)
        assertEquals(13, viewModel.uiState.value.catalogCount?.shown)

        advancePoll()
        advancePoll()
        assertEquals(countsBefore + 1, repository.catalogCountCallCount)
    }

    @Test
    fun `a plain poll tick does not read the count`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        val countsBefore = repository.catalogCountCallCount

        repeat(4) { advancePoll() }

        assertEquals(countsBefore, repository.catalogCountCallCount)
    }

    @Test
    fun `a failed count re-read keeps the number shown`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        repository.catalogCountResult = Result.failure(IOException("offline"))

        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStopped)
        clock.advanceBy(Duration.ofSeconds(10))
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)

        assertEquals(CatalogCount(active = 12, visible = 12), viewModel.uiState.value.catalogCount)
    }

    // ---- Live refresh, restart phases, finished and overdue commands ----

    private val now: Instant get() = clock.instant()

    private fun restartCommand(
        status: DeviceCommandStatus,
        ackedAt: Instant? = null,
        requestedAt: Instant = now.minusSeconds(60),
    ) = TestFixtures.deviceCommand(
        id = "restart-1",
        type = DeviceCommandType.RESTART_APP,
        status = status,
        requestedAt = requestedAt,
        ackedAt = ackedAt,
    )

    @Test
    fun `a visible screen re-reads the devices every 30 seconds`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        val base = repository.getDevicesCallCount

        advanceBy(DeviceLinkingViewModel.COMMAND_POLL_INTERVAL)
        assertEquals(base, repository.getDevicesCallCount)

        advanceBy(DeviceLinkingViewModel.COMMAND_POLL_INTERVAL)
        assertEquals(base + 1, repository.getDevicesCallCount)
    }

    @Test
    fun `while a command is in flight the devices are re-read every 15 seconds`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        viewModel.onEvent(DeviceLinkingUiEvent.RefreshCatalog("device-1"))
        val base = repository.getDevicesCallCount

        advanceBy(DeviceLinkingViewModel.COMMAND_POLL_INTERVAL)

        assertEquals(base + 1, repository.getDevicesCallCount)
    }

    @Test
    fun `a refresh shows the tablet's newer report`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        repository.devicesResult = Result.success(listOf(TestFixtures.device(lastPingAt = now)))

        advanceBy(DeviceLinkingViewModel.DEVICES_REFRESH_INTERVAL)

        assertEquals(now, viewModel.uiState.value.devices.single().lastPingAt)
    }

    @Test
    fun `a refresh replaces a tablet's commands instead of merging`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        viewModel.onEvent(DeviceLinkingUiEvent.RefreshCatalog("device-1"))
        repository.commandsResults["device-1"] = Result.success(
            listOf(TestFixtures.deviceCommand(id = "cmd-1", status = DeviceCommandStatus.ACKED, ackedAt = now)),
        )

        advanceBy(DeviceLinkingViewModel.COMMAND_POLL_INTERVAL)

        assertEquals(
            DeviceCommandStatus.ACKED,
            viewModel.uiState.value.commandFor("device-1", DeviceCommandType.REFRESH_CATALOG)?.status,
        )
    }

    @Test
    fun `an acknowledged restart without a newer report is still restarting`() = screenTest {
        repository.devicesResult = Result.success(listOf(TestFixtures.device(lastPingAt = now.minusSeconds(90))))
        repository.commandsResults["device-1"] =
            Result.success(listOf(restartCommand(DeviceCommandStatus.ACKED, ackedAt = now.minusSeconds(10))))

        val state = createViewModel().uiState.value

        assertEquals(RestartPhase.RESTARTING, state.restartPhase(state.devices.single()))
        assertTrue(state.isCommandBusy("device-1", DeviceCommandType.RESTART_APP))
        assertTrue(state.needsFastPolling)
    }

    @Test
    fun `a report after the ack means the tablet is back`() = screenTest {
        repository.devicesResult = Result.success(listOf(TestFixtures.device(lastPingAt = now.minusSeconds(90))))
        repository.commandsResults["device-1"] =
            Result.success(listOf(restartCommand(DeviceCommandStatus.ACKED, ackedAt = now.minusSeconds(10))))
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        repository.devicesResult = Result.success(listOf(TestFixtures.device(lastPingAt = now.plusSeconds(5))))
        clock.advanceBy(Duration.ofSeconds(15))

        advanceBy(DeviceLinkingViewModel.COMMAND_POLL_INTERVAL)

        val state = viewModel.uiState.value
        assertEquals(RestartPhase.BACK, state.restartPhase(state.devices.single()))
        assertFalse(state.isCommandBusy("device-1", DeviceCommandType.RESTART_APP))
    }

    @Test
    fun `no report three minutes after the ack means the tablet is not back`() = screenTest {
        repository.devicesResult = Result.success(listOf(TestFixtures.device(lastPingAt = now.minusSeconds(90))))
        repository.commandsResults["device-1"] =
            Result.success(listOf(restartCommand(DeviceCommandStatus.ACKED, ackedAt = now)))
        val viewModel = createViewModel()
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        clock.advanceBy(RESTART_BACK_TIMEOUT)

        advanceBy(DeviceLinkingViewModel.COMMAND_POLL_INTERVAL)

        val state = viewModel.uiState.value
        assertEquals(RestartPhase.NOT_BACK, state.restartPhase(state.devices.single()))
        assertFalse(state.isCommandBusy("device-1", DeviceCommandType.RESTART_APP))
    }

    @Test
    fun `an expired command stays visible for ten minutes after it expired`() = screenTest {
        val expired = TestFixtures.deviceCommand(
            id = "cmd-1",
            status = DeviceCommandStatus.EXPIRED,
            requestedAt = now.minus(Duration.ofMinutes(10)),
            expiresAt = now,
        )
        repository.commandsResults["device-1"] = Result.success(listOf(expired))

        clock.advanceBy(Duration.ofMinutes(5))
        val reopened = createViewModel().uiState.value
        assertEquals(DeviceCommandStatus.EXPIRED, reopened.commandFor("device-1", DeviceCommandType.REFRESH_CATALOG)?.status)

        clock.advanceBy(Duration.ofMinutes(6))
        val later = createViewModel().uiState.value
        assertNull(later.commandFor("device-1", DeviceCommandType.REFRESH_CATALOG))
    }

    @Test
    fun `a command long past its deadline no longer blocks the button`() = screenTest {
        val overdue = TestFixtures.deviceCommand(
            id = "cmd-1",
            status = DeviceCommandStatus.PENDING,
            requestedAt = now.minus(Duration.ofMinutes(11)),
            expiresAt = now.minus(Duration.ofMinutes(1)),
        )
        repository.commandsResults["device-1"] = Result.success(listOf(overdue))

        val state = createViewModel().uiState.value

        assertFalse(state.isCommandBusy("device-1", DeviceCommandType.REFRESH_CATALOG))
        assertFalse(state.hasCommandsInFlight)
        assertFalse(state.needsFastPolling)
    }


    // --- fix-pairing-wait-timeout (F-60) ---

    @Test
    fun `a slow status endpoint still reports within the timeout`() = screenTest {
        repository.statusDelay = Duration.ofSeconds(1)
        val viewModel = createViewModel()
        viewModel.readyToPair()
        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

        // 15 reads would now take 45 s; the bound is time, not reads.
        advanceBy(DeviceLinkingViewModel.PAIR_EXCHANGE_TIMEOUT)

        assertInstanceOf(PairingPhase.TabletDidNotRespond::class.java, viewModel.uiState.value.pairingPhase)
    }

    @Test
    fun `the notice does not come before the timeout`() = screenTest {
        val viewModel = createViewModel()
        viewModel.readyToPair()
        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)

        advanceBy(DeviceLinkingViewModel.PAIR_EXCHANGE_TIMEOUT.minusSeconds(1))

        assertInstanceOf(PairingPhase.WaitingForTablet::class.java, viewModel.uiState.value.pairingPhase)
    }

    @Test
    fun `an exchange just before the timeout still pairs`() = screenTest {
        val viewModel = createViewModel()
        viewModel.readyToPair()
        viewModel.onEvent(DeviceLinkingUiEvent.SubmitPair)
        advanceBy(DeviceLinkingViewModel.PAIR_EXCHANGE_TIMEOUT.minusSeconds(3))
        repository.pairingStatusResult = Result.success(PairingSessionStatus("APPROVED", tokenExchanged = true))

        advanceBy(Duration.ofSeconds(2))

        assertFalse(viewModel.uiState.value.isPairDialogVisible)
    }
}
