package com.qhathia.mobile.staff.core.domain.model

import java.math.BigDecimal

/**
 * The display settings of the active store.
 *
 * @property language the kiosk language held by the store; `null` when unknown. The phone app's own
 *   language is device-owned and never read from here.
 * @property pricesIncludeIgv declares how product prices must be read; nothing is recalculated.
 */
data class StoreSettings(
    val storeId: String,
    val language: AppLanguage?,
    val pricesIncludeIgv: Boolean,
    val igvRate: BigDecimal,
)

/** The languages the app ships, with their BCP-47 [tag]. */
enum class AppLanguage(val tag: String) {
    SPANISH("es"),
    ENGLISH("en"),
    ;

    companion object {
        /** Resolves a language tag ("en", "en-US"); anything unsupported is `null`. */
        fun fromTag(tag: String?): AppLanguage? {
            val primary = tag?.substringBefore('-')?.substringBefore('_')?.lowercase() ?: return null
            return entries.firstOrNull { it.tag == primary }
        }
    }
}
