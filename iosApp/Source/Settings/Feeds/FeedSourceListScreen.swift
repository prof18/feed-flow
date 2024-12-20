//
//  FeedListScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import FeedFlowKit

struct FeedSourceListScreen: View {

    @StateObject
    private var vmStoreOwner = VMStoreOwner<FeedSourceListViewModel>(Deps.shared.getFeedSourceListViewModel())

    @Environment(AppState.self) private var appState

    @State
    private var feedState: FeedSourceListState = FeedSourceListState(
        feedSourcesWithoutCategory: [],
        feedSourcesWithCategory: []
    )

    var body: some View {
        FeedSourceListScreenContent(
            feedState: $feedState,
            deleteFeedSource: { feedSource in
                vmStoreOwner.instance.deleteFeedSource(feedSource: feedSource)
            },
            renameFeedSource: { feedSource, newName in
                vmStoreOwner.instance.updateFeedName(feedSource: feedSource, newName: newName)
            }
        )
        .id(appState.redrawAfterFeedSourceEdit)
        .task {
            for await state in vmStoreOwner.instance.feedSourcesState {
                self.feedState = state
            }
        }
    }
}
