//
//  BookmarksSection.swift
//  FeedFlow
//
//  Created by AI Assistant on 19/03/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct BookmarksSection: View {
    let bookmarks: [DrawerItem]
    let onSelect: (DrawerItem) -> Void
    let onFeedFilterSelected: (FeedFilter) -> Void

    var body: some View {
        ForEach(bookmarks, id: \.self) { drawerItem in
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
                onSelect(drawerItem)
                onFeedFilterSelected(FeedFilter.Bookmarks())
            }
        }
    }
}
