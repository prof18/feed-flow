//
//  TimelineSection.swift
//  FeedFlow
//
//  Created by AI Assistant on 19/03/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct TimelineSection: View {
    let timeline: [DrawerItem]
    let onSelect: (DrawerItem) -> Void
    let onFeedFilterSelected: (FeedFilter) -> Void

    var body: some View {
        ForEach(timeline, id: \.self) { drawerItem in
            HStack {
                Label(feedFlowStrings.drawerTitleTimeline, systemImage: "newspaper")
                Spacer()
                if let timelineItem = drawerItem as? DrawerItem.Timeline,
                   timelineItem.unreadCount > 0 {
                    Text("\(timelineItem.unreadCount)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 8)
                        .background(Color.secondary.opacity(0.2))
                        .clipShape(Capsule())
                }
            }
            .contentShape(Rectangle())
            .onTapGesture {
                onSelect(drawerItem)
                onFeedFilterSelected(FeedFilter.Timeline())
            }
        }
    }
}
