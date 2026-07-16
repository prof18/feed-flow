//
//  AddAccountScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/07/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct AddAccountScreen: View {
    @Environment(\.dismiss)
    private var dismiss

    let supportedAccounts: [SyncAccounts]

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    ForEach(supportedAccounts, id: \.self) { account in
                        switch account {
                        case .dropbox:
                            NavigationLink(destination: DropboxSyncScreen()) {
                                Label {
                                    Text("Dropbox")
                                } icon: {
                                    Image("dropbox")
                                        .renderingMode(.template)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 32, height: 32)
                                        .foregroundStyle(.blue)
                                }
                            }
                            .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("dropbox"))

                        case .icloud:
                            NavigationLink(destination: ICloudSyncScreen(onAccountLinked: { dismiss() })) {
                                Label {
                                    Text("iCloud")
                                } icon: {
                                    Image(systemName: "icloud")
                                        .fontWeight(.bold)
                                        .foregroundStyle(.blue)
                                }
                            }
                            .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("icloud"))

                        case .freshRss:
                            NavigationLink(destination: FreshRssSyncScreen(onAccountLinked: { dismiss() })) {
                                Label {
                                    Text("FreshRSS")
                                } icon: {
                                    Image("freshrss")
                                        .renderingMode(.template)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 22, height: 22)
                                        .foregroundStyle(.blue)
                                }
                            }
                            .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("fresh_rss"))

                        case .miniflux:
                            NavigationLink(destination: MinifluxSyncScreen(onAccountLinked: { dismiss() })) {
                                Label {
                                    Text("Miniflux")
                                } icon: {
                                    Image("miniflux")
                                        .renderingMode(.template)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 22, height: 22)
                                        .foregroundStyle(.blue)
                                }
                            }
                            .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("miniflux"))

                        case .bazqux:
                            NavigationLink(destination: BazquxSyncScreen(onAccountLinked: { dismiss() })) {
                                Label {
                                    Text("BazQux")
                                } icon: {
                                    Image("bazqux")
                                        .renderingMode(.template)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 22, height: 22)
                                        .foregroundStyle(.blue)
                                }
                            }
                            .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("bazqux"))

                        case .googleDrive:
                            NavigationLink(destination: GoogleDriveSyncScreen()) {
                                Label {
                                    Text("Google Drive")
                                } icon: {
                                    Image("googledrive")
                                        .renderingMode(.template)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 22, height: 22)
                                        .foregroundStyle(.blue)
                                }
                            }
                            .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("google_drive"))

                        case .feedbin:
                            NavigationLink(destination: FeedbinSyncScreen(onAccountLinked: { dismiss() })) {
                                Label {
                                    Text("Feedbin")
                                } icon: {
                                    Image("feedbin")
                                        .renderingMode(.template)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 22, height: 22)
                                        .foregroundStyle(.blue)
                                }
                            }
                            .accessibilityIdentifier(AccountAccessibilityIdentifiers.provider("feedbin"))

                        case .local:
                            EmptyView()
                        }
                    }
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        self.dismiss()
                    } label: {
                        if isiOS26OrLater() {
                            Image(systemName: "xmark")
                        } else {
                            Image(systemName: "xmark.circle")
                        }
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .navigationTitle(feedFlowStrings.addAccountButton)
        }
    }
}
