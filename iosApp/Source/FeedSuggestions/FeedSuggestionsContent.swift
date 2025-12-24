//
//  FeedSuggestionsContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct FeedSuggestionsContent: View {
    @Environment(\.colorScheme) var colorScheme
    
    // TODO: do not inject the view model, but data and callbacks
    let viewModel: FeedSuggestionsViewModel

    @State private var suggestedCategories: [SuggestedFeedCategory] = []
    @State private var selectedFeeds: [String] = []
    @State private var expandedCategories: [String] = []
    @State private var isLoading = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // Header
                    VStack(spacing: 12) {
                        Text(feedFlowStrings.feedSuggestionsTitle)
                            .font(.largeTitle)
                            .fontWeight(.bold)

                        Text(feedFlowStrings.feedSuggestionsDescription)
                            .font(.body)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .padding(.top, 24)

                    // Categories
                    ForEach(suggestedCategories, id: \.id) { category in
                        CategoryCard(
                            category: category,
                            isExpanded: expandedCategories.contains(category.id),
                            selectedFeeds: selectedFeeds,
                            onToggle: {
                                viewModel.toggleCategoryExpansion(categoryId: category.id)
                            },
                            onFeedToggle: { feedUrl in
                                viewModel.toggleFeedSelection(feedUrl: feedUrl)
                            }
                        )
                    }

                    // Footer with action button
                    if !selectedFeeds.isEmpty {
                        VStack(spacing: 16) {
                            HStack {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                                Text(feedFlowStrings.feedSuggestionsSelectedCount("\(selectedFeeds.count)"))
                                    .fontWeight(.medium)
                            }
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(Color.green.opacity(0.1))
                            .cornerRadius(12)

                            Button(action: {
                                viewModel.completeOnboarding()
                                // TODO: navigate back when the adding is complete
                            }) {
                                HStack {
                                    if isLoading {
                                        ProgressView()
                                            .tint(.white)
                                    } else {
                                        Text(feedFlowStrings.feedSuggestionsAddButton)
                                            .fontWeight(.semibold)
                                        Image(systemName: "arrow.right")
                                    }
                                }
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.accentColor)
                                .foregroundColor(.white)
                                .cornerRadius(12)
                            }
                            .disabled(isLoading)
                        }
                        .padding(.horizontal)
                        .padding(.bottom, 24)
                    }
                }
                .padding()
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        // TODO: implement back action
                    }) {
                        Text(feedFlowStrings.actionDone)
                            .fontWeight(.semibold)
                    }
                }
            }
        }
        .task {
            for await categories in viewModel.suggestedCategoriesState {
                self.suggestedCategories = categories.compactMap { $0 }
            }
        }
        .task {
            for await feeds in viewModel.selectedFeedsState {
                self.selectedFeeds = feeds.compactMap { $0 }
            }
        }
        .task {
            for await expanded in viewModel.expandedCategoriesState {
                self.expandedCategories = expanded.compactMap { $0 }
            }
        }
        .task {
            for await loading in viewModel.isLoadingState {
                if let loadingValue = loading as? Bool {
                    self.isLoading = loadingValue
                }
            }
        }
    }
}

private struct CategoryCard: View {
    let category: SuggestedFeedCategory
    let isExpanded: Bool
    let selectedFeeds: [String]
    let onToggle: () -> Void
    let onFeedToggle: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button(action: onToggle) {
                HStack {
                    Text(category.icon)
                        .font(.title2)

                    Text(category.name)
                        .font(.headline)

                    Spacer()

                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(Color.secondary.opacity(0.1))
                .cornerRadius(12)
            }
            .buttonStyle(PlainButtonStyle())

            if isExpanded {
                VStack(spacing: 8) {
                    ForEach(category.feeds, id: \.url) { feed in
                        FeedChip(
                            feed: feed,
                            isSelected: selectedFeeds.contains(feed.url),
                            onToggle: {
                                onFeedToggle(feed.url)
                            }
                        )
                    }
                }
                .padding(.horizontal)
            }
        }
        .padding(.horizontal)
    }
}

private struct FeedChip: View {
    let feed: SuggestedFeed
    let isSelected: Bool
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(feed.name)
                        .font(.subheadline)
                        .fontWeight(.medium)

                    Text(feed.description_)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }

                Spacer()

                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(isSelected ? .accentColor : .secondary)
            }
            .padding()
            .background(isSelected ? Color.accentColor.opacity(0.1) : Color.secondary.opacity(0.05))
            .cornerRadius(8)
        }
        .buttonStyle(PlainButtonStyle())
    }
}
