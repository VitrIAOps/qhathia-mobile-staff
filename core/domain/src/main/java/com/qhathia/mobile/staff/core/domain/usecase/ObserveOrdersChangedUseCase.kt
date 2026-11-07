package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The "orders changed elsewhere" signal; a screen that lists orders re-queries on each emission. */
class ObserveOrdersChangedUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke(): Flow<Unit> = repository.ordersChanged
}
