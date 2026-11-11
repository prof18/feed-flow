//
//  FeedSuggestionsContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Nuke
import NukeUI
import SwiftUI
import UIKit

@MainActor
struct FeedSuggestionsContent: View {
    let viewModel: FeedSuggestionsViewModel

    @State private var suggestedCategories: [SuggestedFeedCategory] = []
    @State private var selectedCategoryId: String = ""
    @State private var operationStates: [String: FeedSuggestionOperationState] = [:]
    @State private var undoOffer: FeedSuggestionUndoOffer?
    @State private var isLoading: Bool = true
    @State private var undoSnackbarId: UUID?

    @Environment(AppState.self)
    private var appState

    private var selectedCategory: SuggestedFeedCategory? {
        suggestedCategories.first(where: { $0.id == selectedCategoryId }) ?? suggestedCategories.first
    }

    private var filteredFeeds: [SuggestedFeed] {
        selectedCategory?.feeds ?? []
    }

    private var undoTimeoutMillis: Int64 {
        UIAccessibility.isVoiceOverRunning ? 30_000 : 10_000
    }

    var body: some View {
        Group {
            if isLoading {
                ProgressView()
            } else {
                VStack(spacing: 0) {
                    CategoryFilterRow(
                        categories: suggestedCategories,
                        selectedCategoryId: selectedCategoryId,
                        onCategorySelected: { categoryId in
                            selectedCategoryId = categoryId
                        }
                    )
                    .padding(.top, Spacing.small)
                    .padding(.bottom, Spacing.regular)

                    Divider()

                    if filteredFeeds.isEmpty {
                        let emptyMessage = suggestedCategories.isEmpty
                            ? feedFlowStrings.feedSuggestionsEmptyCatalog
                            : feedFlowStrings.feedSuggestionsEmptyCategory
                        ContentUnavailableView(emptyMessage, systemImage: "lightbulb")
                            .frame(maxHeight: .infinity)
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 0) {
                                ForEach(filteredFeeds, id: \.url) { feed in
                                    VStack(alignment: .leading, spacing: 0) {
                                        SuggestedFeedRow(
                                            feed: feed,
                                            operation: operationStates[feed.url],
                                            selectedCategoryName: selectedCategory?.name,
                                            onAddFeed: {
                                                if let categoryName = selectedCategory?.name {
                                                    viewModel.addFeed(
                                                        feed: feed,
                                                        categoryName: categoryName,
                                                        undoTimeoutMillis: undoTimeoutMillis
                                                    )
                                                }
                                            }
                                        )

                                        if feed.url != filteredFeeds.last?.url {
                                            Divider()
                                                .padding(.leading, 72)
                                        }
                                    }
                                }
                            }
                        }
                        .contentMargins(.bottom, 96, for: .scrollContent)
                    }
                }
                .accessibilityIdentifier(FeedSuggestionsIds.screen)
            }
        }
        .navigationTitle(feedFlowStrings.feedSuggestionsTitle)
        .navigationBarTitleDisplayMode(.inline)
        .onDisappear {
            showUndoSnackbar(for: nil)
            viewModel.endVisit()
        }
        .onChange(of: undoOffer?.generation) { _, _ in
            showUndoSnackbar(for: undoOffer)
        }
        .task {
            for await categories in viewModel.suggestedCategoriesState {
                self.suggestedCategories = categories.compactMap { $0 }
                if selectedCategoryId.isEmpty, let first = categories.first {
                    selectedCategoryId = first.id
                }
            }
        }
        .task {
            for await categoryId in viewModel.selectedCategoryIdState {
                if let id = categoryId {
                    self.selectedCategoryId = id
                }
            }
        }
        .task {
            for await states in viewModel.operationStates {
                self.operationStates = states
            }
        }
        .task {
            for await offer in viewModel.undoOfferState {
                self.undoOffer = offer
            }
        }
        .task {
            for await loading in viewModel.isLoadingState {
                self.isLoading = loading as? Bool ?? false
            }
        }
    }

    private func showUndoSnackbar(for offer: FeedSuggestionUndoOffer?) {
        if let previousId = undoSnackbarId {
            let presentation = appState.snackbarPresentation
            if presentation.messageID == previousId {
                withAnimation { _ = presentation.beginDismissal(previousId) }
            } else {
                appState.snackbarQueue.removeAll { $0.id == previousId }
            }
            undoSnackbarId = nil
        }
        guard let offer else { return }
        let snackbar = SnackbarData(
            title: feedFlowStrings.feedSuggestionAddedMessage(offer.feedName, offer.categoryName),
            subtitle: nil,
            showBanner: true,
            action: SnackbarAction(
                label: feedFlowStrings.feedSuggestionUndo,
                accessibilityIdentifier: FeedSuggestionsIds.undoButton
            ) { [viewModel] in
                viewModel.undoLastAdd(generation: offer.generation)
            },
            duration: .milliseconds(undoTimeoutMillis)
        )
        undoSnackbarId = snackbar.id
        appState.snackbarQueue.append(snackbar)
    }
}

