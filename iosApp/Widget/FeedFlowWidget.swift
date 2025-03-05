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
    let kind: String = "com.prof18.feedflow.widget.main"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            if #available(iOS 17.0, *) {
                WidgetEntryView(entry: entry)
                    .containerBackground(.fill.tertiary, for: .widget)
            } else {
                WidgetEntryView(entry: entry)
                    .padding()
                    .background()
            }
        }
        .configurationDisplayName("FeedFlow")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .systemExtraLarge])
    }
}
