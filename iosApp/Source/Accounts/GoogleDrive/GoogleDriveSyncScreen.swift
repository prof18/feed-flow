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
    @Environment(\.dismiss) private var dismiss
    @Environment(AppState.self) private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<GoogleDriveSyncViewModel>(
        Deps.shared.getGoogleDriveSyncViewModel()
    )

    private let googleDriveDataSource = Deps.shared.getGoogleDriveDataSource()

    @State private var googleDriveConnectionUiState: AccountConnectionUiState = AccountConnectionUiState.Loading()
    @State private var snackbarMessage: String?
    @State private var showSnackbar = false

    var body: some View {
        GoogleDriveSyncScreenContent(
            googleDriveConnectionUiState: googleDriveConnectionUiState,
            onBackClick: {
                dismiss()
            },
            onConnectClick: {
                googleDriveDataSource.authenticate { success in
                    if success.boolValue {
                        vmStoreOwner.instance.onAuthorizationSuccess()
                    } else {
                        vmStoreOwner.instance.onAuthorizationFailed()
                    }
                }
            },
            onBackupClick: {
                vmStoreOwner.instance.triggerBackup()
            },
            onDisconnectClick: {
                vmStoreOwner.instance.unlink()
            },
            snackbarMessage: $snackbarMessage,
            showSnackbar: $showSnackbar
        )
        .task {
            for await state in vmStoreOwner.instance.googleDriveConnectionUiState {
                googleDriveConnectionUiState = state
            }
        }
        .task {
            for await message in vmStoreOwner.instance.googleDriveSyncMessageState {
                handleMessage(message: message)
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

    private func handleMessage(message: GoogleDriveSynMessages) {
        switch message {
        case is GoogleDriveSynMessages.Error:
            appState.snackbarQueue.append(
                SnackbarData(
                    title: feedFlowStrings.googleDriveSyncError,
                    subtitle: nil,
                    showBanner: true
                )
            )
        case is GoogleDriveSynMessages.ProceedToAuth:
            // OAuth is handled natively on iOS
            break
        default:
            break
        }
    }
}
