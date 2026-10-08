package com.qhathia.mobile.staff.feature.productregister.ui

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountError
import com.qhathia.mobile.staff.core.domain.model.DiscountException
import com.qhathia.mobile.staff.core.domain.model.DiscountType
import com.qhathia.mobile.staff.core.domain.model.ProductDraft
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.usecase.DraftProductFromImageUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.OptimizeDescriptionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RegisterProductUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SaveProductDiscountUseCase
import com.qhathia.mobile.staff.core.testing.FakeCategoryRepository
import com.qhathia.mobile.staff.core.testing.FakeProductAiRepository
import com.qhathia.mobile.staff.core.testing.FakeProductDiscountRepository
import com.qhathia.mobile.staff.core.testing.FakeProductRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.MutableClock
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiEffect
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiEvent
import com.qhathia.mobile.staff.feature.productregister.model.toPreviewData
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ProductRegisterViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private lateinit var categoryRepository: FakeCategoryRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var aiRepository: FakeProductAiRepository
    private lateinit var discountRepository: FakeProductDiscountRepository

    @BeforeEach
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        productRepository = FakeProductRepository()
        aiRepository = FakeProductAiRepository()
        discountRepository = FakeProductDiscountRepository()
    }

    private fun createViewModel() = ProductRegisterViewModel(
        getCategoriesUseCase = GetCategoriesUseCase(categoryRepository),
        registerProductUseCase = RegisterProductUseCase(productRepository),
        draftProductFromImageUseCase = DraftProductFromImageUseCase(aiRepository),
        optimizeDescriptionUseCase = OptimizeDescriptionUseCase(aiRepository),
        saveProductDiscountUseCase = SaveProductDiscountUseCase(discountRepository, MutableClock()),
    )

    /** Turns the toggle on, types [input] in the dialog's current mode and applies it. */
    private fun ProductRegisterViewModel.applyDiscount(input: String, type: DiscountType = DiscountType.PERCENTAGE) {
        onEvent(ProductRegisterUiEvent.DiscountToggled(true))
        val draft = checkNotNull(uiState.value.discountDraft).withType(type).withInput(input)
        onEvent(ProductRegisterUiEvent.DiscountDraftChanged(draft))
        onEvent(ProductRegisterUiEvent.DiscountApplied)
    }

    private fun ProductRegisterViewModel.fillValidForm() {
        onEvent(ProductRegisterUiEvent.NameChanged("Lámpara"))
        onEvent(ProductRegisterUiEvent.PriceChanged("89.90"))
        onEvent(ProductRegisterUiEvent.QuantityChanged("10"))
        onEvent(ProductRegisterUiEvent.SelectCategory("category-1"))
        onEvent(ProductRegisterUiEvent.TagsChanged("importado, premium"))
        onEvent(ProductRegisterUiEvent.ImagesPicked(listOf(SelectedImage("content://1", "image/jpeg"))))
    }

    @Test
    fun `on init loads categories`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(1, state.categories.size)
        assertFalse(state.isCategoriesLoading)
    }

    @Test
    fun `submit gate is disabled until all fields are valid`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isSubmitEnabled)

        viewModel.fillValidForm()

        assertTrue(viewModel.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `tags are optional and a blank field registers the product with none`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.onEvent(ProductRegisterUiEvent.TagsChanged("   ,  "))
        assertTrue(viewModel.uiState.value.isSubmitEnabled)

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.Submit)
            assertEquals(ProductRegisterUiEffect.NavigateBackWithSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(emptyList<String>(), productRepository.registeredProduct?.tags)
    }

    @Test
    fun `negative quantity sets error and blocks submit`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.onEvent(ProductRegisterUiEvent.QuantityChanged("-3"))

        assertNotNull(viewModel.uiState.value.quantityError)
        assertFalse(viewModel.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `successful submit sends parsed tags and emits NavigateBackWithSuccess`() = runTest {
        productRepository.registerResult = Result.success("p1")
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.Submit)
            assertEquals(ProductRegisterUiEffect.NavigateBackWithSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("importado", "premium"), productRepository.registeredProduct?.tags)
    }

    @Test
    fun `plan limit submit emits NavigateBackWithPlanLimit`() = runTest {
        productRepository.registerResult =
            Result.failure(RegisterProductException(RegisterProductError.PlanLimitReached))
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.Submit)
            assertEquals(ProductRegisterUiEffect.NavigateBackWithPlanLimit, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `generic error keeps the form and emits ShowError`() = runTest {
        productRepository.registerResult =
            Result.failure(RegisterProductException(RegisterProductError.Network))
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.Submit)
            assertTrue(awaitItem() is ProductRegisterUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `dirty cancel shows discard dialog then confirm navigates back`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.NameChanged("x"))

        viewModel.onEvent(ProductRegisterUiEvent.CancelRequested)
        assertTrue(viewModel.uiState.value.showDiscardDialog)

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.ConfirmDiscard)
            assertEquals(ProductRegisterUiEffect.NavigateBackCancelled, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `clean cancel navigates back without dialog`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.CancelRequested)
            assertEquals(ProductRegisterUiEffect.NavigateBackCancelled, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `unsupported image format emits ShowError and is not added`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(
                ProductRegisterUiEvent.ImagesPicked(listOf(SelectedImage("content://x", "image/gif"))),
            )
            assertTrue(awaitItem() is ProductRegisterUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(viewModel.uiState.value.images.isEmpty())
    }

    @Test
    fun `generate with ai on empty form populates fields from the draft`() = runTest {
        aiRepository.draftResult = Result.success(
            ProductDraft(description = "Foco LED", categoryId = "category-1", tags = listOf("foco", "led")),
        )
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.ImagesPicked(listOf(SelectedImage("content://1", "image/jpeg"))))

        viewModel.onEvent(ProductRegisterUiEvent.GenerateDescriptionWithAi)

        val state = viewModel.uiState.value
        assertEquals("Foco LED", state.description)
        assertEquals("category-1", state.selectedCategoryId)
        assertEquals("foco, led", state.tags)
        assertFalse(state.showAiReplaceDialog)
    }

    @Test
    fun `draft category that is not loaded is not applied`() = runTest {
        aiRepository.draftResult = Result.success(
            ProductDraft(description = "Foco LED", categoryId = "unknown-cat", tags = listOf("foco")),
        )
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.ImagesPicked(listOf(SelectedImage("content://1", "image/jpeg"))))

        viewModel.onEvent(ProductRegisterUiEvent.GenerateDescriptionWithAi)

        val state = viewModel.uiState.value
        assertEquals("Foco LED", state.description)
        assertNull(state.selectedCategoryId)
    }

    @Test
    fun `empty draft leaves the form unchanged`() = runTest {
        aiRepository.draftResult =
            Result.success(ProductDraft(description = "", categoryId = null, tags = emptyList()))
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.ImagesPicked(listOf(SelectedImage("content://1", "image/jpeg"))))

        viewModel.onEvent(ProductRegisterUiEvent.GenerateDescriptionWithAi)

        val state = viewModel.uiState.value
        assertEquals("", state.description)
        assertNull(state.selectedCategoryId)
        assertEquals("", state.tags)
    }

    @Test
    fun `generate with existing data shows replace dialog before drafting`() = runTest {
        aiRepository.draftResult = Result.success(
            ProductDraft(description = "Nuevo", categoryId = "category-1", tags = listOf("a")),
        )
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.ImagesPicked(listOf(SelectedImage("content://1", "image/jpeg"))))
        viewModel.onEvent(ProductRegisterUiEvent.DescriptionChanged("Mi texto"))

        viewModel.onEvent(ProductRegisterUiEvent.GenerateDescriptionWithAi)

        assertTrue(viewModel.uiState.value.showAiReplaceDialog)
        assertEquals(0, aiRepository.draftCallCount)
        assertEquals("Mi texto", viewModel.uiState.value.description)

        viewModel.onEvent(ProductRegisterUiEvent.ConfirmAiReplace)

        assertFalse(viewModel.uiState.value.showAiReplaceDialog)
        assertEquals("Nuevo", viewModel.uiState.value.description)
    }

    @Test
    fun `dismiss replace dialog keeps existing data and does not draft`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.ImagesPicked(listOf(SelectedImage("content://1", "image/jpeg"))))
        viewModel.onEvent(ProductRegisterUiEvent.DescriptionChanged("Mi texto"))
        viewModel.onEvent(ProductRegisterUiEvent.GenerateDescriptionWithAi)
        assertTrue(viewModel.uiState.value.showAiReplaceDialog)

        viewModel.onEvent(ProductRegisterUiEvent.DismissAiReplace)

        assertFalse(viewModel.uiState.value.showAiReplaceDialog)
        assertEquals(0, aiRepository.draftCallCount)
        assertEquals("Mi texto", viewModel.uiState.value.description)
    }

    @Test
    fun `optimize replaces only the description when all ai fields are filled`() = runTest {
        aiRepository.optimizeResult = Result.success("Descripción optimizada")
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.onEvent(ProductRegisterUiEvent.DescriptionChanged("Texto original"))
        val before = viewModel.uiState.value
        assertTrue(before.isOptimizeMode)

        viewModel.onEvent(ProductRegisterUiEvent.GenerateDescriptionWithAi)

        val state = viewModel.uiState.value
        assertEquals("Descripción optimizada", state.description)
        assertEquals(before.selectedCategoryId, state.selectedCategoryId)
        assertEquals(before.tags, state.tags)
        assertEquals(0, aiRepository.draftCallCount)
        assertEquals(1, aiRepository.optimizeCallCount)
    }

    @Test
    fun `ai failure emits ShowError and leaves fields unchanged`() = runTest {
        aiRepository.draftResult = Result.failure(IllegalStateException("boom"))
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.ImagesPicked(listOf(SelectedImage("content://1", "image/jpeg"))))

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.GenerateDescriptionWithAi)
            assertTrue(awaitItem() is ProductRegisterUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
        val state = viewModel.uiState.value
        assertEquals("", state.description)
        assertFalse(state.isAiLoading)
    }

    @Test
    fun `catalog retry clears the category error`() = runTest {
        categoryRepository.categoriesResult = Result.failure(IllegalStateException("boom"))
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isCategoriesError)

        categoryRepository.categoriesResult = Result.success(listOf(TestFixtures.category()))
        viewModel.onEvent(ProductRegisterUiEvent.RetryCatalog)

        assertFalse(viewModel.uiState.value.isCategoriesError)
    }

    @Test
    fun `discount toggle is locked until a valid price is entered`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isDiscountToggleEnabled)

        viewModel.onEvent(ProductRegisterUiEvent.DiscountToggled(true))
        assertNull(viewModel.uiState.value.discountDraft)

        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("189"))
        assertTrue(viewModel.uiState.value.isDiscountToggleEnabled)
    }

    @Test
    fun `turning the toggle on opens the dialog for the entered price`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("189"))

        viewModel.onEvent(ProductRegisterUiEvent.DiscountToggled(true))

        val draft = viewModel.uiState.value.discountDraft
        assertEquals(189.0, draft?.price)
        assertEquals("", draft?.input)
        assertNull(viewModel.uiState.value.discount)
    }

    @Test
    fun `applying keeps the discount on the form and closes the dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("189"))

        viewModel.applyDiscount("20")

        val state = viewModel.uiState.value
        assertEquals(Discount(DiscountType.PERCENTAGE, 20.0), state.discount)
        assertNull(state.discountDraft)
        assertTrue(state.isDirty)
        assertTrue(discountRepository.calls.isEmpty())
    }

    @Test
    fun `an invalid value cannot be applied`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("189"))

        viewModel.applyDiscount("101")

        assertNull(viewModel.uiState.value.discount)
        assertNotNull(viewModel.uiState.value.discountDraft)
    }

    @Test
    fun `dismissing the dialog without a discount leaves the toggle off`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("189"))
        viewModel.onEvent(ProductRegisterUiEvent.DiscountToggled(true))

        viewModel.onEvent(ProductRegisterUiEvent.DiscountDismissed)

        assertNull(viewModel.uiState.value.discountDraft)
        assertNull(viewModel.uiState.value.discount)
    }

    @Test
    fun `tapping the summary reopens the dialog prefilled and dismissing keeps the discount`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("189"))
        viewModel.applyDiscount("15.50", DiscountType.FIXED_AMOUNT)

        viewModel.onEvent(ProductRegisterUiEvent.DiscountSummaryClicked)
        val draft = viewModel.uiState.value.discountDraft
        assertEquals(DiscountType.FIXED_AMOUNT, draft?.type)
        assertEquals("15.50", draft?.input)

        viewModel.onEvent(ProductRegisterUiEvent.DiscountDismissed)
        assertEquals(Discount(DiscountType.FIXED_AMOUNT, 15.5), viewModel.uiState.value.discount)
    }

    @Test
    fun `turning the toggle off removes the discount`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("189"))
        viewModel.applyDiscount("20")

        viewModel.onEvent(ProductRegisterUiEvent.DiscountToggled(false))

        assertNull(viewModel.uiState.value.discount)
    }

    @Test
    fun `a price change that no longer fits the discount removes it`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("189"))
        viewModel.applyDiscount("100", DiscountType.FIXED_AMOUNT)

        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("150"))
        assertNotNull(viewModel.uiState.value.discount)

        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("99"))
        assertNull(viewModel.uiState.value.discount)
    }

    @Test
    fun `clearing the price removes the discount and locks the toggle`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("189"))
        viewModel.applyDiscount("20")

        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged(""))

        assertNull(viewModel.uiState.value.discount)
        assertFalse(viewModel.uiState.value.isDiscountToggleEnabled)
    }

    @Test
    fun `submitting with a discount creates it for the new product after registering`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.applyDiscount("20")

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.Submit)
            assertEquals(ProductRegisterUiEffect.NavigateBackWithSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("get:new-product-1", "create:new-product-1"), discountRepository.calls)
        assertEquals(Discount(DiscountType.PERCENTAGE, 20.0), discountRepository.lastCreated?.discount)
    }

    @Test
    fun `submitting without a discount issues no discount request`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.Submit)
            assertEquals(ProductRegisterUiEffect.NavigateBackWithSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(discountRepository.calls.isEmpty())
    }

    @Test
    fun `a discount that fails after registering returns with the discount-failed result`() = runTest {
        discountRepository.createFailure = DiscountException(DiscountError.Network)
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.applyDiscount("20")

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.Submit)
            assertEquals(ProductRegisterUiEffect.NavigateBackWithDiscountFailed, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, productRepository.registerCallCount)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `a failed registration issues no discount request and keeps the discount on the form`() = runTest {
        productRepository.registerResult =
            Result.failure(RegisterProductException(RegisterProductError.Network))
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.applyDiscount("20")

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.Submit)
            assertTrue(awaitItem() is ProductRegisterUiEffect.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(discountRepository.calls.isEmpty())
        assertNotNull(viewModel.uiState.value.discount)
    }

    @Test
    fun `the final price is the entered price with the discount applied and follows the price`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("189"))
        assertNull(viewModel.uiState.value.discountedPrice)

        viewModel.applyDiscount("20")
        assertEquals(151.2, viewModel.uiState.value.discountedPrice)

        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("200"))
        assertEquals(160.0, viewModel.uiState.value.discountedPrice)
    }

    /** The least a preview needs: a name, a price and a photo — nothing else on the form. */
    private fun ProductRegisterViewModel.fillPreviewMinimum() {
        onEvent(ProductRegisterUiEvent.NameChanged("Lámpara"))
        onEvent(ProductRegisterUiEvent.PriceChanged("89.90"))
        onEvent(ProductRegisterUiEvent.ImagesPicked(listOf(SelectedImage("content://1", "image/jpeg"))))
    }

    @Test
    fun `preview is unavailable on an empty form`() = runTest {
        assertFalse(createViewModel().uiState.value.isPreviewEnabled)
    }

    @Test
    fun `preview needs only a name, a price and a photo`() = runTest {
        val viewModel = createViewModel()

        viewModel.fillPreviewMinimum()

        val state = viewModel.uiState.value
        assertTrue(state.isPreviewEnabled)
        // No category, stock or tags yet: the product can be previewed before it can be registered.
        assertFalse(state.isSubmitEnabled)
    }

    @Test
    fun `preview is unavailable without a name, a valid price or a photo`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillPreviewMinimum()

        viewModel.onEvent(ProductRegisterUiEvent.NameChanged("  "))
        assertFalse(viewModel.uiState.value.isPreviewEnabled)
        viewModel.onEvent(ProductRegisterUiEvent.NameChanged("Lámpara"))

        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("0"))
        assertFalse(viewModel.uiState.value.isPreviewEnabled)
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("89.90"))

        viewModel.onEvent(ProductRegisterUiEvent.RemoveImage("content://1"))
        assertFalse(viewModel.uiState.value.isPreviewEnabled)
    }

    @Test
    fun `preview is unavailable while registration is in progress`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillPreviewMinimum()

        assertFalse(viewModel.uiState.value.copy(isSubmitting = true).isPreviewEnabled)
    }

    @Test
    fun `requesting the preview opens it and dismissing closes it`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillPreviewMinimum()

        viewModel.onEvent(ProductRegisterUiEvent.PreviewRequested)
        assertTrue(viewModel.uiState.value.showPreview)

        viewModel.onEvent(ProductRegisterUiEvent.PreviewDismissed)
        assertFalse(viewModel.uiState.value.showPreview)
    }

    @Test
    fun `requesting the preview without the minimum data does nothing`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(ProductRegisterUiEvent.PreviewRequested)

        assertFalse(viewModel.uiState.value.showPreview)
    }

    @Test
    fun `the preview neither changes the form nor registers anything`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProductRegisterUiEvent.PreviewRequested)
        viewModel.onEvent(ProductRegisterUiEvent.PreviewDismissed)
        assertFalse(viewModel.uiState.value.isDirty)

        viewModel.fillPreviewMinimum()
        val before = viewModel.uiState.value
        viewModel.onEvent(ProductRegisterUiEvent.PreviewRequested)
        viewModel.onEvent(ProductRegisterUiEvent.PreviewDismissed)

        assertEquals(before, viewModel.uiState.value)
        assertEquals(0, productRepository.registerCallCount)
        assertTrue(discountRepository.calls.isEmpty())
    }

    @Test
    fun `preview data carries the complete form`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.onEvent(ProductRegisterUiEvent.DescriptionChanged("  Luz cálida  "))
        viewModel.onEvent(
            ProductRegisterUiEvent.ImagesPicked(listOf(SelectedImage("content://2", "image/png"))),
        )

        val data = checkNotNull(viewModel.uiState.value.toPreviewData())

        assertEquals("Lámpara", data.name)
        assertEquals(89.9, data.price)
        assertEquals("Luz cálida", data.description)
        assertEquals(TestFixtures.category().name, data.categoryName)
        assertEquals(10, data.stock)
        // In the order they were added: the first photo is the primary one.
        assertEquals(listOf("content://1", "content://2"), data.imageModels)
    }

    @Test
    fun `preview data leaves out what the merchant has not entered`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillPreviewMinimum()
        viewModel.onEvent(ProductRegisterUiEvent.DescriptionChanged("   "))
        viewModel.onEvent(ProductRegisterUiEvent.QuantityChanged("-3"))

        val data = checkNotNull(viewModel.uiState.value.toPreviewData())

        assertNull(data.description)
        assertNull(data.categoryName)
        assertNull(data.stock)
    }

    @Test
    fun `preview data shows the price with the discount applied`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillPreviewMinimum()
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged("189"))
        viewModel.applyDiscount("20")

        assertEquals(151.2, viewModel.uiState.value.toPreviewData()?.price)
    }

    @Test
    fun `there is no preview data without a valid price`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillPreviewMinimum()
        viewModel.onEvent(ProductRegisterUiEvent.PriceChanged(""))

        assertNull(viewModel.uiState.value.toPreviewData())
    }
}
