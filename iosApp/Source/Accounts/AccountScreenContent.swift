//
//  AccountScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 28/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import SwiftUI

struct AccountsScreenContent: View {
    @State private var showAddAccountSheet = false

    let syncAccount: SyncAccounts
    let supportedAccounts: [SyncAccounts]

    var body: some View {
        Form {
            Section {
                Text(feedFlowStrings.accountsDescription)
            }

            switch syncAccount {
            case .dropbox:
                NavigationLink(destination: DropboxSyncScreen()) {
                    HStack {
                        Image("dropbox")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 32, height: 32)
                            .foregroundStyle(.primary)
                        Text("Dropbox")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }

            case .icloud:
                NavigationLink(destination: ICloudSyncScreen(isFromAddAccount: false)) {
                    HStack {
                        Image(systemName: "icloud")
                            .fontWeight(.bold)
                        Text("iCloud")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }

            case .freshRss:
                NavigationLink(destination: FreshRssSyncScreen(isFromAddAccount: false)) {
                    HStack {
                        Image("freshrss")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .foregroundStyle(.primary)
                        Text("FreshRSS")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }

            case .local:
                EmptyView()
            }

            Button {
                self.showAddAccountSheet.toggle()
            } label: {
                Label(feedFlowStrings.addAccountButton, systemImage: "plus.app")
            }.disabled(syncAccount != SyncAccounts.local)
        }
        .sheet(isPresented: $showAddAccountSheet) {
            AddAccountScreen(
                supportedAccounts: supportedAccounts
            )
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

#Preview {
    AccountsScreenContent(
        syncAccount: SyncAccounts.local,
        supportedAccounts: [
            SyncAccounts.dropbox,
            SyncAccounts.icloud
        ]
    )
}
