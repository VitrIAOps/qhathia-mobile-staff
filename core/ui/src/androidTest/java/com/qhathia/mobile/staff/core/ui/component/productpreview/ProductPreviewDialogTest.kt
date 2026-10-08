package com.qhathia.mobile.staff.core.ui.component.productpreview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The preview dialog's behavior on a device: the tabs and the orientation switch choose what is
 * drawn, the drawing is announced as one image that ignores taps, and data the merchant left out is
 * replaced by the placeholder.
 */
class ProductPreviewDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val product = ProductPreviewData(
        name = "Taladro",
        price = 380.0,
        description = "Percutor 650W",
        categoryName = "Herramientas",
        stock = 22,
        imageModels = persistentListOf("content://sample/1"),
    )

    private var dismissCount = 0

    private fun setDialog(data: ProductPreviewData = product) {
        composeTestRule.setContent {
            QhathiaTheme {
                var tab by remember { mutableStateOf(ProductPreviewTab.Item) }
                var orientation by remember { mutableStateOf(ProductPreviewOrientation.Vertical) }
                ProductPreviewDialogContent(
                    data = data,
                    tab = tab,
                    orientation = orientation,
                    isLandscape = false,
                    onTabSelect = { tab = it },
                    onOrientationSelect = { orientation = it },
                    onDismiss = { dismissCount++ },
                )
            }
        }
    }

    @Test
    fun opensOnTheVerticalItem() {
        setDialog()

        composeTestRule
            .onNodeWithContentDescription("Vista previa del item vertical: Taladro, S/ 380.00")
            .assertIsDisplayed()
    }

    @Test
    fun orientationSwitch_showsTheHorizontalItem() {
        setDialog()

        composeTestRule.onNodeWithContentDescription("Horizontal").performClick()

        composeTestRule
            .onNodeWithContentDescription("Vista previa del item horizontal: Taladro, S/ 380.00")
            .assertIsDisplayed()
    }

    @Test
    fun detailTab_showsTheDetailAndLocksTheOrientation() {
        setDialog()

        composeTestRule.onNodeWithText("Detalle").performClick()

        composeTestRule
            .onNodeWithContentDescription("Vista previa del detalle de producto: Taladro, S/ 380.00")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Vertical").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Gira tu teléfono para ver mejor el detalle").assertIsDisplayed()
    }

    @Test
    fun tappingThePreview_doesNothing() {
        setDialog()

        composeTestRule
            .onNodeWithContentDescription("Vista previa del item vertical: Taladro, S/ 380.00")
            .performClick()

        assertEquals(0, dismissCount)
        composeTestRule
            .onNodeWithContentDescription("Vista previa del item vertical: Taladro, S/ 380.00")
            .assertIsDisplayed()
    }

    @Test
    fun theDrawnControls_areNotExposedIndividually() {
        setDialog()
        composeTestRule.onNodeWithText("Detalle").performClick()

        composeTestRule.onNodeWithText("Agregar al carrito").assertDoesNotExist()
    }

    @Test
    fun closeAction_dismisses() {
        setDialog()

        composeTestRule.onNodeWithContentDescription("Cerrar vista previa").performClick()

        assertEquals(1, dismissCount)
    }

    @Test
    fun horizontalItem_showsThePlaceholderForAMissingCategory() {
        composeTestRule.setContent {
            QhathiaTheme { DiscoveryHorizontalItem(data = product.copy(categoryName = null)) }
        }

        composeTestRule.onNodeWithText("DATO NO INGRESADO").assertIsDisplayed()
    }

    @Test
    fun detail_showsThePlaceholderForMissingDescriptionAndStock() {
        composeTestRule.setContent {
            QhathiaTheme { DiscoveryProductDetail(data = product.copy(description = null, stock = null)) }
        }

        // The detail is drawn at its 1440dp design width, so it is asserted by presence, not visibility.
        composeTestRule.onAllNodesWithText("DATO NO INGRESADO").assertCountEquals(2)
    }

    @Test
    fun detail_showsTheEnteredStock() {
        composeTestRule.setContent {
            QhathiaTheme { DiscoveryProductDetail(data = product) }
        }

        composeTestRule.onNodeWithText("22 disponibles").assertExists()
    }
}
