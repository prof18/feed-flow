//
//  SidebarDrawer.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import NukeUI
import SwiftUI

@MainActor
// swiftlint:disable type_body_length
struct SidebarDrawer: View {

  @Environment(AppState.self) private var appState
  @Binding var selectedDrawerItem: DrawerItem?

  let navDrawerState: NavDrawerState
  let onFeedFilterSelected: (FeedFilter) -> Void
  let onMarkAllReadClick: () -> Void
  let onDeleteOldFeedClick: () -> Void
  let onForceRefreshClick: () -> Void
  let deleteAllFeeds: () -> Void
  let onShowSettingsClick: () -> Void
  let onAddFeedClick: () -> Void
  let onEditFeedClick: (FeedSource) -> Void
  let onDeleteFeedClick: (FeedSource) -> Void

  var body: some View {
    List(selection: $selectedDrawerItem) {
      timelineSection
      readSection
      bookmarksSection
      categoriesSection
      feedSourcesWithoutCategorySection
      feedSourcesWithCategorySection
      if isOnVisionOSDevice() {
        visionOsSection
      }
    }
    .listStyle(.sidebar)
  }

  @ViewBuilder
  private var timelineSection: some View {
    ForEach(navDrawerState.timeline, id: \.self) { drawerItem in
      HStack {
        Label(feedFlowStrings.drawerTitleTimeline, systemImage: "newspaper")
        Spacer()
        if let timelineItem = drawerItem as? DrawerItem.Timeline,
           timelineItem.unreadCount > 0 {
          Text("\(timelineItem.unreadCount)")
            .font(.caption)
            .foregroundColor(.secondary)
            .padding(.horizontal, 8)
            .background(Color.secondary.opacity(0.2))
            .clipShape(Capsule())
        }
      }
      .contentShape(Rectangle())
      .onTapGesture {
        self.selectedDrawerItem = drawerItem
        self.onFeedFilterSelected(FeedFilter.Timeline())
      }
    }
  }

  @ViewBuilder
  private var readSection: some View {
    ForEach(navDrawerState.read, id: \.self) { drawerItem in
      HStack {
        Label(feedFlowStrings.drawerTitleRead, systemImage: "text.badge.checkmark")
        Spacer()
      }
      .contentShape(Rectangle())
      .onTapGesture {
        self.selectedDrawerItem = drawerItem
        self.onFeedFilterSelected(FeedFilter.Read())
      }
    }
  }

  @ViewBuilder
  private var bookmarksSection: some View {
    ForEach(navDrawerState.bookmarks, id: \.self) { drawerItem in
      HStack {
        Label(feedFlowStrings.drawerTitleBookmarks, systemImage: "bookmark.square")
        Spacer()
        if let bookmarksItem = drawerItem as? DrawerItem.Bookmarks,
           bookmarksItem.unreadCount > 0 {
          Text("\(bookmarksItem.unreadCount)")
            .font(.caption)
            .foregroundColor(.secondary)
            .padding(.horizontal, 8)
            .background(Color.secondary.opacity(0.2))
            .clipShape(Capsule())
        }
      }
      .contentShape(Rectangle())
      .onTapGesture {
        self.selectedDrawerItem = drawerItem
        self.onFeedFilterSelected(FeedFilter.Bookmarks())
      }
    }
  }

  @ViewBuilder
  private var categoriesSection: some View {
    if !navDrawerState.categories.isEmpty {
      Section(
        content: {
          ForEach(navDrawerState.categories, id: \.self) { drawerItem in
            if let categoryItem = drawerItem as? DrawerItem.DrawerCategory {
              HStack {
                Label(categoryItem.category.title, systemImage: "tag")
                Spacer()
                if categoryItem.unreadCount > 0 {
                  Text("\(categoryItem.unreadCount)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 8)
                    .background(Color.secondary.opacity(0.2))
                    .clipShape(Capsule())
                }
              }
              .contentShape(Rectangle())
              .onTapGesture {
                self.selectedDrawerItem = categoryItem
                self.onFeedFilterSelected(FeedFilter.Category(feedCategory: categoryItem.category))
              }
            }
          }
        },
        header: {
          Text(feedFlowStrings.drawerTitleCategories)
        }
      )
    }
  }

  @ViewBuilder
  private var feedSourcesWithoutCategorySection: some View {
    if !navDrawerState.feedSourcesWithoutCategory.isEmpty {
      Section(
        content: {
          ForEach(navDrawerState.feedSourcesWithoutCategory, id: \.self) { drawerItem in
            if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
              makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
            }
          }
        },
        header: {
          makeAddFeedButton(title: feedFlowStrings.drawerTitleFeedSources)
        }
      )
    }
  }

