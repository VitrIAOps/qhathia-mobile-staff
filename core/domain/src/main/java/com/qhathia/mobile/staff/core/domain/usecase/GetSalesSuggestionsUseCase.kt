package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.StoreSuggestions
import com.qhathia.mobile.staff.core.domain.repository.StoreInsightsRepository
import javax.inject.Inject

/** Reads the "sugerencias para vender más" rows shown under the Inicio summary card. */
class GetSalesSuggestionsUseCase @Inject constructor(
    private val repository: StoreInsightsRepository,
) {
    suspend operator fun invoke(): Result<StoreSuggestions> = repository.getSuggestions()
}
