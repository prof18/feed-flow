import FeedFlowKit
import SwiftUI

struct SwipeActionSection: View {
    @Binding var leftSwipeAction: SwipeActionType
    @Binding var rightSwipeAction: SwipeActionType

    var body: some View {
        Section(feedFlowStrings.settingsFeedListTitle) {
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
        }
    }
}
