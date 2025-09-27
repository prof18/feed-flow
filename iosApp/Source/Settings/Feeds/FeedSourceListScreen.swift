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
                case .databaseError:
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.databaseError(state.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )

                case let .feedErrorState(state):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.feedErrorMessage(state.feedName, state.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )

                case .syncError:
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.syncErrorMessage(state.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                }
            }
        }
    }
}
