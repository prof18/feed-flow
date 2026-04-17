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

                        case .icloud:
                            NavigationLink(destination: ICloudSyncScreen(isFromAddAccount: true)) {
                                Label {
                                    Text("iCloud")
                                } icon: {
                                    Image(systemName: "icloud")
                                        .fontWeight(.bold)
                                        .foregroundStyle(.blue)
                                }
                            }

                        case .freshRss:
                            NavigationLink(destination: FreshRssSyncScreen(isFromAddAccount: true)) {
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

                        case .miniflux:
                            NavigationLink(destination: MinifluxSyncScreen(isFromAddAccount: true)) {
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

                        case .bazqux:
                            NavigationLink(destination: BazquxSyncScreen(isFromAddAccount: true)) {
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

                        case .feedbin:
                            NavigationLink(destination: FeedbinSyncScreen(isFromAddAccount: true)) {
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
