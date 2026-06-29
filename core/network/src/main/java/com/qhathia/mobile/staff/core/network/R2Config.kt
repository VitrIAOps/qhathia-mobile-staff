package com.qhathia.mobile.staff.core.network

/**
 * Runtime configuration for Cloudflare R2 object storage, supplied by the application module from
 * its `BuildConfig` (values sourced from `local.properties`, never committed). Mirrors
 * [NetworkConfig] so secrets stay out of the network module itself.
 *
 * @property endpoint S3-compatible endpoint, e.g. `https://<account>.r2.cloudflarestorage.com`.
 * @property bucket target bucket name (path-style addressing).
 * @property region signing region — R2 uses `auto`.
 * @property accessKeyId R2 access key id.
 * @property secretAccessKey R2 secret access key.
 * @property publicBaseUrl public base URL the uploaded object is served from (e.g. an R2.dev or
 *   custom domain), used to build the URL registered with the products media API.
 */
data class R2Config(
    val endpoint: String,
    val bucket: String,
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val publicBaseUrl: String,
)
