//
//  EditFeedScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 20/12/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct EditFeedScreen: View {
  @Environment(AppState.self) private var appState

  @Environment(\.presentationMode) private var presentationMode

  @StateObject
  private var vmStoreOwner = VMStoreOwner<EditFeedViewModel>(Deps.shared.getEditFeedViewModel())

  var feedSource: FeedSource

  @State private var categorySelectorObserver = CategorySelectorObserver()

  @State private var showError = false
  @State private var errorMessage = ""
  @State private var categoryItems: [CategoriesState.CategoryItem] = []
  @State private var isAddingFeed: Bool = false
  @State var feedURL = ""
  @State var feedName = ""

  var body: some View {
    NavigationStack {
      EditFeedScreenContent(
        feedURL: $feedURL,
        feedName: $feedName,
        showError: $showError,
        errorMessage: $errorMessage,
        categoryItems: $categoryItems,
        isAddingFeed: $isAddingFeed,
        categorySelectorObserver: categorySelectorObserver,
        updateFeedUrlTextFieldValue: { value in
          vmStoreOwner.instance.updateFeedUrlTextFieldValue(feedUrlTextFieldValue: value)
        },
        updateFeedNameTextFieldValue: { value in
          vmStoreOwner.instance.updateFeedNameTextFieldValue(feedNameTextFieldValue: value)
        },
        deleteCategory: { categoryId in
          vmStoreOwner.instance.deleteCategory(categoryId: categoryId)
        },
        addNewCategory: { categoryName in
          vmStoreOwner.instance.addNewCategory(categoryName: categoryName)
        },
        addFeed: {
          vmStoreOwner.instance.editFeed()
        }
      )
      .onAppear {
        vmStoreOwner.instance.loadFeedToEdit(feedSource: feedSource)
      }
      .task {
        for await state in vmStoreOwner.instance.feedUrlState {
          self.feedURL = state
        }
      }
      .task {
        for await state in vmStoreOwner.instance.feedNameState {
          self.feedName = state
        }
      }
      .task {
        for await state in vmStoreOwner.instance.feedEditedState {
          switch onEnum(of: state) {
          case .feedEdited(let feedEditedState):
            self.appState.snackbarQueue.append(
              SnackbarData(
                title: feedFlowStrings.feedEditedMessage(feedEditedState.feedName),
                subtitle: nil,
                showBanner: true
              )
            )
            self.appState.redrawAfterFeedSourceEdit.toggle()
            self.presentationMode.wrappedValue.dismiss()

          case .idle:
            errorMessage = ""
            showError = false

          case .loading:
            break

          case .error(let error):
            switch onEnum(of: error) {
            case .invalidUrl:
              errorMessage = feedFlowStrings.invalidRssUrl

            case .invalidTitleLink:
              errorMessage = feedFlowStrings.missingTitleAndLink
            }

            isAddingFeed = false
            showError = true
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
}
