//
//  FeedSuggestionsScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FeedSuggestionsScreen: View {
    @StateObject private var vmStoreOwner = VMStoreOwner<FeedSuggestionsViewModel>(Deps.shared.getFeedSuggestionsViewModel())

    var body: some View {
        FeedSuggestionsContent(
            viewModel: vmStoreOwner.instance
        )
    }
}
