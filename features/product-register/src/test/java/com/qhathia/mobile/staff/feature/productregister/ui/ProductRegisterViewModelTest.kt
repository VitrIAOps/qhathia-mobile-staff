package com.qhathia.mobile.staff.feature.productregister.ui

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RegisterProductUseCase
import com.qhathia.mobile.staff.core.testing.FakeCategoryRepository
import com.qhathia.mobile.staff.core.testing.FakeMediaStorageRepository
import com.qhathia.mobile.staff.core.testing.FakeProductRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiEffect
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiEvent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
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
    private lateinit var mediaRepository: FakeMediaStorageRepository

    @BeforeEach
    fun setUp() {
        categoryRepository = FakeCategoryRepository()
        productRepository = FakeProductRepository()
        mediaRepository = FakeMediaStorageRepository()
    }

    private fun createViewModel() = ProductRegisterViewModel(
        getCategoriesUseCase = GetCategoriesUseCase(categoryRepository),
        registerProductUseCase = RegisterProductUseCase(productRepository, mediaRepository),
    )

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
    fun `blank tags keep submit disabled`() = runTest {
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        assertTrue(viewModel.uiState.value.isSubmitEnabled)

        viewModel.onEvent(ProductRegisterUiEvent.TagsChanged("   ,  "))

        assertFalse(viewModel.uiState.value.isSubmitEnabled)
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
        productRepository.createResult = Result.success("p1")
        val viewModel = createViewModel()
        viewModel.fillValidForm()

        viewModel.uiEffect.test {
            viewModel.onEvent(ProductRegisterUiEvent.Submit)
            assertEquals(ProductRegisterUiEffect.NavigateBackWithSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("importado", "premium"), productRepository.createdProduct?.tags)
    }

    @Test
    fun `plan limit submit emits NavigateBackWithPlanLimit`() = runTest {
        productRepository.createResult =
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
        productRepository.createResult =
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
    fun `catalog retry clears the category error`() = runTest {
        categoryRepository.categoriesResult = Result.failure(IllegalStateException("boom"))
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isCategoriesError)

        categoryRepository.categoriesResult = Result.success(listOf(TestFixtures.category()))
        viewModel.onEvent(ProductRegisterUiEvent.RetryCatalog)

        assertFalse(viewModel.uiState.value.isCategoriesError)
    }
}
