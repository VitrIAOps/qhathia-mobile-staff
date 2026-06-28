package com.qhathia.mobile.staff.feature.productinventory.ui

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductsUseCase
import com.qhathia.mobile.staff.core.testing.FakeCategoryRepository
import com.qhathia.mobile.staff.core.testing.FakeProductRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEffect
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEvent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

class InventoryViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var productRepository: FakeProductRepository
    private lateinit var categoryRepository: FakeCategoryRepository

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        categoryRepository = FakeCategoryRepository()
    }

    private fun createViewModel() = InventoryViewModel(
        getProductsUseCase = GetProductsUseCase(productRepository),
        getCategoriesUseCase = GetCategoriesUseCase(categoryRepository),
    )

    @Test
    fun `on init loads products and categories`() = runTest {
        productRepository.productsResult = Result.success(
            listOf(TestFixtures.product(id = "p1"), TestFixtures.product(id = "p2")),
        )
        categoryRepository.categoriesResult = Result.success(listOf(TestFixtures.category()))

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(2, state.productCount)
        assertEquals(2, state.visibleProducts.size)
        assertEquals(1, state.categories.size)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
    }

    @Test
    fun `given empty result then product count is zero`() = runTest {
        productRepository.productsResult = Result.success(emptyList())

        val viewModel = createViewModel()

        assertEquals(0, viewModel.uiState.value.productCount)
    }

    @Test
    fun `given failure then isError is set`() = runTest {
        productRepository.productsResult = Result.failure(IOException("offline"))

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isError)
    }

    @Test
    fun `given failure when retry then ShowError is emitted`() = runTest {
        productRepository.productsResult = Result.failure(IOException("offline"))
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.Retry)
            assertTrue(awaitItem() is InventoryUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when select category then re-queries with category id`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryUiEvent.SelectCategory("c2"))

        assertEquals("c2", productRepository.lastCategoryId)
        assertEquals("c2", viewModel.uiState.value.selectedCategoryId)
        assertEquals(2, productRepository.callCount) // init + select
    }

    @Test
    fun `availability sub-filter combines with active category`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryUiEvent.SelectCategory("c2"))
        viewModel.onEvent(InventoryUiEvent.SelectAvailability(Availability.LOW_STOCK))

        assertEquals("c2", productRepository.lastCategoryId)
        assertEquals(Availability.LOW_STOCK, productRepository.lastAvailability)
    }

    @Test
    fun `clear availability re-queries without availability and keeps category`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.SelectCategory("c2"))
        viewModel.onEvent(InventoryUiEvent.SelectAvailability(Availability.AVAILABLE))

        viewModel.onEvent(InventoryUiEvent.ClearAvailability)

        assertEquals("c2", productRepository.lastCategoryId)
        assertEquals(null, productRepository.lastAvailability)
        assertEquals(null, viewModel.uiState.value.selectedAvailability)
    }

    @Test
    fun `search filters loaded products client-side without re-query`() = runTest {
        productRepository.productsResult = Result.success(
            listOf(
                TestFixtures.product(id = "p1", name = "Taladro Bosch"),
                TestFixtures.product(id = "p2", name = "Martillo"),
            ),
        )
        val viewModel = createViewModel()
        val callsAfterLoad = productRepository.callCount

        viewModel.onEvent(InventoryUiEvent.Search("mar"))

        val state = viewModel.uiState.value
        assertEquals(1, state.visibleProducts.size)
        assertEquals(1, state.productCount)
        assertEquals("Martillo", state.visibleProducts.first().name)
        assertEquals(callsAfterLoad, productRepository.callCount) // no new network call
    }
}
