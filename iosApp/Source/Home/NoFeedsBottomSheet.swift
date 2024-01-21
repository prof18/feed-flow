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
                    Text(localizer.no_feed_modal_message.localized)
                        .font(.body)
                        .multilineTextAlignment(.leading)
                        .accessibilityIdentifier(TestingTag.shared.NO_FEED_BOTTOM_SHEET_MESSAGE)
                }

                Section {
                    Button {
                        onAddFeedClick()
                    } label: {
                        Label(localizer.add_feed.localized, systemImage: "plus.app")
                    }
                    .accessibilityIdentifier(TestingTag.shared.NO_FEED_BOTTOM_SHEET_ADD_BUTTON)

                    Button {
                        onImportExportClick()
                    } label: {
                        Label(localizer.import_export_opml.localized, systemImage: "arrow.up.arrow.down")
                    }
                    .accessibilityIdentifier(TestingTag.shared.NO_FEED_BOTTOM_SHEET_IMPORT_BUTTON)
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(Color.secondaryBackgroundColor)
            .navigationTitle(localizer.no_feed_modal_title.localized)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        self.presentationMode.wrappedValue.dismiss()
                    } label: {
                        Text(localizer.action_done.localized).bold()
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
