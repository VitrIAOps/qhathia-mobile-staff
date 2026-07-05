package com.qhathia.mobile.staff.core.network.dto

/**
 * A single image ready to be sent as an `images` part of `POST /api/v1/products/with-images`:
 * the encoded [bytes], their [contentType] (e.g. `image/jpeg`) and the [filename] to advertise in
 * the part's `Content-Disposition`.
 */
data class MultipartImage(
    val bytes: ByteArray,
    val contentType: String,
    val filename: String,
)
