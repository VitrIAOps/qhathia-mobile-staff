package com.qhathia.mobile.staff.core.common.validation

/**
 * Lightweight, framework-free email format check (avoids `android.util.Patterns`, which is not
 * available in JVM unit tests). Good enough for client-side gating before a network call.
 */
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

fun isValidEmail(value: String): Boolean = EMAIL_REGEX.matches(value.trim())
