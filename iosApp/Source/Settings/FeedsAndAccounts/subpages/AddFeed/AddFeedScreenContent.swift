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

    var categorySelectorObserver: CategorySelectorObserver
    let viewModel: AddFeedViewModel

    let showCloseButton: Bool
    let updateFeedUrlTextFieldValue: (String) -> Void
    let addFeed: () -> Void

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

// Previews disabled - require AddFeedViewModel instance
