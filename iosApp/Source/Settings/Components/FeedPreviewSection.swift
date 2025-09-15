//
//  FeedPreviewSection.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 05/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FeedPreviewSection: View {
    let feedFontSizes: FeedFontSizes
    let imageUrl: String?
    let articleDescription: String?
    let dateFormat: DateFormat
    @Binding var scaleFactor: Double
    let onScaleFactorChange: (Double) -> Void

    var body: some View {
        VStack(alignment: .leading) {
            VStack {
                FeedItemView(
                    feedItem: FeedItem(
                        id: "1",
                        url: "https://www.example.com",
                        title: feedFlowStrings.settingsFontScaleTitleExample,
                        subtitle: articleDescription,
                        content: nil,
                        imageUrl: imageUrl,
                        feedSource: FeedSource(
                            id: "1",
                            url: "https://www.example.it",
                            title: feedFlowStrings.settingsFontScaleFeedSourceExample,
                            category: nil,
                            lastSyncTimestamp: nil,
                            logoUrl: nil,
                            websiteUrl: nil,
                            fetchFailed: false,
                            linkOpeningPreference: .default,
                            isHiddenFromTimeline: false,
                            isPinned: false,
                            isNotificationEnabled: false
                        ),
                        pubDateMillis: nil,
                        isRead: false,
                        dateString: dateFormat == .normal ? "25/12" : "12/25",
                        commentsUrl: nil,
                        isBookmarked: false
                    ),
                    index: 0,
                    feedFontSizes: feedFontSizes
                )
                .background(Color.secondaryBackgroundColor)
                .padding(Spacing.small)
                .padding(.top, Spacing.small)
                .cornerRadius(Spacing.small)
                .shadow(color: Color.black.opacity(0.1), radius: 5, x: 0, y: 2)
            }

            Text(feedFlowStrings.settingsFeedListScaleTitle)
                .padding(.top)

            // 16 default
            // 12 min ( -4 )
            // 32 max ( +16 )
            HStack {
                Image(systemName: "minus")

                Slider(
                    value: Binding(
                        get: { scaleFactor },
                        set: { newValue in
                            scaleFactor = newValue
                            onScaleFactorChange(newValue)
                        }
                    ),
                    in: -4 ... 16,
                    step: 1.0
                )

                Image(systemName: "plus")
            }
        }.padding(.bottom, Spacing.regular)
    }
}
