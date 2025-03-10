package com.oztechan.ccc.android.viewmodel.widget

import co.touchlab.kermit.Logger
import com.oztechan.ccc.android.viewmodel.widget.WidgetData.Companion.MAXIMUM_NUMBER_OF_CURRENCY
import com.oztechan.ccc.client.core.shared.util.getFormatted
import com.oztechan.ccc.client.core.shared.util.getRateFromCode
import com.oztechan.ccc.client.core.shared.util.isNotPassed
import com.oztechan.ccc.client.core.shared.util.nowAsDateString
import com.oztechan.ccc.client.core.viewmodel.BaseEffect
import com.oztechan.ccc.client.core.viewmodel.BaseSEEDViewModel
import com.oztechan.ccc.client.core.viewmodel.util.launchIgnored
import com.oztechan.ccc.client.datasource.currency.CurrencyDataSource
import com.oztechan.ccc.client.service.backend.BackendApiService
import com.oztechan.ccc.client.storage.app.AppStorage
import com.oztechan.ccc.client.storage.calculation.CalculationStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetViewModel(
    private val calculationStorage: CalculationStorage,
    private val backendApiService: BackendApiService,
    private val currencyDataSource: CurrencyDataSource,
    private val appStorage: AppStorage
) : BaseSEEDViewModel<WidgetState, BaseEffect, WidgetEvent, WidgetData>(), WidgetEvent {

    // region SEED
    private val _state = MutableStateFlow(WidgetState())
    override val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<WidgetEffect>()
    override val effect = _effect.asSharedFlow()

    override val event = this as WidgetEvent

    override val data = WidgetData()
    // endregion

    init {
        viewModelScope.launchIgnored {
            _state.update {
                it.copy(
                    currentBase = calculationStorage.getBase(),
                    isPremium = appStorage.getPremiumEndDate().isNotPassed()
                )
            }
        }
    }

    private suspend fun refreshWidgetData() {
        _state.update {
            it.copy(
                currencyList = listOf(),
                lastUpdate = "",
                currentBase = calculationStorage.getBase(),
                isPremium = appStorage.getPremiumEndDate().isNotPassed()
            )
        }

        if (_state.value.isPremium) {
            getFreshWidgetData()
        }
    }

    private fun getFreshWidgetData() = viewModelScope.launch {
        val conversion = backendApiService
            .getConversion(calculationStorage.getBase())

        currencyDataSource.getActiveCurrencies()
            .filterNot { it.code == calculationStorage.getBase() }
            .onEach {
                it.rate = conversion.getRateFromCode(it.code)
                    ?.getFormatted(calculationStorage.getPrecision())
                    .orEmpty()
            }
            .take(MAXIMUM_NUMBER_OF_CURRENCY)
            .let { currencyList ->
                _state.update {
                    it.copy(
                        currencyList = currencyList,
                        lastUpdate = nowAsDateString()
                    )
                }
            }
    }

    private suspend fun updateBase(isToNext: Boolean) {
        val activeCurrencies = currencyDataSource.getActiveCurrencies()

        val newBaseIndex = activeCurrencies
            .map { it.code }
            .indexOf(calculationStorage.getBase())
            .let {
                if (isToNext) {
                    it + 1
                } else {
                    it - 1
                }
            }.let {
                (it + activeCurrencies.size) % activeCurrencies.size // it handles index -1 and index size +1
            }

        calculationStorage.setBase(activeCurrencies[newBaseIndex].code)
    }

    // region Event
    override fun onPreviousClick() = viewModelScope.launchIgnored {
        Logger.d { "WidgetViewModel onPreviousClick" }
        updateBase(false)
        refreshWidgetData()
    }

    override fun onNextClick() = viewModelScope.launchIgnored {
        Logger.d { "WidgetViewModel onNextClick" }
        updateBase(true)
        refreshWidgetData()
    }

    override fun onRefreshClick() = viewModelScope.launchIgnored {
        Logger.d { "WidgetViewModel onRefreshClick" }
        refreshWidgetData()
    }

    override fun onOpenAppClick() = viewModelScope.launchIgnored {
        Logger.d { "WidgetViewModel onOpenAppClick" }
        _effect.emit(WidgetEffect.OpenApp)
    }
    // endregion
}
