//
//  SidebarDrawer.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared
import NukeUI

@MainActor
struct SidebarDrawer: View {

    @Binding var selectedDrawerItem: DrawerItem?

    let navDrawerState: NavDrawerState
    let onFeedFilterSelected: (FeedFilter) -> Void

    var body: some View {
        List(selection: $selectedDrawerItem) {
            timelineSection
            readSection
            categoriesSection
            feedSourcesWithoutCategorySection
            feedSourcesWithCategorySection
        }
        .modify {
            if #available(iOS 17.0, *) {
                $0.listStyle(.sidebar)
            } else {
                $0.listStyle(.insetGrouped)
            }
        }
    }

    @ViewBuilder
    private var timelineSection: some View {
        ForEach(navDrawerState.timeline, id: \.self) { drawerItem in
            HStack {
                Label(localizer.drawer_title_timeline.localized, systemImage: "newspaper")
                Spacer()
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
                Label(localizer.drawer_title_read.localized, systemImage: "text.badge.checkmark")
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
    private var categoriesSection: some View {
        if !navDrawerState.categories.isEmpty {
            Section(
                content: {
                    ForEach(navDrawerState.categories, id: \.self) { drawerItem in
                        if let categoryItem = drawerItem as? DrawerItem.DrawerCategory {
                            HStack {
                                Label(categoryItem.category.title, systemImage: "tag")
                                Spacer()
                            }
                            .contentShape(Rectangle())
                            .onTapGesture {
                                self.selectedDrawerItem = categoryItem
                                self.onFeedFilterSelected(FeedFilter.Category(feedCategory: categoryItem.category))
                            }
                        }
                    }
                }, header: {
                    Text(localizer.drawer_title_categories.localized)
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
                }, header: {
                    Text(localizer.drawer_title_feed_sources.localized)
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
                        let categoryWrapper = category as
                        DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper

                        let title = categoryWrapper.feedSourceCategory?.title ?? localizer.no_category.localized
                        makeCategoryDropdown(
                            drawerItems: navDrawerState.feedSourcesByCategory[categoryWrapper] ?? [],
                            title: title
                        )
                        .accessibilityIdentifier("\(TestingTag.shared.FEED_SOURCE_SELECTOR)_\(title)")

                    }
                },
                header: {
                    Text(localizer.drawer_title_feed_sources.localized)
                }
            )
        }
    }

    @ViewBuilder
    private func makeFeedSourceDrawerItem(drawerItem: DrawerItem.DrawerFeedSource) -> some View {
        HStack {
            if let imageUrl = drawerItem.feedSource.logoUrl {
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

            Text(drawerItem.feedSource.title)
                .lineLimit(2)
                .font(.system(size: 16))
                .padding(.bottom, 2)
                .padding(.leading, Spacing.small)

            Spacer()
        }

        .contentShape(Rectangle())
        .onTapGesture {
            self.selectedDrawerItem = drawerItem
            self.onFeedFilterSelected( FeedFilter.Source(feedSource: drawerItem.feedSource )
            )
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
}

#Preview {
    SidebarDrawer(selectedDrawerItem: .constant(nil), navDrawerState: PreviewItemsKt.navDrawerState, onFeedFilterSelected: { _ in })
}
