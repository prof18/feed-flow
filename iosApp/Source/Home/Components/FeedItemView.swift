//
//  FeedListView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import Nuke
import shared
import NukeUI
import OrderedCollections

struct FeedItemView: View {

    let feedItem: FeedItem

    var body: some View {
        VStack(alignment: .leading) {

            Text(feedItem.feedSource.title)
                .font(.system(size: 12))
                .padding(.top, Spacing.small)

            HStack {
                TitleAndSubtitleCell(
                    feedItem: feedItem
                )
                .frame(maxHeight: .infinity)

                FeedItemImage(
                    feedItem: feedItem
                )
            }

            if let dateString = feedItem.dateString {
                Text(dateString)
                    .font(.system(size: 12))
                    .padding(.bottom, Spacing.small)
            }

        }
    }
}

struct TitleAndSubtitleCell: View {

    let feedItem: FeedItem

    func getPaddingTop(feedItem: FeedItem) -> CGFloat {
        if feedItem.title != nil {
            return Spacing.xxsmall
        } else {
            return CGFloat(0)
        }
    }

    var body: some View {
        VStack(alignment: .leading) {
            if let title = feedItem.title {
                Text(title)
                    .font(.system(size: 16))
                    .bold()
            }

            if let subtitle = feedItem.subtitle {
                Text(subtitle)
                    .lineLimit(3)
                    .font(.system(size: 14))
                    .padding(.top, getPaddingTop(feedItem: feedItem))
            }
        }

    }
}

struct FeedItemImage: View {

    let feedItem: FeedItem

    var body: some View {
        if let imageUrl = feedItem.imageUrl {
            Spacer()
            LazyImage(url: URL(string: imageUrl)) { state in
                if let image = state.image {
                    image
                        .resizable()
                        .scaledToFill()
                        .frame(width: 100)
                        .cornerRadius(16)
                        .clipped()
                } else if state.error != nil {
                    EmptyView()
                } else {
                    Color(.secondarySystemBackground)
                        .frame(width: 100)
                }
            }
            .padding(.leading, Spacing.regular)
        } else {
            Spacer()
        }
    }
}

struct FeedItemView_Previews: PreviewProvider {
    static var previews: some View {
        FeedItemView(
            feedItem: PreviewItemsKt.feedItemsForPreview[2]
        )
    }
}

struct FeedItemViewIpad_Previews: PreviewProvider {
    static var previews: some View {
        FeedItemView(
            feedItem: PreviewItemsKt.feedItemsForPreview[2]
        )
        .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch) (4th generation)"))
    }
}
