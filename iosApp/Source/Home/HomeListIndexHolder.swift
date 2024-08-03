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

    let homeViewModel: HomeViewModel?

    var isLoading: Bool = false
    var lastAppearedIndex = 0

    private var lastReadIndex = 0
    private var timer: Timer?
    private var isClearing = false

    init(homeViewModel: HomeViewModel) {
        self.homeViewModel = homeViewModel
    }

    init(fakeHomeViewModel: Bool) {
        // The variable is just to remind not to declare empty constructors by mistake
        self.homeViewModel = nil
    }

    func getLastReadIndex() -> Int {
        return lastReadIndex
    }

    func refresh() {
        self.timer?.invalidate()
        self.isLoading = true
        self.lastReadIndex = 0
    }

    func clear() {
        self.lastReadIndex = 0
        timer?.invalidate()
        isClearing = true

        _ = Timer.scheduledTimer(withTimeInterval: 2, repeats: false) { [weak self] _ in
            guard let self else { return }
            self.isClearing = false
            self.lastReadIndex = 0
        }
    }

    func updateReadIndex(index: Int) {
        if !isClearing && !self.isLoading && index < lastAppearedIndex && index > lastReadIndex {
            lastReadIndex = index
            timer?.invalidate()
            timer = Timer.scheduledTimer(withTimeInterval: 2, repeats: false) { [weak self] _ in
                guard let self else { return }
                self.homeViewModel?.markAsReadOnScroll(lastVisibleIndex: Int32(self.getLastReadIndex()))
            }
        }
    }
}
