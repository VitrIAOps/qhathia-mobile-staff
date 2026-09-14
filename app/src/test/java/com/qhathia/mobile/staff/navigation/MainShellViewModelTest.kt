package com.qhathia.mobile.staff.navigation

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.usecase.ObservePendingOrderCountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RefreshPendingOrderCountUseCase
import com.qhathia.mobile.staff.core.testing.FakeOrderRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MainShellViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var repository: FakeOrderRepository

    @BeforeEach
    fun setUp() {
        repository = FakeOrderRepository()
    }

    private fun createViewModel() = MainShellViewModel(
        observePendingOrderCount = ObservePendingOrderCountUseCase(repository),
        refreshPendingOrderCount = RefreshPendingOrderCountUseCase(repository),
    )

    @Test
    fun `entering the shell refreshes the pending count`() = runTest {
        repository.pendingCountResult = Result.success(2)

        createViewModel()
        runCurrent()

        assertEquals(1, repository.refreshPendingCountCallCount)
    }

    @Test
    fun `the badge count follows the shared flow`() = runTest {
        val viewModel = createViewModel()

        viewModel.pendingOrderCount.test {
            assertEquals(0, awaitItem())

            repository.emitPendingCount(2)
            assertEquals(2, awaitItem())

            repository.emitPendingCount(1)
            assertEquals(1, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
