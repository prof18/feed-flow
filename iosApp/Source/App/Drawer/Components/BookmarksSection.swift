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
        if let bookmarksItem = bookmarks.first as? DrawerItem.Bookmarks {
            HStack {
                Label(feedFlowStrings.drawerTitleBookmarks, systemImage: "bookmark.square")
                Spacer()
                if bookmarksItem.unreadCount > 0 {
                    Text("\(bookmarksItem.unreadCount)")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.secondary.opacity(0.15))
                        .clipShape(Capsule())
                }
            }
            .contentShape(Rectangle())
            .onTapGesture {
                onSelect()
                onFeedFilterSelected(FeedFilter.Bookmarks())
            }
            .tag(SidebarSelection.bookmarks)
            .listRowBackground(sidebarSelectionBackground(isSelected: isSelected, isCompact: isCompact))
        }
    }
}
