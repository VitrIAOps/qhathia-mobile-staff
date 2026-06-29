package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.repository.MediaStorageRepository

/** Controllable [MediaStorageRepository] for tests; captures the images it was asked to upload. */
class FakeMediaStorageRepository : MediaStorageRepository {

    var uploadResult: Result<String> = Result.success("https://cdn.example.com/products/img.jpg")

    val uploadedImages: MutableList<SelectedImage> = mutableListOf()
    var uploadCount: Int = 0
        private set

    override suspend fun uploadImage(image: SelectedImage): Result<String> {
        uploadCount++
        uploadedImages += image
        return uploadResult
    }
}
