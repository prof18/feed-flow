//
//  CompactView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import Foundation
import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct CompactView: View {

    @EnvironmentObject
    var appState: AppState

    @Binding
    var selectedDrawerItem: DrawerItem?

    @StateObject
    private var indexHolder = HomeListIndexHolder()

    @State
    var navDrawerState: NavDrawerState = NavDrawerState(
        timeline: [],
        categories: [],
        feedSourcesWithoutCategory: [],
        feedSourcesByCategory: [:]
    )

    @State
    var scrollUpTrigger: Bool = false

    let homeViewModel: HomeViewModel

    var body: some View {
        NavigationStack(path: $appState.path) {
            SidebarDrawer(
                selectedDrawerItem: $selectedDrawerItem,
                navDrawerState: navDrawerState,
                onFeedFilterSelected: { feedFilter in
                    indexHolder.clear()
                    appState.navigate(route: CompactViewRoute.feed)
                    scrollUpTrigger.toggle()
                    homeViewModel.onFeedFilterSelected(selectedFeedFilter: feedFilter)
                }
            ).navigationDestination(for: CommonRoute.self) { route in
                    switch route {
                    case .aboutScreen:
                        AboutScreen()

                    case .importExportScreen:
                        ImportExportScreen()
                    }
                }
                .navigationDestination(for: CompactViewRoute.self) { route in
                    switch route {
                    case .feed:
                        HomeScreen(
                            toggleListScroll: $scrollUpTrigger,
                            homeViewModel: homeViewModel
                        )
                        .environmentObject(indexHolder)
                    }
                }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task {
            do {
                let stream = asyncSequence(for: homeViewModel.navDrawerStateFlow)
                for try await state in stream {
                    self.navDrawerState = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
    }
}
