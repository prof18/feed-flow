//
//  Koin.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 04/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import shared

func startKoin() {
    let appEnvironment: AppEnvironment
    #if DEBUG
        appEnvironment = AppEnvironment.Debug()
    #else
        appEnvironment = AppEnvironment.Release()
    #endif

    let langCode = Locale.current.language.languageCode?.identifier ?? "en"

    let koinApplication = doInitKoinIos(
        htmlParser: IosHtmlParser(),
        appEnvironment: appEnvironment,
        languageCode: langCode,
        dropboxDataSource: DropboxDataSourceIos()
    )
    _koin = koinApplication.koin
    _feedFlowStrings = KotlinDependencies.shared.getFeedFlowStrings()
}

private var _koin: Koin_coreKoin?
var koin: Koin_coreKoin {
    return _koin!
}

private var _feedFlowStrings: FeedFlowStrings?
var feedFlowStrings: FeedFlowStrings {
    return _feedFlowStrings!
}
