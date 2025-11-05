//
//  FeedbinSyncContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FeedbinSyncContent: View {
    @State private var username: String = ""
    @State private var password: String = ""
    @State private var isPasswordVisible = false

    let uiState: AccountConnectionUiState
    let isLoginLoading: Bool
    let onDisconnectClick: () -> Void
    let onLoginClick: (String, String) -> Void

    var body: some View {
        content
            .navigationTitle("Feedbin")
    }

    @ViewBuilder private var content: some View {
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

    private func usernameSection() -> some View {
        Section(
            content: {
                TextField(feedFlowStrings.accountTextFieldUsername, text: $username)
                    .textContentType(.username)
                    .keyboardType(.emailAddress)
                    .disableAutocorrection(true)
            },
            header: {
                Text(feedFlowStrings.accountTextFieldUsername)
            },
            footer: {
                Text(feedFlowStrings.feedbinUsernameHint)
                    .foregroundColor(.gray)
                    .font(.caption)
            }
        )
    }

    private func passwordSection() -> some View {
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
                        action: { isPasswordVisible.toggle() },
                        label: {
                            Image(systemName: isPasswordVisible ? "eye.fill" : "eye.slash.fill")
                                .foregroundColor(.gray)
                        }
                    )
                }
            },
            header: {
                Text(feedFlowStrings.accountTextFieldPassword)
            },
            footer: {
                Text(feedFlowStrings.feedbinPasswordHint)
                    .foregroundColor(.gray)
                    .font(.caption)
            }
        )
    }

    private func loginButton() -> some View {
        Section {
            Button(
                action: {
                    onLoginClick(username, password)
                },
                label: {
                    HStack {
                        Spacer()
                        Text(feedFlowStrings.accountConnectButton)
                        Spacer()
                    }
                }
            )
            .disabled(isLoginLoading || username.isEmpty || password.isEmpty)
        }
    }

    private func disconnectedView() -> some View {
        Form {
            usernameSection()
            passwordSection()
            loginButton()
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
                        Text(feedFlowStrings.noFeedbinSyncYet)
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
                Text(feedFlowStrings.feedbinAccountConnected)
                    .font(.body)
                    .multilineTextAlignment(.leading)

                if let syncedState = state.syncState as? AccountSyncUIState.Synced {
                    VStack(alignment: .leading) {
                        if let lastDownloadDate = syncedState.lastDownloadDate {
                            Text(feedFlowStrings.feedbinLastSync(lastDownloadDate))
                                .font(.footnote)
                        }
                    }
                    .padding(.top, Spacing.regular)
                }
            }
        }
    }
}
