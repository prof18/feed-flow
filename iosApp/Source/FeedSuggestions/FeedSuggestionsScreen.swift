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
    @StateObject private var vmStoreOwner = VMStoreOwner<FeedSuggestionsViewModel>(
        Deps.shared.getFeedSuggestionsViewModel()
    )

    @Environment(\.dismiss)
    private var dismiss

    var body: some View {
        NavigationStack {
            FeedSuggestionsContent(
                viewModel: vmStoreOwner.instance
            )
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                    }
                }
            }
        }
    }
}
