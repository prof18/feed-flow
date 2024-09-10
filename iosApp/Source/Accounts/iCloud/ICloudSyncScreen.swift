//
//  ICloudSyncScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/07/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import FeedFlowKit

struct ICloudSyncScreen: View {
    @Environment(AppState.self) private var appState

    @StateObject
    private var vmStoreOwner = VMStoreOwner<ICloudSyncViewModel>(Deps.shared.getICloudSyncViewModel())

    @State private var uiState: AccountConnectionUiState = AccountConnectionUiState.Unlinked()

    var body: some View {
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
        .task {
            for await state in vmStoreOwner.instance.iCloudConnectionUiState {
                self.uiState = state
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
