package com.qhathia.mobile.staff.core.network.storage

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.R2Config
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import java.net.URLEncoder

/**
 * Uploads an object to Cloudflare R2 with a SigV4-signed, path-style `PUT`, returning the public
 * URL the object is served from. Runs on the dedicated R2 client (no bearer token).
 */
class R2StorageService(
    private val client: HttpClient,
    private val config: R2Config,
    private val signer: AwsV4Signer,
) {

    suspend fun upload(key: String, bytes: ByteArray, contentType: String): NetworkResult<String> =
        runCatching {
            val host = config.endpoint
                .substringAfter("://")
                .trimEnd('/')
            val encodedKey = key.split('/').joinToString("/") { encodeSegment(it) }
            val canonicalUri = "/${config.bucket}/$encodedKey"
            val url = "${config.endpoint.trimEnd('/')}/${config.bucket}/$encodedKey"

            val signed = signer.signPut(
                host = host,
                canonicalUri = canonicalUri,
                payload = bytes,
                contentType = contentType,
                accessKeyId = config.accessKeyId,
                secretAccessKey = config.secretAccessKey,
                region = config.region,
            )

            client.put(url) {
                header("x-amz-date", signed.amzDate)
                header("x-amz-content-sha256", signed.contentSha256)
                header(HttpHeaders.Authorization, signed.authorization)
                contentType(ContentType.parse(contentType))
                setBody(bytes)
            }

            "${config.publicBaseUrl.trimEnd('/')}/$encodedKey"
        }.toNetworkResult()

    /** RFC 3986 path-segment encoding (S3 SigV4 canonical URI rules; spaces as %20, not '+'). */
    private fun encodeSegment(segment: String): String =
        URLEncoder.encode(segment, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
}
