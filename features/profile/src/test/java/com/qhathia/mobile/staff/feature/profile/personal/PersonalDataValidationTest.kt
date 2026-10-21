package com.qhathia.mobile.staff.feature.profile.personal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PersonalDataValidationTest {

    @Test
    fun `a name needs 2 to 150 characters after trimming`() {
        assertTrue(PersonalDataValidation.isValidName("Al"))
        assertTrue(PersonalDataValidation.isValidName("a".repeat(150)))
        assertFalse(PersonalDataValidation.isValidName(" C "))
        assertFalse(PersonalDataValidation.isValidName("   "))
        assertFalse(PersonalDataValidation.isValidName("a".repeat(151)))
    }

    @Test
    fun `a phone is 9 digits starting with 9`() {
        assertTrue(PersonalDataValidation.isValidPhone("987654321", storedPhone = null))
        assertFalse(PersonalDataValidation.isValidPhone("887654321", storedPhone = null))
        assertFalse(PersonalDataValidation.isValidPhone("98765432", storedPhone = null))
    }

    @Test
    fun `an empty phone is valid only when there was none`() {
        assertTrue(PersonalDataValidation.isValidPhone("", storedPhone = null))
        assertFalse(PersonalDataValidation.isValidPhone("", storedPhone = "999888777"))
    }

    @Test
    fun `the phone keeps digits only, at most nine`() {
        assertEquals("999888777", PersonalDataValidation.sanitizePhone("999 888 777"))
        assertEquals("519998887", PersonalDataValidation.sanitizePhone("+51 999 888 777"))
        assertEquals("", PersonalDataValidation.sanitizePhone("abc"))
    }
}
