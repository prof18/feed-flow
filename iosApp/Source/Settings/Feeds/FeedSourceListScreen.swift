//
//  FeedListScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct FeedSourceListScreen: View {

    @EnvironmentObject
    private var appState: AppState

    @StateObject
    private var feedSourceViewModel = KotlinDependencies.shared.getFeedSourceListViewModel()

    @State
    private var feedState: FeedSourceListState = FeedSourceListState(
        feedSourcesWithoutCategory: [],
        feedSourcesWithCategory: []
    )

    var body: some View {
        FeedSourceListScreenContent(
            feedState: $feedState,
            deleteFeedSource: { feedSource in
                feedSourceViewModel.deleteFeedSource(feedSource: feedSource)
            },
            renameFeedSource: { feedSource, newName in
                feedSourceViewModel.updateFeedName(feedSource: feedSource, newName: newName)
            }
        )
        .task {
            do {
                let stream = asyncSequence(for: feedSourceViewModel.feedSourcesStateFlow)
                for try await state in stream {
                    self.feedState = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
    }
}
