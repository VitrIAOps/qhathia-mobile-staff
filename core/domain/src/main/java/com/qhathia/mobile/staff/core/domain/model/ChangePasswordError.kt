package com.qhathia.mobile.staff.core.domain.model

/** Why a password change failed, derived by the data layer from the HTTP outcome. */
sealed interface ChangePasswordError {
    /** `401` — the current password is wrong. */
    data object WrongCurrent : ChangePasswordError

    /** `400` — the new password equals the current one. */
    data object SameAsCurrent : ChangePasswordError

    /** `422` — the new password is outside 8–100 characters or a field is blank. */
    data object Validation : ChangePasswordError

    /** No connectivity or timeout. */
    data object Network : ChangePasswordError

    data class Unknown(val message: String?) : ChangePasswordError
}

/** Carries a [ChangePasswordError] through a failed [Result]. */
class ChangePasswordException(val error: ChangePasswordError) : Exception(error.toString())
