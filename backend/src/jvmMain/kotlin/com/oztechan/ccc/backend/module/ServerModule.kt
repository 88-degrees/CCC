package com.oztechan.ccc.backend.module

import co.touchlab.kermit.Logger
import com.oztechan.ccc.backend.controller.server.ServerController
import com.oztechan.ccc.backend.routes.getCurrencyByName
import com.oztechan.ccc.backend.routes.getError
import com.oztechan.ccc.backend.routes.getRoot
import com.oztechan.ccc.backend.routes.getVersion
import com.oztechan.ccc.common.core.infrastructure.di.DISPATCHER_IO
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject

@Suppress("unused")
internal fun Application.serverModule() {
    Logger.i { "ServerModuleKt Application.serverModule" }

    val serverController: ServerController by inject()
    val globalScope: CoroutineScope by inject()
    val ioDispatcher: CoroutineDispatcher by inject(named(DISPATCHER_IO))

    routing {
        globalScope.launch(ioDispatcher) {
            getError()
            getRoot()
            getCurrencyByName(serverController)
            getVersion()
        }
    }
}
