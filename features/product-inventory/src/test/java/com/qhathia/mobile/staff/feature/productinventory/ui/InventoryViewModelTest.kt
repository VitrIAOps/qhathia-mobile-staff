package com.qhathia.mobile.staff.feature.productinventory.ui

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountError
import com.qhathia.mobile.staff.core.domain.model.DiscountException
import com.qhathia.mobile.staff.core.domain.model.DiscountType
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.domain.usecase.DeleteProductUseCase
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
        deleteProductUseCase = DeleteProductUseCase(productRepository),
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
        assertEquals(listOf("get:p1", "get:p1", "create:p1", "delete:old"), discountRepository.calls)
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

    /** Opens the discount dialog for p1 and enters a 10 % discount, ready to apply. */
    private fun InventoryViewModel.withDiscountTyped() {
        onEvent(InventoryUiEvent.EditDiscountClicked("p1"))
        val draft = checkNotNull(uiState.value.discountDraft)
        onEvent(InventoryUiEvent.DiscountDraftChanged(draft.withQuickPercentage(10)))
    }

    @Test
    fun `a saved discount re-reads the products with the active filters`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1", price = 100.0)))
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.SelectCategory("c1"))
        viewModel.onEvent(InventoryUiEvent.SelectAvailability(Availability.LOW_STOCK))
        val callsBefore = productRepository.callCount
        viewModel.withDiscountTyped()
        productRepository.productsResult = Result.success(
            listOf(TestFixtures.product(id = "p1", price = 100.0, discountedPrice = 90.0)),
        )

        viewModel.onEvent(InventoryUiEvent.DiscountApplied)

        assertEquals(callsBefore + 1, productRepository.callCount)
        assertEquals("c1", productRepository.lastCategoryId)
        assertEquals(Availability.LOW_STOCK, productRepository.lastAvailability)
        // The row shows the server's discounted price, not one computed by the app.
        assertEquals(90.0, viewModel.uiState.value.visibleProducts.single().discountedPrice)
    }

    @Test
    fun `the re-read after a saved discount keeps the rows on screen`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        viewModel.withDiscountTyped()
        val gate = CompletableDeferred<Unit>()
        productRepository.productsGate = gate

        viewModel.onEvent(InventoryUiEvent.DiscountApplied)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("p1"), viewModel.uiState.value.visibleProducts.map { it.id })
        gate.complete(Unit)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `a failed re-read after a saved discount leaves the rows and shows no error`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        viewModel.withDiscountTyped()
        productRepository.productsResult = Result.failure(IOException("offline"))

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.DiscountApplied)
            assertTrue(awaitItem() is InventoryUiEffect.ShowMessage)
            expectNoEvents()
        }
        assertFalse(viewModel.uiState.value.isError)
        assertEquals(listOf("p1"), viewModel.uiState.value.visibleProducts.map { it.id })
    }

    @Test
    fun `the re-read after a saved discount keeps the search`() = runTest {
        val products = listOf(
            TestFixtures.product(id = "p1", name = "Taladro Bosch"),
            TestFixtures.product(id = "p2", name = "Martillo"),
        )
        productRepository.productsResult = Result.success(products)
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.Search("taladro"))
        viewModel.withDiscountTyped()

        viewModel.onEvent(InventoryUiEvent.DiscountApplied)

        assertEquals("taladro", viewModel.uiState.value.searchQuery)
        assertEquals(listOf("p1"), viewModel.uiState.value.visibleProducts.map { it.id })
        assertEquals(2, viewModel.uiState.value.loadedProducts.size)
    }

    @Test
    fun `a filter change during the re-read is not overwritten by it`() = runTest {
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "p1")))
        val viewModel = createViewModel()
        viewModel.withDiscountTyped()
        productRepository.productsGate = CompletableDeferred()
        viewModel.onEvent(InventoryUiEvent.DiscountApplied)

        // The filter change replaces the re-read still in flight.
        productRepository.productsGate = null
        productRepository.productsResult = Result.success(listOf(TestFixtures.product(id = "c1-product")))
        viewModel.onEvent(InventoryUiEvent.SelectCategory("c1"))

        assertEquals(listOf("c1-product"), viewModel.uiState.value.visibleProducts.map { it.id })
        assertFalse(viewModel.uiState.value.isLoading)
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

    // ---- Delete product ----

    private fun twoProducts() {
        productRepository.productsResult = Result.success(
            listOf(
                TestFixtures.product(id = "p1", name = "Taladro Bosch"),
                TestFixtures.product(id = "p2", name = "Lámpara LED"),
            ),
        )
    }

    @Test
    fun `choosing delete asks for confirmation and sends nothing`() = runTest {
        twoProducts()
        val viewModel = createViewModel()

        viewModel.onEvent(InventoryUiEvent.DeleteProductClicked("p1"))

        assertEquals("p1", viewModel.uiState.value.deleteCandidate?.id)
        assertTrue(productRepository.deletedIds.isEmpty())
    }

    @Test
    fun `dismissing the confirmation closes it without deleting`() = runTest {
        twoProducts()
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.DeleteProductClicked("p1"))

        viewModel.onEvent(InventoryUiEvent.DeleteDismissed)

        assertNull(viewModel.uiState.value.deleteCandidate)
        assertTrue(productRepository.deletedIds.isEmpty())
        assertEquals(2, viewModel.uiState.value.productCount)
    }

    @Test
    fun `confirming removes the product from the list and says so`() = runTest {
        twoProducts()
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.DeleteProductClicked("p1"))

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.DeleteConfirmed)
            assertEquals(InventoryUiEffect.ShowMessage(R.string.inventory_product_deleted), awaitItem())
        }

        val state = viewModel.uiState.value
        assertEquals(listOf("p1"), productRepository.deletedIds)
        assertEquals(listOf("p2"), state.visibleProducts.map { it.id })
        assertEquals(listOf("p2"), state.loadedProducts.map { it.id })
        assertNull(state.deleteCandidate)
        assertFalse(state.isDeleting)
        // The row is dropped locally: no reload after the initial one.
        assertEquals(1, productRepository.callCount)
    }

    @Test
    fun `while deleting the dialog stays open and cannot be dismissed`() = runTest {
        twoProducts()
        val gate = CompletableDeferred<Unit>()
        productRepository.deleteGate = gate
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.DeleteProductClicked("p1"))

        viewModel.onEvent(InventoryUiEvent.DeleteConfirmed)
        viewModel.onEvent(InventoryUiEvent.DeleteDismissed)
        viewModel.onEvent(InventoryUiEvent.DeleteConfirmed)

        assertTrue(viewModel.uiState.value.isDeleting)
        assertEquals("p1", viewModel.uiState.value.deleteCandidate?.id)
        assertEquals(listOf("p1"), productRepository.deletedIds)
        gate.complete(Unit)
    }

    @Test
    fun `a product already deleted elsewhere is removed too`() = runTest {
        twoProducts()
        productRepository.deleteResult = Result.failure(UpdateProductException(UpdateProductError.NotFound))
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.DeleteProductClicked("p1"))

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.DeleteConfirmed)
            assertEquals(
                InventoryUiEffect.ShowMessage(R.string.inventory_product_already_deleted),
                awaitItem(),
            )
        }

        assertEquals(listOf("p2"), viewModel.uiState.value.visibleProducts.map { it.id })
    }

    @Test
    fun `a failed delete keeps the product and shows the error`() = runTest {
        twoProducts()
        productRepository.deleteResult = Result.failure(UpdateProductException(UpdateProductError.Network))
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.DeleteProductClicked("p1"))

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.DeleteConfirmed)
            assertEquals(InventoryUiEffect.ShowError(R.string.inventory_delete_error), awaitItem())
        }

        val state = viewModel.uiState.value
        assertEquals(2, state.productCount)
        assertNull(state.deleteCandidate)
        assertFalse(state.isDeleting)
    }

    @Test
    fun `deleting under a search removes the product from the full list too`() = runTest {
        twoProducts()
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.Search("Taladro"))
        viewModel.onEvent(InventoryUiEvent.DeleteProductClicked("p1"))
        viewModel.onEvent(InventoryUiEvent.DeleteConfirmed)

        viewModel.onEvent(InventoryUiEvent.Search(""))

        assertEquals(listOf("p2"), viewModel.uiState.value.visibleProducts.map { it.id })
    }

    @Test
    fun `a product in pending orders keeps the confirmation with the server text`() = runTest {
        twoProducts()
        val warning = UpdateProductError.HasPendingOrders("Está en 1 pedido pendiente (#400776).", listOf("400776"))
        productRepository.deleteResults += Result.failure(UpdateProductException(warning))
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.DeleteProductClicked("p1"))

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.DeleteConfirmed)
            expectNoEvents()
        }
        val state = viewModel.uiState.value
        assertEquals("p1", state.deleteCandidate?.id)
        assertEquals(warning, state.pendingOrders)
        assertFalse(state.isDeleting)
        assertEquals(2, state.productCount)
    }

    @Test
    fun `deleting anyway forces the delete and removes the row`() = runTest {
        twoProducts()
        productRepository.deleteResults += Result.failure(
            UpdateProductException(UpdateProductError.HasPendingOrders(null, listOf("400776"))),
        )
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.DeleteProductClicked("p1"))
        viewModel.onEvent(InventoryUiEvent.DeleteConfirmed)

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.DeleteConfirmed)
            assertEquals(InventoryUiEffect.ShowMessage(R.string.inventory_product_deleted), awaitItem())
        }
        assertEquals(listOf(false, true), productRepository.deleteForces)
        assertEquals(1, viewModel.uiState.value.productCount)
        assertNull(viewModel.uiState.value.pendingOrders)
    }

    @Test
    fun `cancelling the pending-orders warning sends nothing more`() = runTest {
        twoProducts()
        productRepository.deleteResults += Result.failure(
            UpdateProductException(UpdateProductError.HasPendingOrders(null, listOf("400776"))),
        )
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.DeleteProductClicked("p1"))
        viewModel.onEvent(InventoryUiEvent.DeleteConfirmed)

        viewModel.onEvent(InventoryUiEvent.DeleteDismissed)

        assertNull(viewModel.uiState.value.deleteCandidate)
        assertNull(viewModel.uiState.value.pendingOrders)
        assertEquals(1, productRepository.deletedIds.size)
        assertEquals(2, viewModel.uiState.value.productCount)
    }

    // ---- Reload on return, pull to refresh ----

    @Test
    fun `the first screen start is the initial load and later ones reload quietly`() = runTest {
        twoProducts()
        val viewModel = createViewModel()
        val afterInit = productRepository.callCount

        viewModel.onEvent(InventoryUiEvent.ScreenStarted)
        assertEquals(afterInit, productRepository.callCount)

        viewModel.onEvent(InventoryUiEvent.ScreenStarted)
        assertEquals(afterInit + 1, productRepository.callCount)
        assertEquals(2, viewModel.uiState.value.productCount)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `a quiet reload on return that fails keeps the rows and says nothing`() = runTest {
        twoProducts()
        val viewModel = createViewModel()
        viewModel.onEvent(InventoryUiEvent.ScreenStarted)
        productRepository.productsResult = Result.failure(java.io.IOException("offline"))

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.ScreenStarted)
            expectNoEvents()
        }
        assertEquals(2, viewModel.uiState.value.productCount)
        assertFalse(viewModel.uiState.value.isError)
    }

    @Test
    fun `pulling down reloads and clears the indicator`() = runTest {
        twoProducts()
        val viewModel = createViewModel()
        val afterInit = productRepository.callCount

        viewModel.onEvent(InventoryUiEvent.PullToRefresh)

        assertEquals(afterInit + 1, productRepository.callCount)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `the indicator shows while the pull is in flight`() = runTest {
        twoProducts()
        val viewModel = createViewModel()
        productRepository.productsGate = kotlinx.coroutines.CompletableDeferred()

        viewModel.onEvent(InventoryUiEvent.PullToRefresh)
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertEquals(2, viewModel.uiState.value.productCount)

        productRepository.productsGate?.complete(Unit)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `a pull that fails keeps the rows and says so`() = runTest {
        twoProducts()
        val viewModel = createViewModel()
        productRepository.productsResult = Result.failure(java.io.IOException("offline"))

        viewModel.uiEffect.test {
            viewModel.onEvent(InventoryUiEvent.PullToRefresh)
            assertEquals(InventoryUiEffect.ShowError(R.string.inventory_error), awaitItem())
        }
        assertEquals(2, viewModel.uiState.value.productCount)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }
}
