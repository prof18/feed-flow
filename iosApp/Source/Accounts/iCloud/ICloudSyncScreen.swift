//
//  ICloudSyncScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/07/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct ICloudSyncScreen: View {
    @EnvironmentObject private var appState: AppState

    @StateObject
    private var viewModel: ICloudSyncViewModel = KotlinDependencies.shared.getICloudSyncViewModel()

    @State private var uiState: AccountConnectionUiState = AccountConnectionUiState.Unlinked()

    var body: some View {
        ICloudSyncScreenContent(
            connectionState: uiState,
            onConnectClick: {
                viewModel.setICloudAuth()
            },
            onBackupClick: {
                viewModel.triggerBackup()
            },
            onDisconnectClick: {
                viewModel.unlink()
            }
        )
        .task {
            do {
                let stream = asyncSequence(for: viewModel.iCloudConnectionUiStateFlow)
                for try await state in stream {
                    self.uiState = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: viewModel.syncMessageQueue)
                for try await message in stream where message.isError() {
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.errorAccountSync,
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
    }
}
