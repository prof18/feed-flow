//
//  WidgetProvider.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 02/03/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI
import WidgetKit

struct Provider: TimelineProvider {
    let feedFlowStrings: WidgetStrings
    let appEnvironment: AppEnvironment

    init() {
        #if DEBUG
            appEnvironment = AppEnvironment.Debug()
        #else
            appEnvironment = AppEnvironment.Release()
        #endif

        let currentLocale = Locale.current
        let languageCode = currentLocale.language.languageCode?.identifier
        let regionCode = currentLocale.region?.identifier

        feedFlowStrings = getWidgetStrings(languageCode: languageCode, regionCode: regionCode)
    }

    func placeholder(in _: Context) -> WidgetEntry {
        WidgetEntry(
            date: Date(),
            feedItems: [],
            widgetTitle: feedFlowStrings.widgetTitle,
            widgetEmptyScreenTitle: feedFlowStrings.widgetEmptyScreenTitle,
            widgetEmptyScreenContent: feedFlowStrings.widgetEmptyScreenContent
        )
    }

    func getSnapshot(in _: Context, completion: @escaping (WidgetEntry) -> Void) {
        let entry = WidgetEntry(
            date: Date(),
            feedItems: getFeedItems(appEnvironment: appEnvironment),
            widgetTitle: feedFlowStrings.widgetTitle,
            widgetEmptyScreenTitle: feedFlowStrings.widgetEmptyScreenTitle,
            widgetEmptyScreenContent: feedFlowStrings.widgetEmptyScreenContent
        )
        completion(entry)
    }

    func getTimeline(in _: Context, completion: @escaping (Timeline<WidgetEntry>) -> Void) {
        let currentDate = Date()
        let refreshDate = Calendar.current.date(byAdding: .hour, value: 1, to: currentDate)!
        let entry = WidgetEntry(
            date: currentDate,
            feedItems: getFeedItems(appEnvironment: appEnvironment),
            widgetTitle: feedFlowStrings.widgetTitle,
            widgetEmptyScreenTitle: feedFlowStrings.widgetEmptyScreenTitle,
            widgetEmptyScreenContent: feedFlowStrings.widgetEmptyScreenContent
        )

        let timeline = Timeline(entries: [entry], policy: .after(refreshDate))
        completion(timeline)
    }
}
