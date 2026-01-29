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
        switch count {
        case let positiveCount where positiveCount > 0:
            Text("\(positiveCount)")
                .font(.caption)
                .foregroundColor(.secondary)
                .padding(.horizontal, 8)
                .background(Color.secondary.opacity(0.2))
                .clipShape(Capsule())
        default:
            EmptyView()
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

        // 1. Delete (least frequent + destructive - keep far from accidental taps)
        Button {
            onDelete(feedSource)
        } label: {
            Label(feedFlowStrings.deleteFeed, systemImage: "trash")
        }

        // 2. Open website (rare - only for checking if feed/website is still alive)
        if let websiteUrl = feedSource.websiteUrlFallback() {
            Button {
                onOpenWebsite(websiteUrl)
            } label: {
                Label(feedFlowStrings.openWebsiteButton, systemImage: "globe")
            }
        }

        // 3. Edit feed (medium frequency - occasional settings adjustment)
        Button {
            onEdit(feedSource)
        } label: {
            Label(feedFlowStrings.editFeedSourceNameButton, systemImage: "pencil")
        }

        // 4. Change category (frequent - organizing feeds)
        Button {
            onChangeCategory(feedSource)
        } label: {
            Label(feedFlowStrings.changeCategory, systemImage: "folder")
        }

        // 5. Pin (most frequent - at bottom for easy thumb reach)
        Button {
            onPin(feedSource)
        } label: {
            Label(
                feedSource.isPinned ? feedFlowStrings.menuRemoveFromPinned : feedFlowStrings.menuAddToPinned,
                systemImage: feedSource.isPinned ? "pin.slash" : "pin"
            )
        }
    }
}
