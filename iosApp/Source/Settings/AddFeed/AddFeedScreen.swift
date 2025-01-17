//
//  SwiftUIView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import FeedFlowKit

struct AddFeedScreen: View {

    @Environment(AppState.self) private var appState

    @StateObject
    private var vmStoreOwner = VMStoreOwner<AddFeedViewModel>(Deps.shared.getAddFeedViewModel())

    @State private var categorySelectorObserver = CategorySelectorObserver()

    @State private var showError = false
    @State private var errorMessage = ""
    @State private var categoryItems: [CategoriesState.CategoryItem] = []
    @State private var isAddingFeed: Bool = false
    @State var feedURL = ""

    var showCloseButton: Bool

    init(showCloseButton: Bool = false) {
        self.showCloseButton = showCloseButton
    }

    var body: some View {
        NavigationStack {
            AddFeedScreenContent(
                feedURL: $feedURL,
                showError: $showError,
                errorMessage: $errorMessage,
                categoryItems: $categoryItems,
                isAddingFeed: $isAddingFeed,
                categorySelectorObserver: categorySelectorObserver,
                showCloseButton: showCloseButton,
                updateFeedUrlTextFieldValue: { value in
                    vmStoreOwner.instance.updateFeedUrlTextFieldValue(feedUrlTextFieldValue: value)
                },
                deleteCategory: { categoryId in
                    vmStoreOwner.instance.deleteCategory(categoryId: categoryId)
                },
                addNewCategory: { categoryName in
                    vmStoreOwner.instance.addNewCategory(categoryName: categoryName)
                },
                addFeed: {
                    vmStoreOwner.instance.addFeed()
                }
            )
        }
        .task {
            for await state in vmStoreOwner.instance.feedAddedState {
                switch onEnum(of: state) {
                case .feedAdded(let addedState):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.feedAddedMessage(addedState.feedName),
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                    self.feedURL = ""
                    self.isAddingFeed = false

                case .feedNotAdded:
                    errorMessage = ""
                    showError = false

                case .error(let errorState):
                    switch onEnum(of: errorState) {
                    case .invalidUrl:
                        errorMessage = feedFlowStrings.invalidRssUrl

                    case .invalidTitleLink:
                        errorMessage = feedFlowStrings.missingTitleAndLink
                    }

                    isAddingFeed = false
                    showError = true

                case .loading:
                    break
                }
            }
        }
        .task {
            for await state in vmStoreOwner.instance.categoriesState {
                self.categorySelectorObserver.selectedCategory = state.categories.first { $0.isSelected }
                self.categoryItems = state.categories
            }
        }
    }
}
