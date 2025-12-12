//
//  FeedSourceListScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FeedSourceListScreen: View {
    @StateObject private var vmStoreOwner = VMStoreOwner<FeedSourceListViewModel>(
        Deps.shared.getFeedSourceListViewModel())

    @Environment(AppState.self)
    private var appState

    @State private var feedState: FeedSourceListState = .init(
        feedSourcesWithoutCategory: [],
        feedSourcesWithCategory: []
    )

    var body: some View {
        @Bindable var appState = appState

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
        .snackbar(messageQueue: $appState.snackbarQueue)
        .task {
            for await state in vmStoreOwner.instance.feedSourcesState {
                self.feedState = state
            }
        }
        .task {
            for await state in vmStoreOwner.instance.errorState {
                switch onEnum(of: state) {
                case let .databaseError(errorState):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.databaseError(errorState.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )

                case let .feedErrorState(feedError):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.feedErrorMessageImproved(feedError.feedName),
                            subtitle: nil,
                            showBanner: true
                        )
                    )

                case let .syncError(errorState):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.syncErrorMessage(errorState.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                }
            }
        }
    }
}
