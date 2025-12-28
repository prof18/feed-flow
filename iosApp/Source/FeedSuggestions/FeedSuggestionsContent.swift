//
//  FeedSuggestionsContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import NukeUI
import SwiftUI

@MainActor
struct FeedSuggestionsContent: View {
    let viewModel: FeedSuggestionsViewModel

    @State private var suggestedCategories: [SuggestedFeedCategory] = []
    @State private var selectedCategoryId: String = ""
    @State private var feedStatesMap: [String: FeedAddState] = [:]
    @State private var isLoading: Bool = true

    @Environment(\.dismiss) private var dismiss

    private var selectedCategory: SuggestedFeedCategory? {
        suggestedCategories.first(where: { $0.id == selectedCategoryId }) ?? suggestedCategories.first
    }

    private var filteredFeeds: [SuggestedFeed] {
        selectedCategory?.feeds ?? []
    }

    private func getFeedState(_ feedUrl: String) -> FeedAddState {
        feedStatesMap[feedUrl] ?? .notAdded
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
                    .padding(.vertical, Spacing.regular)

                    Divider()

                    ScrollView {
                        LazyVStack(spacing: 0) {
                            ForEach(filteredFeeds, id: \.url) { feed in
                                SuggestedFeedRow(
                                    feed: feed,
                                    feedState: getFeedState(feed.url),
                                    onAddFeed: {
                                        if let categoryName = selectedCategory?.name {
                                            viewModel.addFeed(feed: feed, categoryName: categoryName)
                                        }
                                    }
                                )

                                if feed.url != filteredFeeds.last?.url {
                                    Divider()
                                        .padding(.leading, 64)
                                }
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle(feedFlowStrings.feedSuggestionsTitle)
        .navigationBarTitleDisplayMode(.inline)
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
            for await statesMap in viewModel.feedStatesMapState {
                self.feedStatesMap = statesMap
            }
        }
        .task {
            for await loading in viewModel.isLoadingState {
                self.isLoading = loading as? Bool ?? false
            }
        }
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

    @Environment(\.colorScheme) private var colorScheme

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
                    .strokeBorder(isSelected ? Color.accentColor : Color(UIColor.separator), lineWidth: isSelected ? 2 : 1)
            )
        }
        .buttonStyle(.plain)
        .foregroundColor(isSelected ? .accentColor : .primary)
    }
}

private struct SuggestedFeedRow: View {
    let feed: SuggestedFeed
    let feedState: FeedAddState
    let onAddFeed: () -> Void

    var body: some View {
        HStack(spacing: Spacing.regular) {
            if let logoUrl = feed.logoUrl, let url = URL(string: logoUrl) {
                LazyImage(url: url) { state in
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

            VStack(alignment: .leading, spacing: 4) {
                Text(feed.name)
                    .font(.headline)
                    .fontWeight(.bold)
                    .lineLimit(1)

                Text(feed.description_)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
            }

            Spacer()

            AddButton(feedState: feedState, onTap: onAddFeed)
        }
        .padding(.horizontal, Spacing.regular)
        .padding(.vertical, Spacing.small)
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
    let feedState: FeedAddState
    let onTap: () -> Void

    var body: some View {
        switch feedState {
        case .added:
            Button(action: {}) {
                HStack(spacing: 4) {
                    Image(systemName: "checkmark")
                        .font(.system(size: 12, weight: .semibold))
                    Text("Added")
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color(UIColor.secondarySystemFill))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .strokeBorder(Color(UIColor.separator), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            .foregroundColor(.secondary)
            .disabled(true)

        case .adding:
            Button(action: {}) {
                ProgressView()
                    .controlSize(.small)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(Color.clear)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .strokeBorder(Color(UIColor.separator), lineWidth: 1)
                    )
            }
            .buttonStyle(.plain)
            .disabled(true)

        case .notAdded:
            Button(action: onTap) {
                HStack(spacing: 4) {
                    Image(systemName: "plus")
                        .font(.system(size: 12, weight: .semibold))
                    Text("Add")
                        .font(.subheadline)
                        .fontWeight(.medium)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.clear)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .strokeBorder(Color(UIColor.separator), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
            .foregroundColor(.primary)
        }
    }
}
