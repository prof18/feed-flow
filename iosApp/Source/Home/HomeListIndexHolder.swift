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
    
    private var lastReadIndex = 0
    private var originalUnreadFeedCount = 0
    var isLoading: Bool = false
    
    func setUnreadCount(count: Int) {
        self.originalUnreadFeedCount = count
        self.unreadCount = count
    }
    
    func updateReadIndex(index: Int) {
        if !isLoading {
            lastReadIndex = index
            self.unreadCount = originalUnreadFeedCount - index
        }
    }
    
    func getLastReadIndex() -> Int {
        return lastReadIndex
    }
    
    func refresh() {
        self.isLoading = true
        self.unreadCount = originalUnreadFeedCount
    }
}
