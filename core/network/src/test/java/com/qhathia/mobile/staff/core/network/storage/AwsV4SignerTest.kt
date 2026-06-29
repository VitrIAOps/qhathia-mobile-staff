package com.qhathia.mobile.staff.core.network.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AwsV4SignerTest {

    private val signer = AwsV4Signer()

    /**
     * Documented AWS S3 SigV4 "PUT Object — single chunk" example
     * (docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html). Reproducing its
     * signature exactly proves the canonical-request, string-to-sign and signing-key steps are correct.
     */
    @Test
    fun `matches the AWS documented PUT Object signature`() {
        val payloadHash = "44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072"
        val authorization = signer.sign(
            method = "PUT",
            canonicalUri = "/test%24file.text",
            canonicalQuery = "",
            headers = mapOf(
                "date" to "Fri, 24 May 2013 00:00:00 GMT",
                "host" to "examplebucket.s3.amazonaws.com",
                "x-amz-content-sha256" to payloadHash,
                "x-amz-date" to "20130524T000000Z",
                "x-amz-storage-class" to "REDUCED_REDUNDANCY",
            ),
            payloadHashHex = payloadHash,
            accessKeyId = "AKIAIOSFODNN7EXAMPLE",
            secretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
            region = "us-east-1",
            service = "s3",
            amzDate = "20130524T000000Z",
        )

        assertTrue(
            authorization.contains(
                "Signature=98ad721746da40c64f1a55b78f14c238d841ea1380cd77a1b5971af0ece108bd",
            ),
            "Unexpected authorization: $authorization",
        )
        assertTrue(
            authorization.contains(
                "SignedHeaders=date;host;x-amz-content-sha256;x-amz-date;x-amz-storage-class",
            ),
        )
        assertTrue(
            authorization.contains(
                "Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request",
            ),
        )
    }

    @Test
    fun `signPut signs the content-type, host, content-sha and date headers`() {
        val fixedSigner = AwsV4Signer(clock = { java.time.Instant.parse("2026-06-28T12:00:00Z") })

        val signed = fixedSigner.signPut(
            host = "acc.r2.cloudflarestorage.com",
            canonicalUri = "/bucket/products/abc.jpg",
            payload = "hello".toByteArray(),
            contentType = "image/jpeg",
            accessKeyId = "key",
            secretAccessKey = "secret",
            region = "auto",
        )

        assertEquals("20260628T120000Z", signed.amzDate)
        assertTrue(signed.contentSha256.isNotBlank())
        assertTrue(
            signed.authorization.contains(
                "SignedHeaders=content-type;host;x-amz-content-sha256;x-amz-date",
            ),
            "Unexpected authorization: ${signed.authorization}",
        )
    }
}
