package com.qhathia.mobile.staff.core.notifications.di

import com.qhathia.mobile.staff.core.domain.repository.PushTokenProvider
import com.qhathia.mobile.staff.core.notifications.FirebaseMessagingPushTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the notifications-layer implementations of domain ports. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {

    @Binds
    @Singleton
    abstract fun bindPushTokenProvider(
        impl: FirebaseMessagingPushTokenProvider,
    ): PushTokenProvider
}
