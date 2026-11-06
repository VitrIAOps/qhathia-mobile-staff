package com.qhathia.mobile.staff.feature.devicelinking.settings

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.TestResult
import com.qhathia.mobile.staff.core.domain.usecase.RenameDeviceUseCase
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.BrightnessMode
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsChange
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsError
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsException
import com.qhathia.mobile.staff.core.domain.usecase.GetDeviceSettingsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetDevicesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UnlinkDeviceUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UpdateDeviceSettingsUseCase
import com.qhathia.mobile.staff.core.testing.FakeDeviceRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.devicelinking.R
import com.qhathia.mobile.staff.feature.devicelinking.navigation.TABLET_SETTINGS_ARG_ID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

private const val DEVICE_ID = "device-1"

class TabletSettingsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var repository: FakeDeviceRepository

    @BeforeEach
    fun setUp() {
        repository = FakeDeviceRepository()
    }

    /** Every view model of the test, stopped at the end so their refresh loops do not run forever. */
    private val viewModels = mutableListOf<TabletSettingsViewModel>()

    /** `runTest` that stops every screen at the end, for the same reason as in Dispositivos. */
    private fun screenTest(body: suspend TestScope.() -> Unit): TestResult = runTest {
        try {
            body()
        } finally {
            viewModels.forEach { it.onEvent(TabletSettingsUiEvent.ScreenStopped) }
        }
    }

    private fun createViewModel() = TabletSettingsViewModel(
        savedStateHandle = SavedStateHandle(mapOf(TABLET_SETTINGS_ARG_ID to DEVICE_ID)),
        getDeviceSettingsUseCase = GetDeviceSettingsUseCase(repository),
        getDevicesUseCase = GetDevicesUseCase(repository),
        updateDeviceSettingsUseCase = UpdateDeviceSettingsUseCase(repository),
        unlinkDeviceUseCase = UnlinkDeviceUseCase(repository),
        renameDeviceUseCase = RenameDeviceUseCase(repository),
    ).also { viewModels += it }

    private fun failure(error: DeviceSettingsError) = Result.failure<Nothing>(DeviceSettingsException(error))

    // ---- Load ----

    @Test
    fun `loads the settings and the tablet`() = runTest {
        repository.settingsResult = Result.success(TestFixtures.deviceSettings(brightnessMode = BrightnessMode.HIGH))

        val state = createViewModel().uiState.value

        assertEquals(TabletSettingsLoad.LOADED, state.load)
        assertEquals(BrightnessMode.HIGH, state.settings?.brightnessMode)
        assertEquals("Lenovo Tab M11", state.device?.model)
    }

    @Test
    fun `a settings failure shows the error state, and retry loads again`() = runTest {
        repository.settingsResult = failure(DeviceSettingsError.Network)
        val viewModel = createViewModel()
        assertEquals(TabletSettingsLoad.ERROR, viewModel.uiState.value.load)

        repository.settingsResult = Result.success(TestFixtures.deviceSettings())
        viewModel.onEvent(TabletSettingsUiEvent.Retry)

        assertEquals(TabletSettingsLoad.LOADED, viewModel.uiState.value.load)
    }

    @Test
    fun `a 404 says the tablet is no longer linked`() = runTest {
        repository.settingsResult = failure(DeviceSettingsError.NotFound)

        assertEquals(TabletSettingsLoad.NOT_FOUND, createViewModel().uiState.value.load)
    }

    @Test
    fun `a device list failure hides Información but keeps the settings`() = runTest {
        repository.devicesResult = Result.failure(IOException("offline"))

        val state = createViewModel().uiState.value

        assertEquals(TabletSettingsLoad.LOADED, state.load)
        assertNotNull(state.settings)
        assertNull(state.device)
    }

    // ---- Changes ----

    @Test
    fun `a change sends only its own field and keeps the server's answer`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(TabletSettingsUiEvent.SettingChanged(DeviceSettingsChange.KioskMode(false)))

        assertEquals(listOf(DEVICE_ID to DeviceSettingsChange.KioskMode(false)), repository.settingsChanges)
        assertFalse(viewModel.uiState.value.settings!!.kioskMode)
        assertTrue(viewModel.uiState.value.saving.isEmpty())
    }

    @Test
    fun `selecting the current value sends nothing`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(TabletSettingsUiEvent.SettingChanged(DeviceSettingsChange.Brightness(BrightnessMode.AUTO)))

        assertTrue(repository.settingsChanges.isEmpty())
    }

    @Test
    fun `a failure puts back only that control and says so`() = runTest {
        val viewModel = createViewModel()
        repository.updateSettingsResult = failure(DeviceSettingsError.Network)

        viewModel.uiEffect.test {
            viewModel.onEvent(TabletSettingsUiEvent.SettingChanged(DeviceSettingsChange.VoiceAssistant(false)))
            assertEquals(TabletSettingsUiEffect.ShowMessage(R.string.tablet_settings_save_error), awaitItem())
        }
        assertTrue(viewModel.uiState.value.settings!!.voiceAssistantEnabled)
    }

    @Test
    fun `while saving the control is busy and other controls still save`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.updateSettingsGate = gate
        val viewModel = createViewModel()

        viewModel.onEvent(TabletSettingsUiEvent.SettingChanged(DeviceSettingsChange.IdleShowcase(false)))
        viewModel.onEvent(TabletSettingsUiEvent.SettingChanged(DeviceSettingsChange.IdleShowcase(true)))
        viewModel.onEvent(TabletSettingsUiEvent.SettingChanged(DeviceSettingsChange.AutoSync(false)))

        val busy = viewModel.uiState.value
        assertTrue(busy.isSaving(SettingKey.IDLE_SHOWCASE))
        assertTrue(busy.isSaving(SettingKey.AUTO_SYNC))
        assertFalse(busy.settings!!.idleShowcaseEnabled)
        assertFalse(busy.settings!!.autoSyncEnabled)
        assertEquals(
            listOf(
                DEVICE_ID to DeviceSettingsChange.IdleShowcase(false),
                DEVICE_ID to DeviceSettingsChange.AutoSync(false),
            ),
            repository.settingsChanges,
        )

        gate.complete(Unit)

        val done = viewModel.uiState.value
        assertTrue(done.saving.isEmpty())
        assertFalse(done.settings!!.idleShowcaseEnabled)
        assertFalse(done.settings!!.autoSyncEnabled)
    }

    // ---- PIN ----

    private fun TabletSettingsViewModel.typePin(pin: String, confirmation: String = pin) {
        onEvent(TabletSettingsUiEvent.OpenPinDialog)
        onEvent(TabletSettingsUiEvent.PinChanged(pin))
        onEvent(TabletSettingsUiEvent.PinConfirmationChanged(confirmation))
    }

    @Test
    fun `saving needs two matching PINs of 4 to 6 digits`() = runTest {
        val viewModel = createViewModel()

        viewModel.typePin("1234", "1235")
        assertFalse(viewModel.uiState.value.pinDialog!!.canSave)
        assertTrue(viewModel.uiState.value.pinDialog!!.isMismatch)

        viewModel.typePin("123")
        assertFalse(viewModel.uiState.value.pinDialog!!.canSave)

        viewModel.typePin("12ab34")
        assertEquals("1234", viewModel.uiState.value.pinDialog!!.pin)

        viewModel.typePin("1234567")
        assertEquals("123456", viewModel.uiState.value.pinDialog!!.pin)
        assertTrue(viewModel.uiState.value.pinDialog!!.canSave)
    }

    @Test
    fun `setting the PIN closes the dialog and marks it set`() = runTest {
        val viewModel = createViewModel()
        viewModel.typePin("1234")

        viewModel.uiEffect.test {
            viewModel.onEvent(TabletSettingsUiEvent.SavePin)
            assertEquals(TabletSettingsUiEffect.ShowMessage(R.string.tablet_pin_saved), awaitItem())
        }
        assertEquals(listOf(DEVICE_ID to DeviceSettingsChange.SetExitPin("1234")), repository.settingsChanges)
        assertNull(viewModel.uiState.value.pinDialog)
        assertTrue(viewModel.uiState.value.settings!!.exitPinSet)
    }

    @Test
    fun `a failed PIN save keeps the dialog and the digits`() = runTest {
        val viewModel = createViewModel()
        repository.updateSettingsResult = failure(DeviceSettingsError.Network)
        viewModel.typePin("1234")

        viewModel.onEvent(TabletSettingsUiEvent.SavePin)

        val dialog = viewModel.uiState.value.pinDialog!!
        assertEquals("1234", dialog.pin)
        assertFalse(dialog.isSaving)
        assertEquals(R.string.tablet_pin_error, dialog.errorRes)
    }

    @Test
    fun `removing the PIN asks first, then sends removeExitPin`() = runTest {
        repository.settingsResult = Result.success(TestFixtures.deviceSettings(exitPinSet = true))
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.OpenPinDialog)

        viewModel.onEvent(TabletSettingsUiEvent.RemovePinRequested)
        assertTrue(viewModel.uiState.value.pinDialog!!.isConfirmingRemoval)
        assertTrue(repository.settingsChanges.isEmpty())

        viewModel.uiEffect.test {
            viewModel.onEvent(TabletSettingsUiEvent.RemovePinConfirmed)
            assertEquals(TabletSettingsUiEffect.ShowMessage(R.string.tablet_pin_removed), awaitItem())
        }
        assertEquals(listOf(DEVICE_ID to DeviceSettingsChange.RemoveExitPin), repository.settingsChanges)
        assertFalse(viewModel.uiState.value.settings!!.exitPinSet)
        assertTrue(viewModel.uiState.value.isKioskWithoutPin)
    }

    @Test
    fun `cancelling the removal sends nothing`() = runTest {
        repository.settingsResult = Result.success(TestFixtures.deviceSettings(exitPinSet = true))
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.OpenPinDialog)
        viewModel.onEvent(TabletSettingsUiEvent.RemovePinRequested)

        viewModel.onEvent(TabletSettingsUiEvent.RemovePinCancelled)

        assertFalse(viewModel.uiState.value.pinDialog!!.isConfirmingRemoval)
        assertTrue(repository.settingsChanges.isEmpty())
    }

    // ---- Unlink ----

    @Test
    fun `unlink is sent only after confirmation and closes the screen`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(TabletSettingsUiEvent.UnlinkRequested)
            assertTrue(viewModel.uiState.value.isUnlinkConfirmVisible)
            assertTrue(repository.unlinkedDevices.isEmpty())

            viewModel.onEvent(TabletSettingsUiEvent.UnlinkConfirmed)

            assertEquals(TabletSettingsUiEffect.Unlinked(DEVICE_ID), awaitItem())
        }
        assertEquals(listOf(DEVICE_ID), repository.unlinkedDevices)
        assertFalse(viewModel.uiState.value.isUnlinkConfirmVisible)
    }

    @Test
    fun `dismissing the unlink dialog sends nothing`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(TabletSettingsUiEvent.UnlinkRequested)
        viewModel.onEvent(TabletSettingsUiEvent.UnlinkDismissed)
        viewModel.onEvent(TabletSettingsUiEvent.UnlinkConfirmed)

        assertFalse(viewModel.uiState.value.isUnlinkConfirmVisible)
        assertTrue(repository.unlinkedDevices.isEmpty())
    }

    @Test
    fun `a failed unlink shows a message and keeps the screen usable`() = runTest {
        repository.unlinkResult = Result.failure(IOException("offline"))
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(TabletSettingsUiEvent.UnlinkRequested)
            viewModel.onEvent(TabletSettingsUiEvent.UnlinkConfirmed)
            assertEquals(TabletSettingsUiEffect.ShowMessage(R.string.device_unlink_error), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isUnlinking)
    }

    // ---- Rename ----

    private fun tabletNamed(name: String) {
        repository.devicesResult = Result.success(listOf(TestFixtures.device(id = DEVICE_ID, name = name)))
    }

    @Test
    fun `renaming updates the name and says so`() = runTest {
        tabletNamed("Tablet 1")
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.RenameClicked)
        assertEquals("Tablet 1", viewModel.uiState.value.renameDialog?.value)

        viewModel.onEvent(TabletSettingsUiEvent.RenameChanged("Caja 2"))
        viewModel.uiEffect.test {
            viewModel.onEvent(TabletSettingsUiEvent.RenameConfirmed)
            assertEquals(TabletSettingsUiEffect.ShowMessage(R.string.tablet_settings_renamed), awaitItem())
        }

        assertEquals(listOf(DEVICE_ID to "Caja 2"), repository.renames)
        assertEquals("Caja 2", viewModel.uiState.value.device?.name)
        assertNull(viewModel.uiState.value.renameDialog)
    }

    @Test
    fun `renaming a tablet that is gone says it is no longer linked`() = runTest {
        tabletNamed("Tablet 1")
        repository.renameResult = failure(DeviceSettingsError.NotFound)
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.RenameClicked)
        viewModel.onEvent(TabletSettingsUiEvent.RenameChanged("Caja 2"))

        viewModel.onEvent(TabletSettingsUiEvent.RenameConfirmed)

        assertEquals(TabletSettingsLoad.NOT_FOUND, viewModel.uiState.value.load)
        assertNull(viewModel.uiState.value.renameDialog)
    }

    @Test
    fun `a rejected name keeps the dialog with the error`() = runTest {
        tabletNamed("Tablet 1")
        repository.renameResult = failure(DeviceSettingsError.InvalidInput)
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.RenameClicked)
        viewModel.onEvent(TabletSettingsUiEvent.RenameChanged("Caja 2"))

        viewModel.onEvent(TabletSettingsUiEvent.RenameConfirmed)

        val dialog = checkNotNull(viewModel.uiState.value.renameDialog)
        assertEquals(R.string.tablet_settings_rename_invalid, dialog.errorRes)
        assertFalse(dialog.isSaving)
    }

    @Test
    fun `a blank or unchanged name cannot be saved`() = runTest {
        tabletNamed("Tablet 1")
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.RenameClicked)
        assertFalse(checkNotNull(viewModel.uiState.value.renameDialog).canSave)

        viewModel.onEvent(TabletSettingsUiEvent.RenameChanged("   "))
        assertFalse(checkNotNull(viewModel.uiState.value.renameDialog).canSave)

        viewModel.onEvent(TabletSettingsUiEvent.RenameConfirmed)
        assertTrue(repository.renames.isEmpty())
    }

    // ---- Silent refresh and unlink outcomes ----

    private fun advanceRefresh() {
        mainDispatcher.dispatcher.scheduler.advanceTimeBy(TabletSettingsViewModel.REFRESH_INTERVAL.toMillis())
        mainDispatcher.dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `a visible screen shows a change made elsewhere within 30 seconds`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.ScreenStarted)
        repository.settingsResult = Result.success(TestFixtures.deviceSettings(voiceAssistantEnabled = false))

        advanceRefresh()

        assertEquals(false, viewModel.uiState.value.settings?.voiceAssistantEnabled)
    }

    @Test
    fun `a later screen start re-reads at once`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.ScreenStarted)
        viewModel.onEvent(TabletSettingsUiEvent.ScreenStopped)
        repository.settingsResult = Result.success(TestFixtures.deviceSettings(kioskMode = false))

        viewModel.onEvent(TabletSettingsUiEvent.ScreenStarted)

        assertEquals(false, viewModel.uiState.value.settings?.kioskMode)
    }

    @Test
    fun `no silent refresh while a dialog is open`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.ScreenStarted)
        viewModel.onEvent(TabletSettingsUiEvent.OpenPinDialog)
        repository.settingsResult = Result.success(TestFixtures.deviceSettings(voiceAssistantEnabled = false))

        advanceRefresh()

        assertEquals(true, viewModel.uiState.value.settings?.voiceAssistantEnabled)
    }

    @Test
    fun `a refresh that answers 404 says the tablet is gone`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.ScreenStarted)
        repository.settingsResult = failure(DeviceSettingsError.NotFound)

        advanceRefresh()

        assertEquals(TabletSettingsLoad.NOT_FOUND, viewModel.uiState.value.load)
    }

    @Test
    fun `a stopped screen does not refresh`() = screenTest {
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.ScreenStarted)
        viewModel.onEvent(TabletSettingsUiEvent.ScreenStopped)
        repository.settingsResult = Result.success(TestFixtures.deviceSettings(voiceAssistantEnabled = false))

        advanceRefresh()

        assertEquals(true, viewModel.uiState.value.settings?.voiceAssistantEnabled)
    }

    @Test
    fun `unlinking a tablet that is already gone leaves as already gone`() = screenTest {
        repository.unlinkResult = failure(DeviceSettingsError.NotFound)
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.UnlinkRequested)

        viewModel.uiEffect.test {
            viewModel.onEvent(TabletSettingsUiEvent.UnlinkConfirmed)
            assertEquals(TabletSettingsUiEffect.Unlinked(DEVICE_ID, alreadyGone = true), awaitItem())
        }
    }

    @Test
    fun `unlinking without connection stays with the network message`() = screenTest {
        repository.unlinkResult = failure(DeviceSettingsError.Network)
        val viewModel = createViewModel()
        viewModel.onEvent(TabletSettingsUiEvent.UnlinkRequested)

        viewModel.uiEffect.test {
            viewModel.onEvent(TabletSettingsUiEvent.UnlinkConfirmed)
            assertEquals(TabletSettingsUiEffect.ShowMessage(R.string.device_unlink_error_network), awaitItem())
        }
        assertFalse(viewModel.uiState.value.isUnlinking)
    }
}
