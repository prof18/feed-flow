//
//  DropboxSyncScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 28/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import FeedFlowKit

struct DropboxSyncScreen: View {
    @EnvironmentObject private var appState: AppState

    @StateObject
    private var vmStoreOwner = VMStoreOwner<DropboxSyncViewModel>(Deps.shared.getDropboxSyncViewModel())

    @State private var uiState: AccountConnectionUiState = AccountConnectionUiState.Unlinked()

    var body: some View {
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
