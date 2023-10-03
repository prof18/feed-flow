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

struct FeedSourceListScreen: View {

    @EnvironmentObject var appState: AppState
    @StateObject var feedSourceViewModel = KotlinDependencies.shared.getFeedSourceListViewModel()

    @State var feedState: [FeedSourceState] = []

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
                self.appState.snackbarQueue.append(
                    SnackbarData(
                        title: localizer.generic_error_message.localized,
                        subtitle: nil,
                        showBanner: true
                    )
                )
            }
        }
    }
}

private struct FeedSourceListContent: View {

    @Environment(\.presentationMode) var presentationMode

    @State private var showAddFeed = false
    @Binding var feedState: [FeedSourceState]

    let deleteFeedSource: (FeedSource) -> Void

    var body: some View {
        NavigationStack {
            VStack {
                if feedState.isEmpty {
                    VStack {
                        Text(localizer.no_feeds_found_message.localized)
                            .font(.body)

                        NavigationLink(value: SheetPage.addFeed) {
                            Text(localizer.add_feed.localized)
                        }
                    }
                } else {
                    List {
                        ForEach(feedState, id: \.self.categoryId) { feedSourceState in

                            Section {
                                ForEach(feedSourceState.feedSources, id: \.self.id) { feedSource in
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
                                    .padding(.horizontal, Spacing.small)
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

                            } header: {
                                Text(feedSourceState.categoryName ?? localizer.no_category.localized)
                                    .font(.system(size: 16))
                                    .foregroundStyle(.black)
                                    .bold()
                                    .padding(.horizontal, Spacing.small)

                            }
                            .textCase(nil)
                            .listRowInsets(
                                EdgeInsets(
                                    top: .zero,
                                    leading: .zero,
                                    bottom: .zero,
                                    trailing: Spacing.small)
                            )
                        }
                    }

                    .listStyle(.sidebar)
                    .scrollContentBackground(.hidden)

                    .sheet(isPresented: $showAddFeed) {
                        AddFeedScreen()
                    }
                }
            }
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

struct FeedSourceListContent_Previews: PreviewProvider {
    static var previews: some View {
        FeedSourceListContent(
            feedState: .constant(PreviewItemsKt.feedSourcesState),
            deleteFeedSource: { _ in }
        )
    }
}

struct FeedSourceListContentEmpty_Previews: PreviewProvider {
    static var previews: some View {
        FeedSourceListContent(
            feedState: .constant([]),
            deleteFeedSource: { _ in }
        )
    }
}
