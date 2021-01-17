//
//  MainView.swift
//  ios
//
//  Created by Mustafa Ozhan on 16/11/2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import SwiftUI
import UIKit
import Client

struct CalculatorView: View {

    @ObservedObject
    var calculatorVMWrapper: CalculatorVMWrapper

    init(calculatorVMWrapper: CalculatorVMWrapper) {
        self.calculatorVMWrapper = calculatorVMWrapper
        LoggerKt.kermit.d(withMessage: {"CalculatorView init"})
    }

    var body: some View {
        VStack {
            Text("Test")
        VStack{

            Text(MR.strings().nsBundle.localizedString(forKey: "app_name_ccc", value: nil, table: nil))
                .background(MR.colors().asd.)
//                .background(MR.colors().asd.getC)
//            Text(MR.colors().themedCo)

        }
        .onAppear {
            calculatorVMWrapper.observeStates()
            calculatorVMWrapper.observeEffect()
        }
        .onReceive(calculatorVMWrapper.effect) { onEffect(effect: $0) }
        .onDisappear { calculatorVMWrapper.stopObserving() }
    }

    private func getColor() -> Color {
        let c = MR.colors().asd.color

        return Color(UIColor(red: CGFloat(Int(c.red)), green: CGFloat(Int(c.green)), blue: CGFloat(Int(c.blue)), alpha: CGFloat(Int(c.alpha))))

        return UIColor(
    }



    private func onEffect(effect: CalculatorEffect) {
        LoggerKt.kermit.d(withMessage: {effect.description})
        switch effect {
        default:
            LoggerKt.kermit.d(withMessage: {"unknown effect"})
        }
    }
}

#if DEBUG
struct MainViewPreviews: PreviewProvider {
    @Environment(\.koin) static var koin: Koin

    static var previews: some View {
        CalculatorView(
            calculatorVMWrapper: CalculatorVMWrapper(viewModel: koin.get())
        ).makeForPreviewProvider()
    }
}
#endif
