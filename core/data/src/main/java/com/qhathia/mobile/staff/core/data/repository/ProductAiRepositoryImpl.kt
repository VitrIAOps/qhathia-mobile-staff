package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.local.ImageCompressor
import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.domain.model.ProductDraft
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.repository.ProductAiRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.MultipartImage
import com.qhathia.mobile.staff.core.network.dto.RegenerateDescriptionRequestDto
import com.qhathia.mobile.staff.core.network.service.ProductApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Maps the AI drafting endpoints from transport [NetworkResult]s to domain [Result]s. Drafting
 * compresses the chosen image off the main thread (the ≤500 KB output is well within the endpoint's
 * 5 MB cap) and sends it as a single multipart `image` part. Both endpoints are fail-open, so a
 * successful [Result] may still carry an empty draft or an echoed description; only HTTP/transport
 * errors produce a failed [Result].
 */
class ProductAiRepositoryImpl @Inject constructor(
    private val apiService: ProductApiService,
    private val imageCompressor: ImageCompressor,
) : ProductAiRepository {

    override suspend fun draftFromImage(image: SelectedImage): Result<ProductDraft> =
        withContext(Dispatchers.IO) {
            val compressed = imageCompressor.compress(image)
                .getOrElse { return@withContext Result.failure(it) }
            val part = MultipartImage(
                bytes = compressed.bytes,
                contentType = compressed.contentType,
                filename = "image.jpg",
            )
            when (val result = apiService.draftFromImage(part)) {
                is NetworkResult.Success -> Result.success(result.data.toDomain())
                is NetworkResult.HttpError ->
                    Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))
                is NetworkResult.NetworkError -> Result.failure(result.cause)
            }
        }

    override suspend fun optimizeDescription(currentDescription: String): Result<String> =
        when (
            val result =
                apiService.regenerateDescription(RegenerateDescriptionRequestDto(currentDescription))
        ) {
            is NetworkResult.Success -> Result.success(result.data.description)
            is NetworkResult.HttpError ->
                Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }
}
