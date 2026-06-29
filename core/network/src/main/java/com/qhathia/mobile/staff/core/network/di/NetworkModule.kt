package com.qhathia.mobile.staff.core.network.di

import com.qhathia.mobile.staff.core.network.AuthTokenProvider
import com.qhathia.mobile.staff.core.network.KtorClientFactory
import com.qhathia.mobile.staff.core.network.NetworkConfig
import com.qhathia.mobile.staff.core.network.R2Config
import com.qhathia.mobile.staff.core.network.service.AuthApiService
import com.qhathia.mobile.staff.core.network.service.CategoryApiService
import com.qhathia.mobile.staff.core.network.service.ProductApiService
import com.qhathia.mobile.staff.core.network.storage.AwsV4Signer
import com.qhathia.mobile.staff.core.network.storage.R2StorageService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides the network singletons. [NetworkConfig] and the [AuthTokenProvider] binding are
 * supplied by other modules (`:app` and `:core:data` respectively).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    @PublicClient
    fun providePublicClient(config: NetworkConfig, json: Json): HttpClient =
        KtorClientFactory.create(config, json, tokenProvider = null)

    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedClient(
        config: NetworkConfig,
        json: Json,
        tokenProvider: AuthTokenProvider,
    ): HttpClient = KtorClientFactory.create(config, json, tokenProvider)

    @Provides
    @Singleton
    fun provideAuthApiService(@PublicClient client: HttpClient): AuthApiService =
        AuthApiService(client)

    @Provides
    @Singleton
    fun provideProductApiService(@AuthenticatedClient client: HttpClient): ProductApiService =
        ProductApiService(client)

    @Provides
    @Singleton
    fun provideCategoryApiService(@AuthenticatedClient client: HttpClient): CategoryApiService =
        CategoryApiService(client)

    @Provides
    @Singleton
    @R2Client
    fun provideR2Client(): HttpClient = KtorClientFactory.createR2()

    @Provides
    @Singleton
    fun provideAwsV4Signer(): AwsV4Signer = AwsV4Signer()

    @Provides
    @Singleton
    fun provideR2StorageService(
        @R2Client client: HttpClient,
        config: R2Config,
        signer: AwsV4Signer,
    ): R2StorageService = R2StorageService(client, config, signer)
}