  @ViewBuilder
  private var feedSourcesWithCategorySection: some View {
    if !navDrawerState.feedSourcesByCategory.isEmpty {
      Section(
        content: {
          ForEach(
            navDrawerState.feedSourcesByCategory.keys.sorted {
              $0.feedSourceCategory?.title ?? "" < $1.feedSourceCategory?.title ?? ""
            },
            id: \.self
          ) { category in
            let categoryWrapper =
              category as DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper

            let title = categoryWrapper.feedSourceCategory?.title ?? feedFlowStrings.noCategory
            makeCategoryDropdown(
              drawerItems: navDrawerState.feedSourcesByCategory[categoryWrapper] ?? [],
              title: title
            ).accessibilityIdentifier("\(TestingTag.shared.FEED_SOURCE_SELECTOR)_\(title)")
          }
        },
        header: {
          makeAddFeedButton(title: feedFlowStrings.drawerTitleFeedSources)
        }
      )
    }
  }

  @ViewBuilder
  private func makeAddFeedButton(title: String) -> some View {
    HStack {
      Text(title)
      Spacer()
      Button(
        action: {
          onAddFeedClick()
        },
        label: {
          Image(systemName: "plus.app")
        }
      )
    }
    .if(appState.sizeClass == .compact) { view in
      view.listRowInsets(
        EdgeInsets(
          top: Spacing.small,
          leading: Spacing.small,
          bottom: Spacing.small,
          trailing: Spacing.small
        )
      )
    }.if(appState.sizeClass == .regular) { view in
      view.listRowInsets(
        EdgeInsets(
          top: Spacing.small,
          leading: Spacing.small,
          bottom: Spacing.small,
          trailing: -Spacing.xsmall
        )
      )
    }
  }

  @ViewBuilder
  private func makeFeedSourceDrawerItem(drawerItem: DrawerItem.DrawerFeedSource) -> some View {
    HStack {
      makeFeedSourceIcon(logoUrl: drawerItem.feedSource.logoUrl)

      makeFeedSourceTitle(title: drawerItem.feedSource.title)

      Spacer()

      makeUnreadCountBadge(count: drawerItem.unreadCount)
    }
    .contentShape(Rectangle())
    .onTapGesture {
      self.selectedDrawerItem = drawerItem
      self.onFeedFilterSelected(FeedFilter.Source(feedSource: drawerItem.feedSource))
    }
    .contextMenu {
      makeFeedSourceContextMenu(feedSource: drawerItem.feedSource)
    }
  }

  @ViewBuilder
  private func makeFeedSourceIcon(logoUrl: String?) -> some View {
    if let imageUrl = logoUrl {
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
  }

  @ViewBuilder
  private func makeFeedSourceTitle(title: String) -> some View {
    Text(title)
      .lineLimit(2)
      .font(.system(size: 16))
      .padding(.bottom, 2)
      .padding(.leading, Spacing.small)
  }

  @ViewBuilder
  private func makeUnreadCountBadge(count: Int64) -> some View {
    if count > 0 {
      Text("\(count)")
        .font(.caption)
        .foregroundColor(.secondary)
        .padding(.horizontal, 8)
        .background(Color.secondary.opacity(0.2))
        .clipShape(Capsule())
    }
  }

  @ViewBuilder
  private func makeFeedSourceContextMenu(feedSource: FeedSource) -> some View {
    Button {
      onEditFeedClick(feedSource)
    } label: {
      Label(feedFlowStrings.editFeedSourceNameButton, systemImage: "pencil")
    }

    Button {
      onDeleteFeedClick(feedSource)
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

  @ViewBuilder
  private func makeCategoryDropdown(drawerItems: [DrawerItem], title: String) -> some View {
    DisclosureGroup(
      content: {
        ForEach(drawerItems, id: \.self) { drawerItem in
          if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
            makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
              .listRowInsets(
                EdgeInsets(
                  top: Spacing.small,
                  leading: .zero,
                  bottom: Spacing.small,
                  trailing: Spacing.small)
              )
          } else {
            EmptyView()
          }
        }
      },
      label: {
        Text(title)
      }
    )
  }

  @ViewBuilder
  private var visionOsSection: some View {
    Spacer()

    Divider()

    Button {
      onMarkAllReadClick()
    } label: {
      Label(feedFlowStrings.markAllReadButton, systemImage: "checkmark")
    }

    Button {
      onDeleteOldFeedClick()
    } label: {
      Label(feedFlowStrings.clearOldArticlesButton, systemImage: "trash")
    }

    Button {
      onForceRefreshClick()
    } label: {
      Label(feedFlowStrings.forceFeedRefresh, systemImage: "arrow.clockwise")
    }

    #if DEBUG
      Button {
        deleteAllFeeds()
      } label: {
        Label("Delete Database", systemImage: "trash")
      }
    #endif

    Button {
      onShowSettingsClick()
    } label: {
      Label(feedFlowStrings.settingsButton, systemImage: "gear")
    }
  }
}
// swiftlint:enable type_body_length

#Preview {
  SidebarDrawer(
    selectedDrawerItem: .constant(nil),
    navDrawerState: navDrawerState,
    onFeedFilterSelected: { _ in },
    onMarkAllReadClick: {},
    onDeleteOldFeedClick: {},
    onForceRefreshClick: {},
    deleteAllFeeds: {},
    onShowSettingsClick: {},
    onAddFeedClick: {},
    onEditFeedClick: { _ in },
    onDeleteFeedClick: { _ in }
  )
}
