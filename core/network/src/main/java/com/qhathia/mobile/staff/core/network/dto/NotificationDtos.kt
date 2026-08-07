package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request body for `POST /api/v1/notifications/tokens`. `storeId`/`userId` come from the JWT. */
@Serializable
data class RegisterTokenRequestDto(
    @SerialName("token") val token: String,
    @SerialName("platform") val platform: String,
    @SerialName("deviceLabel") val deviceLabel: String? = null,
)

/** Request body for `DELETE /api/v1/notifications/tokens`. */
@Serializable
data class UnregisterTokenRequestDto(
    @SerialName("token") val token: String,
)
