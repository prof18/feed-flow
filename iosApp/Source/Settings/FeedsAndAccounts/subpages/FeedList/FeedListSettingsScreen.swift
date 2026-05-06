import FeedFlowKit
import SwiftUI

struct FeedListSettingsScreen: View {
    @Environment(AppState.self) 
    private var appState
    
    @StateObject private var vmStoreOwner = VMStoreOwner<FeedListSettingsViewModel>(
        Deps.shared.getFeedListSettingsViewModel()
    )
    private let feedFlowStrings = Deps.shared.getStrings()

    @State private var settingsState = FeedListSettingsState(
        isHideDescriptionEnabled: false,
        isHideImagesEnabled: false,
        isHideDateEnabled: false,
        dateFormat: .normal,
        timeFormat: .hours24,
        feedLayout: .list,
        fontScale: 0,
        leftSwipeActionType: .toggleReadStatus,
        rightSwipeActionType: .toggleBookmarkStatus,
        isRemoveTitleFromDescriptionEnabled: false,
        feedOrder: .newestFirst,
        isHideUnreadDotEnabled: false,
        isHideFeedSourceEnabled: false,
        descriptionLineLimit: .three
    )
    @State private var feedFontSizes: FeedFontSizes = defaultFeedFontSizes()
    @State private var scaleFactor: Double = 0.0

    var body: some View {
        @Bindable var appState = appState

        FeedListSettingsScreenContent(
            settingsState: settingsState,
            feedFontSizes: feedFontSizes,
            imageUrl: previewImageUrl,
            articleDescription: previewArticleDescription,
            scaleFactor: $scaleFactor,
            isHideDescriptionEnabled: Binding(
                get: { settingsState.isHideDescriptionEnabled },
                set: { newValue in
                    vmStoreOwner.instance.updateHideDescription(value: newValue)
                }
            ),
            isHideImagesEnabled: Binding(
                get: { settingsState.isHideImagesEnabled },
                set: { newValue in
                    vmStoreOwner.instance.updateHideImages(value: newValue)
                }
            ),
            isHideDateEnabled: Binding(
                get: { settingsState.isHideDateEnabled },
                set: { vmStoreOwner.instance.updateHideDate(value: $0) }
            ),
            dateFormat: Binding(
                get: { settingsState.dateFormat },
                set: { vmStoreOwner.instance.updateDateFormat(format: $0) }
            ),
            timeFormat: Binding(
                get: { settingsState.timeFormat },
                set: { vmStoreOwner.instance.updateTimeFormat(format: $0) }
            ),
            feedLayout: Binding(
                get: { settingsState.feedLayout },
                set: { vmStoreOwner.instance.updateFeedLayout(feedLayout: $0) }
            ),
            leftSwipeAction: Binding(
                get: { settingsState.leftSwipeActionType },
                set: { vmStoreOwner.instance.updateSwipeAction(direction: .left, action: $0) }
            ),
            rightSwipeAction: Binding(
                get: { settingsState.rightSwipeActionType },
                set: { vmStoreOwner.instance.updateSwipeAction(direction: .right, action: $0) }
            ),
            isRemoveTitleFromDescriptionEnabled: Binding(
                get: { settingsState.isRemoveTitleFromDescriptionEnabled },
                set: { vmStoreOwner.instance.updateRemoveTitleFromDescription(value: $0) }
            ),
            feedOrder: Binding(
                get: { settingsState.feedOrder },
                set: { vmStoreOwner.instance.updateFeedOrder(feedOrder: $0) }
            ),
            isHideUnreadDotEnabled: Binding(
                get: { settingsState.isHideUnreadDotEnabled },
                set: { vmStoreOwner.instance.updateHideUnreadDot(value: $0) }
            ),
            isHideFeedSourceEnabled: Binding(
                get: { settingsState.isHideFeedSourceEnabled },
                set: { vmStoreOwner.instance.updateHideFeedSource(value: $0) }
            ),
            descriptionLineLimit: Binding(
                get: { settingsState.descriptionLineLimit },
                set: { vmStoreOwner.instance.updateDescriptionLineLimit(limit: $0) }
            ),
            onScaleFactorChange: { newValue in
                vmStoreOwner.instance.updateFontScale(value: Int32(newValue))
            }
        )
        .navigationTitle(Text(feedFlowStrings.settingsFeedListTitle))
        .navigationBarTitleDisplayMode(.inline)
        .snackbar(messageQueue: $appState.snackbarQueue)
        .task {
            for await state in vmStoreOwner.instance.state {
                self.settingsState = state
                self.scaleFactor = Double(state.fontScale)
            }
        }
        .task {
            for await fontSizes in vmStoreOwner.instance.feedFontSizeState {
                self.feedFontSizes = fontSizes
            }
        }
    }

    private var previewImageUrl: String? {
        settingsState.isHideImagesEnabled ? nil : "https://lipsum.app/200x200"
    }

    private var previewArticleDescription: String? {
        settingsState.isHideDescriptionEnabled ? nil : feedFlowStrings.settingsFontScaleSubtitleExample
    }
}
