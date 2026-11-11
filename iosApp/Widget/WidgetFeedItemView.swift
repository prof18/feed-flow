//
//  WidgetFeedItemView.swift
//  FeedFlowWidgetExtension
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI
import WidgetKit

struct WidgetFeedItemView: View {
    let feedItem: FeedItemWidget
    let lineLimit: Int

    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                HStack(spacing: Spacing.xsmall) {
                    Text(feedItem.feedSourceTitle)
                        .lineLimit(1)

                    if let dateString = feedItem.dateString {
                        Text("•")
                        Text(dateString)
                            .lineLimit(1)
                            .layoutPriority(1)
                    }
                }
                .font(.caption2)
                .foregroundColor(.secondary)

                if let title = feedItem.title {
                    Text(title)
                        .font(.subheadline)
                        .lineLimit(lineLimit)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                        .foregroundColor(.primary)
                }
            }

            Spacer()
        }
    }
}
