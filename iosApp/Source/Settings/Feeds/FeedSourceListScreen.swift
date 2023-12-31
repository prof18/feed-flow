//
//  FeedListScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared
import KMPNativeCoroutinesAsync
import NukeUI

struct FeedSourceListScreen: View {

    @EnvironmentObject
    private var appState: AppState

    @StateObject
    private var feedSourceViewModel = KotlinDependencies.shared.getFeedSourceListViewModel()

    @State
    private var feedState: FeedSourceListState = FeedSourceListState(
        feedSourcesWithoutCategory: [],
        feedSourcesWithCategory: []
    )

    var body: some View {
        FeedSourceListContent(
            feedState: $feedState,
            deleteFeedSource: { feedSource in
                feedSourceViewModel.deleteFeedSource(feedSource: feedSource)
            }
        )
        .task {
            do {
                let stream = asyncSequence(for: feedSourceViewModel.feedSourcesStateFlow)
                for try await state in stream {
                    self.feedState = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
    }
}

private struct FeedSourceListContent: View {

    @Environment(\.presentationMode)
    private var presentationMode

    @State
    private var showAddFeed = false

    @Binding
    var feedState: FeedSourceListState

    let deleteFeedSource: (FeedSource) -> Void

    var body: some View {
        NavigationStack {
            VStack {
                if feedState.isEmpty() {
                    VStack {
                        Spacer()

                        Text(localizer.no_feeds_found_message.localized)
                            .font(.body)

                        NavigationLink(value: SheetPage.addFeed) {
                            Text(localizer.add_feed.localized)
                        }

                        Spacer()
                    }
                } else {

                    if feedState.feedSourcesWithoutCategory.count > 0 {
                        List {
                            ForEach(feedState.feedSourcesWithoutCategory, id: \.self.id) { feedSource in
                                FeedSourceListItem(feedSource: feedSource)
                            }
                        }
                        .padding(.top, -Spacing.medium)
                    }

                    List {
                        ForEach(feedState.feedSourcesWithCategory, id: \.self.categoryId) { feedSourceState in
                            DisclosureGroup(
                                content: {
                                    ForEach(feedSourceState.feedSources, id: \.self.id) { feedSource in
                                       FeedSourceListItem(feedSource: feedSource)
                                        .padding(.trailing, Spacing.small)
                                        .id(feedSource.id)
                                        .contextMenu {
                                            Button {
                                                deleteFeedSource(feedSource)
                                            } label: {
                                                Label(
                                                    localizer.delete_feed.localized,
                                                    systemImage: "trash"
                                                )
                                            }
                                        }
                                    }
                                },
                                label: {
                                    Text(feedSourceState.categoryName ?? localizer.no_category.localized)
                                        .font(.system(size: 16))
                                        .foregroundStyle(Color(UIColor.label))
                                        .bold()
                                        .padding(Spacing.regular)
                                }
                            )
                            .listRowInsets(
                                EdgeInsets(
                                    top: .zero,
                                    leading: .zero,
                                    bottom: .zero,
                                    trailing: Spacing.small)
                            )
                        }
                    }
                    .padding(.top, -Spacing.medium)
                    .sheet(isPresented: $showAddFeed) {
                        AddFeedScreen()
                    }
                }

                Spacer()
            }
            .scrollContentBackground(.hidden)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(
                        action: {
                            self.presentationMode.wrappedValue.dismiss()
                        },
                        label: {
                            Image(systemName: "xmark")
                        }
                    )
                    .padding(.leading, Spacing.small)

                }

                ToolbarItem(placement: .navigationBarLeading) {
                    Text(localizer.feeds_title.localized)
                        .font(.title2)
                        .padding(.vertical, Spacing.medium)
                }

                ToolbarItem(placement: .primaryAction) {
                    NavigationLink(value: SheetPage.addFeed) {
                        Image(systemName: "plus")
                    }
                    .padding(.trailing, Spacing.small)

                }
            }
            .navigationDestination(for: SheetPage.self) { page in
                switch page {
                case .addFeed:
                    AddFeedScreen()
                }
            }
        }
    }
}

struct FeedSourceListItem: View {

    let feedSource: FeedSource

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

struct FeedSourceListContent_Previews: PreviewProvider {
    static var previews: some View {
        FeedSourceListContent(
            feedState: .constant(
                FeedSourceListState(
                    feedSourcesWithoutCategory: [],
                    feedSourcesWithCategory: PreviewItemsKt.feedSourcesState
                )
            ),
            deleteFeedSource: { _ in }
        )
    }
}

struct FeedSourceListContentEmpty_Previews: PreviewProvider {
    static var previews: some View {
        FeedSourceListContent(
            feedState: .constant(
                FeedSourceListState(
                    feedSourcesWithoutCategory: [],
                    feedSourcesWithCategory: []
                )
            ),
            deleteFeedSource: { _ in }
        )
    }
}
