package com.qhathia.mobile.staff.core.domain.model

/**
 * Stock availability of a product. Mirrors the backend `AvailabilityStatus` enum and drives the
 * inventory availability sub-filter (Disponibles / Stock bajo / Consultar disponibilidad).
 */
enum class Availability {
    AVAILABLE,
    LOW_STOCK,
    CONSULT,
}
