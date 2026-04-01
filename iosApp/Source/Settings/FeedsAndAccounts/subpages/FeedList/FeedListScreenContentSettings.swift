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
    @Binding var isHideUnreadDotEnabled: Bool
    @Binding var isHideFeedSourceEnabled: Bool
    @Binding var descriptionLineLimit: DescriptionLineLimit
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
                    feedLayout: feedLayout,
                    feedItemDisplaySettings: FeedItemDisplaySettings(
                        isHideUnreadDotEnabled: isHideUnreadDotEnabled,
                        isHideFeedSourceEnabled: isHideFeedSourceEnabled,
                        descriptionLineLimit: descriptionLineLimit
                    )
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
                        Text(feedFlowStrings.feedLayoutTitle)
                    }

                    Toggle(isOn: $isHideDescriptionEnabled) {
                        Text(feedFlowStrings.settingsHideDescription)
                    }.onTapGesture {
                        isHideDescriptionEnabled.toggle()
                    }

                    Toggle(isOn: $isHideImagesEnabled) {
                        Text(feedFlowStrings.settingsHideImages)
                    }.onTapGesture {
                        isHideImagesEnabled.toggle()
                    }

                    Toggle(isOn: $isHideDateEnabled) {
                        Text(feedFlowStrings.settingsHideDate)
                    }.onTapGesture {
                        isHideDateEnabled.toggle()
                    }

                    Toggle(isOn: $isHideUnreadDotEnabled) {
                        Text(feedFlowStrings.settingsHideUnreadDot)
                    }.onTapGesture {
                        isHideUnreadDotEnabled.toggle()
                    }

                    Toggle(isOn: $isHideFeedSourceEnabled) {
                        Text(feedFlowStrings.settingsHideFeedSource)
                    }.onTapGesture {
                        isHideFeedSourceEnabled.toggle()
                    }

                    Picker(selection: $descriptionLineLimit) {
                        Text(feedFlowStrings.settingsDescriptionLinesThree)
                            .tag(DescriptionLineLimit.three)
                        Text(feedFlowStrings.settingsDescriptionLinesFive)
                            .tag(DescriptionLineLimit.five)
                        Text(feedFlowStrings.settingsDescriptionLinesNoLimit)
                            .tag(DescriptionLineLimit.noLimit)
                    } label: {
                        Text(feedFlowStrings.settingsDescriptionMaxLines)
                    }

                    Toggle(isOn: $isRemoveTitleFromDescriptionEnabled) {
                        Text(feedFlowStrings.settingsHideDuplicatedTitleFromDesc)
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
                        Text(feedFlowStrings.settingsSwipeActionOpenInBrowser)
                            .tag(SwipeActionType.openInBrowser)
                        Text(feedFlowStrings.settingsSwipeActionNone)
                            .tag(SwipeActionType.none)
                    } label: {
                        Text(feedFlowStrings.settingsLeftSwipeAction)
                    }

                    Picker(selection: $rightSwipeAction) {
                        Text(feedFlowStrings.settingsSwipeActionToggleRead)
                            .tag(SwipeActionType.toggleReadStatus)
                        Text(feedFlowStrings.settingsSwipeActionToggleBookmark)
                            .tag(SwipeActionType.toggleBookmarkStatus)
                        Text(feedFlowStrings.settingsSwipeActionOpenInBrowser)
                            .tag(SwipeActionType.openInBrowser)
                        Text(feedFlowStrings.settingsSwipeActionNone)
                            .tag(SwipeActionType.none)
                    } label: {
                        Text(feedFlowStrings.settingsRightSwipeAction)
                    }

                    Picker(selection: $feedOrder) {
                        Text(feedFlowStrings.settingsFeedOrderNewestFirst)
                            .tag(FeedOrder.newestFirst)
                        Text(feedFlowStrings.settingsFeedOrderOldestFirst)
                            .tag(FeedOrder.oldestFirst)
                    } label: {
                        Text(feedFlowStrings.settingsFeedOrderTitle)
                    }
                }
            })
            .scrollContentBackground(.hidden)
            .background(Color.secondaryBackgroundColor)
        }
    }
}

private func formatDateTimeExample(dateFormat: DateFormat, timeFormat: TimeFormat) -> String {
    let datePart: String = switch dateFormat {
    case .normal:
        "25/12"
    case .american:
        "12/25"
    case .iso:
        "2025-12-25"
    }
    let timePart = timeFormat == .hours24 ? "14:30" : "2:30 PM"
    return "\(datePart) - \(timePart)"
}
