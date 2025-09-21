//
//  BlockedWordsScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/09/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct BlockedWordsScreenContent: View {
    @State private var newWord: String = ""

    let words: [String]
    let onAddWord: (String) -> Void
    let onRemoveWord: (String) -> Void

    var body: some View {
        VStack(alignment: .leading) {
            Text(feedFlowStrings.blockedWordsDescription)
                .padding(.horizontal, Spacing.regular)
                .padding(.top, Spacing.regular)
                .foregroundColor(.secondary)

            Form {
                Section(feedFlowStrings.addWordPlaceholder) {
                HStack {
                    TextField(feedFlowStrings.addWordPlaceholder, text: $newWord)
                        .disableAutocorrection(true)
                        .textInputAutocapitalization(.never)
                        .onSubmit {
                            addWord()
                        }
                        .hoverEffect()

                    if !newWord.isEmpty {
                        Button {
                            addWord()
                        } label: {
                            Image(systemName: "checkmark.circle.fill")
                                .tint(.green)
                        }
                        .hoverEffect()
                    }
                }
                }

                if words.isEmpty {
                    Section {
                        Text(feedFlowStrings.blockedWordsEmptyState)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity, alignment: .center)
                    }
                } else {
                    Section(feedFlowStrings.settingsBlockedWords) {
                        ForEach(words, id: \.self) { word in
                            HStack {
                                Text(word)
                                    .frame(maxWidth: .infinity, alignment: .leading)

                                Button {
                                    onRemoveWord(word)
                                } label: {
                                    Image(systemName: "trash")
                                        .foregroundColor(.red)
                                }
                                .buttonStyle(.borderless)
                            }
                        }
                    }
                }
            }
        }
        .scrollContentBackground(.hidden)
        .scrollDismissesKeyboard(.interactively)
        .background(Color.secondaryBackgroundColor)
    }

    private func addWord() {
        let trimmedWord = newWord.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmedWord.isEmpty {
            onAddWord(trimmedWord)
            newWord = ""
        }
    }
}
