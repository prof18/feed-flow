//
//  GoogleDriveSyncScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 05/11/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct GoogleDriveSyncScreenContent: View {
    var connectionState: AccountConnectionUiState
    let onConnectClick: () -> Void
    let onBackupClick: () -> Void
    let onDisconnectClick: () -> Void

    var body: some View {
        content
            .navigationTitle("Google Drive")
    }

    @ViewBuilder private var content: some View {
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
                        Text(feedFlowStrings.noGoogleDriveSyncYet)
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
                Text(feedFlowStrings.googleDriveSyncSuccess)
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

    @ViewBuilder private var disconnectedView: some View {
        VStack {
            Form {
                Section {
                    VStack(alignment: .leading) {
                        Text(feedFlowStrings.googleDriveSyncCommonDescription)
                            .font(.body)
                    }
                }

                Button(
                    action: {
                        onConnectClick()
                    },
                    label: {
                        Label(feedFlowStrings.accountConnectButton, systemImage: "link")
                    }
                )
            }
        }
    }
}
