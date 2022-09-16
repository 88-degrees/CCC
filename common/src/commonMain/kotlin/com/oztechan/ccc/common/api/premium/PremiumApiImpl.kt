/*
 * Copyright (c) 2021 Mustafa Ozhan. All rights reserved.
 */
package com.oztechan.ccc.common.api.premium

import com.oztechan.ccc.common.BuildKonfig
import com.oztechan.ccc.common.api.model.CurrencyResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.path
import io.ktor.http.takeFrom

internal class PremiumApiImpl(private val client: HttpClient) : PremiumApi {

    override suspend fun getRates(base: String): CurrencyResponse = client.get {
        url {
//            takeFrom(BuildKonfig.BASE_URL_API_PREMIUM)
            takeFrom("https://gist.githubusercontent.com/mustafaozhan/fa6d05e65919085f871adc825accea46/raw/d3bf3a7771e872e0c39541fe23b4058f4ae24c41/response.json")
            path(PATH_PREMIUM_VERSION, BuildKonfig.API_KEY_PREMIUM, PATH_LATEST, base)
        }
    }.body()

    companion object {
        private const val PATH_LATEST = "latest"
        private const val PATH_PREMIUM_VERSION = "v6"
    }
}
