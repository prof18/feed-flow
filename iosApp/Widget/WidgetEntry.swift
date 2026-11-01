//
//  SimpleEntry.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 02/03/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//
import FeedFlowKit
import WidgetKit

struct WidgetEntry: TimelineEntry {
    let date: Date
    let feedItems: [FeedItemWidget]
    let widgetTitle: String
    let widgetEmptyScreenTitle: String
    let widgetEmptyScreenContent: String
}
