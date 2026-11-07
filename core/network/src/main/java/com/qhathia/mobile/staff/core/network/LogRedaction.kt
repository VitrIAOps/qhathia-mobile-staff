package com.qhathia.mobile.staff.core.network

/**
 * A JSON string field whose value must not reach a log: any key ending in `password` or `token`
 * (`password`, `newPassword`, `accessToken`, `currentRefreshToken`, the push `token`…), plus
 * `exitPin`, `phone` and `email`. Keys that only start that way (`passwordUpdatedAt`, `tokenType`,
 * `exitPinSet`) do not match. The value pattern skips escaped quotes, so nothing of it leaks.
 */
private val SECRET_FIELD = Regex(
    "\"(\\w*(?:password|token)|exitPin|phone|email)\"(\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"",
    RegexOption.IGNORE_CASE,
)

/**
 * [message] with the value of every secret field replaced by `***`. Works on any fragment of a log
 * line, not only on whole JSON, since Ktor prefixes and may truncate bodies.
 */
internal fun redactSecrets(message: String): String =
    SECRET_FIELD.replace(message) { match -> "\"${match.groupValues[1]}\"${match.groupValues[2]}\"***\"" }
