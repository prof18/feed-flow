//
//  FeedSourceDrawerItem.swift
//  FeedFlow
//
//  Created by AI Assistant on 19/03/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Nuke
import NukeUI
import SwiftUI

struct FeedSourceDrawerItem: View {
    let drawerItem: DrawerItem.DrawerFeedSource
    let onSelect: (DrawerItem.DrawerFeedSource) -> Void
    let onEdit: (FeedSource) -> Void
    let onPin: (FeedSource) -> Void
    let onChangeCategory: (FeedSource) -> Void
    let onDelete: (FeedSource) -> Void
    let onOpenWebsite: (String) -> Void
    let onMarkAllRead: (FeedSource) -> Void

    var body: some View {
        HStack {
            if drawerItem.feedSource.fetchFailed {
                makeFeedFailureIcon()
            }
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
    private func makeFeedFailureIcon() -> some View {
        Image(systemName: "exclamationmark.triangle.fill")
            .foregroundColor(Color(red: 1.0, green: 0.56, blue: 0.0)) // #FF8F00
            .font(.system(size: 16))
            .padding(.trailing, Spacing.small)
    }

    @ViewBuilder
    private func makeFeedSourceIcon(logoUrl: String?) -> some View {
        if let imageUrl = logoUrl {
            LazyImage(request: ImageRequest.resized(url: URL(string: imageUrl), size: CGSize(width: 24, height: 24))) { state in
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
                .font(.caption2)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(Color.secondary.opacity(0.15))
                .clipShape(Capsule())
        }
    }

    @ViewBuilder
    private func makeFeedSourceContextMenu(feedSource: FeedSource) -> some View {
        // Fetch failed warning (informational - keep at top)
        if feedSource.fetchFailed {
            Label(
                feedFlowStrings.feedFetchFailedTooltipShort,
                systemImage: "exclamationmark.triangle.fill"
            )
        }

        if drawerItem.unreadCount > 0 {
            Button {
                onMarkAllRead(feedSource)
            } label: {
                Label(feedFlowStrings.markAllReadButton, systemImage: "checkmark.circle")
            }
        }

        if let websiteUrl = feedSource.websiteUrlFallback() {
            Button {
                onOpenWebsite(websiteUrl)
            } label: {
                Label(feedFlowStrings.openWebsiteButton, systemImage: "globe")
            }
        }

        Button {
            onEdit(feedSource)
        } label: {
            Label(feedFlowStrings.editFeed, systemImage: "pencil")
        }

        Button {
            onChangeCategory(feedSource)
        } label: {
            Label(feedFlowStrings.changeCategory, systemImage: "folder")
        }

        Button {
            onPin(feedSource)
        } label: {
            Label(
                feedSource.isPinned ? feedFlowStrings.menuRemoveFromPinned : feedFlowStrings.menuAddToPinned,
                systemImage: feedSource.isPinned ? "pin.slash" : "pin"
            )
        }

        Divider()

        Button(role: .destructive) {
            onDelete(feedSource)
        } label: {
            Label(feedFlowStrings.deleteFeed, systemImage: "trash")
        }
    }
}
