//
//  HomeListIndexHolder.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 02/04/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import OrderedCollections

class HomeListIndexHolder: ObservableObject {
 
    @Published var unreadCount: Int = 0
    
    var lastReadItemIndex: Int = 0
    private var visibleFeedItemsIds: OrderedSet<Int> = []
    private var originalUnreadCountFromData: Int = 0
    
    func updateUnreadCount(unreadCountFromData: Int) {
        self.originalUnreadCountFromData = unreadCountFromData
        let computedUnread = unreadCountFromData - lastReadItemIndex
        if unreadCountFromData > computedUnread {
            self.unreadCount = computedUnread
        } else {
            self.unreadCount = unreadCountFromData
        }
    }
    
    func addIndex(index: Int) {
        self.visibleFeedItemsIds.append(index)
        updateIndex()
    }
    
    func removeIndex(index: Int) {
        self.visibleFeedItemsIds.remove(index)
        updateIndex()
    }
    
    private func updateIndex() {
        let sortedSet = visibleFeedItemsIds.sorted()
        let index = sortedSet.first ?? 0
        
        if index > lastReadItemIndex {
            self.lastReadItemIndex = index - 1
            self.unreadCount = originalUnreadCountFromData - self.lastReadItemIndex
        }
    }
}
