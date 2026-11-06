package com.qhathia.mobile.staff.core.domain.model

/**
 * A pairing the merchant approved. Approval is not linking yet: the tablet still has to exchange
 * the code, which is followed through [sessionId]. [deviceId] is the device the approval created,
 * so an unanswered approval can be undone. [name] is the name the backend gave it.
 */
data class PairedDevice(
    val deviceId: String,
    val sessionId: String,
    val name: String?,
)

/** Where an approved pairing session stands; [tokenExchanged] means the tablet is linked. */
data class PairingSessionStatus(
    val status: String?,
    val tokenExchanged: Boolean,
)
