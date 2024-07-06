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

    @StateObject
    private var viewModel: DropboxSyncViewModel = KotlinDependencies.shared.getDropboxSyncViewModel()

    @State private var uiState: DropboxConnectionUiState = DropboxConnectionUiState.Unlinked()

    var body: some View {
        DropboxSyncScreenContent(
            connectionState: uiState,
            onDropboxAuthSuccess: {
                viewModel.saveDropboxAuth()
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
                let stream = asyncSequence(for: viewModel.dropboxConnectionUiStateFlow)
                for try await state in stream {
                    self.uiState = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: viewModel.dropboxSyncMessageState)
                for try await message in stream where message  is DropboxSynMessages.Error {
                        self.appState.snackbarQueue.append(
                            SnackbarData(
                                title: feedFlowStrings.dropboxSyncError,
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

#Preview {
    DropboxSyncScreen()
}
