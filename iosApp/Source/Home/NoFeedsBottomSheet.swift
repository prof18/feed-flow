//
//  NoFeedsBottomSheet.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 07/01/24.
//  Copyright © 2024. All rights reserved.
//

import SwiftUI
import shared

struct NoFeedsBottomSheet: View {

    @Environment(\.presentationMode) private var presentationMode

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
                        .accessibilityIdentifier(TestingTag.shared.NO_FEED_BOTTOM_SHEET_MESSAGE)
                }

                Section {
                    Button {
                        onAddFeedClick()
                    } label: {
                        Label(feedFlowStrings.addFeed, systemImage: "plus.app")
                    }
                    .accessibilityIdentifier(TestingTag.shared.NO_FEED_BOTTOM_SHEET_ADD_BUTTON)

                    Button {
                        onImportExportClick()
                    } label: {
                        Label(feedFlowStrings.importExportOpml, systemImage: "arrow.up.arrow.down")
                    }
                    .accessibilityIdentifier(TestingTag.shared.NO_FEED_BOTTOM_SHEET_IMPORT_BUTTON)
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(Color.secondaryBackgroundColor)
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
        .presentationDetents([.height(preferredHeight), .large])
    }
}

#Preview {
    NoFeedsBottomSheet(onAddFeedClick: {}, onImportExportClick: {})
}
