/*
 * Copyright (c) 2021 Mustafa Ozhan. All rights reserved.
 */

package com.github.mustafaozhan.ccc.common

import com.github.mustafaozhan.ccc.common.model.PlatformType
import com.russhwolf.settings.JsSettings
import com.russhwolf.settings.Settings
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platform = PlatformType.JS

actual val platformCoroutineContext: CoroutineContext = Dispatchers.Default

actual fun Module.getSettingsDefinition() = single<Settings> { JsSettings(get()) }

actual fun getPlatformCommonModule(useFakes: Boolean): Module = module {
    // todo JS SqlDelight is not ready yet. https://github.com/cashapp/sqldelight/issues/1667
}

actual fun runTest(block: suspend () -> Unit): dynamic = GlobalScope.promise { block() }
