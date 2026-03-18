//
//  Koin.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 04/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation

private class NoOpNotifier: Notifier {
    func showNewArticlesNotification(feedSourcesToNotify: [FeedSourceToNotify]) -> Bool { false }
}

func startKoin(notifier: (any Notifier)? = nil) {
    let appEnvironment: AppEnvironment
    #if DEBUG
        appEnvironment = AppEnvironment.Debug()
    #else
        appEnvironment = AppEnvironment.Release()
    #endif

    let currentLocale = Locale.current
    let languageCode = currentLocale.language.languageCode?.identifier
    let regionCode = currentLocale.region?.identifier

    _ = doInitKoinIos(
        htmlParser: IosHtmlParser(),
        appEnvironment: appEnvironment,
        languageCode: languageCode,
        regionCode: regionCode,
        dropboxDataSource: DropboxDataSourceIos(),
        googleDrivePlatformClient: GoogleDrivePlatformClient(),
        appVersion: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "",
        telemetry: TelemetryReporter(),
        feedItemParserWorker: FeedItemParserWorkerIos(),
        notifier: notifier ?? NoOpNotifier()
    )
    _feedFlowStrings = Deps.shared.getFeedFlowStrings()
}

private var _feedFlowStrings: FeedFlowStrings?
var feedFlowStrings: FeedFlowStrings {
    guard let strings = _feedFlowStrings else {
        fatalError("FeedFlowStrings not initialized. Make sure to call startKoin first.")
    }
    return strings
}
