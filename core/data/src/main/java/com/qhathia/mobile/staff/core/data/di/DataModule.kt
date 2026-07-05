package com.qhathia.mobile.staff.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.qhathia.mobile.staff.core.data.local.DataStoreSessionStorage
import com.qhathia.mobile.staff.core.data.local.KeystoreSessionCipher
import com.qhathia.mobile.staff.core.data.local.SessionCipher
import com.qhathia.mobile.staff.core.data.local.SessionStorage
import com.qhathia.mobile.staff.core.data.repository.AuthRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.CategoryRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.ProductAiRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.ProductRepositoryImpl
import com.qhathia.mobile.staff.core.data.repository.SessionAuthTokenProvider
import com.qhathia.mobile.staff.core.data.repository.SessionRepositoryImpl
import com.qhathia.mobile.staff.core.domain.repository.AuthRepository
import com.qhathia.mobile.staff.core.domain.repository.CategoryRepository
import com.qhathia.mobile.staff.core.domain.repository.ProductAiRepository
import com.qhathia.mobile.staff.core.domain.repository.ProductRepository
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
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
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindProductAiRepository(impl: ProductAiRepositoryImpl): ProductAiRepository

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

        private const val SESSION_DATASTORE_NAME = "qhathia_session"
    }
}
