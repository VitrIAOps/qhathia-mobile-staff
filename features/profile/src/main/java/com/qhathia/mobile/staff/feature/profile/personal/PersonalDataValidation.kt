package com.qhathia.mobile.staff.feature.profile.personal

/** The backend's rules for `PUT /users/me`, checked before sending. */
internal object PersonalDataValidation {

    /** `fullName` length accepted by the backend, counted after trimming. */
    val NAME_LENGTH = 2..150

    const val PHONE_DIGITS = 9

    private val PERUVIAN_MOBILE = Regex("^9\\d{8}$")

    fun isValidName(name: String): Boolean = name.trim().length in NAME_LENGTH

    /**
     * Empty, or a 9-digit Peruvian mobile starting with 9. Empty is always valid: with no phone saved
     * it means "still none", and with one saved it removes it (`"phone": ""` clears it, §36).
     */
    fun isValidPhone(phone: String): Boolean = phone.isEmpty() || PERUVIAN_MOBILE.matches(phone)

    /** What the phone field keeps from typed or pasted text: digits only, at most nine. */
    fun sanitizePhone(input: String): String = input.filter(Char::isDigit).take(PHONE_DIGITS)
}
