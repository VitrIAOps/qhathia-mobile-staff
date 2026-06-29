package com.qhathia.mobile.staff.core.network.storage

import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Minimal AWS Signature Version 4 signer for a single object `PUT`. Self-contained (JDK crypto only)
 * and deterministic given a fixed [clock], so it can be unit-tested against the documented AWS S3
 * SigV4 test vectors. Only what an R2 upload needs is implemented — no query signing, no chunking.
 */
class AwsV4Signer(private val clock: () -> Instant = { Instant.now() }) {

    /** Headers that must be attached to the signed request. */
    data class SignedHeaders(
        val authorization: String,
        val amzDate: String,
        val contentSha256: String,
    )

    /**
     * Signs a `PUT` of [payload] to [canonicalUri] (already URI-encoded, path-style: `/bucket/key`)
     * on [host], signing the `content-type`, `host`, `x-amz-content-sha256` and `x-amz-date` headers.
     */
    fun signPut(
        host: String,
        canonicalUri: String,
        payload: ByteArray,
        contentType: String,
        accessKeyId: String,
        secretAccessKey: String,
        region: String,
        service: String = "s3",
    ): SignedHeaders {
        val now = clock()
        val amzDate = AMZ_DATE.format(now)
        val payloadHash = sha256Hex(payload)
        val headers = sortedMapOf(
            "content-type" to contentType,
            "host" to host,
            "x-amz-content-sha256" to payloadHash,
            "x-amz-date" to amzDate,
        )
        val authorization = sign(
            method = "PUT",
            canonicalUri = canonicalUri,
            canonicalQuery = "",
            headers = headers,
            payloadHashHex = payloadHash,
            accessKeyId = accessKeyId,
            secretAccessKey = secretAccessKey,
            region = region,
            service = service,
            amzDate = amzDate,
        )
        return SignedHeaders(authorization, amzDate, payloadHash)
    }

    /**
     * Computes the `Authorization` header value for the given canonical request inputs. Header keys
     * in [headers] must be lowercase; the signed-headers list is derived from their sorted order.
     */
    fun sign(
        method: String,
        canonicalUri: String,
        canonicalQuery: String,
        headers: Map<String, String>,
        payloadHashHex: String,
        accessKeyId: String,
        secretAccessKey: String,
        region: String,
        service: String,
        amzDate: String,
    ): String {
        val sortedHeaders = headers.toSortedMap()
        val canonicalHeaders = sortedHeaders.entries.joinToString("") { (k, v) -> "$k:${v.trim()}\n" }
        val signedHeaders = sortedHeaders.keys.joinToString(";")

        val canonicalRequest = buildString {
            append(method).append('\n')
            append(canonicalUri).append('\n')
            append(canonicalQuery).append('\n')
            append(canonicalHeaders).append('\n')
            append(signedHeaders).append('\n')
            append(payloadHashHex)
        }

        val dateStamp = amzDate.substring(0, 8)
        val scope = "$dateStamp/$region/$service/aws4_request"
        val stringToSign = buildString {
            append(ALGORITHM).append('\n')
            append(amzDate).append('\n')
            append(scope).append('\n')
            append(sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8)))
        }

        val signingKey = signingKey(secretAccessKey, dateStamp, region, service)
        val signature = hmac(signingKey, stringToSign).toHex()

        return "$ALGORITHM Credential=$accessKeyId/$scope, " +
            "SignedHeaders=$signedHeaders, Signature=$signature"
    }

    private fun signingKey(
        secret: String,
        dateStamp: String,
        region: String,
        service: String,
    ): ByteArray {
        val kDate = hmac("AWS4$secret".toByteArray(Charsets.UTF_8), dateStamp)
        val kRegion = hmac(kDate, region)
        val kService = hmac(kRegion, service)
        return hmac(kService, "aws4_request")
    }

    private fun hmac(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val ALGORITHM = "AWS4-HMAC-SHA256"
        private val AMZ_DATE: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    }
}
