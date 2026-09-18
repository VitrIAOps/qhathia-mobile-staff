package com.qhathia.mobile.staff

import android.app.Application
import com.qhathia.mobile.staff.locale.AppLocaleController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Application entry point; bootstraps Hilt's dependency graph and the app-locale sync. */
@HiltAndroidApp
class QhathiaStaffApp : Application() {

    @Inject
    lateinit var appLocaleController: AppLocaleController

    override fun onCreate() {
        super.onCreate()
        appLocaleController.start()
    }
}
