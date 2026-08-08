package com.qhathia.mobile.staff.feature.orderrequests.model

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.feature.orderrequests.R

/**
 * The motives the DS reject form offers. The contract stores `reason` as free text, so what
 * travels to the backend is the option's own label — except for [OTHER], which sends whatever the
 * merchant typed.
 */
enum class RejectionReason(@param:StringRes val labelRes: Int) {
    NO_STOCK(R.string.order_reject_reason_no_stock),
    INSUFFICIENT_QUANTITY(R.string.order_reject_reason_insufficient_quantity),
    CUSTOMER_CHANGED_MIND(R.string.order_reject_reason_customer_changed_mind),
    PAYMENT_ISSUE(R.string.order_reject_reason_payment_issue),

    /** Renders a free-text field instead of a fixed label; the typed motive is what is sent. */
    OTHER(R.string.order_reject_reason_other),
}
