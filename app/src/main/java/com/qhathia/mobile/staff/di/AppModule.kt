package com.qhathia.mobile.staff.di

import com.qhathia.mobile.staff.BuildConfig
import com.qhathia.mobile.staff.core.network.NetworkConfig
import com.qhathia.mobile.staff.core.network.R2Config
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** App-level bindings — supplies environment config from `BuildConfig` to the network layer. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig(
        baseUrl = BuildConfig.BASE_URL,
        enableLogging = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun provideR2Config(): R2Config = R2Config(
        endpoint = BuildConfig.R2_ENDPOINT,
        bucket = BuildConfig.R2_BUCKET,
        region = BuildConfig.R2_REGION,
        accessKeyId = BuildConfig.R2_ACCESS_KEY_ID,
        secretAccessKey = BuildConfig.R2_SECRET_ACCESS_KEY,
        publicBaseUrl = BuildConfig.R2_PUBLIC_BASE_URL,
    )
}
