//
//  NoFeedsBottomSheet.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 07/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct NoFeedsBottomSheet: View {
    @Environment(\.presentationMode)
    private var presentationMode
    @Environment(AppState.self)
    private var appState
    @Environment(\.dismiss)
    private var dismiss

    let onAddFeedClick: () -> Void
    let onImportExportClick: () -> Void

    var preferredHeight: CGFloat = 400

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text(feedFlowStrings.noFeedModalMessage)
                        .font(.body)
                        .multilineTextAlignment(.leading)
                }

                Section {
                    Button {
                        onAddFeedClick()
                    } label: {
                        Label(feedFlowStrings.addFeed, systemImage: "plus.app")
                    }

                    Button {
                        onImportExportClick()
                    } label: {
                        Label(feedFlowStrings.importExportOpml, systemImage: "arrow.up.arrow.down")
                    }

                    Button {
                        self.dismiss()
                        appState.navigate(route: CommonViewRoute.accounts)
                    } label: {
                        Label(feedFlowStrings.settingsAccounts, systemImage: "arrow.triangle.2.circlepath")
                    }
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .if(!isiOS26OrLater()) { view in
                            view.background(Color.secondaryBackgroundColor)
            }
            .navigationTitle(feedFlowStrings.noFeedModalTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        self.presentationMode.wrappedValue.dismiss()
                    } label: {
                        Text(feedFlowStrings.actionDone).bold()
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
}

#Preview {
    NoFeedsBottomSheet(onAddFeedClick: {}, onImportExportClick: {})
}
