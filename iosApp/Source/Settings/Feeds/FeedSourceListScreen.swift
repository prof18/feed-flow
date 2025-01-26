//
//  FeedListScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FeedSourceListScreen: View {

  @StateObject
  private var vmStoreOwner = VMStoreOwner<FeedSourceListViewModel>(
    Deps.shared.getFeedSourceListViewModel())

  @Environment(AppState.self) private var appState

  @State
  private var feedState: FeedSourceListState = FeedSourceListState(
    feedSourcesWithoutCategory: [],
    feedSourcesWithCategory: []
  )

  var body: some View {
    ZStack {
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

      @Bindable var appState = appState
      VStack(spacing: 0) {

        Spacer()

        Snackbar(messageQueue: $appState.snackbarQueue)
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
        case .databaseError:
          self.appState.snackbarQueue.append(
            SnackbarData(
              title: feedFlowStrings.databaseError,
              subtitle: nil,
              showBanner: true
            )
          )

        case .feedErrorState(let state):
          self.appState.snackbarQueue.append(
            SnackbarData(
              title: feedFlowStrings.feedErrorMessage(state.feedName),
              subtitle: nil,
              showBanner: true
            )
          )

        case .syncError:
          self.appState.snackbarQueue.append(
            SnackbarData(
              title: feedFlowStrings.syncErrorMessage,
              subtitle: nil,
              showBanner: true
            )
          )
        }
      }
    }
  }
}
