//
//  SwiftUIView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct AddFeedScreen: View {

    @EnvironmentObject
    private var appState: AppState

    @Environment(\.presentationMode)
    private var presentationMode

    @StateObject
    private var addFeedViewModel: AddFeedViewModel = KotlinDependencies.shared.getAddFeedViewModel()

    @StateObject
    private var categorySelectorObserver = CategorySelectorObserver()

    @State
    private var feedURL = ""

    @State
    private var showError = false

    @State
    private var errorMessage = ""

    @State
    private var categoryItems: [CategoriesState.CategoryItem] = []

    @State
    private var newCategory: String = ""

    @State
    private var isAddingFeed: Bool = false

    let showCloseButton: Bool

    init(showCloseButton: Bool = false) {
        self.showCloseButton = showCloseButton
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(
                    content: {
                        TextField(
                            localizer.feed_url.localized,
                            text: $feedURL
                        )
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
                addFeedViewModel.updateFeedUrlTextFieldValue(feedUrlTextFieldValue: value)
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
        .task {
            do {
                let stream = asyncSequence(for: addFeedViewModel.feedAddedState)
                for try await state in stream {
                    switch state {
                    case let addedState as FeedAddedState.FeedAdded:
                        //                        self.addFeedViewModel.clearAddDoneState()
                        self.appState.snackbarQueue.append(
                            SnackbarData(
                                title: addedState.message.localized(),
                                subtitle: nil,
                                showBanner: true
                            )
                        )
                        presentationMode.wrappedValue.dismiss()

                    case is FeedAddedState.FeedNotAdded:
                        errorMessage = ""
                        showError = false

                    case let errorState as FeedAddedState.Error:
                        errorMessage = errorState.errorMessage.localized()
                        isAddingFeed = false
                        showError = true

                    default:
                        break
                    }
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: addFeedViewModel.categoriesState)
                for try await state in stream {
                    self.categorySelectorObserver.selectedCategory = state.categories.first { $0.isSelected }
                    self.categoryItems = state.categories
                }
            } catch {
                self.appState.emitGenericError()
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
                            addFeedViewModel.deleteCategory(categoryId: categoryItem.id)
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
                        addFeedViewModel.addNewCategory(categoryName: CategoryName(name: newCategory))
                        newCategory = ""
                    }
                Spacer()
                if !newCategory.isEmpty {
                    Button {
                        addFeedViewModel.addNewCategory(categoryName: CategoryName(name: newCategory))
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
            Task {
                isAddingFeed.toggle()
                addFeedViewModel.addFeed()
            }
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

class CategorySelectorObserver: ObservableObject {
    @Published var selectedCategory: CategoriesState.CategoryItem? {
        didSet {
            if let selectedCategory = selectedCategory {
                selectedCategory.onClick(CategoryId(value: selectedCategory.id))
            }
        }
    }
}

#Preview {
    AddFeedScreen()
}
