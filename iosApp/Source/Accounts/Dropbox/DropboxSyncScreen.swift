//
//  DropboxSyncScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 28/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import SwiftUI

struct DropboxSyncScreen: View {
    @Environment(AppState.self) private var appState

    @StateObject
    private var vmStoreOwner = VMStoreOwner<DropboxSyncViewModel>(Deps.shared.getDropboxSyncViewModel())

    @State private var uiState: AccountConnectionUiState = .Unlinked()

    var body: some View {
        @Bindable var appState = appState

        DropboxSyncScreenContent(
            connectionState: uiState,
            onDropboxAuthSuccess: {
                vmStoreOwner.instance.saveDropboxAuth()
            },
            onBackupClick: {
                vmStoreOwner.instance.triggerBackup()
            },
            onDisconnectClick: {
                vmStoreOwner.instance.unlink()
            }
        )
        .snackbar(messageQueue: $appState.snackbarQueue)
        .task {
            for await state in vmStoreOwner.instance.dropboxConnectionUiState {
                self.uiState = state
            }
        }
        .task {
            for await state in vmStoreOwner.instance.dropboxSyncMessageState where state is DropboxSynMessages.Error {
                self.appState.snackbarQueue.append(
                    SnackbarData(
                        title: feedFlowStrings.dropboxSyncError,
                        subtitle: nil,
                        showBanner: true
                    )
                )
            }
        }
        .task {
            for await state in vmStoreOwner.instance.syncMessageQueue where state.isError() {
                self.appState.snackbarQueue.append(
                    SnackbarData(
                        title: feedFlowStrings.errorAccountSync,
                        subtitle: nil,
                        showBanner: true
                    )
                )
            }
        }
    }
}
