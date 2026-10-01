//
//  BookmarksSection.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 19/03/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct BookmarksSection: View {
    let bookmarks: [DrawerItem]
    let isSelected: Bool
    let isCompact: Bool
    let onSelect: () -> Void
    let onFeedFilterSelected: (FeedFilter) -> Void

    var body: some View {
        if bookmarks.first is DrawerItem.Bookmarks {
            Button {
                onSelect()
                onFeedFilterSelected(FeedFilter.Bookmarks())
            } label: {
                HStack {
                    Label(feedFlowStrings.drawerTitleBookmarks, systemImage: "bookmark.square")
                    Spacer()
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .tag(SidebarSelection.bookmarks)
            .accessibilityIdentifier(DrawerAccessibilityIdentifiers.bookmarks)
            .listRowBackground(sidebarSelectionBackground(isSelected: isSelected, isCompact: isCompact))
        }
    }
}
