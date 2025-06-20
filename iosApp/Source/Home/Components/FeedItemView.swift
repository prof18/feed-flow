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

    var body: some View {
        VStack(alignment: .leading) {
            HStack {
                if !feedItem.isRead {
                    Circle()
                        .fill(Color.accentColor)
                        .frame(width: 10, height: 10)
                        .padding(.top, Spacing.small)
                }

                Text(feedItem.feedSource.title)
                    .font(.system(size: CGFloat(feedFontSizes.feedMetaFontSize)))
                    .padding(.top, Spacing.small)

                Spacer()

                if feedItem.isBookmarked {
                    Image(systemName: "bookmark.fill")
                        .font(.system(size: 12))
                        .foregroundColor(Color.accentColor)
                        .padding(.top, Spacing.small)
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
            }
        }
        .padding(.horizontal, Spacing.regular)
        .padding(.vertical, Spacing.small)
        .if(feedLayout == .card) { view in
            view
                .background(Color(.secondarySystemBackground))
                .cornerRadius(16)
                .padding(.horizontal, Spacing.small)
                .padding(.vertical, Spacing.small)
        }
    }

    @ViewBuilder
    private var titleAndSubtitleCell: some View {
        VStack(alignment: .leading) {
            if let title = feedItem.title {
                Text(title)
                    .font(.system(size: CGFloat(feedFontSizes.feedTitleFontSize)))
                    .bold()
            }

            if let subtitle = feedItem.subtitle {
                Text(subtitle)
                    .lineLimit(3)
                    .font(.system(size: CGFloat(feedFontSizes.feedDescFontSize)))
                    .padding(.top, getPaddingTop(feedItem: feedItem))
            }
        }
    }

    @ViewBuilder
    private var feedItemImage: some View {
        if let imageUrl = feedItem.imageUrl {
            Spacer()
            LazyImage(url: URL(string: imageUrl)) { state in
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

#Preview {
    VStack {
        Text("List Layout")
            .font(.headline)
            .padding()

        FeedItemView(
            feedItem: feedItemsForPreview[2],
            index: 0,
            feedFontSizes: defaultFeedFontSizes(),
            feedLayout: .list
        )

        Text("Card Layout")
            .font(.headline)
            .padding()

        FeedItemView(
            feedItem: feedItemsForPreview[2],
            index: 0,
            feedFontSizes: defaultFeedFontSizes(),
            feedLayout: .card
        )
    }
}
