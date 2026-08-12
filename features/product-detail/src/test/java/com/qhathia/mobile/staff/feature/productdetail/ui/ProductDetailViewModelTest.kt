package com.qhathia.mobile.staff.feature.productdetail.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductDetailUseCase
import com.qhathia.mobile.staff.core.domain.usecase.OptimizeDescriptionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UpdateProductUseCase
import com.qhathia.mobile.staff.core.testing.FakeCategoryRepository
import com.qhathia.mobile.staff.core.testing.FakeProductAiRepository
import com.qhathia.mobile.staff.core.testing.FakeProductRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiEffect
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiEvent
import com.qhathia.mobile.staff.feature.productdetail.navigation.PRODUCT_DETAIL_ARG_ID
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

    @BeforeEach
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        productRepository = FakeProductRepository()
        aiRepository = FakeProductAiRepository()
    }

    private fun createViewModel(productId: String = PRODUCT_ID) = ProductDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(PRODUCT_DETAIL_ARG_ID to productId)),
        getProductDetailUseCase = GetProductDetailUseCase(productRepository),
        getCategoriesUseCase = GetCategoriesUseCase(categoryRepository),
        updateProductUseCase = UpdateProductUseCase(productRepository),
        optimizeDescriptionUseCase = OptimizeDescriptionUseCase(aiRepository),
    )

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

    private companion object {
        const val PRODUCT_ID = "product-42"
    }
}
