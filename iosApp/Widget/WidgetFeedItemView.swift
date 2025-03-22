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
                Text(feedItem.feedSourceTitle)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .lineLimit(1)

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
