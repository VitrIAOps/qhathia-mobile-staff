package com.qhathia.mobile.staff.feature.home.model

import androidx.compose.runtime.Immutable
import com.qhathia.mobile.staff.core.domain.model.MissingProductSuggestion
import com.qhathia.mobile.staff.core.domain.model.RestockSuggestion
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant

/**
 * Immutable snapshot of the Inicio tab.
 *
 * [merchantFirstName] comes from the session, not from the insights endpoints — it is null
 * during a logout race, and the greeting then renders without a name rather than a placeholder one.
 * [pendingOrderCount] is the app-wide count, so the bell badge can never disagree with the
 * Solicitudes tab badge.
 *
 * A failed load sets [isError] but leaves [summary], the suggestion rows and [lastUpdatedAt]
 * untouched: stale data plus an error beats a blank screen, the same rule the Solicitudes screen
 * already follows.
 */
@Immutable
data class HomeUiState(
    val merchantFirstName: String? = null,
    val selectedPeriod: SalesPeriod = SalesPeriod.WEEKLY,
    val summary: SalesSummary? = null,
    val missingProducts: ImmutableList<MissingProductSuggestion> = persistentListOf(),
    val restockProducts: ImmutableList<RestockSuggestion> = persistentListOf(),
    val pendingOrderCount: Int = 0,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val lastUpdatedAt: Instant? = null,
) {
    /** True only before anything has ever loaded — the one case that replaces the card entirely. */
    val isInitialLoading: Boolean get() = isLoading && summary == null
}
