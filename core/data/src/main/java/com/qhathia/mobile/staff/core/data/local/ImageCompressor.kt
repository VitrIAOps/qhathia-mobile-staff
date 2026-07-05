package com.qhathia.mobile.staff.core.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/** A compressed image ready to upload: re-encoded JPEG bytes and their content type. */
data class CompressedImage(
    val bytes: ByteArray,
    val contentType: String,
)

/**
 * Decodes a picked JPG/PNG image and re-encodes it to JPEG at or below [MAX_BYTES] while preserving
 * acceptable quality (descending quality, then downscaling). Rejects unsupported formats and images
 * that cannot be reduced to the limit, surfacing [RegisterProductError.ImageTooLargeOrInvalid].
 */
class ImageCompressor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun compress(image: SelectedImage): Result<CompressedImage> {
        if (image.mimeType.lowercase() !in SUPPORTED_MIME_TYPES) {
            return invalid()
        }
        val uri = Uri.parse(image.uri)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        }.getOrElse { return invalid() }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, MAX_DIMENSION, MAX_DIMENSION)
        }
        var bitmap = runCatching {
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        }.getOrNull() ?: return invalid()

        var quality = INITIAL_QUALITY
        while (true) {
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
            if (bytes.size <= MAX_BYTES) {
                return Result.success(CompressedImage(bytes, "image/jpeg"))
            }
            if (quality > MIN_QUALITY) {
                quality -= QUALITY_STEP
                continue
            }
            val newWidth = (bitmap.width * DOWNSCALE_FACTOR).toInt()
            val newHeight = (bitmap.height * DOWNSCALE_FACTOR).toInt()
            if (newWidth < MIN_DIMENSION || newHeight < MIN_DIMENSION) {
                return invalid()
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            quality = INITIAL_QUALITY
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun invalid(): Result<CompressedImage> =
        Result.failure(RegisterProductException(RegisterProductError.ImageTooLargeOrInvalid))

    companion object {
        private const val MAX_BYTES = 500 * 1024
        private const val MAX_DIMENSION = 2048
        private const val MIN_DIMENSION = 200
        private const val INITIAL_QUALITY = 90
        private const val MIN_QUALITY = 40
        private const val QUALITY_STEP = 15
        private const val DOWNSCALE_FACTOR = 0.8
        private val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png")
    }
}
