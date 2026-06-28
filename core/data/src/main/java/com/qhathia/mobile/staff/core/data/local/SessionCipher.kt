package com.qhathia.mobile.staff.core.data.local

/** Symmetric encryption boundary for token-at-rest, decoupled so it can be faked in tests. */
interface SessionCipher {
    /** Encrypts [plaintext] and returns a self-contained, storable string. */
    fun encrypt(plaintext: String): String

    /** Reverses [encrypt]. */
    fun decrypt(stored: String): String
}
