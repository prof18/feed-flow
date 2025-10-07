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
        if feedSource.fetchFailed {
            Label(
                feedFlowStrings.feedFetchFailedTooltipShort,
                systemImage: "exclamationmark.triangle.fill"
            )
        }
        
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

        if let websiteUrl = feedSource.websiteUrl {
            Button {
                onOpenWebsite(websiteUrl)
            } label: {
                Label(feedFlowStrings.openWebsiteButton, systemImage: "globe")
            }
        }

        Button {
            onDelete(feedSource)
        } label: {
            Label(feedFlowStrings.deleteFeed, systemImage: "trash")
        }
    }
}
