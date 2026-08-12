package com.qhathia.mobile.staff.core.common.navigation

/**
 * The intent extra a notification uses to ask the app to open a specific section, and the values it
 * accepts.
 *
 * These live in `:core:common` because the producer (`:core:notifications`) and the consumer
 * (`:app`) cannot see each other: the notifications module deliberately does not depend on `:app`,
 * so it can neither reference `MainActivity` nor the feature modules' route constants. A plain
 * string contract through the module both already depend on keeps that boundary intact.
 */
const val EXTRA_DESTINATION = "com.qhathia.mobile.staff.extra.DESTINATION"

/** Open the Solicitudes (order requests) section. */
const val DESTINATION_ORDER_REQUESTS = "order_requests"
