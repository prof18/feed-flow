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
    @Environment(\.dismiss)
    private var dismiss

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

    var categorySelectorObserver: CategorySelectorObserver
    let viewModel: AddFeedViewModel

    let showCloseButton: Bool
    let updateFeedUrlTextFieldValue: (String) -> Void
    let onNotificationToggled: (Bool) -> Void
    let addFeed: () -> Void
    let forceAddFeed: () -> Void

    var body: some View {
        Form {
            Section(
                content: {
                    TextField(feedFlowStrings.feedUrl, text: $feedURL)
                        .keyboardType(.URL)
                        .textContentType(.URL)
                        .autocorrectionDisabled()
                        .hoverEffect()
                        .focused($isTextFieldFocused)
                        .accessibilityIdentifier(AddFeedAccessibilityIdentifiers.urlInput)
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
                        isTextFieldFocused = false
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

            if showError && canForceAdd {
                Section {
                    VStack(alignment: .leading, spacing: 20) {
                        Toggle(isOn: $acknowledged) {
                            Text(feedFlowStrings.addFeedAnywayAcknowledgement)
                                .font(.subheadline)
                        }

                        Button(
                            action: {
                                isAddingFeed = true
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
                        dismiss()
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
                if !(showError && canForceAdd) {
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

// Previews disabled - require AddFeedViewModel instance
