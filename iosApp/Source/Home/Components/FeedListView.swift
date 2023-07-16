//
//  HomeScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import shared
import OrderedCollections

struct FeedListView: View {
    @Environment(\.openURL) var openURL
    @EnvironmentObject var indexHolder: HomeListIndexHolder
    @EnvironmentObject var browserSelector: BrowserSelector
    
    var loadingState: FeedUpdateStatus?
    var feedState: [FeedItem]
    var showLoading: Bool
    
    let onReloadClick: () -> Void
    let onAddFeedClick: () -> Void
    
    var body: some View {
        if loadingState is NoFeedSourcesStatus {
            NoFeedsSourceView(
                onAddFeedClick: onAddFeedClick
            )
        } else if loadingState?.isLoading() == false && feedState.isEmpty {
            EmptyFeedView(
                onReloadClick: onReloadClick
            )
        } else if feedState.isEmpty {
            ProgressView()
        } else {
            VStack(alignment: .center) {
                
                if let feedCount = loadingState?.refreshedFeedCount, let totalFeedCount = loadingState?.totalFeedCount {
                
                    if showLoading {
                        
                        let feedRefreshCounter = "\(feedCount)/\(totalFeedCount)"
                    
                        let loadingFeedString = LocalizationUtils.shared.formatString(
                            resource: MR.strings().loading_feed_message,
                            args: [feedRefreshCounter]
                        )
                        Text(loadingFeedString)
                            .font(.body)
                    }
                }
                
                List {
                    ForEach(feedState, id: \.self.id) { feedItem in
                        
                        FeedItemView(
                            feedItem: feedItem
                        )
                        .id(feedItem.id)
                        .contentShape(Rectangle())
                        .onTapGesture {
                            openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: feedItem.url))
                        }
                        .onDisappear {
                            if let index = feedState.firstIndex(of: feedItem) {
                                self.indexHolder.updateReadIndex(index: index)
                            }
                        }
                    }
                }
                .listStyle(PlainListStyle())
                .refreshable {
                    onReloadClick()
                }
            }
        }
    }
    
}

struct FeedListView_Previews: PreviewProvider {
    static var previews: some View {
        FeedListView(
            loadingState: FinishedFeedUpdateStatus(),
            feedState: PreviewItemsKt.feedItemsForPreview,
            showLoading: false,
            onReloadClick: {},
            onAddFeedClick: {}
        )
    }
}

struct FeedListViewIpad_Previews: PreviewProvider {
    static var previews: some View {
        FeedListView(
            loadingState: FinishedFeedUpdateStatus(),
            feedState: PreviewItemsKt.feedItemsForPreview,
            showLoading: false,
            onReloadClick: {},
            onAddFeedClick: {}
        )
        .previewDevice(PreviewDevice(rawValue: "iPad Pro (11-inch) (4th generation)"))
    }
}


struct FeedListViewNoFeed_Previews: PreviewProvider {
    
    static let noFeedSourceStatus = NoFeedSourcesStatus()
    
    static var previews: some View {
        
        FeedListView(
            loadingState: noFeedSourceStatus,
            feedState: [],
            showLoading: false,
            onReloadClick: {},
            onAddFeedClick: {}
        )
    }
}

struct FeedListViewEmptyFeed_Previews: PreviewProvider {
    static var previews: some View {
        
        FeedListView(
            loadingState: FinishedFeedUpdateStatus(),
            feedState: [],
            showLoading: false,
            onReloadClick: {},
            onAddFeedClick: {}
        )
    }
}
