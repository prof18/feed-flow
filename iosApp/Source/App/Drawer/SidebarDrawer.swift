//
//  SidebarDrawer.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import shared

struct SidebarDrawer: View {

    @Binding
    var selectedDrawerItem: DrawerItem?

    let navDrawerState: NavDrawerState
    let onFeedFilterSelected: (FeedFilter) -> Void

    var body: some View {
        List(selection: $selectedDrawerItem) {
            ForEach(navDrawerState.timeline, id: \.self) { drawerItem in
                DrawerTimelineItem()
                    .onTapGesture {
                        self.selectedDrawerItem = drawerItem
                        self.onFeedFilterSelected(FeedFilter.Timeline())
                    }
            }

            Section(
                content: {
                    ForEach(navDrawerState.categories, id: \.self) { drawerItem in
                        if let categoryItem = drawerItem as? DrawerItem.DrawerCategory {
                            DrawerCategoryItem(
                                drawerItem: categoryItem
                            ).onTapGesture {
                                self.selectedDrawerItem = categoryItem
                                self.onFeedFilterSelected(
                                    FeedFilter.Category(feedCategory: categoryItem.category)
                                )
                            }
                        }
                    }
                }, header: {
                    Text(localizer.drawer_title_categories.localized)
                }
            )

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

                        DisclosureGroup(
                            content: {
                                ForEach(
                                    navDrawerState.feedSourcesByCategory[categoryWrapper] ?? [],
                                    id: \.self
                                ) { drawerItem in
                                    if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
                                        HStack {

                                            Image(systemName: "square.stack.3d.up")

                                            Text(drawerFeedSource.feedSource.title)
                                                .lineLimit(2)
                                                .font(.system(size: 16))
                                                .padding(.bottom, 2)
                                                .padding(.leading, Spacing.small)

                                            Spacer()
                                        }
                                        .listRowInsets(
                                            EdgeInsets(
                                                top: Spacing.small,
                                                leading: .zero,
                                                bottom: Spacing.small,
                                                trailing: Spacing.small)
                                        )
                                        .contentShape(Rectangle())
                                        .onTapGesture {
                                            self.selectedDrawerItem = drawerItem
                                            self.onFeedFilterSelected(
                                                FeedFilter.Source(
                                                    feedSource: drawerFeedSource.feedSource
                                                )
                                            )
                                        }
                                    } else {
                                        EmptyView()
                                    }
                                }
                            },
                            label: {
                                Text(categoryWrapper.feedSourceCategory?.title ?? localizer.no_category.localized)
                            }
                        )
                    }
                },
                header: {
                    Text(localizer.drawer_title_feed_sources.localized)
                }
            )
        }
        .listStyle(.sidebar)
    }
}

private struct DrawerTimelineItem: View {
    var body: some View {
        HStack {
            Label(
                localizer.drawer_title_timeline.localized,
                systemImage: "newspaper"
            )

            Spacer()
        }
        .contentShape(Rectangle())
    }
}

private struct DrawerCategoryItem: View {

    let drawerItem: DrawerItem.DrawerCategory

    var body: some View {
        HStack {
            Label(
                drawerItem.category.title,
                systemImage: "tag"
            )

            Spacer()
        }
        .contentShape(Rectangle())
    }
}
