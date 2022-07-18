/*
 * Copyright (c) 2021 Mustafa Ozhan. All rights reserved.
 */
package com.oztechan.ccc.common.api.repo

import co.touchlab.kermit.Logger
import com.oztechan.ccc.common.api.service.ApiService
import com.oztechan.ccc.common.mapper.toModel
import com.oztechan.ccc.common.model.EmptyParameterException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class ApiRepositoryImpl(
    private val apiService: ApiService,
    private val ioDispatcher: CoroutineDispatcher
) : ApiRepository {

    override suspend fun getRatesByBackend(
        base: String
    ) = withContext(ioDispatcher) {
        Logger.v { "ApiRepositoryImpl getRatesByBackend $base" }

        if (base.isEmpty()) {
            throw EmptyParameterException()
        } else {
            apiService.getRatesByBackend(base).toModel(base)
        }
    }

    override suspend fun getRatesByPremiumAPI(
        base: String
    ) = withContext(ioDispatcher) {
        Logger.v { "ApiRepositoryImpl getRatesByPremiumAPI $base" }

        if (base.isEmpty()) {
            throw EmptyParameterException()
        } else {
            apiService.getRatesByPremiumAPI(base).toModel(base)
        }
    }
}
