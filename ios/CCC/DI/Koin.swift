//
//  Koin.swift
//  CCC
//
//  Created by Mustafa Ozhan on 16/11/2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import SwiftUI
import Client

var koin: Koin_coreKoin = {
    let userDefaults = UserDefaults(suiteName: "application_user_defaults")!

    return IOSKoinKt.doInitIOS(
        userDefaults: userDefaults,
        analyticsManager: AnalyticsManagerImpl()
    ).koin
}()

extension Koin_coreKoin {
    // swiftlint:disable force_cast
    func get<T>() -> T {
        return koin.getDependency(objCObject: T.self) as! T
    }
}
