package com.qhathia.mobile.staff.feature.productregister.model

import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MissingFieldsTest {

    @Test
    fun `an empty form lists everything Agregar needs, in the form's order`() {
        assertEquals(
            listOf(MissingField.NAME, MissingField.PRICE, MissingField.STOCK, MissingField.CATEGORY, MissingField.PHOTO),
            ProductRegisterUiState().missingForSubmit,
        )
    }

    @Test
    fun `a complete form misses nothing`() {
        val state = ProductRegisterUiState(
            name = "Taladro",
            price = "189.90",
            quantity = "5",
            selectedCategoryId = "c1",
            images = persistentListOf(SelectedImage("content://1", "image/jpeg")),
        )
        assertEquals(emptyList<MissingField>(), state.missingForSubmit)
    }

    @Test
    fun `an invalid stock is named too`() {
        val state = ProductRegisterUiState(
            name = "Taladro",
            price = "189.90",
            quantity = "-1",
            selectedCategoryId = "c1",
            images = persistentListOf(SelectedImage("content://1", "image/jpeg")),
        )
        assertEquals(listOf(MissingField.STOCK), state.missingForSubmit)
    }
}
