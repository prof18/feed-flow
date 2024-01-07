//
//  NoFeedsBottomSheet.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 07/01/24.
//  Copyright © 2024. All rights reserved.
//

import SwiftUI

struct NoFeedsBottomSheet: View {

    @Environment(\.presentationMode)
    private var presentationMode

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
                }

                Section {
                    Button {
                        onAddFeedClick()
                    } label: {
                        Label(
                            localizer.add_feed.localized,
                            systemImage: "plus.app"
                        )
                    }

                    Button {
                        onImportExportClick()
                    } label: {
                        Label(
                            localizer.import_export_opml.localized,
                            systemImage: "arrow.up.arrow.down"
                        )
                    }
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
