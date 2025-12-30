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

    @State private var showCategorySheet = false
    @State private var showDeleteFeedDialog = false
    @FocusState private var isTextFieldFocused: Bool

    @Binding var feedURL: String
    @Binding var feedName: String
    @Binding var showError: Bool
    @Binding var errorMessage: String
    @Binding var isAddingFeed: Bool
    @Binding var linkOpeningPreference: LinkOpeningPreference
    @Binding var isHidden: Bool
    @Binding var isPinned: Bool

    var categorySelectorObserver: CategorySelectorObserver
    let viewModel: EditFeedViewModel

    let updateFeedUrlTextFieldValue: (String) -> Void
    let updateFeedNameTextFieldValue: (String) -> Void
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
                    .focused($isTextFieldFocused)
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
                        .focused($isTextFieldFocused)
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

            Section {
                Button(
                    action: {
                        showCategorySheet = true
                    },
                    label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(feedFlowStrings.addFeedCategoryTitle)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)

                                Text(
                                    categorySelectorObserver.selectedCategory?.name ??
                                    feedFlowStrings.noCategorySelectedHeader
                                )
                                .font(.body)
                                .foregroundColor(.primary)
                            }

                            Spacer()

                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .contentShape(Rectangle())
                    }
                )
                .buttonStyle(.plain)
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
        .sheet(isPresented: $showCategorySheet) {
            EditCategorySheetContainerForEdit(
                viewModel: viewModel,
                categorySelectorObserver: categorySelectorObserver,
                onSave: {}
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

    private var saveButton: some View {
        Button {
            isTextFieldFocused = false
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
