//
//  AccountScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 28/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import shared

struct AccountsScreenContent: View {
    let syncAccount: SyncAccounts

    var body: some View {
        Form {
            Section {
                Text(feedFlowStrings.accountsDescription)
            }

            NavigationLink(destination: DropboxSyncScreen()) {
                HStack {
                    AccountsItem(
                        title: "Dropbox",
                        icon: "shippingbox"
                    )

                    if syncAccount == .dropbox {
                        Image(systemName: "checkmark")
                    }
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                HStack {
                    Text(feedFlowStrings.settingsAccounts)
                        .font(.headline)

                    Text("BETA")
                        .font(.footnote)
                        .foregroundColor(.gray)
                }
            }
        }
    }
}

private struct AccountsItem: View {
    let title: String
    let icon: String

    var body: some View {
        HStack {
            Image(systemName: icon)
            Text(title)
                .font(.body)
            Spacer()
        }
    }
}

#Preview {
    AccountsScreenContent(syncAccount: SyncAccounts.dropbox)
}
