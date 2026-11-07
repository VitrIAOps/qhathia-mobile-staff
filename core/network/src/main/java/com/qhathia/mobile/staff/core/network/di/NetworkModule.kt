package com.qhathia.mobile.staff.core.network.di

import com.qhathia.mobile.staff.core.network.AuthTokenProvider
import com.qhathia.mobile.staff.core.network.KtorClientFactory
import com.qhathia.mobile.staff.core.network.NetworkConfig
import com.qhathia.mobile.staff.core.network.service.AuthApiService
import com.qhathia.mobile.staff.core.network.service.CatalogApiService
import com.qhathia.mobile.staff.core.network.service.CategoryApiService
import com.qhathia.mobile.staff.core.network.service.DashboardApiService
import com.qhathia.mobile.staff.core.network.service.DeviceApiService
import com.qhathia.mobile.staff.core.network.service.InsightsApiService
import com.qhathia.mobile.staff.core.network.service.NotificationApiService
import com.qhathia.mobile.staff.core.network.service.OrderApiService
import com.qhathia.mobile.staff.core.network.service.ProductApiService
import com.qhathia.mobile.staff.core.network.service.StoreApiService
import com.qhathia.mobile.staff.core.network.service.SubscriptionApiService
import com.qhathia.mobile.staff.core.network.service.UserApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import javax.inject.Provider
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
    fun provideAuthApiService(
        @PublicClient publicClient: HttpClient,
        @AuthenticatedClient authenticatedClient: Provider<HttpClient>,
        json: Json,
    ): AuthApiService = AuthApiService(publicClient, authenticatedClient, json)

    @Provides
    @Singleton
    fun provideNotificationApiService(
        @AuthenticatedClient client: HttpClient,
    ): NotificationApiService = NotificationApiService(client)

    @Provides
    @Singleton
    fun provideProductApiService(
        @AuthenticatedClient client: HttpClient,
        json: Json,
    ): ProductApiService = ProductApiService(client, json)

    @Provides
    @Singleton
    fun provideOrderApiService(
        @AuthenticatedClient client: HttpClient,
        json: Json,
    ): OrderApiService = OrderApiService(client, json)

    @Provides
    @Singleton
    fun provideCategoryApiService(@AuthenticatedClient client: HttpClient): CategoryApiService =
        CategoryApiService(client)

    @Provides
    @Singleton
    fun provideDeviceApiService(
        @AuthenticatedClient client: HttpClient,
        json: Json,
    ): DeviceApiService = DeviceApiService(client, json)

    @Provides
    @Singleton
    fun provideDashboardApiService(@AuthenticatedClient client: HttpClient): DashboardApiService =
        DashboardApiService(client)

    @Provides
    @Singleton
    fun provideInsightsApiService(@AuthenticatedClient client: HttpClient): InsightsApiService =
        InsightsApiService(client)

    @Provides
    @Singleton
    fun provideUserApiService(@AuthenticatedClient client: HttpClient): UserApiService =
        UserApiService(client)

    @Provides
    @Singleton
    fun provideSubscriptionApiService(
        @AuthenticatedClient client: HttpClient,
    ): SubscriptionApiService = SubscriptionApiService(client)

    @Provides
    @Singleton
    fun provideStoreApiService(@AuthenticatedClient client: HttpClient): StoreApiService =
        StoreApiService(client)

    @Provides
    @Singleton
    fun provideCatalogApiService(@AuthenticatedClient client: HttpClient): CatalogApiService =
        CatalogApiService(client)
}
