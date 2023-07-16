//
//  HomeListIndexHolder.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 02/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import OrderedCollections
import Combine
import shared

class HomeListIndexHolder: ObservableObject {
    
    var isLoading: Bool = false
    private var lastReadIndex = 0
    private var timer: Timer?
    
    func getLastReadIndex() -> Int {
        return lastReadIndex
    }
    
    func refresh() {
        self.isLoading = true
        self.lastReadIndex = 0
    }
    
    func updateReadIndex(index: Int) {
        if !self.isLoading && index > lastReadIndex {
            lastReadIndex = index
            timer?.invalidate()
            timer = Timer.scheduledTimer(withTimeInterval: 2, repeats: false) { [self] _ in
                KotlinDependencies.shared.getHomeViewModel()
                    .updateReadStatus(lastVisibleIndex: Int32(getLastReadIndex()))
            }
        }
    }
}
