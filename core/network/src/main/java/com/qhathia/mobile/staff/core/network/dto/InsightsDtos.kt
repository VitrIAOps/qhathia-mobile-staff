package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/**
 * `GET /api/v1/insights/unmet-searches` (vo-api) — kiosk searches that found no product, grouped by
 * canonical term and ordered by [UnmetSearchGroupDto.personCount]. The endpoint is fail-open: an
 * aggregation failure answers `groups: []` rather than an error.
 */
@Serializable
data class UnmetSearchesDto(
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val totalPeople: Int = 0,
    val groups: List<UnmetSearchGroupDto> = emptyList(),
)

/**
 * One canonical [term]. [personCount] — distinct kiosk sessions — is the demand metric;
 * [searchCount] counts every attempt, including one shopper retrying. [examples] are the most
 * frequent raw texts behind the term.
 */
@Serializable
data class UnmetSearchGroupDto(
    val term: String = "",
    val personCount: Int = 0,
    val searchCount: Int = 0,
    val examples: List<UnmetSearchExampleDto> = emptyList(),
)

@Serializable
data class UnmetSearchExampleDto(
    val rawText: String = "",
    val count: Int = 0,
)
