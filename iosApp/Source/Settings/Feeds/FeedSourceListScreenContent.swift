//
//  FeedSourceListScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/01/24.
//  Copyright © 2024. All rights reserved.
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
    let renameFeedSource: (FeedSource, String) -> Void

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
                    FeedSourceListItem(
                        feedSource: feedSource,
                        feedSourceTitle: feedSource.title,
                        deleteFeedSource: deleteFeedSource,
                        renameFeedSource: renameFeedSource
                    )
                    .id(feedSource.id )
                    .listRowInsets(EdgeInsets())
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
                            FeedSourceListItem(
                                feedSource: feedSource,
                                feedSourceTitle: feedSource.title,
                                deleteFeedSource: deleteFeedSource,
                                renameFeedSource: renameFeedSource
                            )
                            .id(feedSource.id)
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
}

@MainActor
private struct FeedSourceListItem: View {
    @State var feedSource: FeedSource
    @State var feedSourceTitle: String

    @State var isRenameEnabled: Bool = false

    let deleteFeedSource: (FeedSource) -> Void
    let renameFeedSource: (FeedSource, String) -> Void

    @FocusState var isTextFieldFocused: Bool?

    var body: some View {
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
                .padding(.leading, Spacing.regular)
            } else {
                Image(systemName: "square.stack.3d.up")
                    .padding(.leading, Spacing.regular)
            }

            VStack(alignment: .leading) {
                HStack(alignment: .center) {
                    TextField("", text: $feedSourceTitle)
                        .focused($isTextFieldFocused, equals: true)
                        .disabled(!isRenameEnabled)
                        .font(.system(size: 16))
                        .padding(.top, Spacing.regular)
                        .padding(.bottom, 2)

                    Spacer()

                    if isRenameEnabled {
                        Button {
                            renameFeedSource(feedSource, feedSourceTitle)
                            isRenameEnabled = false
                            isTextFieldFocused = false
                        } label: {
                            Image(systemName: "checkmark.circle.fill")
                                .tint(.green)
                        }
                        .padding(.top, Spacing.regular)
                    }
                }

                Text(feedSource.url)
                    .font(.system(size: 12))
                    .padding(.top, 0)
                    .padding(.bottom, Spacing.regular)

            }
            .padding(.leading, Spacing.small)
        }
        .padding(.trailing, Spacing.small)
        .contentShape(Rectangle())
        .hoverEffect()
        .listRowInsets(
            EdgeInsets(
                top: .zero,
                leading: .zero,
                bottom: .zero,
                trailing: .zero
            )
        )
        .if(!isRenameEnabled) { view in
            view.contextMenu {
                Button {
                    deleteFeedSource(feedSource)
                } label: {
                    Label(feedFlowStrings.deleteFeed, systemImage: "trash")
                }
                .accessibilityIdentifier(TestingTag.shared.FEED_SOURCE_DELETE_BUTTON)

                Button {
                    isRenameEnabled = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        isTextFieldFocused = true
                    }
                } label: {
                    Label(feedFlowStrings.renameFeedSourceNameButton, systemImage: "pencil")
                }

                if isOnVisionOSDevice() {
                    Button {
                        // No-op so it will close itslef
                    } label: {
                        Label(feedFlowStrings.closeMenuButton, systemImage: "xmark")
                    }
                }
            }
        }
    }

}

#Preview {
    FeedSourceListScreenContent(
        feedState: .constant(
            FeedSourceListState(
                feedSourcesWithoutCategory: [],
                feedSourcesWithCategory: feedSourcesState
            )
        ),
        deleteFeedSource: { _ in },
        renameFeedSource: { _, _ in }
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
        deleteFeedSource: { _ in },
        renameFeedSource: { _, _ in }
    )
}
