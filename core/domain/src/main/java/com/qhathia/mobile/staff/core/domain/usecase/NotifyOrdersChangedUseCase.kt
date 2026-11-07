package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/** Tells the screens listing orders to re-query, e.g. after a notification tap. */
class NotifyOrdersChangedUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    operator fun invoke() = repository.notifyOrdersChanged()
}
