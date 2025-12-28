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
    @Environment(AppState.self)
    private var appState

    let supportedAccounts: [SyncAccounts]

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    ForEach(supportedAccounts, id: \.self) { account in
                        switch account {
                        case .dropbox:
                            Button {
                                self.dismiss()
                                self.appState.navigate(route: CommonViewRoute.dropboxSync)
                            } label: {
                                HStack {
                                    Label {
                                        Text("Dropbox")
                                    } icon: {
                                        Image("dropbox")
                                            .renderingMode(.template)
                                            .resizable()
                                            .scaledToFit()
                                            .frame(width: 32, height: 32)
                                    }
                                    Spacer()
                                    Image(systemName: "chevron.right")
                                        .font(.footnote.weight(.semibold))
                                        .foregroundStyle(.tertiary)
                                }
                                .contentShape(Rectangle())
                                .foregroundStyle(.primary)
                            }
                            .buttonStyle(.plain)

                        case .icloud:
                            NavigationLink(destination: ICloudSyncScreen(isFromAddAccount: true)) {
                                Label {
                                    Text("iCloud")
                                } icon: {
                                    Image(systemName: "icloud")
                                        .fontWeight(.bold)
                                        .foregroundStyle(.primary)
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
                                        .foregroundStyle(.primary)
                                }
                            }

                        case .miniflux:
                            NavigationLink(destination: MinifluxSyncScreen(isFromAddAccount: true)) {
                                Label {
                                    Text("Miniflux")
                                } icon: {
                                    Image("freshrss")
                                        .renderingMode(.template)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 22, height: 22)
                                        .foregroundStyle(.primary)
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
                                        .foregroundStyle(.primary)
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

#Preview {
    AddAccountScreen(
        supportedAccounts: [SyncAccounts.dropbox, SyncAccounts.icloud]
    )
}
