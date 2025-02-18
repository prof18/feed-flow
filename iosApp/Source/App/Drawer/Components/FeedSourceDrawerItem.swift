//
//  FeedSourceDrawerItem.swift
//  FeedFlow
//
//  Created by AI Assistant on 19/03/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import NukeUI
import SwiftUI

struct FeedSourceDrawerItem: View {
    let drawerItem: DrawerItem.DrawerFeedSource
    let onSelect: (DrawerItem.DrawerFeedSource) -> Void
    let onEdit: (FeedSource) -> Void
    let onPin: (FeedSource) -> Void
    let onDelete: (FeedSource) -> Void

    var body: some View {
        HStack {
            makeFeedSourceIcon(logoUrl: drawerItem.feedSource.logoUrl)
            makeFeedSourceTitle(title: drawerItem.feedSource.title)
            Spacer()
            makeUnreadCountBadge(count: drawerItem.unreadCount)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            onSelect(drawerItem)
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
            onEdit(feedSource)
        } label: {
            Label(feedFlowStrings.editFeedSourceNameButton, systemImage: "pencil")
        }

        Button {
            onPin(feedSource)
        } label: {
            Label(
                feedSource.isPinned ? feedFlowStrings.menuRemoveFromPinned : feedFlowStrings.menuAddToPinned,
                systemImage: feedSource.isPinned ? "pin.slash" : "pin"
            )
        }

        Button {
            onDelete(feedSource)
        } label: {
            Label(feedFlowStrings.deleteFeed, systemImage: "trash")
        }

        if isOnVisionOSDevice() {
            Button {
                // No-op so it will close itself
            } label: {
                Label(feedFlowStrings.closeMenuButton, systemImage: "xmark")
            }
        }
    }
}
