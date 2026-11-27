package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.OrderNotificationTray
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Solicitudes opened [orderId]'s detail from a notification: the request is done, and so is that
 * order's notification. A tap on the notification already removed it; "Ver solicitud" does not, since
 * Android dismisses only on the main tap.
 */
class ClearOrderToOpenUseCase @Inject constructor(
    private val repository: OrderRepository,
    private val tray: OrderNotificationTray,
) {
    operator fun invoke(orderId: String) {
        repository.clearOrderToOpen(orderId)
        tray.cancel(orderId)
    }
}
