//
//  AccountsScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 28/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct AccountsScreen: View {
    @EnvironmentObject private var appState: AppState

    @StateObject private var viewModel: AccountsViewModel = KotlinDependencies.shared.getAccountsViewModel()

    @State private var syncAccount: SyncAccounts = SyncAccounts.local

    var body: some View {
        AccountsScreenContent(
            syncAccount: syncAccount
        ).task {
            do {
                let stream = asyncSequence(for: viewModel.accountsStateFlow)
                for try await account in stream {
                    self.syncAccount = account
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
    }
}

#Preview {
    AccountsScreen()
}
