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

    @State private var newCategory: String = ""

    @Binding var feedURL: String
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
                        .accessibilityIdentifier(TestingTag.shared.FEED_URL_INPUT)
                },
                header: {
                    Text(localizer.feed_url.localized)
                },
                footer: {
                    if showError {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundColor(.red)
                            .accessibilityIdentifier(TestingTag.shared.INVALID_URL_ERROR_MESSAGE)
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
                        Text(title)
                            .tag(categoryItem as CategoriesState.CategoryItem?)
                            .accessibilityIdentifier("\(TestingTag.shared.CATEGORY_RADIO_BUTTON)_\(title)")
                    }
                }
            }
            .accessibilityIdentifier(TestingTag.shared.CATEGORY_SELECTOR)

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
                    .accessibilityIdentifier(TestingTag.shared.BACK_BUTTON)
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
                        .accessibilityIdentifier("\(TestingTag.shared.DELETE_CATEGORY_BUTTON)_\(name)")
                    }
                }
            }

            HStack {
                TextField(localizer.new_category_hint.localized, text: $newCategory, axis: .horizontal)
                    .onSubmit {
                        addNewCategory(CategoryName(name: newCategory))
                        newCategory = ""
                    }
                    .accessibilityIdentifier(TestingTag.shared.CATEGORY_TEXT_INPUT)

                Spacer()

                if !newCategory.isEmpty {
                    Button {
                        addNewCategory(CategoryName(name: newCategory))
                        newCategory = ""
                    } label: {
                        Image(systemName: "checkmark.circle.fill")
                            .tint(.green)
                    }
                    .accessibilityIdentifier(TestingTag.shared.ADD_CATEGORY_BUTTON)
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
        .accessibilityIdentifier(TestingTag.shared.ADD_FEED_BUTTON)
    }
}

#Preview {
    AddFeedScreenContent(
        feedURL: .constant("https://marcogomiero.com/feed"),
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
        feedURL: .constant("https://marcogomiero.com"),
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
