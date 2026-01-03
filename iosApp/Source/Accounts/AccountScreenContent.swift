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

            case .googleDrive:
                NavigationLink(destination: GoogleDriveSyncScreen()) {
                    HStack {
                        Image("googledrive")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .foregroundStyle(.primary)
                        Text("Google Drive")
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

            case .miniflux:
                NavigationLink(destination: MinifluxSyncScreen(isFromAddAccount: false)) {
                    HStack {
                        Image("miniflux")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .foregroundStyle(.primary)
                        Text("Miniflux")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }

            case .bazqux:
                NavigationLink(destination: BazquxSyncScreen(isFromAddAccount: false)) {
                    HStack {
                        Image("bazqux")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .foregroundStyle(.primary)

                        Text("BazQux")
                            .font(.body)
                        Spacer()
                        Image(systemName: "checkmark")
                    }
                }

            case .feedbin:
                NavigationLink(destination: FeedbinSyncScreen(isFromAddAccount: false)) {
                    HStack {
                        Image("feedbin")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .foregroundStyle(.primary)
                        Text("Feedbin")
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
