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
    @Binding var isGridLayoutEnabled: Bool
    @Binding var leftSwipeAction: SwipeActionType
    @Binding var rightSwipeAction: SwipeActionType
    @Binding var isRemoveTitleFromDescriptionEnabled: Bool
    @Binding var feedOrder: FeedOrder
    @Binding var isHideUnreadDotEnabled: Bool
    @Binding var isHideFeedSourceEnabled: Bool
    @Binding var descriptionLineLimit: DescriptionLineLimit
    let onScaleFactorChange: (Double) -> Void

    private let feedFlowStrings = Deps.shared.getStrings()
    private let previewMaxWidth: CGFloat = 360
    private let imageCardPreviewMaxWidth: CGFloat = 320
    private let previewHeroImageAspectRatio: CGFloat = 3.0

    var body: some View {
        let isCompactImageCardPreview = feedLayout == .bigImage || feedLayout == .grid

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
                            articleOpenMode: .default,
                            isHiddenFromTimeline: false,
                            isPinned: false,
                            isNotificationEnabled: false,
                            isHideImagesEnabled: false,
                            pinnedPosition: 0,
                            position: 0
                        ),
                        pubDateMillis: nil,
                        isRead: false,
                        dateString: isHideDateEnabled
                            ? nil
                            : formatDateTimeExample(dateFormat: dateFormat, timeFormat: timeFormat),
                        commentsUrl: nil,
                        isBookmarked: false,
                        contentDirection: nil,
                        audioUrl: nil
                    ),
                    index: 0,
                    feedFontSizes: feedFontSizes,
                    feedLayout: feedLayout,
                    isGridCell: true,
                    heroImageAspectRatio: previewHeroImageAspectRatio,
                    feedItemDisplaySettings: FeedItemDisplaySettings(
                        isHideUnreadDotEnabled: isHideUnreadDotEnabled,
                        isHideFeedSourceEnabled: isHideFeedSourceEnabled,
                        descriptionLineLimit: descriptionLineLimit
                    )
                )
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: isCompactImageCardPreview ? imageCardPreviewMaxWidth : previewMaxWidth)
                .padding(Spacing.small)
                .padding(.top, Spacing.small)
                .background(Color(UIColor.secondarySystemGroupedBackground))
                .cornerRadius(Spacing.small)
                .shadow(color: Color.black.opacity(0.1), radius: 5, x: 0, y: 2)
                .padding(.horizontal, Spacing.regular)
                .padding(.vertical, isCompactImageCardPreview ? Spacing.xsmall : Spacing.small)
            }
            .frame(maxWidth: .infinity)
            .background(Color.secondaryBackgroundColor)

            Form(content: {
                Section {
                    Picker(selection: $feedLayout) {
                        Text(feedFlowStrings.settingsFeedLayoutList)
                            .tag(FeedLayout.list)
                        Text(feedFlowStrings.settingsFeedLayoutCard)
                            .tag(FeedLayout.card)
                        Text(feedFlowStrings.settingsFeedLayoutBigImage)
                            .tag(FeedLayout.bigImage)
                    } label: {
                        Text(feedFlowStrings.feedLayoutTitle)
                    }
                    .accessibilityIdentifier(FeedListSettingsAccessibilityIdentifiers.layoutPicker)

                    if feedLayout.supportsGridToggle {
                        Toggle(isOn: $isGridLayoutEnabled) {
                            Text(feedFlowStrings.settingsFeedLayoutGridToggle)
                        }
                    }

                    textScaleControl
                } header: {
                    Text(feedFlowStrings.settingsFeedListSectionLayout)
                }

                Section {
                    Toggle(isOn: inverted($isHideDescriptionEnabled)) {
                        Text(feedFlowStrings.settingsShowDescription)
                    }
                    .accessibilityIdentifier(FeedListSettingsAccessibilityIdentifiers.showDescriptionToggle)

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
                    .disabled(isHideDescriptionEnabled)

                    Toggle(isOn: $isRemoveTitleFromDescriptionEnabled) {
                        Text(feedFlowStrings.settingsHideDuplicatedTitleFromDesc)
                    }
                    .disabled(isHideDescriptionEnabled)
                    .accessibilityIdentifier(FeedListSettingsAccessibilityIdentifiers.removeTitleToggle)

                    Toggle(isOn: inverted($isHideImagesEnabled)) {
                        Text(feedFlowStrings.settingsShowImages)
                    }
                    .accessibilityIdentifier(FeedListSettingsAccessibilityIdentifiers.hideImagesToggle)

                    Toggle(isOn: inverted($isHideDateEnabled)) {
                        Text(feedFlowStrings.settingsShowDate)
                    }
                    .accessibilityIdentifier(FeedListSettingsAccessibilityIdentifiers.hideDateToggle)

                    Toggle(isOn: inverted($isHideUnreadDotEnabled)) {
                        Text(feedFlowStrings.settingsShowUnreadDot)
                    }
                    .accessibilityIdentifier(FeedListSettingsAccessibilityIdentifiers.hideUnreadDotToggle)

                    Toggle(isOn: inverted($isHideFeedSourceEnabled)) {
                        Text(feedFlowStrings.settingsShowFeedSource)
                    }
                    .accessibilityIdentifier(FeedListSettingsAccessibilityIdentifiers.hideFeedSourceToggle)
                } header: {
                    Text(feedFlowStrings.settingsFeedListSectionShowInList)
                } footer: {
                    if isHideDescriptionEnabled {
                        Text(feedFlowStrings.settingsRequiresDescription)
                    }
                }

                Section {
                    DateFormatSection(dateFormat: $dateFormat)
                        .disabled(isHideDateEnabled)

                    TimeFormatSection(timeFormat: $timeFormat)
                        .disabled(isHideDateEnabled)
                } header: {
                    Text(feedFlowStrings.settingsFeedListSectionDateTime)
                } footer: {
                    if isHideDateEnabled {
                        Text(feedFlowStrings.settingsRequiresDate)
                    }
                }

                Section {
                    Picker(selection: $feedOrder) {
                        Text(feedFlowStrings.settingsFeedOrderNewestFirst)
                            .tag(FeedOrder.newestFirst)
                        Text(feedFlowStrings.settingsFeedOrderOldestFirst)
                            .tag(FeedOrder.oldestFirst)
                    } label: {
                        Text(feedFlowStrings.settingsFeedOrderTitle)
                    }
                    .accessibilityIdentifier(FeedListSettingsAccessibilityIdentifiers.orderPicker)
                } header: {
                    Text(feedFlowStrings.settingsFeedListSectionOrder)
                }

                Section {
                    Picker(selection: $leftSwipeAction) {
                        swipeActionOptions
                    } label: {
                        Text(feedFlowStrings.settingsLeftSwipeAction)
                    }

                    Picker(selection: $rightSwipeAction) {
                        swipeActionOptions
                    } label: {
                        Text(feedFlowStrings.settingsRightSwipeAction)
                    }
                } header: {
                    Text(feedFlowStrings.settingsFeedListSectionGestures)
                }
            })
            .scrollContentBackground(.hidden)
            .background(Color.secondaryBackgroundColor)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.secondaryBackgroundColor.ignoresSafeArea())
    }
    private var textScaleControl: some View {
        VStack(alignment: .leading) {
            HStack {
                Text(feedFlowStrings.settingsFeedListScaleTitle)
                Spacer()
                Text(scaleLabel)
                    .foregroundStyle(.secondary)
                if scaleFactor != 0 {
                    Button(feedFlowStrings.resetButton) {
                        scaleFactor = 0
                        onScaleFactorChange(0)
                    }
                    .buttonStyle(.borderless)
                    .accessibilityIdentifier(FeedListSettingsAccessibilityIdentifiers.scaleResetButton)
                }
            }
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
                .accessibilityLabel(Text(feedFlowStrings.settingsFeedListScaleTitle))
                .accessibilityValue(Text(scaleLabel))

                Image(systemName: "plus")
            }
        }
        .padding(.bottom, Spacing.regular)
    }

    private var scaleLabel: String {
        let step = Int(scaleFactor)
        if step == 0 {
            return feedFlowStrings.settingsFeedListScaleDefault
        }
        return step > 0 ? "+\(step)" : "\(step)"
    }

    @ViewBuilder private var swipeActionOptions: some View {
        Text(feedFlowStrings.settingsSwipeActionToggleRead)
            .tag(SwipeActionType.toggleReadStatus)
        Text(feedFlowStrings.settingsSwipeActionToggleBookmark)
            .tag(SwipeActionType.toggleBookmarkStatus)
        Text(feedFlowStrings.settingsSwipeActionOpenInBrowser)
            .tag(SwipeActionType.openInBrowser)
        Text(feedFlowStrings.settingsSwipeActionNone)
            .tag(SwipeActionType.none)
    }

    private func inverted(_ binding: Binding<Bool>) -> Binding<Bool> {
        Binding(
            get: { !binding.wrappedValue },
            set: { binding.wrappedValue = !$0 }
        )
    }
}

private func formatDateTimeExample(dateFormat: DateFormat, timeFormat: TimeFormat) -> String {
    let datePart = switch dateFormat {
    case .normal:
        "25/12"
    case .dayMonthDot:
        "25.12"
    case .american:
        "12/25"
    case .iso:
        "2025-12-25"
    }
    let timePart = timeFormat == .hours24 ? "14:30" : "2:30 PM"
    return "\(datePart) - \(timePart)"
}

private extension FeedLayout {
    var supportsGridToggle: Bool {
        self == .card || self == .bigImage
    }
}
