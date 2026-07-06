//
//  FeedSourceListScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/01/24.
//  Copyright © 2024. All rights reserved.
//

import FeedFlowKit
import Nuke
import NukeUI
import SwiftUI

@MainActor
struct FeedSourceListScreenContent: View {
    @Environment(\.presentationMode)
    private var presentationMode
    @Environment(AppState.self)
    private var appState
    @Environment(BrowserSelector.self)
    private var browserSelector

    @State private var showAddFeed = false

    @State private var showDeleteAllFeedsDialog = false
    @State private var feedSourcesToDelete: [FeedSource]?

    @State private var showDeleteFeedDialog = false
    @State private var feedToDelete: FeedSource?

    @Binding var feedState: FeedSourceListState

    let deleteFeedSource: (FeedSource) -> Void
    let renameFeedSource: (FeedSource, String) -> Void
    let deleteAllFeedsInCategory: ([FeedSource]) -> Void
    let onReorderCategories: ([FeedSourceState]) -> Void
    let onReorderFeedSources: ([FeedSource]) -> Void

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
        .accessibilityIdentifier(FeedSourceListAccessibilityIdentifiers.screen)
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
        .navigationTitle(Text(feedFlowStrings.feedsTitle))
        .sheet(isPresented: $showAddFeed) {
            AddFeedScreen()
                .environment(appState)
                .environment(browserSelector)
                .toggleStyle(BlueToggleStyle())
        }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                NavigationLink(destination: AddFeedScreen()
                    .environment(appState)
                    .environment(browserSelector)) {
                        Image(systemName: "plus")
                            .foregroundStyle(.primary)
                }
            }
        }
    }

    @ViewBuilder private var emptyView: some View {
        VStack {
            Spacer()

            Text(feedFlowStrings.noFeedsFoundMessage)
                .font(.body)

            NavigationLink(destination: AddFeedScreen()
                .environment(appState)
                .environment(browserSelector)) {
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
                Section {
                    ForEach(feedState.feedSourcesWithoutCategory, id: \.self.id) { feedSource in
                        FeedSourceListItem(
                            feedSource: feedSource,
                            feedSourceTitle: feedSource.title,
                            deleteFeedSource: { source in
                                feedToDelete = source
                                showDeleteFeedDialog = true
                            },
                            renameFeedSource: renameFeedSource
                        )
                        .id(feedSource.id)
                        .listRowInsets(EdgeInsets())
                    }
                    .onMove(
                        perform: feedSourcesMoveAction(Array(feedState.feedSourcesWithoutCategory))
                    )
                } header: {
                    Text(feedFlowStrings.noCategory)
                        .contextMenu {
                            Button(role: .destructive) {
                                feedSourcesToDelete = Array(feedState.feedSourcesWithoutCategory)
                                showDeleteAllFeedsDialog = true
                            } label: {
                                Label(
                                    feedFlowStrings.deleteAllFeedsInCategory,
                                    systemImage: "trash"
                                )
                            }
                        }
                }
            }
        }
    }

    @ViewBuilder private var feedSourcesWithCategoryList: some View {
        List {
            ForEach(feedState.feedSourcesWithCategory, id: \.self.categoryId) { feedSourceState in
                feedSourceCategoryDisclosure(feedSourceState)
            }
            .onMove(
                perform: categoriesMoveAction(Array(feedState.feedSourcesWithCategory))
            )
        }
        .alert(
            feedFlowStrings.deleteAllFeedsConfirmationTitle,
            isPresented: $showDeleteAllFeedsDialog
        ) {
            Button(feedFlowStrings.deleteCategoryCloseButton, role: .cancel) {
                feedSourcesToDelete = nil
            }
            Button(feedFlowStrings.deleteAllFeedsInCategory, role: .destructive) {
                if let feedSources = feedSourcesToDelete {
                    deleteAllFeedsInCategory(feedSources)
                }
                feedSourcesToDelete = nil
            }
        } message: {
            Text(feedFlowStrings.deleteAllFeedsConfirmationMessage)
        }
        .alert(
            feedFlowStrings.deleteFeedConfirmationTitle,
            isPresented: $showDeleteFeedDialog
        ) {
            Button(feedFlowStrings.cancelButton, role: .cancel) {
                feedToDelete = nil
            }
            Button(feedFlowStrings.deleteFeedButton, role: .destructive) {
                if let feed = feedToDelete {
                    deleteFeedSource(feed)
                }
                feedToDelete = nil
            }
        } message: {
            Text(feedFlowStrings.deleteFeedConfirmationMessage)
        }
    }

    @ViewBuilder
    private func feedSourceCategoryDisclosure(_ feedSourceState: FeedSourceState) -> some View {
        DisclosureGroup(
            content: {
                categoryFeedSourceRows(feedSourceState)
            },
            label: {
                categoryDisclosureLabel(feedSourceState)
            }
        )
        .accessibilityIdentifier(
            FeedSourceListAccessibilityIdentifiers.category(feedSourceState.categoryId?.value)
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

    @ViewBuilder
    private func categoryFeedSourceRows(_ feedSourceState: FeedSourceState) -> some View {
        ForEach(feedSourceState.feedSources, id: \.self.id) { feedSource in
            feedSourceListItem(feedSource)
        }
        .onMove(
            perform: feedSourcesMoveAction(Array(feedSourceState.feedSources))
        )
    }

    private func feedSourceListItem(_ feedSource: FeedSource) -> some View {
        FeedSourceListItem(
            feedSource: feedSource,
            feedSourceTitle: feedSource.title,
            deleteFeedSource: { source in
                feedToDelete = source
                showDeleteFeedDialog = true
            },
            renameFeedSource: renameFeedSource
        )
        .id(feedSource.id)
    }

    private func categoryDisclosureLabel(_ feedSourceState: FeedSourceState) -> some View {
        Text(feedSourceState.categoryName ?? feedFlowStrings.noCategory)
            .font(.system(size: 16))
            .foregroundStyle(Color(UIColor.label))
            .padding(Spacing.regular)
            .contextMenu {
                Button(role: .destructive) {
                    feedSourcesToDelete = Array(feedSourceState.feedSources)
                    showDeleteAllFeedsDialog = true
                } label: {
                    Label(
                        feedFlowStrings.deleteAllFeedsInCategory,
                        systemImage: "trash"
                    )
                }
            }
    }

    private func feedSourcesMoveAction(_ feedSources: [FeedSource]) -> ((IndexSet, Int) -> Void)? {
        guard feedSources.count > 1 else { return nil }
        return { source, destination in
            var reorderedFeedSources = feedSources
            reorderedFeedSources.move(fromOffsets: source, toOffset: destination)
            onReorderFeedSources(reorderedFeedSources)
        }
    }

    private func categoriesMoveAction(_ categories: [FeedSourceState]) -> ((IndexSet, Int) -> Void)? {
        guard categories.count > 1 else { return nil }
        return { source, destination in
            var reorderedCategories = categories
            reorderedCategories.move(fromOffsets: source, toOffset: destination)
            onReorderCategories(reorderedCategories)
        }
    }
}

@MainActor
private struct FeedSourceListItem: View {
    @Environment(AppState.self)
    private var appState
    @Environment(BrowserSelector.self)
    private var browserSelector

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
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(feedFlowStrings.feedFetchFailedTooltipShort)
                    .accessibilityIdentifier(FeedSourceListAccessibilityIdentifiers.warning(feedSource.id))
            }

            if let imageUrl = feedSource.logoUrl {
                LazyImage(
                    request: ImageRequest.resized(
                        url: URL(string: imageUrl),
                        size: CGSize(width: 24, height: 24)
                    )
                ) { state in
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
                        .accessibilityIdentifier(FeedSourceListAccessibilityIdentifiers.renameInput(feedSource.id))
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
                        .accessibilityIdentifier(FeedSourceListAccessibilityIdentifiers.renameSave(feedSource.id))
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
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier(FeedSourceListAccessibilityIdentifiers.row(feedSource.id))
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
                if feedSource.fetchFailed {
                    Label(
                        feedFlowStrings.feedFetchFailedTooltipShort,
                        systemImage: "exclamationmark.triangle.fill"
                    )
                }

                NavigationLink(
                    destination: EditFeedScreen(
                        feedSource: feedSource
                    )
                    .environment(appState)
                    .environment(browserSelector)
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
