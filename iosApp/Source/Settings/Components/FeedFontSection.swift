//
//  FeedFontSection.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 05/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FeedFontSection: View {
    let feedFontSizes: FeedFontSizes
    let imageUrl: String?
    let articleDescription: String?
    @Binding var scaleFactor: Double
    @Binding var isHideDescriptionEnabled: Bool
    @Binding var isHideImagesEnabled: Bool
    @Binding var isRemoveTitleFromDescriptionEnabled: Bool
    @Binding var leftSwipeAction: SwipeActionType
    @Binding var rightSwipeAction: SwipeActionType
    @Binding var dateFormat: DateFormat
    @Binding var feedOrder: FeedOrder
    @Binding var feedLayout: FeedLayout
    let onScaleFactorChange: (Double) -> Void

    var body: some View {
        Section(feedFlowStrings.settingsFeedListTitle) {
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
                        feedFontSizes: feedFontSizes,
                        feedLayout: feedLayout
                    )
                    // .if(feedLayout == .list) { view in
                    //     view.background(Color.secondaryBackgroundColor)
                    // }
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

            Picker(selection: $feedLayout) {
                Text(feedFlowStrings.settingsFeedLayoutList)
                    .tag(FeedLayout.list)
                Text(feedFlowStrings.settingsFeedLayoutCard)
                    .tag(FeedLayout.card)
            } label: {
                Label(feedFlowStrings.feedLayoutTitle, systemImage: "rectangle.grid.1x2")
            }

            Toggle(isOn: $isHideDescriptionEnabled) {
                Label(feedFlowStrings.settingsHideDescription, systemImage: "text.page.slash")
            }.onTapGesture {
                isHideDescriptionEnabled.toggle()
            }

            Toggle(isOn: $isHideImagesEnabled) {
                Label(feedFlowStrings.settingsHideImages, systemImage: "square.slash")
            }.onTapGesture {
                isHideImagesEnabled.toggle()
            }

            Toggle(isOn: $isRemoveTitleFromDescriptionEnabled) {
                Label(feedFlowStrings.settingsHideDuplicatedTitleFromDesc, systemImage: "eye.slash")
            }.onTapGesture {
                isRemoveTitleFromDescriptionEnabled.toggle()
            }

            DateFormatSection(dateFormat: $dateFormat)

            Picker(selection: $feedOrder) {
                Text(feedFlowStrings.settingsFeedOrderNewestFirst)
                    .tag(FeedOrder.newestFirst)
                Text(feedFlowStrings.settingsFeedOrderOldestFirst)
                    .tag(FeedOrder.oldestFirst)
            } label: {
                Label(feedFlowStrings.settingsFeedOrderTitle, systemImage: "arrow.up.arrow.down.circle")
            }

            Picker(selection: $leftSwipeAction) {
                Text(feedFlowStrings.settingsSwipeActionToggleRead)
                    .tag(SwipeActionType.toggleReadStatus)
                Text(feedFlowStrings.settingsSwipeActionToggleBookmark)
                    .tag(SwipeActionType.toggleBookmarkStatus)
                Text(feedFlowStrings.settingsSwipeActionNone)
                    .tag(SwipeActionType.none)
            } label: {
                Label(feedFlowStrings.settingsLeftSwipeAction, systemImage: "arrow.left.to.line")
            }

            Picker(selection: $rightSwipeAction) {
                Text(feedFlowStrings.settingsSwipeActionToggleRead)
                    .tag(SwipeActionType.toggleReadStatus)
                Text(feedFlowStrings.settingsSwipeActionToggleBookmark)
                    .tag(SwipeActionType.toggleBookmarkStatus)
                Text(feedFlowStrings.settingsSwipeActionNone)
                    .tag(SwipeActionType.none)
            } label: {
                Label(feedFlowStrings.settingsRightSwipeAction, systemImage: "arrow.right.to.line")
            }
        }
    }
}
