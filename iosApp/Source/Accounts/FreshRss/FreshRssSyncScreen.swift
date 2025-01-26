//
//  FreshRssSyncScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FreshRssSyncScreen: View {
  @Environment(AppState.self) private var appState
  @Environment(\.dismiss) private var dismiss

  @StateObject
  private var vmStoreOwner = VMStoreOwner<FreshRssSyncViewModel>(
    Deps.shared.getFreshRssSyncViewModel()
  )

  @State private var uiState: AccountConnectionUiState = AccountConnectionUiState.Unlinked()
  @State private var isLoginLoading: Bool = false

  let isFromAddAccount: Bool

  var body: some View {

    ZStack {
      FreshRssSyncContent(
        uiState: uiState,
        isLoginLoading: isLoginLoading,
        onDisconnectClick: {
          vmStoreOwner.instance.disconnect()
        },
        onLoginClick: { serverUrl, username, password in
          vmStoreOwner.instance.login(
            username: username,
            password: password,
            url: serverUrl
          )
        }
      )

      @Bindable var appState = appState
      VStack(spacing: 0) {

        Spacer()

        Snackbar(messageQueue: $appState.snackbarQueue)
      }
    }
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
        case .networkFailure(let failure):
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
