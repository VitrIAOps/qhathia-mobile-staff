package com.qhathia.mobile.staff.core.data.mapper

import java.time.Instant
import java.time.OffsetDateTime

/**
 * Parses an ISO-8601 timestamp (`2026-09-13T09:41:55Z` or with an explicit offset). A blank or
 * unparseable value is `null`, so one malformed field never fails a whole response.
 */
internal fun String?.parseIsoInstant(): Instant? {
    val value = this?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
}
