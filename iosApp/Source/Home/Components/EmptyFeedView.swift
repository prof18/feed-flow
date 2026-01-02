//
//  EmptyFeedView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct EmptyFeedView: View {
    @Environment(AppState.self)
    private var appState
    @Environment(\.dismiss)
    private var dismiss

    let currentFeedFilter: FeedFilter
    let onReloadClick: () -> Void
    let onBackToTimelineClick: () -> Void
    let openDrawer: () -> Void
    let columnVisibility: NavigationSplitViewVisibility

    var body: some View {
        VStack {
            if currentFeedFilter is FeedFilter.Read {}

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

            if columnVisibility != .all {
                Button(
                    action: {
                        if appState.sizeClass == .compact {
                            dismiss()
                        } else {
                            openDrawer()
                        }
                    },
                    label: {
                        Text(feedFlowStrings.openAnotherFeed)
                            .frame(maxWidth: .infinity)
                    }
                )
                .buttonStyle(.bordered)
                .padding(.top, Spacing.regular)
                .padding(.horizontal, Spacing.medium)
            }
        }
    }
}

private extension FeedFilter {
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
