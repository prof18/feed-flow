//
//  BlockedWordsScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/09/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct BlockedWordsScreen: View {
    @Environment(AppState.self)
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<BlockedWordsViewModel>(Deps.shared.getBlockedWordsViewModel())

    @State private var words: [String] = []

    var body: some View {
        @Bindable var appState = appState

            BlockedWordsScreenContent(
                words: words,
                onAddWord: { word in
                    vmStoreOwner.instance.onAddWord(input: word)
                },
                onRemoveWord: { word in
                    vmStoreOwner.instance.onRemoveWord(word: word)
                }
            )
            .snackbar(messageQueue: $appState.snackbarQueue)
            .task {
                for await state in vmStoreOwner.instance.wordsState {
                    self.words = state
                }
            }
            .navigationTitle(feedFlowStrings.settingsBlockedWords)
            .navigationBarTitleDisplayMode(.inline)
        }
}
