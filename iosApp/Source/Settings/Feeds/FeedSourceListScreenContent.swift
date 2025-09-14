//
//  FeedSourceListScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import NukeUI
import SwiftUI

@MainActor
struct FeedSourceListScreenContent: View {
    @Environment(\.presentationMode)
    private var presentationMode

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

    @ViewBuilder private var emptyView: some View {
        VStack {
            Spacer()

            Text(feedFlowStrings.noFeedsFoundMessage)
                .font(.body)

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

    @ViewBuilder private var feedSourcesWithoutCategoryList: some View {
        if !feedState.feedSourcesWithoutCategory.isEmpty {
            List {
                ForEach(feedState.feedSourcesWithoutCategory, id: \.self.id) { feedSource in
                    FeedSourceListItem(
                        feedSource: feedSource,
                        feedSourceTitle: feedSource.title,
                        deleteFeedSource: deleteFeedSource,
                        renameFeedSource: renameFeedSource
                    )
                    .id(feedSource.id)
                    .listRowInsets(EdgeInsets())
                }
            }
        }
    }

    @ViewBuilder private var feedSourcesWithCategoryList: some View {
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
                        trailing: Spacing.regular
                    )
                )
            }
        }
    }
}

@MainActor
private struct FeedSourceListItem: View {
    @Environment(AppState.self)
    private var appState

    @State var feedSource: FeedSource
    @State var feedSourceTitle: String

    @State var isRenameEnabled = false

    let deleteFeedSource: (FeedSource) -> Void
    let renameFeedSource: (FeedSource, String) -> Void

    @FocusState var isTextFieldFocused: Bool?

    var body: some View {
        HStack {
            if feedSource.fetchFailed {
                makeFeedFailureIcon()
            }

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
                .padding(.leading, feedSource.fetchFailed ? Spacing.xsmall : Spacing.regular)
            } else {
                Image(systemName: "square.stack.3d.up")
                    .padding(.leading, feedSource.fetchFailed ? Spacing.xsmall : Spacing.regular)
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
                
                Label(
                    feedFlowStrings.feedFetchFailedTooltipShort,
                    systemImage: "exclamationmark.triangle.fill"
                )
                
                NavigationLink(
                    destination: EditFeedScreen(
                        feedSource: feedSource
                    )
                ) {
                    Label(feedFlowStrings.editFeedSourceNameButton, systemImage: "pencil")
                }

                Button {
                    isRenameEnabled = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        isTextFieldFocused = true
                    }
                } label: {
                    Label(
                        feedFlowStrings.renameFeedSourceNameButton,
                        systemImage: "rectangle.and.pencil.and.ellipsis"
                    )
                }

                Button {
                    deleteFeedSource(feedSource)
                } label: {
                    Label(feedFlowStrings.deleteFeed, systemImage: "trash")
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

    @ViewBuilder
    private func makeFeedFailureIcon() -> some View {
        Image(systemName: "exclamationmark.triangle.fill")
            .foregroundColor(Color(red: 1.0, green: 0.56, blue: 0.0)) // #FF8F00
            .font(.system(size: 16))
            .padding(.leading, Spacing.regular)
            .padding(.trailing, Spacing.small)
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
