package com.qhathia.mobile.staff.core.notifications.di

import com.qhathia.mobile.staff.core.domain.repository.OrderNotificationTray
import com.qhathia.mobile.staff.core.domain.repository.PushTokenProvider
import com.qhathia.mobile.staff.core.notifications.FirebaseMessagingPushTokenProvider
import com.qhathia.mobile.staff.core.notifications.OrderNotificationPresenter
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

    /** The presenter both posts order notifications and answers for them, so it is the tray. */
    @Binds
    @Singleton
    abstract fun bindOrderNotificationTray(
        impl: OrderNotificationPresenter,
    ): OrderNotificationTray
}
