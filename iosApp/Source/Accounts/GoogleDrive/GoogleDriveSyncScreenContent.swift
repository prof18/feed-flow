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
    let onBackupClick: () -> Void
    let onDisconnectClick: () -> Void
    @Binding var snackbarMessage: String?
    @Binding var showSnackbar: Bool

    var body: some View {
        NavigationView {
            ZStack {
                switch googleDriveConnectionUiState {
                case .unlinked:
                    DisconnectedView()
                case .loading:
                    LoadingView()
                case let .linked(syncState):
                    ConnectedView(
                        syncState: syncState,
                        onBackupClick: onBackupClick,
                        onDisconnectClick: onDisconnectClick
                    )
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
    var body: some View {
        Form {
            Section {
                Text("Sync your feeds across devices using Google Drive. Your data will be stored in your Google Drive app data folder.")
                    .font(.body)

                Button(action: {
                    GoogleDriveDataSourceIos.startAuth()
                }) {
                    Text("Connect to Google Drive")
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
                Text("Successfully connected to Google Drive")
                    .font(.body)

                switch syncState {
                case .loading:
                    HStack {
                        ProgressView()
                        Text("Syncing...")
                            .font(.body)
                    }
                case .none:
                    Text("No sync performed yet")
                        .font(.caption)
                        .foregroundColor(.secondary)
                case let .synced(lastDownloadDate, lastUploadDate):
                    if let lastUpload = lastUploadDate {
                        Text("Last upload: \(lastUpload)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    if let lastDownload = lastDownloadDate {
                        Text("Last download: \(lastDownload)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }

            Section {
                Button(action: onBackupClick) {
                    Label("Backup Now", systemImage: "arrow.up.doc")
                }
                .disabled(syncState is AccountSyncUIState.Loading)

                Button(action: onDisconnectClick) {
                    Label("Disconnect", systemImage: "link.badge.minus")
                        .foregroundColor(.red)
                }
                .disabled(syncState is AccountSyncUIState.Loading)
            }
        }
    }
}
