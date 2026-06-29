package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.local.ImageCompressor
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.repository.MediaStorageRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.storage.R2StorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Compresses an image (off the main thread) then uploads it to R2, returning the public URL. Object
 * keys are `products/<uuid>.jpg` — a client-generated UUID, so keying is independent of the product
 * id (which does not exist until the product is created). Failures carry a [RegisterProductException].
 */
class MediaStorageRepositoryImpl @Inject constructor(
    private val imageCompressor: ImageCompressor,
    private val r2StorageService: R2StorageService,
) : MediaStorageRepository {

    override suspend fun uploadImage(image: SelectedImage): Result<String> =
        withContext(Dispatchers.IO) {
            val compressed = imageCompressor.compress(image).getOrElse { return@withContext Result.failure(it) }
            val key = "products/${UUID.randomUUID()}.jpg"
            when (val result = r2StorageService.upload(key, compressed.bytes, compressed.contentType)) {
                is NetworkResult.Success -> Result.success(result.data)
                is NetworkResult.HttpError ->
                    Result.failure(RegisterProductException(RegisterProductError.Unknown("R2 HTTP ${result.code}")))
                is NetworkResult.NetworkError ->
                    Result.failure(RegisterProductException(RegisterProductError.Network))
            }
        }
}
