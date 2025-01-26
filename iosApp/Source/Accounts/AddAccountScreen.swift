//
//  AddAccountScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/07/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import SwiftUI
import FeedFlowKit

struct AddAccountScreen: View {

    @Environment(\.dismiss) private var dismiss
    @Environment(AppState.self) private var appState

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
                                Label {
                                    Text("Dropbox")
                                } icon: {
                                    Image("dropbox")
                                        .renderingMode(.template)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 32, height: 32)
                                        .foregroundStyle(.primary)
                                }
                            }

                        case .icloud:
                            NavigationLink(destination: ICloudSyncScreen(isFromAddAccount: true)) {
                                Label {
                                    Text("iCloud")
                                } icon: {
                                    Image(systemName: "icloud")
                                        .fontWeight(.bold)
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
                        Image(systemName: "xmark.circle")
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
