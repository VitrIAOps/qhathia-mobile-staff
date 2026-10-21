package com.qhathia.mobile.staff.feature.productdetail.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountError
import com.qhathia.mobile.staff.core.domain.model.DiscountException
import com.qhathia.mobile.staff.core.domain.model.DiscountType
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.domain.usecase.DeleteProductUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductDetailUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductDiscountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.OptimizeDescriptionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RemoveProductDiscountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SaveProductDiscountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UpdateProductUseCase
import com.qhathia.mobile.staff.core.testing.FakeCategoryRepository
import com.qhathia.mobile.staff.core.testing.FakeProductAiRepository
import com.qhathia.mobile.staff.core.testing.FakeProductDiscountRepository
import com.qhathia.mobile.staff.core.testing.FakeProductRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.MutableClock
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiEffect
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiEvent
import com.qhathia.mobile.staff.feature.productdetail.model.toPreviewData
import com.qhathia.mobile.staff.feature.productdetail.navigation.PRODUCT_DETAIL_ARG_ID
import com.qhathia.mobile.staff.feature.productdetail.R
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

class ProductDetailViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var aiRepository: FakeProductAiRepository
    private lateinit var discountRepository: FakeProductDiscountRepository
    private val clock = MutableClock()

    @BeforeEach
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        productRepository = FakeProductRepository()
        aiRepository = FakeProductAiRepository()
        discountRepository = FakeProductDiscountRepository()
    }

    private fun createViewModel(productId: String = PRODUCT_ID) = ProductDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(PRODUCT_DETAIL_ARG_ID to productId)),
        getProductDetailUseCase = GetProductDetailUseCase(productRepository),
        getCategoriesUseCase = GetCategoriesUseCase(categoryRepository),
        updateProductUseCase = UpdateProductUseCase(productRepository),
        optimizeDescriptionUseCase = OptimizeDescriptionUseCase(aiRepository),
        getProductDiscountUseCase = GetProductDiscountUseCase(discountRepository, clock),
        saveProductDiscountUseCase = SaveProductDiscountUseCase(discountRepository, clock),
        removeProductDiscountUseCase = RemoveProductDiscountUseCase(discountRepository),
        deleteProductUseCase = DeleteProductUseCase(productRepository),
    )

    /** Opens the dialog, types [input] in [type] mode and applies it to the form. */
    private fun ProductDetailViewModel.applyDiscount(input: String, type: DiscountType = DiscountType.PERCENTAGE) {
        onEvent(ProductDetailUiEvent.DiscountToggled(true))
        val draft = checkNotNull(uiState.value.discountDraft).withType(type).withInput(input)
        onEvent(ProductDetailUiEvent.DiscountDraftChanged(draft))
        onEvent(ProductDetailUiEvent.DiscountApplied)
    }

    /** The discount requests issued after the initial load. */
    private fun discountCallsAfterLoad() = discountRepository.calls.drop(1)

    @Test
    fun `on init loads the product for the route id and seeds the form`() = runTest {
        productRepository.productDetailResult = Result.success(
            TestFixtures.productDetail(
                name = "Taladro",
                price = 189.90,
                stock = 7,
                description = "Percutor 650W",
                categoryId = "category-1",
                tags = listOf("herramientas", "bosch"),
                imageUrls = listOf("https://cdn.test/1.jpg"),
            ),
        )

        val state = createViewModel().uiState.value

        assertEquals(PRODUCT_ID, productRepository.lastRequestedId)
        assertFalse(state.isLoading)
        assertFalse(state.isError)
        assertEquals("Taladro", state.name)
        assertEquals("189.9", state.price)
        assertEquals("7", state.quantity)
        assertEquals("Percutor 650W", state.description)
        assertEquals("herramientas, bosch", state.tags)
        assertEquals("category-1", state.selectedCategoryId)
        assertEquals(listOf("https://cdn.test/1.jpg"), state.imageUrls)
        // A freshly loaded product has nothing to save yet.
        assertFalse(state.isDirty)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun `on init also loads the category catalog`() = runTest {
        val state = createViewModel().uiState.value

        assertEquals(1, state.categories.size)
        assertFalse(state.isCategoriesLoading)
        assertFalse(state.isCategoriesError)
    }

    @Test
    fun `a whole price renders without a trailing decimal`() = runTest {
        productRepository.productDetailResult = Result.success(TestFixtures.productDetail(price = 189.0))

        assertEquals("189", createViewModel().uiState.value.price)
    }

    @Test
    fun `a failed load surfaces the error state and retry reloads`() = runTest {
        productRepository.productDetailResult =
            Result.failure(UpdateProductException(UpdateProductError.Network))
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isError)
        assertFalse(viewModel.uiState.value.isLoading)

        productRepository.productDetailResult = Result.success(TestFixtures.productDetail())
        viewModel.onEvent(ProductDetailUiEvent.Retry)

        assertFalse(viewModel.uiState.value.isError)
        assertEquals(2, productRepository.detailCallCount)
    }

    @Test
    fun `an invalid price blocks saving and surfaces a field error`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.PriceChanged("0"))

        assertNotNull(viewModel.uiState.value.priceError)
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun `save sends every field including the ones the form does not expose`() = runTest {
        productRepository.productDetailResult = Result.success(
            TestFixtures.productDetail(allowDiscountInquiry = true, position = 4),
        )
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Taladro Pro"))
        viewModel.onEvent(ProductDetailUiEvent.PriceChanged("199.90"))
        viewModel.onEvent(ProductDetailUiEvent.QuantityChanged("3"))

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.Save)
            assertEquals(ProductDetailUiEffect.NavigateBackSaved, awaitItem())
        }

        assertEquals(PRODUCT_ID, productRepository.updatedId)
        val sent = productRepository.updatedProduct
        assertNotNull(sent)
        assertEquals("Taladro Pro", sent!!.name)
        assertEquals(199.90, sent.price)
        assertEquals(3, sent.stock)
        assertEquals(listOf("herramientas", "bosch"), sent.tags)
        // Carried over from the loaded product — PUT is a full replace.
        assertTrue(sent.allowDiscountInquiry)
        assertEquals(4, sent.position)
    }

    @Test
    fun `a failed save keeps the form open and shows the mapped error`() = runTest {
        productRepository.updateResult =
            Result.failure(UpdateProductException(UpdateProductError.NotFound))
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Taladro Pro"))

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.Save)
            assertTrue(awaitItem() is ProductDetailUiEffect.ShowError)
        }

        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `cancelling a pristine form navigates back without the discard dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.CancelRequested)
            assertEquals(ProductDetailUiEffect.NavigateBackCancelled, awaitItem())
        }
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `cancelling a dirty form opens the discard dialog first`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Otro nombre"))

        viewModel.onEvent(ProductDetailUiEvent.CancelRequested)

        assertTrue(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `optimizing rewrites the description`() = runTest {
        aiRepository.optimizeResult = Result.success("Descripción optimizada")
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.OptimizeDescriptionWithAi)

        assertEquals("Descripción optimizada", viewModel.uiState.value.description)
        assertFalse(viewModel.uiState.value.isAiLoading)
    }

    @Test
    fun `optimizing is a no-op without a description`() = runTest {
        productRepository.productDetailResult = Result.success(TestFixtures.productDetail(description = null))
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.OptimizeDescriptionWithAi)

        assertNull(viewModel.uiState.value.description.ifEmpty { null })
        assertFalse(viewModel.uiState.value.isAiActionEnabled)
    }


    @Test
    fun `on init seeds the discount the product currently has`() = runTest {
        discountRepository.discounts += TestFixtures.productDiscount(
            discount = Discount(DiscountType.PERCENTAGE, 20.0),
        )

        val state = createViewModel().uiState.value

        assertEquals(Discount(DiscountType.PERCENTAGE, 20.0), state.discount)
        assertFalse(state.isDiscountChanged)
        assertTrue(state.isDiscountToggleEnabled)
        assertFalse(state.isDirty)
    }

    @Test
    fun `a product without a discount loads with the toggle off`() = runTest {
        val state = createViewModel().uiState.value

        assertNull(state.discount)
        assertFalse(state.isDiscountError)
    }

    @Test
    fun `a discount that fails to load does not block the form and can be retried`() = runTest {
        discountRepository.getFailure = DiscountException(DiscountError.Network)
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isError)
        assertTrue(viewModel.uiState.value.isDiscountError)
        assertFalse(viewModel.uiState.value.isDiscountToggleEnabled)

        discountRepository.getFailure = null
        discountRepository.discounts += TestFixtures.productDiscount()
        viewModel.onEvent(ProductDetailUiEvent.RetryDiscount)

        assertFalse(viewModel.uiState.value.isDiscountError)
        assertNotNull(viewModel.uiState.value.discount)
    }

    @Test
    fun `applying a discount only changes the form`() = runTest {
        val viewModel = createViewModel()

        viewModel.applyDiscount("20")

        val state = viewModel.uiState.value
        assertEquals(Discount(DiscountType.PERCENTAGE, 20.0), state.discount)
        assertNull(state.discountDraft)
        assertTrue(state.isDirty)
        assertTrue(state.isSaveEnabled)
        assertTrue(discountCallsAfterLoad().isEmpty())
    }

    @Test
    fun `the dialog opens prefilled with the discount on the form and previews the form price`() = runTest {
        discountRepository.discounts += TestFixtures.productDiscount(
            discount = Discount(DiscountType.FIXED_AMOUNT, 15.0),
        )
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.PriceChanged("200"))

        viewModel.onEvent(ProductDetailUiEvent.DiscountSummaryClicked)

        val draft = viewModel.uiState.value.discountDraft
        assertEquals(DiscountType.FIXED_AMOUNT, draft?.type)
        assertEquals("15", draft?.input)
        assertEquals(185.0, draft?.resultingPrice)
    }

    @Test
    fun `turning the toggle off only changes the form`() = runTest {
        discountRepository.discounts += TestFixtures.productDiscount()
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.DiscountToggled(false))

        assertNull(viewModel.uiState.value.discount)
        assertTrue(viewModel.uiState.value.isDirty)
        assertTrue(discountCallsAfterLoad().isEmpty())
        assertEquals(1, discountRepository.discounts.size)
    }

    @Test
    fun `dismissing the dialog leaves the form as it was`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.DiscountToggled(true))

        viewModel.onEvent(ProductDetailUiEvent.DiscountDismissed)

        assertNull(viewModel.uiState.value.discountDraft)
        assertNull(viewModel.uiState.value.discount)
        assertFalse(viewModel.uiState.value.isDirty)
    }

    @Test
    fun `a price change that no longer fits the discount removes it from the form`() = runTest {
        discountRepository.discounts += TestFixtures.productDiscount(
            discount = Discount(DiscountType.FIXED_AMOUNT, 100.0),
        )
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.PriceChanged("80"))

        assertNull(viewModel.uiState.value.discount)
        assertTrue(viewModel.uiState.value.isDiscountChanged)
    }

    @Test
    fun `saving a changed discount replaces it after the product update`() = runTest {
        discountRepository.discounts += TestFixtures.productDiscount(id = "old")
        val viewModel = createViewModel()
        viewModel.applyDiscount("50")

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.Save)
            assertEquals(ProductDetailUiEffect.NavigateBackSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, productRepository.updateCallCount)
        assertEquals(
            listOf("get:$PRODUCT_ID", "delete:old", "create:$PRODUCT_ID"),
            discountCallsAfterLoad(),
        )
        assertEquals(Discount(DiscountType.PERCENTAGE, 50.0), discountRepository.discounts.single().discount)
    }

    @Test
    fun `saving with the discount turned off removes it`() = runTest {
        discountRepository.discounts += TestFixtures.productDiscount(id = "old")
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.DiscountToggled(false))

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.Save)
            assertEquals(ProductDetailUiEffect.NavigateBackSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("get:$PRODUCT_ID", "delete:old"), discountCallsAfterLoad())
        assertTrue(discountRepository.discounts.isEmpty())
    }

    @Test
    fun `saving without touching the discount issues no discount request`() = runTest {
        discountRepository.discounts += TestFixtures.productDiscount()
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Taladro nuevo"))

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.Save)
            assertEquals(ProductDetailUiEffect.NavigateBackSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(discountCallsAfterLoad().isEmpty())
    }

    @Test
    fun `a failed product update issues no discount request and keeps the pending discount`() = runTest {
        productRepository.updateResult = Result.failure(UpdateProductException(UpdateProductError.Network))
        val viewModel = createViewModel()
        viewModel.applyDiscount("20")

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.Save)
            assertTrue(awaitItem() is ProductDetailUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(discountCallsAfterLoad().isEmpty())
        assertEquals(Discount(DiscountType.PERCENTAGE, 20.0), viewModel.uiState.value.discount)
    }

    @Test
    fun `a discount that fails after the product is saved keeps the form dirty for a retry`() = runTest {
        val viewModel = createViewModel()
        viewModel.applyDiscount("20")
        discountRepository.createFailure = DiscountException(DiscountError.Network)

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.Save)
            assertTrue(awaitItem() is ProductDetailUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
        val failed = viewModel.uiState.value
        assertFalse(failed.isSaving)
        assertTrue(failed.isDirty)
        assertTrue(failed.isSaveEnabled)

        discountRepository.createFailure = null
        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.Save)
            assertEquals(ProductDetailUiEffect.NavigateBackSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, productRepository.updateCallCount)
        assertEquals(1, discountRepository.discounts.size)
    }

    @Test
    fun `saving does not touch discounts when they could not be loaded`() = runTest {
        discountRepository.getFailure = DiscountException(DiscountError.Network)
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Taladro nuevo"))

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.Save)
            assertEquals(ProductDetailUiEffect.NavigateBackSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(discountCallsAfterLoad().isEmpty())
    }

    @Test
    fun `leaving with only a discount change asks to discard and discarding sends nothing`() = runTest {
        discountRepository.discounts += TestFixtures.productDiscount()
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.DiscountToggled(false))

        viewModel.onEvent(ProductDetailUiEvent.CancelRequested)
        assertTrue(viewModel.uiState.value.showDiscardDialog)

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.ConfirmDiscard)
            assertEquals(ProductDetailUiEffect.NavigateBackCancelled, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(discountCallsAfterLoad().isEmpty())
        assertEquals(1, discountRepository.discounts.size)
    }

    @Test
    fun `the final price is the form price with the discount applied`() = runTest {
        discountRepository.discounts += TestFixtures.productDiscount(
            discount = Discount(DiscountType.FIXED_AMOUNT, 15.0),
        )
        val viewModel = createViewModel()
        assertEquals(174.0, viewModel.uiState.value.discountedPrice)

        viewModel.onEvent(ProductDetailUiEvent.PriceChanged("100"))
        assertEquals(85.0, viewModel.uiState.value.discountedPrice)

        viewModel.onEvent(ProductDetailUiEvent.DiscountToggled(false))
        assertNull(viewModel.uiState.value.discountedPrice)
    }

    @Test
    fun `tags are optional and clearing them saves the product with none`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.TagsChanged(""))
        assertTrue(viewModel.uiState.value.isSaveEnabled)

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.Save)
            assertEquals(ProductDetailUiEffect.NavigateBackSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(emptyList<String>(), productRepository.updatedProduct?.tags)
    }

    @Test
    fun `preview is available for a loaded product with a photo`() = runTest {
        assertTrue(createViewModel().uiState.value.isPreviewEnabled)
    }

    @Test
    fun `preview is unavailable for a product with no photos`() = runTest {
        productRepository.productDetailResult =
            Result.success(TestFixtures.productDetail(imageUrls = emptyList()))

        assertFalse(createViewModel().uiState.value.isPreviewEnabled)
    }

    @Test
    fun `preview is unavailable when the product failed to load`() = runTest {
        productRepository.productDetailResult =
            Result.failure(UpdateProductException(UpdateProductError.Network))

        assertFalse(createViewModel().uiState.value.isPreviewEnabled)
    }

    @Test
    fun `preview is unavailable once the name or the price becomes invalid`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.NameChanged(""))
        assertFalse(viewModel.uiState.value.isPreviewEnabled)
        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Taladro"))
        assertTrue(viewModel.uiState.value.isPreviewEnabled)

        viewModel.onEvent(ProductDetailUiEvent.PriceChanged("0"))
        assertFalse(viewModel.uiState.value.isPreviewEnabled)
    }

    @Test
    fun `preview is unavailable while loading or saving`() = runTest {
        val loaded = createViewModel().uiState.value

        assertFalse(loaded.copy(isLoading = true).isPreviewEnabled)
        assertFalse(loaded.copy(isSaving = true).isPreviewEnabled)
    }

    @Test
    fun `requesting the preview opens it and dismissing closes it`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.PreviewRequested)
        assertTrue(viewModel.uiState.value.showPreview)

        viewModel.onEvent(ProductDetailUiEvent.PreviewDismissed)
        assertFalse(viewModel.uiState.value.showPreview)
    }

    @Test
    fun `requesting the preview without a photo does nothing`() = runTest {
        productRepository.productDetailResult =
            Result.success(TestFixtures.productDetail(imageUrls = emptyList()))
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.PreviewRequested)

        assertFalse(viewModel.uiState.value.showPreview)
    }

    @Test
    fun `the preview leaves a pristine form pristine and saves nothing`() = runTest {
        val viewModel = createViewModel()
        val before = viewModel.uiState.value

        viewModel.onEvent(ProductDetailUiEvent.PreviewRequested)
        viewModel.onEvent(ProductDetailUiEvent.PreviewDismissed)

        val after = viewModel.uiState.value
        assertEquals(before, after)
        assertFalse(after.isDirty)
        assertFalse(after.isSaveEnabled)
        assertEquals(0, productRepository.updateCallCount)
        assertTrue(discountCallsAfterLoad().isEmpty())
    }

    @Test
    fun `preview data reflects edits that have not been saved`() = runTest {
        productRepository.productDetailResult = Result.success(
            TestFixtures.productDetail(
                name = "Taladro",
                price = 189.0,
                stock = 7,
                description = "Percutor 650W",
                categoryId = "category-1",
                imageUrls = listOf("https://cdn.test/1.jpg", "https://cdn.test/2.jpg"),
            ),
        )
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Taladro percutor"))
        viewModel.onEvent(ProductDetailUiEvent.PriceChanged("210.50"))
        val data = checkNotNull(viewModel.uiState.value.toPreviewData())

        assertEquals("Taladro percutor", data.name)
        assertEquals(210.5, data.price)
        assertEquals("Percutor 650W", data.description)
        assertEquals(TestFixtures.category().name, data.categoryName)
        assertEquals(7, data.stock)
        assertEquals(listOf("https://cdn.test/1.jpg", "https://cdn.test/2.jpg"), data.imageModels)
        assertEquals(0, productRepository.updateCallCount)
    }

    @Test
    fun `preview data leaves out what the form does not hold`() = runTest {
        productRepository.productDetailResult = Result.success(
            TestFixtures.productDetail(description = null, stock = null, categoryId = null),
        )

        val data = checkNotNull(createViewModel().uiState.value.toPreviewData())

        assertNull(data.description)
        assertNull(data.categoryName)
        assertNull(data.stock)
    }

    @Test
    fun `preview data shows the price with the form's discount applied`() = runTest {
        productRepository.productDetailResult = Result.success(TestFixtures.productDetail(price = 189.0))
        val viewModel = createViewModel()

        viewModel.applyDiscount("20")

        assertEquals(151.2, viewModel.uiState.value.toPreviewData()?.price)
    }

    @Test
    fun `there is no preview data without a valid price`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.PriceChanged(""))

        assertNull(viewModel.uiState.value.toPreviewData())
    }

    private companion object {
        const val PRODUCT_ID = "product-42"
    }

    @Test
    fun `the bell on a pristine form leaves for Solicitudes without the dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.OrderRequestsRequested)
            assertEquals(ProductDetailUiEffect.NavigateToOrderRequests, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `the bell on a dirty form asks first and confirming leaves for Solicitudes`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Otro nombre"))

        viewModel.onEvent(ProductDetailUiEvent.OrderRequestsRequested)
        assertTrue(viewModel.uiState.value.showDiscardDialog)

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.ConfirmDiscard)
            assertEquals(ProductDetailUiEffect.NavigateToOrderRequests, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `dismissing the dialog after the bell keeps the form and later cancels go back`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Otro nombre"))

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.OrderRequestsRequested)
            viewModel.onEvent(ProductDetailUiEvent.DismissDiscard)
            expectNoEvents()
            assertEquals("Otro nombre", viewModel.uiState.value.name)

            viewModel.onEvent(ProductDetailUiEvent.CancelRequested)
            viewModel.onEvent(ProductDetailUiEvent.ConfirmDiscard)
            assertEquals(ProductDetailUiEffect.NavigateBackCancelled, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- Delete product ----

    @Test
    fun `asking to delete opens the confirmation and sends nothing`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.DeleteRequested)

        assertTrue(viewModel.uiState.value.showDeleteDialog)
        assertTrue(productRepository.deletedIds.isEmpty())
    }

    @Test
    fun `dismissing the confirmation keeps the form`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Taladro Pro"))
        viewModel.onEvent(ProductDetailUiEvent.DeleteRequested)

        viewModel.onEvent(ProductDetailUiEvent.DeleteDismissed)

        assertFalse(viewModel.uiState.value.showDeleteDialog)
        assertEquals("Taladro Pro", viewModel.uiState.value.name)
        assertTrue(productRepository.deletedIds.isEmpty())
    }

    @Test
    fun `confirming deletes the product and leaves, even with unsaved edits`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Taladro Pro"))
        viewModel.onEvent(ProductDetailUiEvent.DeleteRequested)

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.DeleteConfirmed)
            assertEquals(ProductDetailUiEffect.NavigateBackDeleted(alreadyDeleted = false), awaitItem())
        }

        assertEquals(listOf(PRODUCT_ID), productRepository.deletedIds)
        assertEquals(0, productRepository.updateCallCount)
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `a product already deleted elsewhere also leaves`() = runTest {
        productRepository.deleteResult = Result.failure(UpdateProductException(UpdateProductError.NotFound))
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.DeleteRequested)

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.DeleteConfirmed)
            assertEquals(ProductDetailUiEffect.NavigateBackDeleted(alreadyDeleted = true), awaitItem())
        }
    }

    @Test
    fun `a failed delete stays on the form and shows the error`() = runTest {
        productRepository.deleteResult = Result.failure(UpdateProductException(UpdateProductError.Network))
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Taladro Pro"))
        viewModel.onEvent(ProductDetailUiEvent.DeleteRequested)

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.DeleteConfirmed)
            assertEquals(ProductDetailUiEffect.ShowError(R.string.detail_delete_error), awaitItem())
        }

        val state = viewModel.uiState.value
        assertFalse(state.isDeleting)
        assertFalse(state.showDeleteDialog)
        assertEquals("Taladro Pro", state.name)
    }

    @Test
    fun `while deleting the form cannot be left, saved or dismissed`() = runTest {
        val gate = CompletableDeferred<Unit>()
        productRepository.deleteGate = gate
        val viewModel = createViewModel()
        viewModel.onEvent(ProductDetailUiEvent.NameChanged("Taladro Pro"))
        viewModel.onEvent(ProductDetailUiEvent.DeleteRequested)
        viewModel.onEvent(ProductDetailUiEvent.DeleteConfirmed)

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductDetailUiEvent.DeleteDismissed)
            viewModel.onEvent(ProductDetailUiEvent.CancelRequested)
            viewModel.onEvent(ProductDetailUiEvent.OrderRequestsRequested)
            viewModel.onEvent(ProductDetailUiEvent.DeleteConfirmed)
            expectNoEvents()
        }

        val state = viewModel.uiState.value
        assertTrue(state.isDeleting)
        assertTrue(state.showDeleteDialog)
        assertFalse(state.showDiscardDialog)
        assertFalse(state.isSaveEnabled)
        assertEquals(listOf(PRODUCT_ID), productRepository.deletedIds)
        gate.complete(Unit)
    }

    @Test
    fun `delete is not offered while the product is still loading`() = runTest {
        productRepository.productDetailResult = Result.failure(UpdateProductException(UpdateProductError.Network))
        val viewModel = createViewModel()

        viewModel.onEvent(ProductDetailUiEvent.DeleteRequested)

        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }
}
