//
//  Koin.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 04/03/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import shared

func startKoin() {
    let koinApplication = KoinIOSKt.doInitKoinIos()
    _koin = koinApplication.koin
}

private var _koin: Koin_coreKoin?
var koin: Koin_coreKoin {
    return _koin!
}
