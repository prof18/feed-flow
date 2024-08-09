//
//  EmptyFeedView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import FeedFlowKit

struct EmptyFeedView: View {

    let currentFeedFilter: FeedFilter
    let onReloadClick: () -> Void
    let onBackToTimelineClick: () -> Void

    var body: some View {
        VStack {
            if currentFeedFilter is FeedFilter.Read {

            }

            Text(currentFeedFilter.getEmptyMessage())
                .font(.body)

            Button(
                action: {
                    if currentFeedFilter is FeedFilter.Bookmarks || currentFeedFilter is FeedFilter.Read {
                        onBackToTimelineClick()
                    } else {
                        onReloadClick()
                    }
                },
                label: {
                    Text(currentFeedFilter.getButtonText())
                        .frame(maxWidth: .infinity)
                }
            )
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)
            .padding(.horizontal, Spacing.medium)
        }
    }
}

fileprivate extension FeedFilter {
    func getEmptyMessage() -> String {
        switch self {
        case is FeedFilter.Read:
            return feedFlowStrings.readArticlesEmptyScreenMessage

        case is FeedFilter.Bookmarks:
            return feedFlowStrings.bookmarkedArticlesEmptyScreenMessage

        default:
            return feedFlowStrings.emptyFeedMessage
        }
    }

    func getButtonText() -> String {
        switch self {
        case is FeedFilter.Read, is FeedFilter.Bookmarks:
            return feedFlowStrings.emptyScreenBackToTimeline

        default:
            return feedFlowStrings.refreshFeeds
        }
    }
}

#Preview {
    EmptyFeedView(
        currentFeedFilter: FeedFilter.Timeline(),
        onReloadClick: {},
        onBackToTimelineClick: {}
    )
}

#Preview("Bookmarks") {
    EmptyFeedView(
        currentFeedFilter: FeedFilter.Bookmarks(),
        onReloadClick: {},
        onBackToTimelineClick: {}
    )
}

#Preview("Read") {
    EmptyFeedView(
        currentFeedFilter: FeedFilter.Read(),
        onReloadClick: {},
        onBackToTimelineClick: {}
    )
}
