//
//  WidgetEntryView.swift
//  FeedFlowWidgetExtension
//
//  Created by Marco Gomiero on 02/03/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI
import WidgetKit

struct WidgetEntryView: View {
    var entry: Provider.Entry
    @Environment(\.widgetFamily)
    private var widgetFamily

    var body: some View {
        if entry.feedItems.isEmpty {
            VStack(spacing: Spacing.small) {
                Text(entry.widgetEmptyScreenTitle)
                    .font(.headline)
                Text(entry.widgetEmptyScreenContent)
                    .font(.caption)
            }
            .padding()
        } else {
            VStack(alignment: .leading) {
                feedItemsList
                Spacer()
            }
        }
    }

    @ViewBuilder private var feedItemsList: some View {
        let itemsToShow = numberOfItemsToShow()

        if widgetFamily == .systemSmall && !entry.feedItems.isEmpty {
            smallWidgetFeedItemView(entry.feedItems[0])
        } else if widgetFamily == .systemExtraLarge && !entry.feedItems.isEmpty {
            extraLargeWidgetGridView(items: Array(entry.feedItems.prefix(6)))
        } else {
            regularWidgetView(items: Array(entry.feedItems.prefix(itemsToShow)))
        }
    }

    private func regularWidgetView(items: [FeedItemWidget]) -> some View {
        return VStack(alignment: .leading, spacing: Spacing.small) {
            Text(entry.widgetTitle)
                .font(.headline)
                .padding(.top, 10.0)

            ForEach(items, id: \.id) { item in
                Link(
                    destination: URL(string: "feedflow://feed/\(item.id)") ?? URL(string: "feedflow://") ?? URL(fileURLWithPath: "")
                ) {
                    WidgetFeedItemView(feedItem: item, lineLimit: widgetFamily == .systemMedium ? 1 : 2)
                }
                .buttonStyle(PlainButtonStyle())

                if item.id != items.last?.id {
                    Divider()
                }
            }
        }
        .padding(.bottom, Spacing.small)
    }

    private func extraLargeWidgetGridView(items: [FeedItemWidget]) -> some View {
        let columns = [
            GridItem(.flexible(), spacing: Spacing.medium),
            GridItem(.flexible(), spacing: Spacing.medium)
        ]

        return VStack(alignment: .leading, spacing: 0) {
            Text(entry.widgetTitle)
                .font(.headline)
                .padding(.horizontal, Spacing.medium)
                .padding(.vertical, Spacing.medium)

            LazyVGrid(columns: columns, spacing: Spacing.medium) {
                ForEach(items, id: \.id) { item in
                    Link(
                        destination: URL(string: "feedflow://feed/\(item.id)") ?? URL(string: "feedflow://") ?? URL(fileURLWithPath: "")
                    ) {
                        WidgetFeedItemView(feedItem: item, lineLimit: 2)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .padding(.horizontal, Spacing.medium)
        }
    }

    private func smallWidgetFeedItemView(_ feedItem: FeedItemWidget) -> some View {
        Link(
            destination: URL(string: "feedflow://feed/\(feedItem.id)") ?? URL(string: "feedflow://") ?? URL(fileURLWithPath: "")
        ) {
            VStack(alignment: .leading, spacing: Spacing.xsmall) {
                Text(feedItem.feedSourceTitle)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
                    .padding(.top, Spacing.regular)

                if let title = feedItem.title {
                    Text(title)
                        .font(.subheadline)
                        .lineLimit(3)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                        .foregroundColor(.primary)
                        .padding(.top, Spacing.xsmall)
                }
            }
        }
        .buttonStyle(PlainButtonStyle())
    }

    private func numberOfItemsToShow() -> Int {
        if widgetFamily == .systemLarge {
            return 4
        }
        return 2
    }
}
