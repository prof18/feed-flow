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
                            .foregroundStyle(.blue)
                        Text("Dropbox")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }
                .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("dropbox"))

            case .googleDrive:
                NavigationLink(destination: GoogleDriveSyncScreen()) {
                    HStack {
                        Image("googledrive")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .foregroundStyle(.blue)
                        Text("Google Drive")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }
                .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("google_drive"))

            case .icloud:
                NavigationLink(destination: ICloudSyncScreen(onAccountLinked: nil)) {
                    HStack {
                        Image(systemName: "icloud")
                            .fontWeight(.bold)
                            .foregroundStyle(.blue)
                        Text("iCloud")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }
                .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("icloud"))

            case .freshRss:
                NavigationLink(destination: FreshRssSyncScreen(onAccountLinked: nil)) {
                    HStack {
                        Image("freshrss")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .foregroundStyle(.blue)
                        Text("FreshRSS")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }
                .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("fresh_rss"))

            case .miniflux:
                NavigationLink(destination: MinifluxSyncScreen(onAccountLinked: nil)) {
                    HStack {
                        Image("miniflux")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .foregroundStyle(.blue)
                        Text("Miniflux")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }
                .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("miniflux"))

            case .bazqux:
                NavigationLink(destination: BazquxSyncScreen(onAccountLinked: nil)) {
                    HStack {
                        Image("bazqux")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .foregroundStyle(.blue)

                        Text("BazQux")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }
                .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("bazqux"))

            case .feedbin:
                NavigationLink(destination: FeedbinSyncScreen(onAccountLinked: nil)) {
                    HStack {
                        Image("feedbin")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .foregroundStyle(.blue)
                        Text("Feedbin")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }    
                .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("feedbin"))

            case .local:
                EmptyView()
            }

            Button {
                self.showAddAccountSheet.toggle()
            } label: {
                Label(feedFlowStrings.addAccountButton, systemImage: "plus.app")
            }
            .disabled(syncAccount != SyncAccounts.local)
            .accessibilityIdentifier(AccountAccessibilityIdentifiers.addAccount)
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
