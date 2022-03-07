/*
 * Copyright (c) 2021 Mustafa Ozhan. All rights reserved.
 */
package com.github.mustafaozhan.ccc.client.viewmodel

import com.github.mustafaozhan.ccc.client.helper.SessionManager
import com.github.mustafaozhan.ccc.client.mapper.toUIModel
import com.github.mustafaozhan.ccc.client.util.after
import com.github.mustafaozhan.ccc.client.util.before
import com.github.mustafaozhan.ccc.client.util.getCurrencyConversionByRate
import com.github.mustafaozhan.ccc.client.viewmodel.calculator.CalculatorData.Companion.KEY_AC
import com.github.mustafaozhan.ccc.client.viewmodel.calculator.CalculatorData.Companion.KEY_DEL
import com.github.mustafaozhan.ccc.client.viewmodel.calculator.CalculatorEffect
import com.github.mustafaozhan.ccc.client.viewmodel.calculator.CalculatorViewModel
import com.github.mustafaozhan.ccc.common.api.repo.ApiRepository
import com.github.mustafaozhan.ccc.common.db.currency.CurrencyRepository
import com.github.mustafaozhan.ccc.common.db.offlinerates.OfflineRatesRepository
import com.github.mustafaozhan.ccc.common.model.Currency
import com.github.mustafaozhan.ccc.common.model.CurrencyResponse
import com.github.mustafaozhan.ccc.common.model.Rates
import com.github.mustafaozhan.ccc.common.runTest
import com.github.mustafaozhan.ccc.common.settings.SettingsRepository
import com.github.mustafaozhan.logmob.initLogger
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.given
import io.mockative.mock
import io.mockative.verify
import kotlinx.coroutines.flow.flow
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class CalculatorViewModelTest {

    @Mock
    private val settingsRepository = mock(classOf<SettingsRepository>())

    @Mock
    private val apiRepository = mock(classOf<ApiRepository>())

    @Mock
    private val currencyRepository = mock(classOf<CurrencyRepository>())

    @Mock
    private val offlineRatesRepository = mock(classOf<OfflineRatesRepository>())

    @Mock
    private val sessionManager = mock(classOf<SessionManager>())

    private val viewModel: CalculatorViewModel by lazy {
        CalculatorViewModel(
            settingsRepository,
            apiRepository,
            currencyRepository,
            offlineRatesRepository,
            sessionManager
        )
    }

    private val currency = Currency("USD", "Dollar", "$", 12345.6789, true)
    private val currencyUIModel = currency.toUIModel()
    private val currencyResponse = CurrencyResponse(currency.name, null, Rates())

    @BeforeTest
    fun setup() {
        initLogger(true)

        given(settingsRepository)
            .invocation { currentBase }
            .thenReturn("")

        given(currencyRepository)
            .invocation { collectActiveCurrencies() }
            .thenReturn(flow { listOf(currency) })
    }

    @Test
    fun shouldShowBannerAd() {
        val mockBoolean = Random.nextBoolean()

        given(sessionManager)
            .invocation { shouldShowBannerAd() }
            .thenReturn(mockBoolean)

        assertEquals(mockBoolean, viewModel.shouldShowBannerAd())

        verify(sessionManager)
            .invocation { shouldShowBannerAd() }
            .wasInvoked()
    }

    // Event

    @Test
    fun onBarClick() = viewModel.effect.before {
        viewModel.event.onBarClick()
    }.after {
        assertIs<CalculatorEffect.OpenBar>(it)
    }

    @Test
    fun onSettingsClicked() = viewModel.effect.before {
        viewModel.event.onSettingsClicked()
    }.after {
        assertIs<CalculatorEffect.OpenSettings>(it)
    }

    @Test
    fun onItemClick() = viewModel.state.before {
        viewModel.event.onItemClick(currencyUIModel)
    }.after {
        assertEquals(currencyUIModel.name, it?.base)
        assertEquals(currencyUIModel.rate.toString(), it?.input)
    }

    @Test
    fun onItemLongClick() = viewModel.effect.before {
        viewModel.event.onItemLongClick(currencyUIModel)
    }.after {
        assertIs<CalculatorEffect.ShowRate>(it)
        assertEquals(
            currencyUIModel.getCurrencyConversionByRate(
                viewModel.state.value.base,
                viewModel.data.rates
            ),
            it.text
        )
        assertEquals(currencyUIModel.name, it.name)
    }

    @Test
    fun onKeyPress() = with(viewModel) {
        val key = "1"

        state.before {
            event.onKeyPress(key)
        }.after {
            assertEquals(key, it?.input)
        }

        state.before {
            event.onKeyPress(KEY_AC)
        }.after {
            assertEquals("", it?.input)
        }

        state.before {
            event.onKeyPress(key)
            event.onKeyPress(key)
            event.onKeyPress(KEY_DEL)
        }.after {
            assertEquals(key, it?.input)
        }
    }

    @Test
    fun onBaseChanged() {
        given(settingsRepository)
            .invocation { currentBase }
            .thenReturn(currency.name)

        given(currencyRepository)
            .invocation { currencyRepository.getCurrencyByName(currency.name) }
            .thenReturn(currency)

        runTest {
            given(apiRepository)
                .coroutine { getRatesByBackend(currency.name) }
                .thenReturn(currencyResponse)
        }

        viewModel.state.before {
            viewModel.event.onBaseChange(currency.name)
        }.after {
            assertEquals(currency.name, viewModel.data.rates?.base)
            assertNotNull(viewModel.data.rates)
            assertEquals(currency.name, it?.base)
        }
    }
}
