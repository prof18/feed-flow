//
//  AddFeedScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct AddFeedScreenContent: View {
    @Environment(\.presentationMode)
    private var presentationMode

    @State private var showCategorySheet = false
    @FocusState private var isTextFieldFocused: Bool

    @Binding var feedURL: String
    @Binding var showError: Bool
    @Binding var errorMessage: String
    @Binding var isAddingFeed: Bool
    var showNotificationToggle: Bool
    @Binding var isNotificationEnabled: Bool
    @Binding var canForceAdd: Bool
    @State private var acknowledged: Bool = false
    @State private var showE2eForceAdd = false

    var categorySelectorObserver: CategorySelectorObserver
    let viewModel: AddFeedViewModel

    let showCloseButton: Bool
    let updateFeedUrlTextFieldValue: (String) -> Void
    let onNotificationToggled: (Bool) -> Void
    let addFeed: () -> Void
    let forceAddFeed: () -> Void
    let prepareE2eForceAddFailure: () -> Void

    var body: some View {
        Form {
            Section(
                content: {
                    TextField(feedFlowStrings.feedUrl, text: $feedURL)
                        .keyboardType(.URL)
                        .textContentType(.URL)
                        .disableAutocorrection(true)
                        .hoverEffect()
                        .focused($isTextFieldFocused)
                        .accessibilityIdentifier(AddFeedAccessibilityIdentifiers.urlInput)

                    #if DEBUG
                        Button {
                            let e2eURL = "https://e2e.feedflow.local/feed.xml"
                            feedURL = e2eURL
                            updateFeedUrlTextFieldValue(e2eURL)
                        } label: {
                            Image(systemName: "textformat")
                        }
                        .accessibilityIdentifier(AddFeedAccessibilityIdentifiers.applyE2eUrlButton)
                        .hoverEffect()

                        Button {
                            prepareE2eForceAddFailure()
                            acknowledged = false
                            showE2eForceAdd = true
                        } label: {
                            Image(systemName: "exclamationmark.triangle")
                        }
                        .accessibilityIdentifier(AddFeedAccessibilityIdentifiers.prepareForceAddFailureButton)
                        .hoverEffect()
                    #endif
                },
                header: {
                    Text(feedFlowStrings.feedUrl)
                },
                footer: {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(feedFlowStrings.feedUrlHelpText)
                            .font(.caption)
                            .foregroundColor(.secondary)

                        if showError {
                            Text(errorMessage)
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                    }
                }
            )

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
                .accessibilityIdentifier(AddFeedAccessibilityIdentifiers.categorySelector)
            }

            if showNotificationToggle {
                Section {
                    Toggle(isOn: $isNotificationEnabled) {
                        Text(feedFlowStrings.enableNotificationsForFeed)
                    }
                    .onChange(of: isNotificationEnabled) {
                        onNotificationToggled(isNotificationEnabled)
                    }
                }
            }

            if (showError && canForceAdd) || showE2eForceAdd {
                Section {
                    VStack(alignment: .leading, spacing: 20) {
                        if showE2eForceAdd {
                            Text(feedFlowStrings.invalidRssUrlWithRetryHint)
                                .font(.caption)
                                .foregroundColor(.red)
                        }

                        Toggle(isOn: $acknowledged) {
                            Text(feedFlowStrings.addFeedAnywayAcknowledgement)
                                .font(.subheadline)
                        }

                        #if DEBUG
                            Button {
                                acknowledged = true
                            } label: {
                                Image(systemName: "checkmark.circle")
                            }
                            .accessibilityIdentifier(AddFeedAccessibilityIdentifiers.forceAddAcknowledgeToggle)
                        #endif

                        Button(
                            action: {
                                isAddingFeed = true
                                showE2eForceAdd = false
                                forceAddFeed()
                            },
                            label: {
                                if isAddingFeed {
                                    ProgressView()
                                        .frame(maxWidth: .infinity)
                                } else {
                                    Text(feedFlowStrings.addFeedAnywayButton)
                                        .frame(maxWidth: .infinity)
                                }
                            }
                        )
                        .buttonStyle(.bordered)
                        .disabled(!acknowledged || isAddingFeed)
                        .accessibilityIdentifier(AddFeedAccessibilityIdentifiers.forceAddButton)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .onChange(of: showError) { _, newValue in
            if !newValue {
                acknowledged = false
            }
        }
        .scrollContentBackground(.hidden)
        .scrollDismissesKeyboard(.interactively)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(feedFlowStrings.addFeed)
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: feedURL) {
            updateFeedUrlTextFieldValue(feedURL)
        }
        .sheet(isPresented: $showCategorySheet) {
            EditCategorySheetContainer(
                viewModel: viewModel,
                categorySelectorObserver: categorySelectorObserver,
                onSave: {}
            )
        }
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarLeading) {
                if showCloseButton {
                    Button(action: {
                        presentationMode.wrappedValue.dismiss()
                    }, label: {
                        if isiOS26OrLater() {
                            Image(systemName: "xmark")
                                .scaleEffect(0.8)
                        } else {
                            Image(systemName: "xmark.circle")
                        }
                    })
                }
            }

            ToolbarItemGroup(placement: .navigationBarTrailing) {
                if !((showError && canForceAdd) || showE2eForceAdd) {
                    saveButton
                }
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
        .accessibilityIdentifier(AddFeedAccessibilityIdentifiers.saveButton)
    }
}

private enum AddFeedAccessibilityIdentifiers {
    static let urlInput = "add_feed_url_input"
    static let saveButton = "add_feed_save_button"
    static let categorySelector = "edit_feed_category_selector"
    static let applyE2eUrlButton = "add_feed_apply_e2e_url"
    static let prepareForceAddFailureButton = "add_feed_prepare_force_add_failure"
    static let forceAddAcknowledgeToggle = "add_feed_force_add_acknowledge"
    static let forceAddButton = "add_feed_force_add_button"
}

// Previews disabled - require AddFeedViewModel instance
