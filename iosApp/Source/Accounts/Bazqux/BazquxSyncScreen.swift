//
//  BazquxSyncScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct BazquxSyncScreen: View {
    @Environment(AppState.self)
    private var appState
    @StateObject private var vmStoreOwner = VMStoreOwner<BazquxSyncViewModel>(
        Deps.shared.getBazquxSyncViewModel()
    )

    @State private var uiState: AccountConnectionUiState = .Unlinked()
    @State private var isLoginLoading = false
    @State private var didHandleLinkedAccount = false

    let onAccountLinked: (() -> Void)?

    var body: some View {
        @Bindable var appState = appState

        BazquxSyncContent(
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
                if uiState is AccountConnectionUiState.Linked,
                   !didHandleLinkedAccount,
                   let onAccountLinked {
                    didHandleLinkedAccount = true
                    do {
                        try await Task.sleep(for: .seconds(1))
                    } catch {
                        return
                    }
                    onAccountLinked()
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
                case let .bazquxLoginFailure(failure):
                    if failure is BazquxLoginFailure.YearSubscriptionExpired {
                        errorMessage = feedFlowStrings.bazquxLoginSubscriptionExpired
                    } else if failure is BazquxLoginFailure.FreeTrialExpired {
                        errorMessage = feedFlowStrings.bazquxLoginFreeTrialExpired
                    } else {
                        errorMessage = feedFlowStrings.genericErrorMessage
                    }
                case let .networkFailure(failure):
                    if failure is NetworkFailure.Unauthorised {
                        errorMessage = feedFlowStrings.wrongCredentialsErrorMessage
                    } else {
                        errorMessage = feedFlowStrings.genericErrorMessage
                    }
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
