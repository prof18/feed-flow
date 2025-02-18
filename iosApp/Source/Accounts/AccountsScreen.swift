//
//  AccountsScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 28/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import SwiftUI

struct AccountsScreen: View {
    @StateObject
    private var vmStoreOwner = VMStoreOwner<AccountsViewModel>(Deps.shared.getAccountsViewModel())

    @State private var syncAccount: SyncAccounts = .local

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
