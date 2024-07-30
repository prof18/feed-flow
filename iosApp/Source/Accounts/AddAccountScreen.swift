//
//  AddAccountScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/07/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import SwiftUI
import shared

struct AddAccountScreen: View {

    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var appState: AppState

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
                                Label("Dropbox", systemImage: "shippingbox")
                            }

                        case .icloud:
                            NavigationLink(destination: ICloudSyncScreen()) {
                                Label("iCloud", systemImage: "icloud")
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
