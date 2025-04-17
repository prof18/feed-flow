//
//  SwipeActionsSection.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 05/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct SwipeActionsSection: View {
    @Binding var leftSwipeAction: SwipeActionType
    @Binding var rightSwipeAction: SwipeActionType

    var body: some View {
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
