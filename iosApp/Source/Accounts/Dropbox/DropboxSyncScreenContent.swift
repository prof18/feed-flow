//
//  DropboxSyncScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 29/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import SwiftUI
import shared

struct DropboxSyncScreenContent: View {
    @EnvironmentObject private var appState: AppState

    var connectionState: AccountConnectionUiState
    let onDropboxAuthSuccess: () -> Void
    let onBackupClick: () -> Void
    let onDisconnectClick: () -> Void

    var body: some View {
        content
            .navigationTitle("Dropbox")
            .onReceive(NotificationCenter.default.publisher(for: .didDropboxSuccess)) { _ in
                print("Dropbox Success Notification")
                onDropboxAuthSuccess()
            }
            .onReceive(NotificationCenter.default.publisher(for: .didDropboxCancel)) { _ in
                print("Dropbox Cancel Notification")
            }
            .onReceive(NotificationCenter.default.publisher(for: .didDropboxError)) { _ in
                print("Dropbox Error Notification")
                self.appState.snackbarQueue.append(
                    SnackbarData(
                        title: feedFlowStrings.dropboxSyncError,
                        subtitle: nil,
                        showBanner: true
                    )
                )
            }
    }

    @ViewBuilder
    private var content: some View {
        switch connectionState {
        case is AccountConnectionUiState.Loading:
            ProgressView()

        case is AccountConnectionUiState.Unlinked:
            disconnectedView

        case let state as AccountConnectionUiState.Linked:
            makeLinkedScreen(state: state)
        default:
            EmptyView()
        }
    }

    @ViewBuilder
    private var loadingView: some View {
        ProgressView()
    }

    @ViewBuilder
    private func makeLinkedScreen(state: AccountConnectionUiState.Linked) -> some View {
        VStack {
            Form {
                makeSyncInfoView(state: state)

                Section {
                    switch onEnum(of: state.syncState) {
                    case .loading:
                        HStack {
                            ProgressView()
                                .padding(.leading, 0)

                            Text(feedFlowStrings.accountRefreshProgress)
                                .font(.body)
                                .padding(.horizontal, Spacing.regular)
                        }

                    case .none:
                        Text(feedFlowStrings.noDropboxSyncYet)
                            .font(.body)

                    case .synced:
                        EmptyView()
                    }
                }

                Button(
                    action: {
                        onBackupClick()
                    },
                    label: {
                        Label(feedFlowStrings.backupButton, systemImage: "square.and.arrow.up")
                    }
                )
                .disabled(state.syncState is AccountSyncUIState.Loading)

                Button(
                    action: {
                        onDisconnectClick()
                    },
                    label: {
                        Label(feedFlowStrings.accountDisconnectButton, systemImage: "square.slash")
                    }
                )
                .disabled(state.syncState is AccountSyncUIState.Loading)
            }
        }
    }

    @ViewBuilder
    private func makeSyncInfoView(state: AccountConnectionUiState.Linked) -> some View {
        Section {
            VStack(alignment: .leading) {
                Text(feedFlowStrings.dropboxSyncSuccess)
                    .font(.body)
                    .multilineTextAlignment(.leading)

                if let syncedState = state.syncState as? AccountSyncUIState.Synced {
                    VStack(alignment: .leading) {
                        if let lastUploadDate = syncedState.lastUploadDate {
                            Text(feedFlowStrings.lastUpload(lastUploadDate))
                                .font(.footnote)
                        }

                        if let lastDownloadDate = syncedState.lastDownloadDate {
                            Text(feedFlowStrings.lastDownload(lastDownloadDate))
                                .font(.footnote)
                                .padding(.top, Spacing.xxsmall)
                        }
                    }
                    .padding(.top, Spacing.regular)
                }
            }
        }
    }

    @ViewBuilder
    private var disconnectedView: some View {
        VStack {
            Form {
                Section {
                    VStack(alignment: .leading) {
                        Text(feedFlowStrings.dropboxSyncCommonDescription)
                            .font(.body)

                        Text(feedFlowStrings.dropboxSyncMobileDescription)
                            .font(.body)
                            .padding(.top, Spacing.regular)
                    }
                }

                Button(
                    action: {
                        DropboxDataSourceIos.startAuth()
                    },
                    label: {
                        Label(feedFlowStrings.accountConnectButton, systemImage: "link")
                    }
                )
            }
        }
    }
}

#Preview("Unlinked") {
    DropboxSyncScreenContent(
        connectionState: AccountConnectionUiState.Unlinked(),
        onDropboxAuthSuccess: {},
        onBackupClick: {},
        onDisconnectClick: {}
    )
}

#Preview("Loading") {
    DropboxSyncScreenContent(
        connectionState: AccountConnectionUiState.Linked(syncState: AccountSyncUIState.Loading()),
        onDropboxAuthSuccess: {},
        onBackupClick: {},
        onDisconnectClick: {}
    )
}

#Preview("None") {
    DropboxSyncScreenContent(
        connectionState: AccountConnectionUiState.Linked(syncState: AccountSyncUIState.None()),
        onDropboxAuthSuccess: {},
        onBackupClick: {},
        onDisconnectClick: {}
    )
}

#Preview("Synced") {
    DropboxSyncScreenContent(
        connectionState: AccountConnectionUiState.Linked(
            syncState: AccountSyncUIState.Synced(
                lastDownloadDate: "2024-06-29T10:00:00Z",
                lastUploadDate: "2024-06-29T10:00:00Z"
            )
        ),
        onDropboxAuthSuccess: {
        },
        onBackupClick: {},
        onDisconnectClick: {}
    )
}
