//
//  GoogleDriveSyncScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 05/11/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct GoogleDriveSyncScreen: View {
    @Environment(AppState.self)
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<GoogleDriveSyncViewModel>(
        Deps.shared.getGoogleDriveSyncViewModel()
    )

    @State private var uiState: AccountConnectionUiState = .Loading()

    var body: some View {
        @Bindable var appState = appState

        GoogleDriveSyncScreenContent(
            connectionState: uiState,
            onConnectClick: {
                vmStoreOwner.instance.startAuthentication()
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
            for await state in vmStoreOwner.instance.googleDriveConnectionUiState {
                self.uiState = state
            }
        }
        .task {
            for await state in vmStoreOwner.instance.googleDriveSyncMessageState
             where state is GoogleDriveSynMessages.Error {
                self.appState.snackbarQueue.append(
                    SnackbarData(
                        title: feedFlowStrings.googleDriveSyncError,
                        subtitle: nil,
                        showBanner: true
                    )
                )
            }
        }
        .task {
            for await state in vmStoreOwner.instance.syncMessageQueue where state.isError() {
                if let errorState = state as? any SyncResultError {
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.errorAccountSync(errorState.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                }
            }
        }
    }
}
