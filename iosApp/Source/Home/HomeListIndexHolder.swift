//
//  HomeListIndexHolder.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 02/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation

@Observable
class HomeListIndexHolder {
    @ObservationIgnored let homeViewModel: HomeViewModel?

    private var visibleItems: [String: VisibleFeedItem] = [:]
    private var updatesBlocked = false

    init(homeViewModel: HomeViewModel) {
        self.homeViewModel = homeViewModel
    }

    init(fakeHomeViewModel _: Bool) {
        // The variable is just to remind not to declare empty constructors by mistake
        homeViewModel = nil
    }

    func getLastReadIndex() -> Int {
        return visibleItems.values.map { Int($0.index) }.min() ?? 0
    }

    func pauseUpdates() {
        updatesBlocked = true
        visibleItems.removeAll()
    }

    func resumeUpdates() {
        updatesBlocked = false
    }

    func refresh() {
        visibleItems.removeAll()
    }

    func clear() {
        visibleItems.removeAll()
    }

    func itemAppeared(id: String, index: Int, isRead: Bool) {
        guard !updatesBlocked else { return }
        visibleItems[id] = VisibleFeedItem(id: id, index: Int32(index), isRead: isRead)
        sendVisibleSnapshot()
    }

    func itemDisappeared(id: String) {
        guard !updatesBlocked else { return }
        visibleItems.removeValue(forKey: id)
        guard !visibleItems.isEmpty else { return }
        sendVisibleSnapshot()
    }

    private func sendVisibleSnapshot() {
        let snapshot = visibleItems.values.sorted { $0.index < $1.index }
        homeViewModel?.onVisibleFeedItemsChanged(visibleItems: snapshot)
    }
}
