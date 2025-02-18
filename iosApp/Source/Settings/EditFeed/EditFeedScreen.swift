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
    @State var linkOpeningPreference = LinkOpeningPreference.default
    @State var isHidden = false
    @State var isPinned = false

    var body: some View {
        NavigationStack {
            ZStack {
                EditFeedScreenContent(
                    feedURL: $feedURL,
                    feedName: $feedName,
                    showError: $showError,
                    errorMessage: $errorMessage,
                    categoryItems: $categoryItems,
                    isAddingFeed: $isAddingFeed,
                    linkOpeningPreference: $linkOpeningPreference,
                    isHidden: $isHidden,
                    isPinned: $isPinned,
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
                    updateLinkOpeningPreference: { preference in
                        vmStoreOwner.instance.updateLinkOpeningPreference(preference: preference)
                    },
                    onHiddenToggled: { hidden in
                        vmStoreOwner.instance.updateIsHiddenFromTimeline(isHidden: hidden)
                    },
                    onPinnedToggled: { pinned in
                        vmStoreOwner.instance.updateIsPinned(isPinned: pinned)
                    },
                    addFeed: {
                        vmStoreOwner.instance.editFeed()
                    }
                )

                @Bindable var appState = appState
                VStack(spacing: 0) {
                    Spacer()
                    Snackbar(messageQueue: $appState.snackbarQueue)
                }
            }
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
                    case let .feedEdited(feedEditedState):
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

                    case let .error(error):
                        switch onEnum(of: error) {
                        case .invalidUrl:
                            errorMessage = feedFlowStrings.invalidRssUrl

                        case .invalidTitleLink:
                            errorMessage = feedFlowStrings.missingTitleAndLink

                        case .genericError:
                            errorMessage = feedFlowStrings.editFeedGenericError
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
            .task {
                for await state in vmStoreOwner.instance.linkOpeningPreferenceState {
                    self.linkOpeningPreference = state
                }
            }
            .task {
                for await state in vmStoreOwner.instance.isHiddenFromTimelineState {
                    self.isHidden = state as? Bool ?? false
                }
            }
            .task {
                for await state in vmStoreOwner.instance.isPinnedState {
                    self.isPinned = state as? Bool ?? false
                }
            }
        }
    }
}
