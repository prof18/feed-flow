//
//  EmptyFeedView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared

struct EmptyFeedView: View {

    let currentFeedFilter: FeedFilter
    let onReloadClick: () -> Void
    let onBackToTimelineClick: () -> Void

    var body: some View {
        VStack {
            if currentFeedFilter is FeedFilter.Read {

            }

//            if currentFeedFilter is FeedFilter.Bookmarks {
//                Image(systemName: "bookmark")
//                    .resizable()
//                    .scaledToFit()
//                    .frame(width: 32)
//                    .padding(.bottom, Spacing.regular)
//            }

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
//
//            if !(currentFeedFilter is FeedFilter.Read) &&
//                !(currentFeedFilter is FeedFilter.Bookmarks) {
//                Button(
//                    action: {
//                        onReloadClick()
//                    },
//                    label: {
//                        Text(localizer.refresh_feeds.localized)
//                            .frame(maxWidth: .infinity)
//                    }
//                )
//                .buttonStyle(.bordered)
//                .padding(.top, Spacing.regular)
//                .padding(.horizontal, Spacing.medium)
//            }
        }
    }
}

fileprivate extension FeedFilter {
    func getEmptyMessage() -> String {
        switch self {
        case is FeedFilter.Read:
            return localizer.read_articles_empty_screen_message.localized

        case is FeedFilter.Bookmarks:
            return localizer.bookmarked_articles_empty_screen_message.localized

        default:
            return localizer.empty_feed_message.localized
        }
    }

    func getButtonText() -> String {
        switch self {
        case is FeedFilter.Read, is FeedFilter.Bookmarks:
            return localizer.empty_screen_back_to_timeline.localized

        default:
            return localizer.refresh_feeds.localized
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
