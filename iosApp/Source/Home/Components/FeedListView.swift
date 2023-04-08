//
//  FeedListView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 30/03/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import SwiftUI
import Nuke
import shared
import NukeUI
import OrderedCollections

struct FeedListView: View {
    
    @Environment(\.openURL) var openURL
    @EnvironmentObject var indexHolder: HomeListIndexHolder
    
    let feedState: [FeedItem]
    let onRefresh: () -> Void

    var body: some View {
        List {
            ForEach(feedState, id: \.self.id) { feedItem in
                
                FeedItemView(
                    feedItem: feedItem
                )
                .id(feedItem.id)
                .contentShape(Rectangle())
                .onTapGesture {
                    openURL(URL(string: feedItem.url)!)
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
            onRefresh()
        }
    }
}

struct FeedItemView : View {
    
    let feedItem: FeedItem
    
    var body: some View {
        VStack(alignment: .leading) {
            
            Text(feedItem.feedSource.title)
                .font(.system(size: 12))
                .padding(.top, Spacing.small)
            
            HStack {
                VStack(alignment: .leading) {
                    Text(feedItem.title)
                        .font(.system(size: 16))
                        .bold()
                    
                    if let subtitle = feedItem.subtitle {
                        Text(subtitle)
                            .lineLimit(3)
                            .font(.system(size: 14))
                            .padding(.top, Spacing.xxsmall)
                    }
                }
                
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
                            ProgressView()
                                .scaledToFill()
                                .frame(width: 100)
                        }
                    }
                    .padding(.leading, Spacing.regular)
                } else {
                    Spacer()
                }
                
            }
            .padding(.vertical, Spacing.small)
            
            Text(feedItem.dateString)
                .font(.system(size: 12))
                .padding(.bottom, Spacing.small)
            
        }
    }
}


struct FeedListView_Previews: PreviewProvider {
    
    static let feedState: [FeedItem] = [
        FeedItem(
            id : 0,
            url : "https://www.ilpost.it/2023/02/11/scheumorfismo/",
            title : "Le forme e gli oggetti che ci portiamo dietro nonostante il progresso",
            subtitle : nil,
            content : nil,
            imageUrl : "http://static1.anpoimages.com/wordpr",
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
    
    static var previews: some View {
        FeedListView(
            feedState: feedState,
            onRefresh: {}
        )
    }
}
