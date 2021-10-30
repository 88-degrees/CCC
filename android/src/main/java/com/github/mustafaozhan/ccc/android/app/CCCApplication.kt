/*
 * Copyright (c) 2020 Mustafa Ozhan. All rights reserved.
 */
package com.github.mustafaozhan.ccc.android.app

import android.app.Application
import co.touchlab.kermit.Logger
import com.github.mustafaozhan.ad.initAds
import com.github.mustafaozhan.ccc.android.di.platformModule
import com.github.mustafaozhan.ccc.client.di.initAndroid
import com.github.mustafaozhan.logmob.initCrashlytics
import com.github.mustafaozhan.logmob.initLogger

@Suppress("unused")
class CCCApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initLogger()

        Logger.i { "CCCApplication onCreate" }

        initAds(this)

        initAndroid(
            context = this,
            platformModule = platformModule
        )

        initCrashlytics(
            context = this,
            enableAnalytics = true
        )
    }
}
