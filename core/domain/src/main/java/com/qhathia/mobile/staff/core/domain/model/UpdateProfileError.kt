package com.qhathia.mobile.staff.core.domain.model

/** Why a profile update failed, derived by the data layer from the HTTP outcome. */
sealed interface UpdateProfileError {
    /** `422` — the server rejected the name or the phone. The response does not say which. */
    data object Validation : UpdateProfileError

    /** No connectivity or timeout. */
    data object Network : UpdateProfileError

    data class Unknown(val message: String?) : UpdateProfileError
}

/** Carries an [UpdateProfileError] through a failed [Result]. */
class UpdateProfileException(val error: UpdateProfileError) : Exception(error.toString())
