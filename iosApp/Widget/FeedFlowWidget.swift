//
//  FeedFlowWidget.swift
//  Widget
//
//  Created by Marco Gomiero on 02/03/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI
import WidgetKit

struct FeedFlowWidget: Widget {
    nonisolated static let kind = "com.prof18.feedflow.widget.main"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: Self.kind,
            intent: FeedFlowWidgetConfigurationIntent.self,
            provider: Provider()
        ) { entry in
            WidgetEntryView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("FeedFlow")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .systemExtraLarge])
    }
}
