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
    let feedItem: FeedItem

    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                HStack {
                    Text(feedItem.feedSource.title)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .lineLimit(1)

                    if let dateString = feedItem.dateString {
                        Text("•")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .lineLimit(1)

                        Text(dateString)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                }

                if let title = feedItem.title {
                    Text(title)
                        .font(.subheadline)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                        .foregroundColor(.primary)
                }
            }

            Spacer()

            if let imageUrl = feedItem.imageUrl, !imageUrl.isEmpty {
                if URL(string: imageUrl) != nil {
                    Image(uiImage: imageUrl.loadImageFromCache() ?? UIImage())
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: 30, height: 30)
                        .cornerRadius(Spacing.small)
                        .clipped()
                } else {
                    Color(.systemBackground)
                        .frame(width: 30, height: 30)
                        .cornerRadius(Spacing.small)
                }
            }
        }
    }
}
