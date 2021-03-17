package com.github.mustafaozhan.ccc.client.viewmodel.calculator

import com.github.mustafaozhan.ccc.client.base.BaseData
import com.github.mustafaozhan.ccc.client.base.BaseEffect
import com.github.mustafaozhan.ccc.client.base.BaseEvent
import com.github.mustafaozhan.ccc.client.base.BaseState
import com.github.mustafaozhan.ccc.client.model.Currency
import com.github.mustafaozhan.ccc.client.model.DataState
import com.github.mustafaozhan.ccc.common.model.Rates
import com.github.mustafaozhan.parsermob.ParserMob
import kotlinx.coroutines.flow.MutableStateFlow

// State
data class CalculatorState(
    val input: String = "",
    val base: String = "",
    val currencyList: List<Currency> = listOf(),
    val output: String = "",
    val symbol: String = "",
    val loading: Boolean = true,
    val dataState: DataState = DataState.Error,
) : BaseState() {
    // for ios
    constructor() : this("", "", listOf(), "", "", true, DataState.Error)

    companion object {
        @Suppress("LongParameterList")
        fun MutableStateFlow<CalculatorState>.update(
            input: String = value.input,
            base: String = value.base,
            currencyList: List<Currency> = value.currencyList,
            output: String = value.output,
            symbol: String = value.symbol,
            loading: Boolean = value.loading,
            dataState: DataState = value.dataState
        ) {
            value = value.copy(
                input = input,
                base = base,
                currencyList = currencyList,
                output = output,
                symbol = symbol,
                loading = loading,
                dataState = dataState
            )
        }
    }
}

// Event
interface CalculatorEvent : BaseEvent {
    fun onKeyPress(key: String)
    fun onItemClick(currency: Currency, conversion: String)
    fun onItemLongClick(currency: Currency): Boolean
    fun onBarClick()
    fun onSpinnerItemSelected(base: String)
    fun onSettingsClicked()
    fun onBaseChange(base: String)
}

// Effect
sealed class CalculatorEffect : BaseEffect() {
    object Error : CalculatorEffect()
    object FewCurrency : CalculatorEffect()
    object OpenBar : CalculatorEffect()
    object MaximumInput : CalculatorEffect()
    object OpenSettings : CalculatorEffect()
    data class ShowRate(val text: String, val name: String) : CalculatorEffect()
}

// Data
data class CalculatorData(
    var parser: ParserMob = ParserMob(),
    var rates: Rates? = null
) : BaseData()
