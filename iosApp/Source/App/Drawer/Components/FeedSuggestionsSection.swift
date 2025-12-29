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
    let onFeedSuggestionsClick: () -> Void

    var body: some View {
        HStack {
            Label(feedFlowStrings.feedSuggestionsTitle, systemImage: "lightbulb")
            Spacer()
        }
        .contentShape(Rectangle())
        .onTapGesture {
            onFeedSuggestionsClick()
        }
    }
}
