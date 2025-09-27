//
//  ICloudSyncScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/07/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import SwiftUI

struct ICloudSyncScreen: View {
    @Environment(AppState.self)
    private var appState
    @Environment(\.dismiss)
    private var dismiss

    @StateObject private var vmStoreOwner = VMStoreOwner<ICloudSyncViewModel>(Deps.shared.getICloudSyncViewModel())

    @State private var uiState: AccountConnectionUiState = .Unlinked()

    let isFromAddAccount: Bool

    var body: some View {
        @Bindable var appState = appState

        ICloudSyncScreenContent(
            connectionState: uiState,
            onConnectClick: {
                vmStoreOwner.instance.setICloudAuth()
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
            for await state in vmStoreOwner.instance.iCloudConnectionUiState {
                self.uiState = state
                if uiState is AccountConnectionUiState.Linked {
                    if isFromAddAccount {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                            dismiss()
                            dismiss()
                        }
                    }
                }
            }
        }
        .task {
            for await state in vmStoreOwner.instance.syncMessageQueue where state.isError() {
                if let errorState = state as? SyncResultICloudNotAvailable {
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.icloudConnectionError(errorState.errorCode.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                } else if let errorState = state as? FeedSyncError {
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.errorAccountSync(errorState.code),
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                }
            }
        }
    }
}
