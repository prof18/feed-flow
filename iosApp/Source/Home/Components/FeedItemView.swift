//
//  FeedItemView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Nuke
import NukeUI
import SwiftUI

@MainActor
struct FeedItemView: View {
    let feedItem: FeedItem
    let index: Int
    let feedFontSizes: FeedFontSizes
    var feedLayout: FeedLayout = .list
    var isGridCell = false
    var heroImageAspectRatio: CGFloat = 16.0 / 9.0
    var currentFeedFilter: FeedFilter = .Timeline()
    var feedItemDisplaySettings = FeedItemDisplaySettings(
        isHideUnreadDotEnabled: false,
        isHideFeedSourceEnabled: false,
        descriptionLineLimit: .three
    )

    var body: some View {
        if normalizedFeedLayout == .bigImage {
            imageCardView
        } else {
            VStack(alignment: .leading) {
                let showUnreadDot = !feedItem.isRead && !feedItemDisplaySettings.isHideUnreadDotEnabled
                let showFeedSource = !feedItemDisplaySettings.isHideFeedSourceEnabled
                let showBookmark = feedItem.isBookmarked

                if showUnreadDot || showFeedSource || showBookmark {
                    HStack {
                        if showUnreadDot {
                            Circle()
                                .fill(Color.accentColor)
                                .frame(width: 10, height: 10)
                                .padding(.top, Spacing.small)
                        }

                        if showFeedSource {
                            Text(feedItem.feedSource.title)
                                .font(.system(size: CGFloat(feedFontSizes.feedMetaFontSize)))
                                .padding(.top, Spacing.small)
                                .opacity(readTextOpacity)
                        }

                        Spacer()

                        if showBookmark {
                            Image(systemName: "bookmark.fill")
                                .font(.system(size: 12))
                                .foregroundColor(Color.accentColor)
                                .padding(.top, Spacing.small)
                        }
                    }
                }

                HStack {
                    titleAndSubtitleCell.frame(maxHeight: .infinity)
                    feedItemImage
                }

                if let dateString = feedItem.dateString {
                    Text(dateString)
                        .font(.system(size: CGFloat(feedFontSizes.feedMetaFontSize)))
                        .padding(.bottom, Spacing.small)
                        .opacity(readTextOpacity)
                }
            }
            .padding(.horizontal, Spacing.regular)
            .padding(.vertical, Spacing.small)
            .background(normalizedFeedLayout == .card ? Color(.systemBackground) : Color.clear)
            .clipShape(
                RoundedRectangle(
                    cornerRadius: normalizedFeedLayout == .card ? 16 : 0,
                    style: .continuous
                )
            )
            .overlay {
                if normalizedFeedLayout == .card {
                    cardOutline
                }
            }
            .shadow(
                color: normalizedFeedLayout == .card ? cardShadowColor : .clear,
                radius: normalizedFeedLayout == .card ? 3 : 0,
                x: 0,
                y: normalizedFeedLayout == .card ? 1 : 0
            )
            .padding(.horizontal, normalizedFeedLayout == .card ? Spacing.small : 0)
            .padding(.vertical, normalizedFeedLayout == .card ? Spacing.small : 0)
        }
    }

