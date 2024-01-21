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

    @EnvironmentObject private var appState: AppState

    @Environment(\.presentationMode) private var presentationMode

    @StateObject private var addFeedViewModel: AddFeedViewModel = KotlinDependencies.shared.getAddFeedViewModel()
    @StateObject private var categorySelectorObserver = CategorySelectorObserver()

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
                    addFeedViewModel.updateFeedUrlTextFieldValue(feedUrlTextFieldValue: value)
                },
                deleteCategory: { categoryId in
                    addFeedViewModel.deleteCategory(categoryId: categoryId)
                },
                addNewCategory: { categoryName in
                    addFeedViewModel.addNewCategory(categoryName: categoryName)
                },
                addFeed: {
                    addFeedViewModel.addFeed()
                }
            )
        }
        .task {
            do {
                let stream = asyncSequence(for: addFeedViewModel.feedAddedState)
                for try await state in stream {
                    switch state {
                    case let addedState as FeedAddedState.FeedAdded:
                        self.appState.snackbarQueue.append(
                            SnackbarData(
                                title: addedState.message.localized(),
                                subtitle: nil,
                                showBanner: true
                            )
                        )
                        self.feedURL = ""
                        self.isAddingFeed = false

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

}
