//
//  AddFeedScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/01/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import shared

struct AddFeedScreenContent: View {

    @Environment(\.presentationMode) private var presentationMode

    @State var feedURL = ""
    @State private var newCategory: String = ""

    @Binding var showError: Bool
    @Binding var errorMessage: String
    @Binding var categoryItems: [CategoriesState.CategoryItem]
    @Binding var isAddingFeed: Bool

    @ObservedObject var categorySelectorObserver: CategorySelectorObserver

    let showCloseButton: Bool
    let updateFeedUrlTextFieldValue: (String) -> Void
    let deleteCategory: (Int64) -> Void
    let addNewCategory: (CategoryName) -> Void
    let addFeed: () -> Void

    var body: some View {
        Form {
            Section(
                content: {
                    TextField(localizer.feed_url.localized, text: $feedURL)
                },
                header: {
                    Text(localizer.feed_url.localized)
                },
                footer: {
                    if showError {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                }
            )

            Section(localizer.add_feed_category_title.localized) {
                Picker(
                    selection: $categorySelectorObserver.selectedCategory,
                    label: Text(localizer.add_feed_categories_title.localized)
                ) {
                    ForEach(categoryItems, id: \.self.id) { categoryItem in
                        let title = categoryItem.name ?? localizer.no_category_selected_header.localized
                        Text(title).tag(categoryItem as CategoriesState.CategoryItem?)
                    }
                }
            }

            if !categoryItems.isEmpty {
                categoriesSection
            }
        }
        .scrollContentBackground(.hidden)
        .scrollDismissesKeyboard(.interactively)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(localizer.add_feed.localized)
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: feedURL) { value in
            updateFeedUrlTextFieldValue(value)
        }
        .toolbar {
            if showCloseButton {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button {
                        presentationMode.wrappedValue.dismiss()
                    } label: {
                        Image(systemName: "xmark.circle")
                    }
                }
            }

            ToolbarItem(placement: .navigationBarTrailing) {
                saveButton
            }
        }
    }

    private var categoriesSection: some View {
        Section(localizer.add_feed_categories_title.localized) {
            ForEach(categoryItems, id: \.self.id) { categoryItem in
                if let name = categoryItem.name {
                    HStack {
                        Text(name)
                        Spacer()
                        Button {
                            deleteCategory(categoryItem.id)
                        } label: {
                            Image(systemName: "trash")
                                .tint(.red)
                        }
                    }
                }
            }

            HStack {
                TextField(localizer.new_category_hint.localized, text: $newCategory, axis: .horizontal)
                    .onSubmit {
                        addNewCategory(CategoryName(name: newCategory))
                        newCategory = ""
                    }
                Spacer()
                if !newCategory.isEmpty {
                    Button {
                        addNewCategory(CategoryName(name: newCategory))
                        newCategory = ""
                    } label: {
                        Image(systemName: "checkmark.circle.fill")
                            .tint(.green)
                    }
                }
            }
        }
    }

    private var saveButton: some View {
        Button {
            isAddingFeed.toggle()
            addFeed()
        } label: {
            if isAddingFeed {
                ProgressView()
            } else {
                Text(localizer.action_save.localized).bold()
            }
        }
        .disabled(feedURL.isEmpty)
    }
}

#Preview {
    AddFeedScreenContent(
        feedURL: "https://marcogomiero.com/feed",
        showError: .constant(false),
        errorMessage: .constant(""),
        categoryItems: .constant(PreviewItemsKt.categoryItems),
        isAddingFeed: .constant(false),
        categorySelectorObserver: CategorySelectorObserver(),
        showCloseButton: true,
        updateFeedUrlTextFieldValue: { _ in},
        deleteCategory: { _ in},
        addNewCategory: { _ in },
        addFeed: {}
    )
}

#Preview {
    AddFeedScreenContent(
        feedURL: "https://marcogomiero.com",
        showError: .constant(true),
        errorMessage: .constant("The provided link is not a RSS feed"),
        categoryItems: .constant(PreviewItemsKt.categoryItems),
        isAddingFeed: .constant(false),
        categorySelectorObserver: CategorySelectorObserver(),
        showCloseButton: true,
        updateFeedUrlTextFieldValue: { _ in},
        deleteCategory: { _ in},
        addNewCategory: { _ in },
        addFeed: {}
    )
}
