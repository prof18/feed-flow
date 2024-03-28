//
//  FeedSourceListScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/01/24.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import shared
import NukeUI

@MainActor
struct FeedSourceListScreenContent: View {

    @Environment(\.presentationMode) private var presentationMode

    @State private var showAddFeed = false

    @Binding var feedState: FeedSourceListState

    let deleteFeedSource: (FeedSource) -> Void

    var body: some View {
        VStack {
            if feedState.isEmpty() {
                emptyView
            } else {
                feedSourcesWithoutCategoryList
                feedSourcesWithCategoryList
            }
            Spacer()
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(feedFlowStrings.feedsTitle))
        .sheet(isPresented: $showAddFeed) {
            AddFeedScreen()
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                NavigationLink(destination: AddFeedScreen()) {
                    Image(systemName: "plus")
                }
                .buttonStyle(.bordered)
                .padding(.trailing, Spacing.small)
            }
        }
    }

    @ViewBuilder
    private var emptyView: some View {
        VStack {
            Spacer()

            Text(feedFlowStrings.noFeedsFoundMessage)
                .font(.body)
                .accessibilityIdentifier(TestingTag.shared.NO_FEED_SOURCE_MESSAGE)

            NavigationLink(destination: AddFeedScreen()) {
                Text(feedFlowStrings.addFeed)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .padding(.top, Spacing.regular)
            .padding(.horizontal, Spacing.medium)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private var feedSourcesWithoutCategoryList: some View {
        if feedState.feedSourcesWithoutCategory.count > 0 {
            List {
                ForEach(feedState.feedSourcesWithoutCategory, id: \.self.id) { feedSource in
                    makeFeedSourceListItem(feedSource: feedSource)
                        .id(feedSource.id)
                        .contextMenu {
                            Button {
                                deleteFeedSource(feedSource)
                            } label: {
                                Label(feedFlowStrings.deleteFeed, systemImage: "trash")
                            }
                        }
                }
            }
        }
    }

    @ViewBuilder
    private var feedSourcesWithCategoryList: some View {
        List {
            ForEach(feedState.feedSourcesWithCategory, id: \.self.categoryId) { feedSourceState in
                DisclosureGroup(
                    content: {
                        ForEach(feedSourceState.feedSources, id: \.self.id) { feedSource in
                            makeFeedSourceListItem(feedSource: feedSource)
                                .padding(.trailing, Spacing.small)
                                .id(feedSource.id)
                                .contextMenu {
                                    Button {
                                        deleteFeedSource(feedSource)
                                    } label: {
                                        Label(feedFlowStrings.deleteFeed, systemImage: "trash")
                                    }
                                    .accessibilityIdentifier(TestingTag.shared.FEED_SOURCE_DELETE_BUTTON)
                                }
                        }
                    },
                    label: {
                        Text(feedSourceState.categoryName ?? feedFlowStrings.noCategory)
                            .font(.system(size: 16))
                            .foregroundStyle(Color(UIColor.label))
                            .padding(Spacing.regular)
                    }
                )
                .listRowInsets(
                    EdgeInsets(
                        top: .zero,
                        leading: .zero,
                        bottom: .zero,
                        trailing: Spacing.regular)
                )
                .accessibilityIdentifier(TestingTag.shared.FEED_SOURCE_SELECTOR)
            }
        }
    }

    @ViewBuilder
    private func makeFeedSourceListItem(feedSource: FeedSource) -> some View {
        HStack {
            if let imageUrl = feedSource.logoUrl {
                LazyImage(url: URL(string: imageUrl)) { state in
                    if let image = state.image {
                        image
                            .resizable()
                            .scaledToFill()
                            .frame(width: 24, height: 24)
                            .cornerRadius(16)
                            .clipped()
                    } else {
                        Image(systemName: "square.stack.3d.up")
                    }
                }
            } else {
                Image(systemName: "square.stack.3d.up")
            }

            VStack(alignment: .leading) {
                Text(feedSource.title)
                    .font(.system(size: 16))
                    .padding(.top, Spacing.regular)
                    .padding(.bottom, 2)

                Text(feedSource.url)
                    .font(.system(size: 12))
                    .padding(.top, 0)
                    .padding(.bottom, Spacing.regular)

            }
            .padding(.leading, Spacing.small)
        }
    }
}

#Preview {
    FeedSourceListScreenContent(
        feedState: .constant(
            FeedSourceListState(
                feedSourcesWithoutCategory: [],
                feedSourcesWithCategory: PreviewItemsKt.feedSourcesState
            )
        ),
        deleteFeedSource: { _ in }
    )
}

#Preview {
    FeedSourceListScreenContent(
        feedState: .constant(
            FeedSourceListState(
                feedSourcesWithoutCategory: [],
                feedSourcesWithCategory: []
            )
        ),
        deleteFeedSource: { _ in }
    )
}
