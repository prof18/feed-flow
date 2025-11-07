//
//  ChangeCategorySheet.swift
//  FeedFlow
//
//  Created by Claude on ${DATE}.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct ChangeCategorySheet: View {
    let feedSource: FeedSource
    let categories: [FeedSourceCategory]
    let onCategorySelected: (FeedSourceCategory?) -> Void
    let onDismiss: () -> Void

    @State private var selectedCategory: FeedSourceCategory?

    init(
        feedSource: FeedSource,
        categories: [FeedSourceCategory],
        onCategorySelected: @escaping (FeedSourceCategory?) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.feedSource = feedSource
        self.categories = categories
        self.onCategorySelected = onCategorySelected
        self.onDismiss = onDismiss
        self._selectedCategory = State(initialValue: feedSource.category)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Button(action: {
                        selectedCategory = nil
                    }) {
                        HStack {
                            Text(feedFlowStrings.noCategory)
                                .foregroundColor(.primary)
                            Spacer()
                            if selectedCategory == nil {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }

                Section {
                    ForEach(categories, id: \.id) { category in
                        Button(action: {
                            selectedCategory = category
                        }) {
                            HStack {
                                Text(category.title)
                                    .foregroundColor(.primary)
                                Spacer()
                                if selectedCategory?.id == category.id {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.accentColor)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle(feedFlowStrings.changeCategory)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(feedFlowStrings.deleteCategoryCloseButton) {
                        onDismiss()
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button(feedFlowStrings.actionSave) {
                        onCategorySelected(selectedCategory)
                    }
                }
            }
        }
    }
}
