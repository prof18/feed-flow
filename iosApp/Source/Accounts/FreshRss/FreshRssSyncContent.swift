//
//  FreshRssSyncContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FreshRssSyncContent: View {
  @State private var serverUrl: String = ""
  @State private var username: String = ""
  @State private var password: String = ""
  @State private var isPasswordVisible: Bool = false

  let uiState: AccountConnectionUiState
  let isLoginLoading: Bool
  let onDisconnectClick: () -> Void
  let onLoginClick: (String, String, String) -> Void

  var body: some View {
    content
      .navigationTitle("FreshRSS")
  }

  @ViewBuilder
  private var content: some View {
    switch uiState {
    case is AccountConnectionUiState.Loading:
      loadingView()
    case is AccountConnectionUiState.Unlinked:
      disconnectedView()
    case let linked as AccountConnectionUiState.Linked:
      makeLinkedScreen(state: linked)
    default:
      EmptyView()
    }
  }

  private func loadingView() -> some View {
    VStack {
      Spacer()
      ProgressView()
      Spacer()
    }
  }

  private func disconnectedView() -> some View {
    Form {
      Section(
        content: {
          TextField(feedFlowStrings.accountTextFieldServerUrl, text: $serverUrl)
            .textContentType(.URL)
            .keyboardType(.URL)
            .disableAutocorrection(true)
        },
        header: {
          Text(feedFlowStrings.accountTextFieldServerUrl)
        }
      )

      Section(
        content: {
          TextField(feedFlowStrings.accountTextFieldUsername, text: $username)
            .textContentType(.username)
            .disableAutocorrection(true)
        },
        header: {
          Text(feedFlowStrings.accountTextFieldUsername)
        }
      )

      Section(
        content: {
          HStack {
            if isPasswordVisible {
              TextField(feedFlowStrings.accountTextFieldPassword, text: $password)
                .textContentType(.password)
            } else {
              SecureField(feedFlowStrings.accountTextFieldPassword, text: $password)
                .textContentType(.password)
            }

            Button(
              action: { isPasswordVisible.toggle() }
            ) {
              Image(systemName: isPasswordVisible ? "eye.fill" : "eye.slash.fill")
                .foregroundColor(.gray)
            }
          }
        },
        header: {
          Text(feedFlowStrings.accountTextFieldPassword)
        }
      )

      Section {
        Button(
          action: {
            onLoginClick(serverUrl, username, password)
          }
        ) {
          HStack {
            Spacer()
            Text(feedFlowStrings.accountConnectButton)
            Spacer()
          }
        }
        .disabled(isLoginLoading || serverUrl.isEmpty || username.isEmpty || password.isEmpty)
      }
    }
    .scrollContentBackground(.hidden)
    .scrollDismissesKeyboard(.interactively)
    .background(Color.secondaryBackgroundColor)
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
            Text(feedFlowStrings.noFreshRssSyncYet)
              .font(.body)

          case .synced:
            EmptyView()
          }
        }

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
        Text(feedFlowStrings.freshRssAccountConnected)
          .font(.body)
          .multilineTextAlignment(.leading)

        if let syncedState = state.syncState as? AccountSyncUIState.Synced {
          VStack(alignment: .leading) {
            if let lastDownloadDate = syncedState.lastDownloadDate {
              Text(feedFlowStrings.freshRssLastSync(lastDownloadDate))
                .font(.footnote)
            }
          }
          .padding(.top, Spacing.regular)
        }
      }
    }
  }
}
