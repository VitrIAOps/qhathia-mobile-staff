package com.qhathia.mobile.staff.feature.productinventory.ui

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountError
import com.qhathia.mobile.staff.core.domain.model.DiscountException
import com.qhathia.mobile.staff.core.domain.model.DiscountType
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductDiscountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SaveProductDiscountUseCase
import com.qhathia.mobile.staff.core.testing.FakeCategoryRepository
import com.qhathia.mobile.staff.core.testing.FakeProductDiscountRepository
import com.qhathia.mobile.staff.core.testing.FakeProductRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.MutableClock
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.productinventory.R
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEffect
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEvent
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

class InventoryViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var productRepository: FakeProductRepository
    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var discountRepository: FakeProductDiscountRepository
    private val clock = MutableClock()

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        categoryRepository = FakeCategoryRepository()
        discountRepository = FakeProductDiscountRepository()
    }

    private fun createViewModel() = InventoryViewModel(
        getProductsUseCase = GetProductsUseCase(productRepository),
        getCategoriesUseCase = GetCategoriesUseCase(categoryRepository),
        getProductDiscountUseCase = GetProductDiscountUseCase(discountRepository, clock),
        saveProductDiscountUseCase = SaveProductDiscountUseCase(discountRepository, clock),
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

    @Test
    fun `tapping a product emits a navigate-to-detail effect carrying its id`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.ProductClicked("p7"))

            assertEquals(InventoryUiEffect.NavigateToProductDetail("p7"), awaitItem())
        }
    }

    @Test
    fun `edit discount opens the dialog prefilled with the product's discount and row price`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1", price = 189.0)))
        discountRepository.discounts += TestFixtures.productDiscount(
            discount = Discount(DiscountType.PERCENTAGE, 20.0),
        )
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryUiEvent.EditDiscountClicked("p1"))

        val state = viewModel.uiState.value
        assertEquals("p1", state.discountProductId)
        assertEquals(189.0, state.discountDraft?.price)
        assertEquals("20", state.discountDraft?.input)
        assertEquals(151.2, state.discountDraft?.resultingPrice)
        assertFalse(state.isDiscountLoading)
    }

    @Test
    fun `edit discount on a product without one opens an empty dialog`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryUiEvent.EditDiscountClicked("p1"))

        assertEquals("", viewModel.uiState.value.discountDraft?.input)
    }

    @Test
    fun `when the discount cannot be loaded the dialog stays closed and an error is shown`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        discountRepository.getFailure = DiscountException(DiscountError.Network)
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.EditDiscountClicked("p1"))
            assertTrue(awaitItem() is InventoryUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(viewModel.uiState.value.discountDraft)
        assertFalse(viewModel.uiState.value.isDiscountLoading)
    }

    @Test
    fun `applying saves the discount with replace semantics and confirms`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        discountRepository.discounts += TestFixtures.productDiscount(id = "old")
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.EditDiscountClicked("p1"))
        val draft = checkNotNull(viewModel.uiState.value.discountDraft)
        viewModel.onEvent(InventoryUiEvent.DiscountDraftChanged(draft.withQuickPercentage(50)))

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.DiscountApplied)
            assertTrue(awaitItem() is InventoryUiEffect.ShowMessage)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("get:p1", "get:p1", "delete:old", "create:p1"), discountRepository.calls)
        assertEquals(Discount(DiscountType.PERCENTAGE, 50.0), discountRepository.discounts.single().discount)
        assertNull(viewModel.uiState.value.discountDraft)
        assertNull(viewModel.uiState.value.discountProductId)
    }

    @Test
    fun `a failed save keeps the dialog open with what was typed`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.EditDiscountClicked("p1"))
        val draft = checkNotNull(viewModel.uiState.value.discountDraft)
        viewModel.onEvent(InventoryUiEvent.DiscountDraftChanged(draft.withInput("35")))
        discountRepository.createFailure = DiscountException(DiscountError.Network)

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.DiscountApplied)
            assertTrue(awaitItem() is InventoryUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("35", viewModel.uiState.value.discountDraft?.input)
        assertFalse(viewModel.uiState.value.isDiscountSaving)
    }

    @Test
    fun `an invalid value is not saved and dismissing closes the dialog`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.EditDiscountClicked("p1"))

        viewModel.onEvent(InventoryUiEvent.DiscountApplied)
        assertNotNull(viewModel.uiState.value.discountDraft)
        assertEquals(listOf("get:p1"), discountRepository.calls)

        viewModel.onEvent(InventoryUiEvent.DiscountDismissed)
        assertNull(viewModel.uiState.value.discountDraft)
    }

    @Test
    fun `whether a product has a discount is unknown until its menu is opened`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()

        assertNull(viewModel.uiState.value.hasDiscount("p1"))
        assertTrue(discountRepository.calls.isEmpty())
    }

    @Test
    fun `opening the menu of a product without a discount resolves to register`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryUiEvent.RowActionsOpened("p1"))

        assertEquals(false, viewModel.uiState.value.hasDiscount("p1"))
        assertNull(viewModel.uiState.value.discountLookupProductId)
    }

    @Test
    fun `opening the menu of a product with a discount resolves to edit`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        discountRepository.discounts += TestFixtures.productDiscount()
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryUiEvent.RowActionsOpened("p1"))

        assertEquals(true, viewModel.uiState.value.hasDiscount("p1"))
    }

    @Test
    fun `the lookup happens once and the dialog reuses it instead of loading again`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        discountRepository.discounts += TestFixtures.productDiscount(
            discount = Discount(DiscountType.PERCENTAGE, 20.0),
        )
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryUiEvent.RowActionsOpened("p1"))
        viewModel.onEvent(InventoryUiEvent.RowActionsOpened("p1"))
        viewModel.onEvent(InventoryUiEvent.EditDiscountClicked("p1"))

        assertEquals(listOf("get:p1"), discountRepository.calls)
        assertEquals("20", viewModel.uiState.value.discountDraft?.input)
    }

    @Test
    fun `a failed lookup stays unknown and the option still loads the discount when chosen`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        discountRepository.getFailure = DiscountException(DiscountError.Network)
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryUiEvent.RowActionsOpened("p1"))
        assertNull(viewModel.uiState.value.hasDiscount("p1"))
        assertNull(viewModel.uiState.value.discountLookupProductId)

        discountRepository.getFailure = null
        viewModel.onEvent(InventoryUiEvent.EditDiscountClicked("p1"))
        assertNotNull(viewModel.uiState.value.discountDraft)
        assertEquals(false, viewModel.uiState.value.hasDiscount("p1"))
    }

    @Test
    fun `registering a discount from the menu turns the option into edit`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.RowActionsOpened("p1"))
        viewModel.onEvent(InventoryUiEvent.EditDiscountClicked("p1"))
        val draft = checkNotNull(viewModel.uiState.value.discountDraft)
        viewModel.onEvent(InventoryUiEvent.DiscountDraftChanged(draft.withQuickPercentage(10)))

        viewModel.onEvent(InventoryUiEvent.DiscountApplied)

        assertEquals(true, viewModel.uiState.value.hasDiscount("p1"))
    }

    @Test
    fun `reloading the products forgets what was known about their discounts`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.RowActionsOpened("p1"))
        assertEquals(false, viewModel.uiState.value.hasDiscount("p1"))

        viewModel.refresh()

        assertNull(viewModel.uiState.value.hasDiscount("p1"))
    }

    private fun productsWithRegistered() = listOf(
        TestFixtures.product(id = "p1", name = "Taladro Bosch"),
        TestFixtures.product(id = "p2", name = "Martillo"),
        TestFixtures.product(id = "new", name = "Sierra circular"),
    )

    @Test
    fun `view registered product scrolls to its row when it is visible`() = runTest {
        productRepository.productsResult = Result.success(productsWithRegistered())
        val viewModel = createViewModel()
        val callsAfterLoad = productRepository.callCount

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked("new"))
            assertEquals(InventoryUiEffect.ScrollToProduct(2), awaitItem())
        }
        assertEquals(callsAfterLoad, productRepository.callCount)
    }

    @Test
    fun `view registered product clears a search that hides it and scrolls without re-query`() = runTest {
        productRepository.productsResult = Result.success(productsWithRegistered())
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.Search("mar"))
        val callsAfterLoad = productRepository.callCount

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked("new"))
            assertEquals(InventoryUiEffect.ScrollToProduct(2), awaitItem())
        }
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(3, state.visibleProducts.size)
        assertEquals(callsAfterLoad, productRepository.callCount)
    }

    @Test
    fun `view registered product drops a category filter that excludes it, reloads and scrolls`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.SelectCategory("c2"))
        viewModel.onEvent(InventoryUiEvent.SelectAvailability(Availability.LOW_STOCK))
        viewModel.onEvent(InventoryUiEvent.Search("tal"))
        productRepository.productsResult = Result.success(productsWithRegistered())
        val callsBefore = productRepository.callCount

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked("new"))
            assertEquals(InventoryUiEffect.ScrollToProduct(2), awaitItem())
        }
        val state = viewModel.uiState.value
        assertNull(state.selectedCategoryId)
        assertNull(state.selectedAvailability)
        assertEquals("", state.searchQuery)
        assertNull(productRepository.lastCategoryId)
        assertNull(productRepository.lastAvailability)
        assertEquals(callsBefore + 1, productRepository.callCount)
    }

    @Test
    fun `view registered product drops an availability filter that excludes it, reloads and scrolls`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.SelectAvailability(Availability.CONSULT))
        productRepository.productsResult = Result.success(productsWithRegistered())
        val callsBefore = productRepository.callCount

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked("new"))
            assertEquals(InventoryUiEffect.ScrollToProduct(2), awaitItem())
        }
        assertNull(viewModel.uiState.value.selectedAvailability)
        assertNull(productRepository.lastAvailability)
        assertEquals(callsBefore + 1, productRepository.callCount)
    }

    @Test
    fun `view registered product says so when it is not in the unfiltered list`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        val callsAfterLoad = productRepository.callCount

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked("new"))
            assertEquals(
                InventoryUiEffect.ShowMessage(R.string.inventory_register_view_product_not_found),
                awaitItem(),
            )
        }
        assertEquals(callsAfterLoad, productRepository.callCount)
    }

    @Test
    fun `view registered product reloads only once when dropping the filters does not find it`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.SelectCategory("c2"))
        val callsBefore = productRepository.callCount

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked("new"))
            assertEquals(
                InventoryUiEffect.ShowMessage(R.string.inventory_register_view_product_not_found),
                awaitItem(),
            )
        }
        assertNull(viewModel.uiState.value.selectedCategoryId)
        assertEquals(callsBefore + 1, productRepository.callCount)
    }

    @Test
    fun `view registered product during a load scrolls once the list arrives`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        val gate = CompletableDeferred<Unit>()
        productRepository.productsGate = gate
        productRepository.productsResult = Result.success(productsWithRegistered())
        viewModel.refresh()

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked("new"))
            expectNoEvents()

            gate.complete(Unit)
            assertEquals(InventoryUiEffect.ScrollToProduct(2), awaitItem())
        }
    }

    @Test
    fun `a pending view registered product is dropped when the load fails`() = runTest {
        val viewModel = createViewModel()
        val gate = CompletableDeferred<Unit>()
        productRepository.productsGate = gate
        productRepository.productsResult = Result.failure(IOException("offline"))
        viewModel.refresh()

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked("new"))
            gate.complete(Unit)
            assertEquals(InventoryUiEffect.ShowError(R.string.inventory_error), awaitItem())

            productRepository.productsResult = Result.success(productsWithRegistered())
            viewModel.refresh()
            expectNoEvents()
        }
    }
}
