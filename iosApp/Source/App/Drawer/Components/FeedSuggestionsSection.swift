//
//  FeedSuggestionsSection.swift
//  FeedFlow
//
//  Created by AI Assistant on 19/03/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FeedSuggestionsSection: View {
    let isCompact: Bool
    let onFeedSuggestionsClick: () -> Void

    var body: some View {
        Button(action: onFeedSuggestionsClick) {
            HStack {
                Label(feedFlowStrings.feedSuggestionsTitle, systemImage: "lightbulb")
                Spacer()
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .listRowBackground(sidebarSelectionBackground(isSelected: false, isCompact: isCompact))
    }
}
