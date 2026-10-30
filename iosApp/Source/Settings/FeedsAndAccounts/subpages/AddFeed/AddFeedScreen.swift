//
//  AddFeedScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct AddFeedScreen: View {
    @Environment(AppState.self)
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<AddFeedViewModel>(Deps.shared.getAddFeedViewModel())

    @State private var categorySelectorObserver = CategorySelectorObserver()

    @State private var showError = false
    @State private var errorMessage = ""
    @State private var isAddingFeed = false
    @State private var feedURL = ""
    @State private var showNotificationToggle = false
    @State private var isNotificationEnabled = false
    @State private var youTubeOptionsState: YouTubeOptionsState?
    @State private var canForceAdd = false
    @State private var verificationChallengeUrl: String?
    @State private var verificationExplanationUrl: String?

    var showCloseButton: Bool

    init(showCloseButton: Bool = false) {
        self.showCloseButton = showCloseButton
    }

    var body: some View {
        @Bindable var appState = appState

        NavigationStack {
            AddFeedScreenContent(
                feedURL: $feedURL,
                showError: showError,
                errorMessage: errorMessage,
                isAddingFeed: $isAddingFeed,
                showNotificationToggle: showNotificationToggle,
                youTubeOptionsState: youTubeOptionsState,
                isNotificationEnabled: $isNotificationEnabled,
                canForceAdd: canForceAdd,
                categorySelectorObserver: categorySelectorObserver,
                viewModel: vmStoreOwner.instance,
                showCloseButton: showCloseButton,
                updateFeedUrlTextFieldValue: { value in
                    clearVerification()
                    vmStoreOwner.instance.updateFeedUrlTextFieldValue(feedUrlTextFieldValue: value)
                },
                onNotificationToggled: { enabled in
                    vmStoreOwner.instance.updateNotificationStatus(status: enabled)
                },
                onHideShortsToggled: { enabled in
                    vmStoreOwner.instance.updateHideShortsStatus(status: enabled)
                },
                addFeed: {
                    vmStoreOwner.instance.addFeed()
                },
                forceAddFeed: {
                    vmStoreOwner.instance.forceAddFeed()
                }
            )
            .snackbar(messageQueue: $appState.snackbarQueue)
        }
        .websiteVerificationExplanation(
            url: verificationExplanationUrl,
            onCancel: {
                clearVerification()
                vmStoreOwner.instance.cancelRecovery()
                errorMessage = feedFlowStrings.feedFetchRefusedError
                showError = true
                canForceAdd = true
            },
            onConfirm: {
                clearVerification()
                vmStoreOwner.instance.verifyWebsite()
            }
        )
        .onDisappear {
            vmStoreOwner.instance.cancelRecovery()
        }
        .onAppear {
            categorySelectorObserver.onCategorySelected = { categoryId in
                vmStoreOwner.instance.onCategorySelected(categoryId: CategoryId(value: categoryId))
            }
        }
        .task {
            for await state in vmStoreOwner.instance.feedAddedState {
                switch onEnum(of: state) {
                case let .feedAdded(addedState):
                    let message: String
                    if addedState.shortsFilterFallback {
                        message = feedFlowStrings.hideYoutubeShortsFallback
                    } else if let feedName = addedState.feedName {
                        message = feedFlowStrings.feedAddedMessage(feedName)
                    } else {
                        message = feedFlowStrings.feedAddedMessageWithoutName
                    }

                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: message,
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                    self.feedURL = ""
                    self.isAddingFeed = false
                    self.showError = false
                    self.errorMessage = ""
                    self.canForceAdd = false
                    clearVerification()

                case let .feedAlreadyExists(existingState):
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: feedFlowStrings.feedAlreadyExistsMessage(existingState.feedName),
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                    self.feedURL = ""
                    self.isAddingFeed = false
                    self.showError = false
                    self.errorMessage = ""
                    self.canForceAdd = false
                    clearVerification()

                case .feedNotAdded:
                    errorMessage = ""
                    showError = false
                    canForceAdd = false
                    clearVerification()

                case let .error(errorState):
                    canForceAdd = errorState.canForceAdd
                    clearVerification()
                    switch onEnum(of: errorState) {
                    case .invalidUrl:
                        errorMessage = feedFlowStrings.invalidRssUrlWithRetryHint

                    case .invalidTitleLink:
                        errorMessage = feedFlowStrings.missingTitleAndLink

                    case .genericError:
                        errorMessage = feedFlowStrings.addFeedGenericError

                    case let .fetchFailed(fetchFailure):
                        verificationChallengeUrl = fetchFailure.verificationUrl
                        errorMessage = feedFlowStrings.feedFetchRefusedError
                    }

                    isAddingFeed = false
                    showError = true
                    updateVerificationPresentation()

                case .loading:
                    clearVerification()
                    showError = false
                    errorMessage = ""
                }
            }
        }
        .task {
            for await required in vmStoreOwner.instance.verificationRequiredState
                where !(required as? Bool ?? false) {
                verificationExplanationUrl = nil
            }
        }
        .task {
            vmStoreOwner.instance.updateFeedUrlTextFieldValue(feedUrlTextFieldValue: feedURL)
        }
        .task {
            for await state in vmStoreOwner.instance.youTubeOptionsState {
                self.youTubeOptionsState = state
            }
        }
        .task {
            for await state in vmStoreOwner.instance.categoriesState {
                self.categorySelectorObserver.selectedCategory = state.categories.first {
                    $0.isSelected
                }
            }
        }
        .task {
            for await show in vmStoreOwner.instance.showNotificationToggleState {
                self.showNotificationToggle = show as? Bool ?? false
            }
        }
        .task {
            for await enabled in vmStoreOwner.instance.isNotificationEnabledState {
                self.isNotificationEnabled = enabled as? Bool ?? false
            }
        }
    }

    private func clearVerification() {
        verificationChallengeUrl = nil
        verificationExplanationUrl = nil
    }

    /// A challenged feed opens the verification explanation instead of the generic fetch error.
    private func updateVerificationPresentation() {
        guard verificationChallengeUrl != nil else { return }
        guard vmStoreOwner.instance.verificationAvailable else {
            errorMessage = feedFlowStrings.feedVerificationUnsupported
            canForceAdd = false
            return
        }
        showError = false
        errorMessage = ""
        canForceAdd = false
        verificationExplanationUrl = verificationChallengeUrl
    }
}
