//
//  FeedbinSyncScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FeedbinSyncScreen: View {
    @Environment(AppState.self)
    private var appState
    @Environment(\.dismiss)
    private var dismiss

    @StateObject private var vmStoreOwner = VMStoreOwner<FeedbinSyncViewModel>(
        Deps.shared.getFeedbinSyncViewModel()
    )

    @State private var uiState: AccountConnectionUiState = .Unlinked()
    @State private var isLoginLoading = false

    let isFromAddAccount: Bool

    var body: some View {
        @Bindable var appState = appState

        FeedbinSyncContent(
            uiState: uiState,
            isLoginLoading: isLoginLoading,
            onDisconnectClick: {
                vmStoreOwner.instance.disconnect()
            },
            onLoginClick: { username, password in
                vmStoreOwner.instance.login(
                    username: username,
                    password: password
                )
            }
        )
        .snackbar(messageQueue: $appState.snackbarQueue)
        .loadingDialog(isLoading: isLoginLoading)
        .task {
            for await state in vmStoreOwner.instance.uiState {
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
            for await state in vmStoreOwner.instance.loginLoading {
                self.isLoginLoading = state as? Bool ?? false
            }
        }
        .task {
            for await failure in vmStoreOwner.instance.errorState {
                let errorMessage: String
                switch onEnum(of: failure) {
                case let .networkFailure(failure):
                    if failure is NetworkFailure.Unauthorised {
                        errorMessage = feedFlowStrings.wrongCredentialsErrorMessage
                    } else {
                        errorMessage = feedFlowStrings.genericErrorMessage
                    }
                case .dataNotFound:
                    errorMessage = feedFlowStrings.wrongUrlErrorMessage
                default:
                    errorMessage = feedFlowStrings.genericErrorMessage
                }

                self.appState.snackbarQueue.append(
                    SnackbarData(
                        title: errorMessage,
                        subtitle: nil,
                        showBanner: true
                    )
                )
            }
        }
    }
}
