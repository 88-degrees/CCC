/*
 * Copyright (c) 2021 Mustafa Ozhan. All rights reserved.
 */
package com.oztechan.ccc.client.viewmodel.calculator

import co.touchlab.kermit.Logger
import com.github.submob.scopemob.mapTo
import com.github.submob.scopemob.whether
import com.github.submob.scopemob.whetherNot
import com.oztechan.ccc.analytics.AnalyticsManager
import com.oztechan.ccc.analytics.model.Event
import com.oztechan.ccc.analytics.model.Param
import com.oztechan.ccc.analytics.model.UserProperty
import com.oztechan.ccc.client.base.BaseSEEDViewModel
import com.oztechan.ccc.client.mapper.toRates
import com.oztechan.ccc.client.mapper.toTodayResponse
import com.oztechan.ccc.client.mapper.toUIModelList
import com.oztechan.ccc.client.model.Currency
import com.oztechan.ccc.client.model.RateState
import com.oztechan.ccc.client.repository.ad.AdRepository
import com.oztechan.ccc.client.util.MAXIMUM_FLOATING_POINT
import com.oztechan.ccc.client.util.calculateResult
import com.oztechan.ccc.client.util.getCurrencyConversionByRate
import com.oztechan.ccc.client.util.getFormatted
import com.oztechan.ccc.client.util.launchIgnored
import com.oztechan.ccc.client.util.toStandardDigits
import com.oztechan.ccc.client.util.toSupportedCharacters
import com.oztechan.ccc.client.viewmodel.calculator.CalculatorData.Companion.CHAR_DOT
import com.oztechan.ccc.client.viewmodel.calculator.CalculatorData.Companion.KEY_AC
import com.oztechan.ccc.client.viewmodel.calculator.CalculatorData.Companion.KEY_DEL
import com.oztechan.ccc.client.viewmodel.calculator.CalculatorData.Companion.MAXIMUM_INPUT
import com.oztechan.ccc.client.viewmodel.calculator.CalculatorData.Companion.MAXIMUM_OUTPUT
import com.oztechan.ccc.client.viewmodel.currencies.CurrenciesData.Companion.MINIMUM_ACTIVE_CURRENCY
import com.oztechan.ccc.common.datasource.currency.CurrencyDataSource
import com.oztechan.ccc.common.datasource.offlinerates.OfflineRatesDataSource
import com.oztechan.ccc.common.datasource.settings.SettingsDataSource
import com.oztechan.ccc.common.model.CurrencyResponse
import com.oztechan.ccc.common.model.Rates
import com.oztechan.ccc.common.service.backend.BackendApiService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class CalculatorViewModel(
    private val settingsDataSource: SettingsDataSource,
    private val backendApiService: BackendApiService,
    private val currencyDataSource: CurrencyDataSource,
    private val offlineRatesDataSource: OfflineRatesDataSource,
    private val adRepository: AdRepository,
    private val analyticsManager: AnalyticsManager
) : BaseSEEDViewModel(), CalculatorEvent {
    // region SEED
    private val _state = MutableStateFlow(CalculatorState())
    override val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<CalculatorEffect>()
    override val effect = _effect.asSharedFlow()

    override val event = this as CalculatorEvent

    override val data = CalculatorData()
    // endregion

    init {
        _state.update(base = settingsDataSource.currentBase, input = "")

        state.map { it.base }
            .distinctUntilChanged()
            .onEach {
                Logger.d { "CalculatorViewModel base changed $it" }
                currentBaseChanged(it, true)
            }
            .launchIn(viewModelScope)

        state.map { it.input }
            .distinctUntilChanged()
            .onEach {
                Logger.d { "CalculatorViewModel input changed $it" }
                calculateOutput(it)
            }
            .launchIn(viewModelScope)

        currencyDataSource.collectActiveCurrencies()
            .onEach {
                Logger.d { "CalculatorViewModel currencyList changed\n${it.joinToString("\n")}" }
                _state.update(currencyList = it.toUIModelList())

                analyticsManager.setUserProperty(UserProperty.CurrencyCount(it.count().toString()))
                analyticsManager.setUserProperty(
                    UserProperty.ActiveCurrencies(it.joinToString(",") { currency -> currency.name })
                )
            }
            .launchIn(viewModelScope)
    }

    private fun getRates() = data.rates?.let {
        calculateConversions(it, RateState.Cached(it.date))
    } ?: viewModelScope.launch {
        runCatching { backendApiService.getRates(settingsDataSource.currentBase) }
            .onFailure(::getRatesFailed)
            .onSuccess(::getRatesSuccess)
    }

    private fun getRatesSuccess(currencyResponse: CurrencyResponse) = currencyResponse
        .toRates().let {
            data.rates = it
            calculateConversions(it, RateState.Online(it.date))
        }.also {
            viewModelScope.launch {
                offlineRatesDataSource.insertOfflineRates(currencyResponse.toTodayResponse())
            }
        }

    private fun getRatesFailed(t: Throwable) = viewModelScope.launchIgnored {
        Logger.w(t) { "CalculatorViewModel getRatesFailed" }
        offlineRatesDataSource.getOfflineRatesByBase(
            settingsDataSource.currentBase
        )?.let {
            calculateConversions(it, RateState.Offline(it.date))
        } ?: run {
            Logger.w(Exception("No offline rates")) { this@CalculatorViewModel::class.simpleName.toString() }

            state.value.currencyList.size
                .whether { it > 1 }
                ?.let { _effect.emit(CalculatorEffect.Error) }
                ?: run { _effect.emit(CalculatorEffect.FewCurrency) }

            _state.update(
                rateState = RateState.Error,
                loading = false
            )
        }
    }

    private fun calculateOutput(input: String) = viewModelScope.launch {
        data.parser
            .calculate(input.toSupportedCharacters(), MAXIMUM_FLOATING_POINT)
            .mapTo { if (isFinite()) getFormatted(settingsDataSource.precision) else "" }
            .whether(
                { output -> output.length <= MAXIMUM_OUTPUT },
                { input.length <= MAXIMUM_INPUT }
            )?.let { output ->
                _state.update(output = output)
                state.value.currencyList.size
                    .whether { it < MINIMUM_ACTIVE_CURRENCY }
                    ?.whetherNot { state.value.input.isEmpty() }
                    ?.let { _effect.emit(CalculatorEffect.FewCurrency) }
                    ?: run { getRates() }
            } ?: run {
            _effect.emit(CalculatorEffect.MaximumInput)
            _state.update(
                input = input.dropLast(1),
                loading = false
            )
        }
    }

    private fun calculateConversions(rates: Rates, rateState: RateState) = _state.update(
        currencyList = _state.value.currencyList.onEach {
            it.rate = rates.calculateResult(it.name, _state.value.output)
                .getFormatted(settingsDataSource.precision)
                .toStandardDigits()
        },
        rateState = rateState,
        loading = false
    )

    private fun currentBaseChanged(newBase: String, shouldTrack: Boolean = false) = viewModelScope.launchIgnored {
        data.rates = null
        settingsDataSource.currentBase = newBase
        _state.update(
            loading = true,
            base = newBase,
            input = _state.value.input,
            symbol = currencyDataSource.getCurrencyByName(newBase)?.symbol.orEmpty()
        )

        if (shouldTrack) {
            analyticsManager.trackEvent(Event.BaseChange(Param.Base(newBase)))
            analyticsManager.setUserProperty(UserProperty.BaseCurrency(newBase))
        }
    }

    fun shouldShowBannerAd() = adRepository.shouldShowBannerAd()

    // region Event
    override fun onKeyPress(key: String) {
        Logger.d { "CalculatorViewModel onKeyPress $key" }
        when (key) {
            KEY_AC -> _state.update(input = "")
            KEY_DEL -> state.value.input
                .whetherNot { isEmpty() }
                ?.apply {
                    _state.update(input = substring(0, length - 1))
                }
            else -> _state.update(input = state.value.input + key)
        }
    }

    override fun onItemClick(currency: Currency) = with(currency) {
        Logger.d { "CalculatorViewModel onItemClick ${currency.name}" }
        _state.update(
            base = name,
            input = if (rate.last() == CHAR_DOT) rate.dropLast(1) else rate
        )
    }

    override fun onItemImageLongClick(currency: Currency) {
        Logger.d { "CalculatorViewModel onItemImageLongClick ${currency.name}" }

        analyticsManager.trackEvent(Event.ShowConversion(Param.Base(currency.name)))

        viewModelScope.launch {
            _effect.emit(
                CalculatorEffect.ShowRate(
                    currency.getCurrencyConversionByRate(
                        settingsDataSource.currentBase,
                        data.rates
                    ),
                    currency.name
                )
            )
        }
    }

    override fun onItemAmountLongClick(amount: String) {
        Logger.d { "CalculatorViewModel onItemAmountLongClick $amount" }

        analyticsManager.trackEvent(Event.CopyClipboard)

        viewModelScope.launch {
            _effect.emit(CalculatorEffect.CopyToClipboard(amount))
        }
    }

    override fun onBarClick() = viewModelScope.launchIgnored {
        Logger.d { "CalculatorViewModel onBarClick" }
        _effect.emit(CalculatorEffect.OpenBar)
    }

    override fun onSettingsClicked() = viewModelScope.launchIgnored {
        Logger.d { "CalculatorViewModel onSettingsClicked" }
        _effect.emit(CalculatorEffect.OpenSettings)
    }

    override fun onBaseChange(base: String) {
        Logger.d { "CalculatorViewModel onBaseChange $base" }
        currentBaseChanged(base)
        calculateOutput(_state.value.input)
    }
    // endregion
}
