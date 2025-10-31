//
//  EditFeedScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 20/12/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct EditFeedScreenContent: View {
    @Environment(\.presentationMode)
    private var presentationMode

    @State private var newCategory: String = ""
    @State private var showDeleteCategoryDialog = false
    @State private var showEditCategoryDialog = false
    @State private var showDeleteFeedDialog = false
    @State private var categoryToDelete: String?
    @State private var categoryToEdit: String?
    @State private var editedCategoryName: String = ""

    @Binding var feedURL: String
    @Binding var feedName: String
    @Binding var showError: Bool
    @Binding var errorMessage: String
    @Binding var categoryItems: [CategoriesState.CategoryItem]
    @Binding var isAddingFeed: Bool
    @Binding var linkOpeningPreference: LinkOpeningPreference
    @Binding var isHidden: Bool
    @Binding var isPinned: Bool

    var categorySelectorObserver: CategorySelectorObserver

    let updateFeedUrlTextFieldValue: (String) -> Void
    let updateFeedNameTextFieldValue: (String) -> Void
    let deleteCategory: (String) -> Void
    let addNewCategory: (CategoryName) -> Void
    let updateLinkOpeningPreference: (LinkOpeningPreference) -> Void
    let onHiddenToggled: (Bool) -> Void
    let onPinnedToggled: (Bool) -> Void
    let addFeed: () -> Void
    let updateCategoryName: (String, String) -> Void
    let onDeleteFeed: () -> Void

    var body: some View {
        Form {
            Section(
                content: {
                    TextField(
                        feedFlowStrings.feedName,
                        text: $feedName
                    )
                    .disableAutocorrection(true)
                    .hoverEffect()
                },
                header: {
                    Text(feedFlowStrings.feedName)
                }
            )

            Section(
                content: {
                    TextField(feedFlowStrings.feedUrl, text: $feedURL)
                        .keyboardType(.URL)
                        .textContentType(.URL)
                        .disableAutocorrection(true)
                        .hoverEffect()
                },
                header: {
                    Text(feedFlowStrings.feedUrl)
                },
                footer: {
                    if showError {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                }
            )

            Section(feedFlowStrings.linkOpeningPreference) {
                Picker(
                    selection: $linkOpeningPreference,
                    label: Text(feedFlowStrings.linkOpeningPreference)
                ) {
                    Text(feedFlowStrings.linkOpeningPreferenceDefault)
                        .tag(LinkOpeningPreference.default)
                    Text(feedFlowStrings.linkOpeningPreferenceReaderMode)
                        .tag(LinkOpeningPreference.readerMode)
                    Text(feedFlowStrings.linkOpeningPreferenceInternalBrowser)
                        .tag(LinkOpeningPreference.internalBrowser)
                    Text(feedFlowStrings.linkOpeningPreferencePreferredBrowser)
                        .tag(LinkOpeningPreference.preferredBrowser)
                }
                .onChange(of: linkOpeningPreference) {
                    updateLinkOpeningPreference(linkOpeningPreference)
                }
            }

            Section {
                Toggle(isOn: $isHidden) {
                    Text(feedFlowStrings.hideFeedFromTimelineDescription)
                }
                .onChange(of: isHidden) {
                    onHiddenToggled(isHidden)
                }

                Toggle(isOn: $isPinned) {
                    Text(feedFlowStrings.pinFeedSourceDescription)
                }
                .onChange(of: isPinned) {
                    onPinnedToggled(isPinned)
                }
            }

            Section(feedFlowStrings.addFeedCategoryTitle) {
                @Bindable var categorySelectorObserver = categorySelectorObserver
                Picker(
                    selection: $categorySelectorObserver.selectedCategory,
                    label: Text(feedFlowStrings.addFeedCategoryTitle)
                ) {
                    ForEach(categoryItems, id: \.self.id) { categoryItem in
                        let title = categoryItem.name ?? feedFlowStrings.noCategorySelectedHeader
                        Text(title)
                            .tag(categoryItem as CategoriesState.CategoryItem?)
                    }
                }
                .hoverEffect()
            }

            if !categoryItems.isEmpty {
                categoriesSection
            }

            Section {
                Button(
                    action: {
                        showDeleteFeedDialog = true
                    },
                    label: {
                        Text(feedFlowStrings.deleteFeedButton)
                            .foregroundColor(.red)
                            .frame(maxWidth: .infinity)
                    }
                )
                .buttonStyle(.borderless)
            }
        }
        .scrollContentBackground(.hidden)
        .scrollDismissesKeyboard(.interactively)
        .background(Color.secondaryBackgroundColor)
        .overlay {
            DeleteCategoryDialog(
                isPresented: $showDeleteCategoryDialog,
                categoryToDelete: $categoryToDelete,
                onDelete: deleteCategory
            )
        }
        .overlay {
            EditCategoryDialog(
                isPresented: $showEditCategoryDialog,
                categoryToEdit: $categoryToEdit,
                editedCategoryName: $editedCategoryName,
                onSave: updateCategoryName
            )
        }
        .alert(feedFlowStrings.deleteFeedConfirmationTitle, isPresented: $showDeleteFeedDialog) {
            Button(feedFlowStrings.deleteCategoryCloseButton, role: .cancel) { }
            Button(feedFlowStrings.deleteFeed, role: .destructive) {
                onDeleteFeed()
            }
        } message: {
            Text(feedFlowStrings.deleteFeedConfirmationMessage)
        }
        .navigationTitle(feedFlowStrings.editFeed)
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: feedURL) {
            updateFeedUrlTextFieldValue(feedURL)
        }
        .onChange(of: feedName) {
            updateFeedNameTextFieldValue(feedName)
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                saveButton
            }
        }
    }

    private var categoriesSection: some View {
        Section(feedFlowStrings.addFeedCategoriesTitle) {
            ForEach(categoryItems, id: \.self.id) { categoryItem in
                if let name = categoryItem.name {
                    Text(name)
                        .contextMenu {
                            Button {
                                editedCategoryName = name
                                categoryToEdit = categoryItem.id
                                showEditCategoryDialog = true
                            } label: {
                                Label(feedFlowStrings.editCategory, systemImage: "pencil")
                            }

                            Button(role: .destructive) {
                                categoryToDelete = categoryItem.id
                                showDeleteCategoryDialog = true
                            } label: {
                                Label(feedFlowStrings.deleteFeed, systemImage: "trash")
                            }
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
            }

            if !newCategory.isEmpty {
                Button {
                    addNewCategory(CategoryName(name: newCategory))
                    newCategory = ""
                } label: {
                    Image(systemName: "checkmark.circle.fill")
                        .tint(.green)
                }
                .hoverEffect()
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
    }
}
