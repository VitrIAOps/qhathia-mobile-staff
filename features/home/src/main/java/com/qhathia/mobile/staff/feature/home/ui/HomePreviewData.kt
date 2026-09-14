package com.qhathia.mobile.staff.feature.home.ui

import com.qhathia.mobile.staff.core.domain.model.MissingProductSuggestion
import com.qhathia.mobile.staff.core.domain.model.RestockSuggestion
import com.qhathia.mobile.staff.core.domain.model.SalesBucket
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.feature.home.model.HomeUiState
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.time.Instant

/**
 * Fixed data behind the Inicio previews, shaped like what the dashboard endpoints return and using
 * the design's figures (Figma `Qhatia-DS`, node `1954-6799`). The summary card's own previews live
 * with the card in `:core:ui`. Kept out of the tests, which build their own fixtures from
 * `core:testing`.
 */
internal object HomePreviewData {

    val generatedAt: Instant = Instant.parse("2026-06-29T23:50:00Z")

    private fun bucket(label: String, amount: Long) = SalesBucket(label, BigDecimal(amount))

    /** The design's week: the period in progress is also the best one, 24% above the one before. */
    val summary = SalesSummary(
        period = SalesPeriod.WEEKLY,
        total = BigDecimal(1690),
        deltaPercent = 24.3,
        buckets = listOf(
            bucket("Sem 10", 1240),
            bucket("Sem 11", 1480),
            bucket("Sem 12", 1360),
            bucket("Sem 13", 1690),
        ),
        highlightedBucketIndex = 3,
        highlightedDeltaPercent = 24.3,
        generatedAt = generatedAt,
    )

    val missingProducts = persistentListOf(
        MissingProductSuggestion(
            id = "taladro",
            name = "Taladro percutor Bosch GSB 13 RE Professional",
            personCount = 9,
        ),
        MissingProductSuggestion(
            id = "amoladora",
            name = "Amoladora angular DeWalt DWE4020",
            personCount = 1,
        ),
    )

    val restockProducts = persistentListOf(
        RestockSuggestion(
            id = "restock-1",
            name = "Taladro Percutor Bosch GSB 13 RE",
            imageUrl = null,
            remainingStock = 1,
        ),
        RestockSuggestion(
            id = "restock-2",
            name = "Juego de Brocas Bosch HSS 19 piezas",
            imageUrl = null,
            remainingStock = 3,
        ),
    )

    val loadedState = HomeUiState(
        merchantFirstName = "Maria",
        selectedPeriod = SalesPeriod.WEEKLY,
        summary = summary,
        missingProducts = missingProducts,
        restockProducts = restockProducts,
        pendingOrderCount = 2,
        lastUpdatedAt = generatedAt,
    )

    val loadingState = HomeUiState(merchantFirstName = "Maria", isLoading = true)

    /** A refresh that failed over data that had already loaded. */
    val errorState = loadedState.copy(isError = true)
}
