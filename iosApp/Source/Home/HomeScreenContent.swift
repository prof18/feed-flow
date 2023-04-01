//
//  HomeScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import shared

struct HomeScreenContent: View {
    @Binding var loadingState: FeedUpdateStatus?
    @Binding var feedState: [FeedItem]
    @Binding var errorState: UIErrorState?
    @Binding var showLoading: Bool
    
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
        } else {
            VStack(alignment: .center) {
                
                if let feedCount = loadingState?.refreshedFeedCount, let totalFeedCount = loadingState?.totalFeedCount {
                
                    if showLoading {
                        
                        let feedRefreshCounter = "\(feedCount)/\(totalFeedCount)"
                        
                        Text("Loading feeds \(feedRefreshCounter)")
                            .font(.body)
                    }
                }
                
                FeedListView(
                    feedState: feedState,
                    onRefresh: onReloadClick
                )
            }
        }
    }
    
}

struct HomeScreenFeed_Previews: PreviewProvider {
    static var previews: some View {
        
        let feedState: [FeedItem] = [
            FeedItem(
                id : 0,
                url : "https://www.ilpost.it/2023/02/11/scheumorfismo/",
                title : "Le forme e gli oggetti che ci portiamo dietro nonostante il progresso",
                subtitle : nil,
                content : nil,
                imageUrl : "",
                feedSource : FeedSource(id : 1, url : "https://www.ilpost.it", title : "Il Post"),
                isRead : false,
                pubDateMillis : 1676107668000,
                dateString : "16:22",
                commentsUrl : nil
            ),
            
            FeedItem(
                id : 1,
                url : "https://www.androidpolice.com/google-pixel-7-pro-vs-pixel-6-pro/",
                title : "Google Pixel 7 Pro vs. Pixel 6 Pro: Should you upgrade?",
                subtitle : "The Pixel 7 Pro might not be a dramatic overhaul the way the 6 Pro was, but small refinements elevate the experience",
                content : nil,
                imageUrl : "https://static1.anpoimages.com/wordpress/wp-content/uploads/2022/10/Pixel-7-Pro-vs-Pixel-6-Pro-comparison.jpg",
                feedSource : FeedSource(id : 2, url : "", title : "Android Police"),
                isRead : true,
                pubDateMillis : 1675890077000,
                dateString : "12/02 - 16:22",
                commentsUrl : nil
            ),
            
            FeedItem(
                id : 2,
                url : "https://9to5linux.com/obs-studio-29-0-1-is-out-to-fix-linux-crash-on-wayland-x11-capture-issue",
                title : "OBS Studio 29.0.1 Is Out to Fix Linux Crash on Wayland, X11 Capture Issue",
                subtitle : "<p>OBS Studio 29.0.1 open-source and free software for live streaming and screen recording is now available for download with several bug fixes.</p> <p>The post <a rel:\"nofollow\" href:\"https://9to5linux.com/obs-studio-29-0-1-is-out-to-fix-linux-crash-on-wayland-x11-capture-issue\">OBS Studio 29.0.1 Is Out to Fix Linux Crash on Wayland, X11 Capture Issue</a> appeared first on <a rel:\"nofollow\" href:\"https://9to5linux.com\">9to5Linux</a> - do not reproduce this article without permission. This RSS feed is intended for readers, not scrapers.</p>",
                content : nil,
                imageUrl : nil,
                feedSource : FeedSource(id : 3, url : "https://9to5linux.com", title : "9to5 Linux"),
                isRead : false,
                pubDateMillis : 0,
                dateString : "12/12 - 9:22",
                commentsUrl : nil
            )
        ]
        
        HomeScreenContent(
            loadingState: .constant(FinishedFeedUpdateStatus(refreshedFeedCount: 1, totalFeedCount: 1)),
            feedState: .constant(feedState),
            errorState: .constant(nil),
            showLoading: .constant(false),
            onReloadClick: {},
            onAddFeedClick: {}
        )
    }
}


struct HomeScreenNoFeed_Previews: PreviewProvider {
    
    static let noFeedSourceStatus = NoFeedSourcesStatus()
    
    static var previews: some View {
        
        HomeScreenContent(
            loadingState: .constant(noFeedSourceStatus),
            feedState: .constant([]),
            errorState: .constant(nil),
            showLoading: .constant(false),
            onReloadClick: {},
            onAddFeedClick: {}
        )
    }
}

struct HomeScreenEmptyFeed_Previews: PreviewProvider {
    static var previews: some View {
        
        HomeScreenContent(
            loadingState: .constant(FinishedFeedUpdateStatus(refreshedFeedCount: 1, totalFeedCount: 1)),
            feedState: .constant([]),
            errorState: .constant(nil),
            showLoading: .constant(false),
            onReloadClick: {},
            onAddFeedClick: {}
        )
    }
}
