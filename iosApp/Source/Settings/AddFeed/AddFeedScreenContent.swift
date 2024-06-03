//
//  AddFeedScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/01/24.
//  Copyright © 2024. All rights reserved.
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
    let deleteCategory: (String) -> Void
    let addNewCategory: (CategoryName) -> Void
    let addFeed: () -> Void

    var body: some View {
        Form {
            Section(
                content: {
                    TextField(feedFlowStrings.feedUrl, text: $feedURL)
                        .keyboardType(.URL)
                        .textContentType(.URL)
                        .disableAutocorrection(true)
                        .hoverEffect()
                        .accessibilityIdentifier(TestingTag.shared.FEED_URL_INPUT)
                },
                header: {
                    Text(feedFlowStrings.feedUrl)
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

            Section(feedFlowStrings.addFeedCategoryTitle) {
                Picker(
                    selection: $categorySelectorObserver.selectedCategory,
                    label: Text(feedFlowStrings.addFeedCategoriesTitle)
                ) {
                    ForEach(categoryItems, id: \.self.id) { categoryItem in
                        let title = categoryItem.name ?? feedFlowStrings.noCategorySelectedHeader
                        Text(title)
                            .tag(categoryItem as CategoriesState.CategoryItem?)
                            .accessibilityIdentifier("\(TestingTag.shared.CATEGORY_RADIO_BUTTON)_\(title)")
                    }
                }
                .hoverEffect()
            }
            .accessibilityIdentifier(TestingTag.shared.CATEGORY_SELECTOR)

            if !categoryItems.isEmpty {
                categoriesSection
            }
        }
        .scrollContentBackground(.hidden)
        .scrollDismissesKeyboard(.interactively)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(feedFlowStrings.addFeed)
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
        Section(feedFlowStrings.addFeedCategoriesTitle) {
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
                                .hoverEffect()
                        }
                        .accessibilityIdentifier("\(TestingTag.shared.DELETE_CATEGORY_BUTTON)_\(name)")
                    }
                }
            }

            HStack {
                TextField(feedFlowStrings.newCategoryHint, text: $newCategory, axis: .horizontal)
                    .onSubmit {
                        addNewCategory(CategoryName(name: newCategory))
                        newCategory = ""
                    }
                    .hoverEffect()
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
                    .hoverEffect()
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
                Text(feedFlowStrings.actionSave).bold()
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
