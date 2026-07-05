package com.qhathia.mobile.staff.di

import com.qhathia.mobile.staff.BuildConfig
import com.qhathia.mobile.staff.core.network.NetworkConfig
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
}
