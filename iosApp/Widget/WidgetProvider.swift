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
    init() {
        startKoin()
    }

    func placeholder(in _: Context) -> WidgetEntry {
        WidgetEntry(
            date: Date(),
            feedItems: [],
            widgetTitle: feedFlowStrings.widgetLatestItems,
            widgetEmptyScreenTitle: feedFlowStrings.emptyFeedMessage,
            widgetEmptyScreenContent: feedFlowStrings.widgetCheckFeedSources
        )
    }

    func getSnapshot(in _: Context, completion: @escaping (WidgetEntry) -> Void) {
        Task {
            do {
                let feedItems = try await Deps.shared.getFeedWidgetRepository().getFeedItems(pageSize: 6)
                let entry = WidgetEntry(
                    date: Date(),
                    feedItems: feedItems,
                    widgetTitle: feedFlowStrings.widgetLatestItems,
                    widgetEmptyScreenTitle: feedFlowStrings.emptyFeedMessage,
                    widgetEmptyScreenContent: feedFlowStrings.widgetCheckFeedSources
                )
                DispatchQueue.main.async {
                    completion(entry)
                }
            } catch {
                print("Error fetching feed items: \(error)")
                DispatchQueue.main.async {
                    completion(
                        WidgetEntry(
                            date: Date(),
                            feedItems: [],
                            widgetTitle: feedFlowStrings.widgetLatestItems,
                            widgetEmptyScreenTitle: feedFlowStrings.emptyFeedMessage,
                            widgetEmptyScreenContent: feedFlowStrings.widgetCheckFeedSources
                        )
                    )
                }
            }
        }
    }

    func getTimeline(in _: Context, completion: @escaping (Timeline<WidgetEntry>) -> Void) {
        Task {
            do {
                let feedItems = try await Deps.shared.getFeedWidgetRepository().getFeedItems(pageSize: 6)

                let currentDate = Date()
                let refreshDate = Calendar.current.date(byAdding: .hour, value: 1, to: currentDate)!
                let entry = WidgetEntry(
                    date: currentDate,
                    feedItems: feedItems,
                    widgetTitle: feedFlowStrings.widgetLatestItems,
                    widgetEmptyScreenTitle: feedFlowStrings.emptyFeedMessage,
                    widgetEmptyScreenContent: feedFlowStrings.widgetCheckFeedSources
                )

                DispatchQueue.main.async {
                    let timeline = Timeline(entries: [entry], policy: .after(refreshDate))
                    completion(timeline)
                }
            } catch {
                print("Error fetching feed items: \(error)")
                DispatchQueue.main.async {
                    let entry = WidgetEntry(
                        date: Date(),
                        feedItems: [],
                        widgetTitle: feedFlowStrings.widgetLatestItems,
                        widgetEmptyScreenTitle: feedFlowStrings.emptyFeedMessage,
                        widgetEmptyScreenContent: feedFlowStrings.widgetCheckFeedSources
                    )
                    let refreshDate = Calendar.current.date(byAdding: .hour, value: 1, to: Date())!
                    let timeline = Timeline(entries: [entry], policy: .after(refreshDate))
                    completion(timeline)
                }
            }
        }
    }
}