private struct CategoryFilterRow: View {
    let categories: [SuggestedFeedCategory]
    let selectedCategoryId: String
    let onCategorySelected: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.small) {
                ForEach(categories, id: \.id) { category in
                    CategoryFilterChip(
                        category: category,
                        isSelected: category.id == selectedCategoryId,
                        onTap: { onCategorySelected(category.id) }
                    )
                }
            }
            .padding(.horizontal, Spacing.regular)
        }
    }
}

private struct CategoryFilterChip: View {
    let category: SuggestedFeedCategory
    let isSelected: Bool
    let onTap: () -> Void
    @Environment(\.colorScheme)
    private var colorScheme

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                Text(category.icon)
                    .font(.subheadline)
                Text(category.name)
                    .font(.subheadline)
                    .fontWeight(isSelected ? .semibold : .medium)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .fill(isSelected ? Color.accentColor.opacity(colorScheme == .dark ? 0.25 : 0.15) : Color.clear)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .strokeBorder(
                        isSelected ? Color.accentColor : Color(UIColor.separator),
                        lineWidth: isSelected ? 2 : 1
                    )
            )
        }
        .buttonStyle(.plain)
        .foregroundColor(isSelected ? .accentColor : .primary)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(category.name)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
        .accessibilityIdentifier(FeedSuggestionsIds.category(category.id))
    }
}

private struct SuggestedFeedRow: View {
    let feed: SuggestedFeed
    let operation: FeedSuggestionOperationState?
    let selectedCategoryName: String?
    let onAddFeed: () -> Void

    private var errorMessage: String? {
        operation?.status == .addFailed ? feedFlowStrings.feedSuggestionAddError : nil
    }

    private var addedToCategory: String? {
        guard let category = operation?.addedToCategory, category != selectedCategoryName else { return nil }
        return category
    }

    var body: some View {
        HStack(alignment: .top, spacing: Spacing.regular) {
            if let logoUrl = feed.logoUrl, let url = URL(string: logoUrl) {
                LazyImage(request: ImageRequest.resized(url: url, size: CGSize(width: 40, height: 40))) { state in
                    if let image = state.image {
                        image
                            .resizable()
                            .scaledToFill()
                            .frame(width: 40, height: 40)
                            .cornerRadius(8)
                    } else if state.isLoading {
                        ProgressView()
                            .frame(width: 40, height: 40)
                    } else {
                        FeedPlaceholderIcon()
                    }
                }
            } else {
                FeedPlaceholderIcon()
            }

            VStack(alignment: .leading, spacing: Spacing.xxsmall) {
                Text(feed.name)
                    .font(.headline)
                    .fontWeight(.bold)
                    .lineLimit(2)
                    .accessibilityIdentifier(FeedSuggestionsIds.row(feed.url))

                Text(feed.feedDescription)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)

                if let addedToCategory {
                    Label(
                        feedFlowStrings.feedSuggestionAddedTo(addedToCategory),
                        systemImage: "checkmark.circle.fill"
                    )
                    .font(.caption.weight(.medium))
                    .foregroundStyle(Color.accentColor)
                    .labelStyle(.titleAndIcon)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, Spacing.xxsmall)
                }

                if let errorMessage {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(.red)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            Spacer()

            AddButton(
                feed: feed,
                operation: operation,
                onTap: onAddFeed
            )
        }
        .onChange(of: errorMessage) { _, message in
            if let message {
                UIAccessibility.post(notification: .announcement, argument: message)
            }
        }
        .padding(.horizontal, Spacing.regular)
        .padding(.vertical, Spacing.regular - Spacing.xsmall)
        .accessibilityElement(children: .contain)
    }
}

private struct FeedPlaceholderIcon: View {
    var body: some View {
        Image(systemName: "square.stack.3d.up")
            .font(.system(size: 24))
            .foregroundColor(.secondary)
            .frame(width: 40, height: 40)
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(8)
    }
}

private struct AddButton: View {
    let feed: SuggestedFeed
    let operation: FeedSuggestionOperationState?
    let onTap: () -> Void

    private var label: String {
        switch operation?.status {
        case .adding: feedFlowStrings.addingFeed
        case .addFailed: feedFlowStrings.retryButton
        case .added: feedFlowStrings.feedSuggestionAdded
        default: feedFlowStrings.feedSuggestionAdd
        }
    }

    private var isBusy: Bool {
        operation?.status == .adding
    }

    private var canAdd: Bool {
        operation == nil || operation?.status == .addFailed
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 4) {
                if isBusy {
                    ProgressView()
                        .controlSize(.small)
                } else if !canAdd {
                    Image(systemName: "checkmark")
                        .font(.system(size: 12, weight: .semibold))
                } else if operation == nil {
                    Image(systemName: "plus")
                        .font(.system(size: 12, weight: .semibold))
                }
                Text(label)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .frame(minHeight: 44)
            .background(
                canAdd ? Color.clear : Color(UIColor.secondarySystemFill),
                in: RoundedRectangle(cornerRadius: 8)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .strokeBorder(Color(UIColor.separator), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .foregroundStyle(canAdd ? .primary : .secondary)
        .disabled(!canAdd)
        .accessibilityLabel("\(label) · \(feed.name)")
        .accessibilityIdentifier(FeedSuggestionsIds.addButton(feed.url))
    }
}
