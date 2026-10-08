package com.qhathia.mobile.staff.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.qhathia.mobile.staff.core.data.local.BuildDeviceLabelProvider
import com.qhathia.mobile.staff.core.data.local.DataStoreSessionStorage
import com.qhathia.mobile.staff.core.data.local.DeviceLabelProvider
import com.qhathia.mobile.staff.core.data.repository.AccountSecurityRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.AppPreferencesRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.NotificationPreferencesRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.StoreSettingsRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.SubscriptionRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.UserProfileRepositoryImpl
import com.qhathia.mobile.staff.core.domain.repository.AccountSecurityRepository
import com.qhathia.mobile.staff.core.domain.repository.AppPreferencesRepository
import com.qhathia.mobile.staff.core.domain.repository.NotificationPreferencesRepository
import com.qhathia.mobile.staff.core.domain.repository.StoreSettingsRepository
import com.qhathia.mobile.staff.core.domain.repository.SubscriptionRepository
import com.qhathia.mobile.staff.core.domain.repository.UserProfileRepository
import com.qhathia.mobile.staff.core.data.local.KeystoreSessionCipher
import com.qhathia.mobile.staff.core.data.local.SessionCipher
import com.qhathia.mobile.staff.core.data.local.SessionStorage
import com.qhathia.mobile.staff.core.data.repository.AuthRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.CategoryRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.DeviceRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.NotificationTokenRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.OrderRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.ProductAiRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.ProductDiscountRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.ProductRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.SessionAuthTokenProvider
import com.qhathia.mobile.staff.core.data.repository.SessionRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.StoreInsightsRepositoryImpl
import com.qhathia.mobile.staff.core.domain.repository.AuthRepository
import com.qhathia.mobile.staff.core.domain.repository.CategoryRepository
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import com.qhathia.mobile.staff.core.domain.repository.NotificationTokenRepository
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import com.qhathia.mobile.staff.core.domain.repository.ProductAiRepository
import com.qhathia.mobile.staff.core.domain.repository.ProductDiscountRepository
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import com.qhathia.mobile.staff.core.domain.repository.StoreInsightsRepository
import com.qhathia.mobile.staff.core.network.AuthTokenProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindProductDiscountRepository(impl: ProductDiscountRepositoryImpl): ProductDiscountRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindProductAiRepository(impl: ProductAiRepositoryImpl): ProductAiRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindStoreInsightsRepository(
        impl: StoreInsightsRepositoryImpl,
    ): StoreInsightsRepository

    @Binds
    @Singleton
    abstract fun bindNotificationTokenRepository(
        impl: NotificationTokenRepositoryImpl,
    ): NotificationTokenRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindNotificationPreferencesRepository(
        impl: NotificationPreferencesRepositoryImpl,
    ): NotificationPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAccountSecurityRepository(
        impl: AccountSecurityRepositoryImpl,
    ): AccountSecurityRepository

    @Binds
    @Singleton
    abstract fun bindStoreSettingsRepository(
        impl: StoreSettingsRepositoryImpl,
    ): StoreSettingsRepository

    @Binds
    @Singleton
    abstract fun bindAppPreferencesRepository(
        impl: AppPreferencesRepositoryImpl,
    ): AppPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindDeviceLabelProvider(impl: BuildDeviceLabelProvider): DeviceLabelProvider

    @Binds
    @Singleton
    abstract fun bindSessionStorage(impl: DataStoreSessionStorage): SessionStorage

    @Binds
    @Singleton
    abstract fun bindSessionCipher(impl: KeystoreSessionCipher): SessionCipher

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(impl: SessionAuthTokenProvider): AuthTokenProvider

    companion object {
        @Provides
        @Singleton
        fun provideSessionDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(SESSION_DATASTORE_NAME) },
        )

        @Provides
        @Singleton
        @AppPreferencesDataStore
        fun provideAppPreferencesDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(APP_PREFERENCES_DATASTORE_NAME) },
        )

        private const val SESSION_DATASTORE_NAME = "qhathia_session"
        private const val APP_PREFERENCES_DATASTORE_NAME = "qhathia_app_prefs"
    }
}
