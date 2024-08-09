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

    @StateObject
    private var vmStoreOwner = VMStoreOwner<AccountsViewModel>(Deps.shared.getAccountsViewModel())

    @State private var syncAccount: SyncAccounts = SyncAccounts.local

    var body: some View {
        AccountsScreenContent(
            syncAccount: syncAccount,
            supportedAccounts: vmStoreOwner.instance.getSupportedAccounts()
        ).task {
            for await state in vmStoreOwner.instance.accountsState {
                 self.syncAccount = state
            }
        }
    }
}

#Preview {
    AccountsScreen()
}
