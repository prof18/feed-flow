//
//  ICloudSyncScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 28/07/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct ICloudSyncScreenContent: View {
    @Environment(\.dismiss)
    private var dismiss

    var connectionState: AccountConnectionUiState
    let onConnectClick: () -> Void
    let onBackupClick: () -> Void
    let onDisconnectClick: () -> Void

    var body: some View {
        content
            .navigationTitle("iCloud")
    }

    @ViewBuilder private var content: some View {
        switch onEnum(of: connectionState) {
        case .loading:
            ProgressView()

        case .unlinked:
            disconnectedView

        case let .linked(state):
            makeLinkedScreen(state: state)
        }
    }

    @ViewBuilder private var loadingView: some View {
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
                        Text(feedFlowStrings.noIcloudSyncYet)
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
                Text(feedFlowStrings.icloudSyncSuccess)
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
                    Text(feedFlowStrings.icloudSyncDescription)
                        .font(.body)
                }

                Button(
                    action: {
                        self.onConnectClick()
                    },
                    label: {
                        Label(feedFlowStrings.accountConnectButton, systemImage: "link")
                    }
                )
            }
        }
    }
}
