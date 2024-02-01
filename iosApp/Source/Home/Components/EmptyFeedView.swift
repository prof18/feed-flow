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

    var body: some View {
        VStack {
            Text(currentFeedFilter.getEmptyMessage())
                .font(.body)

            if !(currentFeedFilter is FeedFilter.Read) &&
                !(currentFeedFilter is FeedFilter.Bookmarks) {
                Button(
                    action: {
                        onReloadClick()
                    },
                    label: {
                        Text(localizer.refresh_feeds.localized)
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
}

#Preview {
    EmptyFeedView(
        currentFeedFilter: FeedFilter.Timeline(),
        onReloadClick: {}
    )
}
