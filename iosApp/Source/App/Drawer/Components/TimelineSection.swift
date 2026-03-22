//
//  TimelineSection.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 19/03/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct TimelineSection: View {
    let timeline: [DrawerItem]
    let isSelected: Bool
    let isCompact: Bool
    let onSelect: () -> Void
    let onFeedFilterSelected: (FeedFilter) -> Void

    var body: some View {
        if let timelineItem = timeline.first as? DrawerItem.Timeline {
            HStack {
                Label(feedFlowStrings.drawerTitleTimeline, systemImage: "newspaper")
                Spacer()
                if timelineItem.unreadCount > 0 {
                    Text("\(timelineItem.unreadCount)")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.secondary.opacity(0.15))
                        .clipShape(Capsule())
                }
            }
            .contentShape(Rectangle())
            .onTapGesture {
                onSelect()
                onFeedFilterSelected(FeedFilter.Timeline())
            }
            .tag(SidebarSelection.timeline)
            .listRowBackground(sidebarSelectionBackground(isSelected: isSelected, isCompact: isCompact))
        }
    }
}
