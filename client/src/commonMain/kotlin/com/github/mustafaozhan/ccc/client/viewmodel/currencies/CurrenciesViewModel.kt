/*
 * Copyright (c) 2021 Mustafa Ozhan. All rights reserved.
 */
package com.github.mustafaozhan.ccc.client.viewmodel.currencies

import com.github.mustafaozhan.ccc.client.base.BaseSEEDViewModel
import com.github.mustafaozhan.ccc.client.model.Currency
import com.github.mustafaozhan.ccc.client.model.mapToModel
import com.github.mustafaozhan.ccc.client.util.MINIMUM_ACTIVE_CURRENCY
import com.github.mustafaozhan.ccc.client.util.isEmptyOrNullString
import com.github.mustafaozhan.ccc.client.util.isRewardExpired
import com.github.mustafaozhan.ccc.client.util.toUnit
import com.github.mustafaozhan.ccc.client.viewmodel.currencies.CurrenciesState.Companion.update
import com.github.mustafaozhan.ccc.common.db.dao.CurrencyDao
import com.github.mustafaozhan.ccc.common.settings.SettingsRepository
import com.github.mustafaozhan.logmob.kermit
import com.github.mustafaozhan.scopemob.either
import com.github.mustafaozhan.scopemob.mapTo
import com.github.mustafaozhan.scopemob.whether
import com.github.mustafaozhan.scopemob.whetherNot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class CurrenciesViewModel(
    private val settingsRepository: SettingsRepository,
    private val currencyDao: CurrencyDao
) : BaseSEEDViewModel(), CurrenciesEvent {
    // region SEED
    private val _state = MutableStateFlow(CurrenciesState())
    override val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<CurrenciesEffect>()
    override val effect = _effect.asSharedFlow()

    override val event = this as CurrenciesEvent

    override val data = CurrenciesData()
    // endregion

    init {
        kermit.d { "CurrenciesViewModel init" }
        _state.update(loading = true)

        currencyDao.collectAllCurrencies()
            .mapToModel()
            .onEach { currencyList ->

                _state.update(currencyList = currencyList)
                data.unFilteredList = currencyList.toMutableList()

                verifyListSize()
                verifyCurrentBase()

                filterList(data.query)
                _state.update(selectionVisibility = false)
            }.launchIn(clientScope)

        filterList("")
    }

    private fun verifyListSize() = _state.value.currencyList
        .filter { it.isActive }.size
        .whether { it < MINIMUM_ACTIVE_CURRENCY }
        ?.whetherNot { settingsRepository.firstRun }
        ?.mapTo { clientScope }
        ?.launch { _effect.emit(CurrenciesEffect.FewCurrency) }

    private fun verifyCurrentBase() = settingsRepository.currentBase.either(
        { isEmptyOrNullString() },
        { base ->
            state.value.currencyList
                .filter { it.name == base }
                .toList().firstOrNull()?.isActive == false
        }
    )?.let {
        (state.value.currencyList.firstOrNull { it.isActive }?.name ?: "").let { newBase ->
            settingsRepository.currentBase = newBase
            clientScope.launch { _effect.emit(CurrenciesEffect.ChangeBase(newBase)) }
        }
    }

    private fun filterList(txt: String) = data.unFilteredList
        .filter { (name, longName, symbol) ->
            name.contains(txt, true) ||
                longName.contains(txt, true) ||
                symbol.contains(txt, true)
        }.toMutableList()
        .let {
            _state.update(currencyList = it, loading = false)
        }.run {
            data.query = txt
        }

    fun hideSelectionVisibility() {
        _state.update(selectionVisibility = false)
    }

    fun isRewardExpired() = settingsRepository.adFreeEndDate.isRewardExpired()

    fun isFirstRun() = settingsRepository.firstRun

    override fun onCleared() {
        kermit.d { "CurrenciesViewModel onCleared" }
        super.onCleared()
    }

    // region Event
    override fun updateAllCurrenciesState(state: Boolean) {
        kermit.d { "CurrenciesViewModel updateAllCurrenciesState $state" }
        currencyDao.updateAllCurrencyState(state)
    }

    override fun onItemClick(currency: Currency) {
        kermit.d { "CurrenciesViewModel onItemClick ${currency.name}" }
        currencyDao.updateCurrencyStateByName(currency.name, !currency.isActive)
    }

    override fun onDoneClick() = clientScope.launch {
        kermit.d { "CurrenciesViewModel onDoneClick" }
        data.unFilteredList
            .filter { it.isActive }.size
            .whether { it < MINIMUM_ACTIVE_CURRENCY }
            ?.let { _effect.emit(CurrenciesEffect.FewCurrency) }
            ?: run {
                settingsRepository.firstRun = false
                _effect.emit(CurrenciesEffect.OpenCalculator)
            }
    }.toUnit()

    override fun onItemLongClick() = _state.value.selectionVisibility.let {
        kermit.d { "CurrenciesViewModel onItemLongClick" }
        _state.update(selectionVisibility = !it)
    }

    override fun onCloseClick() = clientScope.launch {
        kermit.d { "CurrenciesViewModel onCloseClick" }
        if (_state.value.selectionVisibility) {
            _state.update(selectionVisibility = false)
        } else {
            _effect.emit(CurrenciesEffect.Back)
        }.run {
            filterList("")
        }
    }.toUnit()

    override fun onQueryChange(query: String) {
        kermit.d { "CurrenciesViewModel onQueryChange $query" }
        filterList(query)
    }
    // endregion
}
