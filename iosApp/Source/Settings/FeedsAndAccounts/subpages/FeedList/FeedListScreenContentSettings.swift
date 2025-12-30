import FeedFlowKit
import SwiftUI

struct FeedListSettingsScreenContent: View {
    let settingsState: FeedListSettingsState
    let feedFontSizes: FeedFontSizes
    let imageUrl: String?
    let articleDescription: String?
    @Binding var scaleFactor: Double
    @Binding var isHideDescriptionEnabled: Bool
    @Binding var isHideImagesEnabled: Bool
    @Binding var isHideDateEnabled: Bool
    @Binding var dateFormat: DateFormat
    @Binding var timeFormat: TimeFormat
    @Binding var feedLayout: FeedLayout
    @Binding var leftSwipeAction: SwipeActionType
    @Binding var rightSwipeAction: SwipeActionType
    @Binding var isRemoveTitleFromDescriptionEnabled: Bool
    @Binding var feedOrder: FeedOrder
    let onScaleFactorChange: (Double) -> Void

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        VStack(spacing: 0) {
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
                        dateString: isHideDateEnabled
                            ? nil
                            : formatDateTimeExample(dateFormat: dateFormat, timeFormat: timeFormat),
                        commentsUrl: nil,
                        isBookmarked: false
                    ),
                    index: 0,
                    feedFontSizes: feedFontSizes,
                    feedLayout: feedLayout
                )
                .fixedSize(horizontal: false, vertical: true)
                .padding(Spacing.small)
                .padding(.top, Spacing.small)
                .background(Color(UIColor.secondarySystemGroupedBackground))
                .cornerRadius(Spacing.small)
                .shadow(color: Color.black.opacity(0.1), radius: 5, x: 0, y: 2)
                .padding(Spacing.regular)
            }
            .background(Color.secondaryBackgroundColor)

            Form(content: {
                Section {
                    VStack(alignment: .leading) {
                        Text(feedFlowStrings.settingsFeedListScaleTitle)
                            .padding(.top)

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

                    Toggle(isOn: $isHideDateEnabled) {
                        Label(feedFlowStrings.settingsHideDate, systemImage: "calendar.badge.minus")
                    }.onTapGesture {
                        isHideDateEnabled.toggle()
                    }

                    Toggle(isOn: $isRemoveTitleFromDescriptionEnabled) {
                        Label(feedFlowStrings.settingsHideDuplicatedTitleFromDesc, systemImage: "eye.slash")
                    }.onTapGesture {
                        isRemoveTitleFromDescriptionEnabled.toggle()
                    }

                    DateFormatSection(dateFormat: $dateFormat)

                    TimeFormatSection(timeFormat: $timeFormat)

                    Picker(selection: $leftSwipeAction) {
                        Text(feedFlowStrings.settingsSwipeActionToggleRead)
                            .tag(SwipeActionType.toggleReadStatus)
                        Text(feedFlowStrings.settingsSwipeActionToggleBookmark)
                            .tag(SwipeActionType.toggleBookmarkStatus)
                        Text(feedFlowStrings.settingsSwipeActionNone)
                            .tag(SwipeActionType.none)
                    } label: {
                        Label(feedFlowStrings.settingsLeftSwipeAction, systemImage: "arrow.right")
                    }

                    Picker(selection: $rightSwipeAction) {
                        Text(feedFlowStrings.settingsSwipeActionToggleRead)
                            .tag(SwipeActionType.toggleReadStatus)
                        Text(feedFlowStrings.settingsSwipeActionToggleBookmark)
                            .tag(SwipeActionType.toggleBookmarkStatus)
                        Text(feedFlowStrings.settingsSwipeActionNone)
                    .tag(SwipeActionType.none)
                    } label: {
                Label(feedFlowStrings.settingsRightSwipeAction, systemImage: "arrow.left")
                    }

                    Picker(selection: $feedOrder) {
                        Text(feedFlowStrings.settingsFeedOrderNewestFirst)
                            .tag(FeedOrder.newestFirst)
                        Text(feedFlowStrings.settingsFeedOrderOldestFirst)
                            .tag(FeedOrder.oldestFirst)
                    } label: {
                        Label(feedFlowStrings.settingsFeedOrderTitle, systemImage: "arrow.up.arrow.down.circle")
                    }
                }
            })
            .scrollContentBackground(.hidden)
            .background(Color.secondaryBackgroundColor)
        }
    }
}

private func formatDateTimeExample(dateFormat: DateFormat, timeFormat: TimeFormat) -> String {
    let datePart = dateFormat == .normal ? "25/12" : "12/25"
    let timePart = timeFormat == .hours24 ? "14:30" : "2:30 PM"
    return "\(datePart) - \(timePart)"
}
