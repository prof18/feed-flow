//
//  DropboxSyncScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 28/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct DropboxSyncScreen: View {
    @EnvironmentObject private var appState: AppState

    @StateObject private var vmStoreOwner = VMStoreOwner<DropboxSyncViewModel>(KotlinDependencies.shared.getDropboxSyncViewModel())

    @State private var uiState: AccountConnectionUiState = AccountConnectionUiState.Unlinked()

    var body: some View {
        DropboxSyncScreenContent(
            connectionState: uiState,
            onDropboxAuthSuccess: {
                vmStoreOwner.instance.saveDropboxAuth()
            },
            onBackupClick: {
                vmStoreOwner.instance.triggerBackup()
            },
            onDisconnectClick: {
                vmStoreOwner.instance.unlink()
            }
        )
        .task {
            do {
                let stream = asyncSequence(for: vmStoreOwner.instance.dropboxConnectionUiStateFlow)
                for try await state in stream {
                    self.uiState = state
                }
            } catch {
                if !(error is CancellationError) {
                                    self.appState.emitGenericError()
                                }
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: vmStoreOwner.instance.dropboxSyncMessageState)
                for try await message in stream where message is DropboxSynMessages.Error {
                        self.appState.snackbarQueue.append(
                            SnackbarData(
                                title: feedFlowStrings.dropboxSyncError,
                                subtitle: nil,
                                showBanner: true
                            )
                        )
                }
            } catch {
                if !(error is CancellationError) {
                                    self.appState.emitGenericError()
                                }
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: vmStoreOwner.instance.syncMessageQueue)
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
                if !(error is CancellationError) {
                                    self.appState.emitGenericError()
                                }
            }
        }
    }
}
