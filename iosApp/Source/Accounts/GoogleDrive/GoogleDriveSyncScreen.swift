//
//  GoogleDriveSyncScreen.swift
//  FeedFlow
//
//  Created by Claude on 05/11/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct GoogleDriveSyncScreen: View {
    @Environment(\.dismiss) private var dismiss

    @StateObject private var vmStoreOwner = VMStoreOwner<GoogleDriveSyncViewModel>(
        Deps.shared.getGoogleDriveSyncViewModel()
    )

    @State private var googleDriveConnectionUiState: AccountConnectionUiState = .unlinked
    @State private var snackbarMessage: String?
    @State private var showSnackbar = false

    var body: some View {
        GoogleDriveSyncScreenContent(
            googleDriveConnectionUiState: googleDriveConnectionUiState,
            onBackClick: {
                dismiss()
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
            for await message in vmStoreOwner.instance.syncMessageQueue {
                handleSyncMessage(message: message)
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .didGoogleDriveSuccess)) { _ in
            // Google Drive auth succeeded
            if let accessToken = getGoogleDriveAccessToken() {
                vmStoreOwner.instance.saveGoogleDriveAuth(accessToken: accessToken)
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .didGoogleDriveCancel)) { _ in
            snackbarMessage = "Google Drive authentication cancelled"
            showSnackbar = true
        }
        .onReceive(NotificationCenter.default.publisher(for: .didGoogleDriveError)) { _ in
            snackbarMessage = "Google Drive authentication error"
            showSnackbar = true
        }
    }

    private func handleMessage(message: GoogleDriveSynMessages) {
        switch message {
        case is GoogleDriveSynMessages.Error:
            snackbarMessage = "Google Drive sync error"
            showSnackbar = true
        case is GoogleDriveSynMessages.ProceedToAuth:
            // OAuth is handled natively on iOS
            break
        default:
            break
        }
    }

    private func handleSyncMessage(message: FeedSyncMessage) {
        switch message {
        case is FeedSyncMessage.BackupSuccess:
            snackbarMessage = "Backup completed successfully"
            showSnackbar = true
        case is FeedSyncMessage.BackupError:
            snackbarMessage = "Backup failed"
            showSnackbar = true
        default:
            break
        }
    }

    private func getGoogleDriveAccessToken() -> String? {
        // This would retrieve the access token from Google Sign-In
        // For now, return a placeholder
        return "google_drive_access_token"
    }
}

extension Notification.Name {
    static let didGoogleDriveSuccess = Notification.Name("didGoogleDriveSuccess")
    static let didGoogleDriveCancel = Notification.Name("didGoogleDriveCancel")
    static let didGoogleDriveError = Notification.Name("didGoogleDriveError")
}
