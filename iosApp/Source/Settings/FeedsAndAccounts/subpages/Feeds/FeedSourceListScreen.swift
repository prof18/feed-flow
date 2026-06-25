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

    @State private var showFeedOperationDialog = false
    @State private var feedOperationLoadingMessage: String?

    var body: some View {
        @Bindable var appState = appState

        FeedSourceListScreenContent(
            feedState: $feedState,
            deleteFeedSource: { feedSource in
                vmStoreOwner.instance.deleteFeedSource(feedSource: feedSource)
            },
            renameFeedSource: { feedSource, newName in
                vmStoreOwner.instance.updateFeedName(feedSource: feedSource, newName: newName)
            },
            deleteAllFeedsInCategory: { feedSources in
                vmStoreOwner.instance.deleteAllFeedsInCategory(feedSources: feedSources)
            }
        )
        .id(appState.redrawAfterFeedSourceEdit)
        .snackbar(messageQueue: $appState.snackbarQueue)
        .loadingDialog(isLoading: showFeedOperationDialog, message: feedOperationLoadingMessage)
        .task {
            for await state in vmStoreOwner.instance.feedOperationState {
                switch onEnum(of: state) {
                case .none:
                    self.feedOperationLoadingMessage = nil
                    self.showFeedOperationDialog = false
                case .deleting:
                    self.feedOperationLoadingMessage = feedFlowStrings.deletingFeedDialogTitle
                    self.showFeedOperationDialog = true
                case .markingAllRead:
                    self.showFeedOperationDialog = false
                }
            }
        }
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

                case let .syncError(errorState):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.syncErrorMessage(errorState.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )

                case .deleteFeedSourceError:
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.deleteFeedSourceError,
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                }
            }
        }
    }
}