    private var imageCardView: some View {
        VStack(alignment: .leading, spacing: 0) {
            heroImage

            VStack(alignment: .leading, spacing: 0) {
                imageCardSourceRow

                if let title = feedItem.title {
                    Text(title)
                        .font(.system(size: CGFloat(feedFontSizes.feedTitleFontSize)))
                        .bold()
                        .lineLimit(isGridCell ? 2 : 3)
                        .foregroundStyle(.primary)
                        .opacity(readTextOpacity)
                        .padding(.top, hasImageCardSourceRow ? Spacing.small : 0)
                }

                if let subtitle = feedItem.subtitle {
                    let lineLimit = feedItemDisplaySettings.descriptionLineLimit == .noLimit
                        ? nil
                        : Int(feedItemDisplaySettings.descriptionLineLimit.lines)
                    Text(subtitle)
                        .lineLimit(lineLimit)
                        .font(.system(size: CGFloat(feedFontSizes.feedDescFontSize)))
                        .foregroundStyle(.secondary)
                        .opacity(readTextOpacity)
                        .padding(.top, feedItem.title == nil ? 0 : Spacing.small)
                }

                if let dateString = feedItem.dateString {
                    Text(dateString)
                        .font(.system(size: CGFloat(feedFontSizes.feedMetaFontSize)))
                        .foregroundStyle(.primary)
                        .opacity(readTextOpacity)
                        .padding(.top, feedItem.title == nil && feedItem.subtitle == nil ? 0 : Spacing.small)
                }
            }
            .padding(.horizontal, Spacing.regular)
            .padding(.vertical, Spacing.regular)
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(cardOutline)
        .shadow(color: cardShadowColor, radius: 3, x: 0, y: 1)
        .padding(.horizontal, normalizedFeedLayout == .bigImage && !isGridCell ? Spacing.regular : 0)
        .padding(.vertical, normalizedFeedLayout == .bigImage && !isGridCell ? Spacing.small : 0)
    }

    private var normalizedFeedLayout: FeedLayout {
        feedLayout == .grid ? .bigImage : feedLayout
    }

    private var cardOutline: some View {
        RoundedRectangle(cornerRadius: 16, style: .continuous)
            .stroke(Color(.separator).opacity(0.65), lineWidth: 1)
    }

    private var cardShadowColor: Color {
        Color.black.opacity(0.08)
    }

    private var hasImageCardSourceRow: Bool {
        let showUnreadDot = !feedItem.isRead && !feedItemDisplaySettings.isHideUnreadDotEnabled
        let showFeedSource = !feedItemDisplaySettings.isHideFeedSourceEnabled
        return showUnreadDot || showFeedSource || feedItem.isBookmarked
    }

    @ViewBuilder private var heroImage: some View {
        if let imageUrl = feedItem.imageUrl {
            Color(.secondarySystemBackground)
                .aspectRatio(heroImageAspectRatio, contentMode: .fit)
                .overlay {
                    LazyImage(
                        request: ImageRequest.resized(
                            url: URL(string: imageUrl),
                            size: CGSize(width: 600, height: 338)
                        )
                    ) { state in
                        if let image = state.image {
                            image
                                .resizable()
                                .scaledToFill()
                        } else {
                            Color(.secondarySystemBackground)
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .clipped()
                }
                .clipped()
                .opacity(readImageOpacity)
                .accessibilityIdentifier(FeedItemAccessibilityIdentifiers.image(feedItem.id))
        }
    }

    @ViewBuilder private var imageCardSourceRow: some View {
        let showUnreadDot = !feedItem.isRead && !feedItemDisplaySettings.isHideUnreadDotEnabled
        let showFeedSource = !feedItemDisplaySettings.isHideFeedSourceEnabled
        let showBookmark = feedItem.isBookmarked

        if showUnreadDot || showFeedSource || showBookmark {
            HStack(spacing: Spacing.small) {
                if showFeedSource {
                    feedSourceLogo

                    Text(feedItem.feedSource.title)
                        .font(.system(size: CGFloat(feedFontSizes.feedMetaFontSize), weight: .medium))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .opacity(readTextOpacity)
                }

                Spacer()

                if showBookmark {
                    Image(systemName: "bookmark.fill")
                        .font(.system(size: 14))
                        .foregroundStyle(Color.accentColor)
                }

                if showUnreadDot {
                    Circle()
                        .fill(Color.accentColor)
                        .frame(width: 9, height: 9)
                }
            }
        }
    }

    @ViewBuilder private var feedSourceLogo: some View {
        if let logoUrl = feedItem.feedSource.logoUrl {
            LazyImage(
                request: ImageRequest.resized(
                    url: URL(string: logoUrl),
                    size: CGSize(width: 24, height: 24)
                )
            ) { state in
                if let image = state.image {
                    image
                        .resizable()
                        .scaledToFill()
                } else {
                    Image(systemName: "square.stack.3d.up")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.accentColor)
                }
            }
            .frame(width: 24, height: 24)
            .background(Color.accentColor.opacity(0.15))
            .clipShape(RoundedRectangle(cornerRadius: 7, style: .continuous))
        } else {
            Image(systemName: "square.stack.3d.up")
                .font(.system(size: 13))
                .foregroundStyle(Color.accentColor)
                .frame(width: 24, height: 24)
                .background(Color.accentColor.opacity(0.15))
                .clipShape(RoundedRectangle(cornerRadius: 7, style: .continuous))
        }
    }

    private var readTextOpacity: Double {
        feedItem.isRead &&
            !(currentFeedFilter is FeedFilter.Read) &&
            !(currentFeedFilter is FeedFilter.Bookmarks)
            ? 0.6 : 1.0
    }

    private var readImageOpacity: Double {
        feedItem.isRead &&
            !(currentFeedFilter is FeedFilter.Read) &&
            !(currentFeedFilter is FeedFilter.Bookmarks)
            ? 0.76 : 1.0
    }

    @ViewBuilder private var titleAndSubtitleCell: some View {
        VStack(alignment: .leading) {
            if let title = feedItem.title {
                Text(title)
                    .font(.system(size: CGFloat(feedFontSizes.feedTitleFontSize)))
                    .bold()
                    .opacity(readTextOpacity)
            }

            if let subtitle = feedItem.subtitle {
                let lineLimit = feedItemDisplaySettings.descriptionLineLimit == .noLimit
                    ? nil
                    : Int(feedItemDisplaySettings.descriptionLineLimit.lines)
                Text(subtitle)
                    .lineLimit(lineLimit)
                    .font(.system(size: CGFloat(feedFontSizes.feedDescFontSize)))
                    .padding(.top, getPaddingTop(feedItem: feedItem))
                    .opacity(readTextOpacity)
            }
        }
    }

    @ViewBuilder private var feedItemImage: some View {
        if let imageUrl = feedItem.imageUrl {
            Spacer()
            LazyImage(
                request: ImageRequest.resized(
                    url: URL(string: imageUrl),
                    size: CGSize(width: 100, height: 100)
                )
            ) { state in
                if let image = state.image {
                    image
                        .resizable()
                        .scaledToFill()
                        .frame(width: 100, height: 100)
                        .cornerRadius(16)
                        .clipped()
                } else if state.error != nil {
                    EmptyView()
                } else {
                    Color(.secondarySystemBackground)
                        .frame(width: 100, height: 100)
                }
            }
            .padding(.leading, Spacing.regular)
            .opacity(readImageOpacity)
            .accessibilityIdentifier(FeedItemAccessibilityIdentifiers.image(feedItem.id))
        } else {
            Spacer()
        }
    }

    private func getPaddingTop(feedItem: FeedItem) -> CGFloat {
        if feedItem.title != nil {
            return Spacing.xxsmall
        } else {
            return CGFloat(0)
        }
    }
}
