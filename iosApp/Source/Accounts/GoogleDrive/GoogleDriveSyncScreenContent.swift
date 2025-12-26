//
//  GoogleDriveSyncScreenContent.swift
//  FeedFlow
//
//  Created by Claude on 05/11/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct GoogleDriveSyncScreenContent: View {
    let googleDriveConnectionUiState: AccountConnectionUiState
    let onBackClick: () -> Void
    let onConnectClick: () -> Void
    let onBackupClick: () -> Void
    let onDisconnectClick: () -> Void
    @Binding var snackbarMessage: String?
    @Binding var showSnackbar: Bool

    var body: some View {
        NavigationView {
            ZStack {
                switch googleDriveConnectionUiState {
                case is AccountConnectionUiState.Unlinked:
                    DisconnectedView(onConnectClick: onConnectClick)
                case is AccountConnectionUiState.Loading:
                    LoadingView()
                case let state as AccountConnectionUiState.Linked:
                    ConnectedView(
                        syncState: state.syncState,
                        onBackupClick: onBackupClick,
                        onDisconnectClick: onDisconnectClick
                    )
                default:
                    EmptyView()
                }

                if showSnackbar, let message = snackbarMessage {
                    VStack {
                        Spacer()
                        Text(message)
                            .padding()
                            .background(Color.secondary)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                            .padding()
                    }
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            showSnackbar = false
                        }
                    }
                }
            }
            .navigationTitle("Google Drive")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBackClick) {
                        Image(systemName: "chevron.left")
                    }
                }
            }
        }
    }
}

private struct LoadingView: View {
    var body: some View {
        VStack {
            ProgressView()
        }
    }
}

private struct DisconnectedView: View {
    let onConnectClick: () -> Void

    var body: some View {
        Form {
            Section {
                Text(feedFlowStrings.googleDriveSyncCommonDescription)
                    .font(.body)

                Button(action: onConnectClick) {
                    Label(feedFlowStrings.googleDriveConnectButton, systemImage: "link")
                }
            }
        }
    }
}

private struct ConnectedView: View {
    let syncState: AccountSyncUIState
    let onBackupClick: () -> Void
    let onDisconnectClick: () -> Void

    var body: some View {
        Form {
            Section {
                Text(feedFlowStrings.googleDriveSyncSuccess)
                    .font(.body)

                switch syncState {
                case is AccountSyncUIState.Loading:
                    HStack {
                        ProgressView()
                        Text(feedFlowStrings.accountRefreshProgress)
                            .font(.body)
                    }
                case is AccountSyncUIState.None:
                    Text(feedFlowStrings.noGoogleDriveSyncYet)
                        .font(.caption)
                        .foregroundColor(.secondary)
                case let synced as AccountSyncUIState.Synced:
                    if let lastUpload = synced.lastUploadDate {
                        Text(feedFlowStrings.lastUpload(lastUpload))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    if let lastDownload = synced.lastDownloadDate {
                        Text(feedFlowStrings.lastDownload(lastDownload))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                default:
                    EmptyView()
                }
            }

            Section {
                Button(action: onBackupClick) {
                    Label(feedFlowStrings.backupButton, systemImage: "arrow.up.doc")
                }
                .disabled(syncState is AccountSyncUIState.Loading)

                Button(action: onDisconnectClick) {
                    Label(feedFlowStrings.accountDisconnectButton, systemImage: "link.badge.minus")
                        .foregroundColor(.red)
                }
                .disabled(syncState is AccountSyncUIState.Loading)
            }
        }
    }
}
