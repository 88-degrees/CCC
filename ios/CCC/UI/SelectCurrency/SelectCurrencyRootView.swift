//
//  SelectCurrencyRootView.swift
//  CCC
//
//  Created by Mustafa Ozhan on 23/01/2021.
//  Copyright © 2021 orgName. All rights reserved.
//

import NavigationStack
import Provider
import SwiftUI

struct SelectCurrencyRootView: View {
    @StateObject var observable = ObservableSEEDViewModel<
        SelectCurrencyState,
        SelectCurrencyEffect,
        SelectCurrencyEvent,
        BaseData,
        SelectCurrencyViewModel
    >()
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var navigationStack: NavigationStackCompat
    @Binding var isBarShown: Bool

    private let analyticsManager: AnalyticsManager = koin.get()

    var body: some View {
        SelectCurrencyView(
            event: observable.event,
            state: observable.state
        )
        .onAppear {
            observable.startObserving()
            analyticsManager.trackScreen(screenName: ScreenName.SelectCurrency())
        }
        .onDisappear { observable.stopObserving() }
        .onReceive(observable.effect) { onEffect(effect: $0) }
    }

    private func onEffect(effect: SelectCurrencyEffect) {
        logger.i(message: { "SelectCurrencyRootView onEffect \(effect.description)" })
        switch effect {
        case let currencyChangeEffect as SelectCurrencyEffect.DismissDialog:
            isBarShown = false
        case is SelectCurrencyEffect.OpenCurrencies:
            navigationStack.push(CurrenciesRootView())
        default:
            logger.i(message: { "SelectCurrencyRootView unknown effect" })
        }
    }
}
