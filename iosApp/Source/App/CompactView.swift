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

    let homeViewModel: HomeViewModel

    @State
    var navDrawerState: NavDrawerState = NavDrawerState(timeline: [], categories: [], feedSourcesByCategory: [:])

    var body: some View {
        NavigationStack(path: $appState.path) {
            SidebarDrawer(
                selectedDrawerItem: $selectedDrawerItem,
                navDrawerState: navDrawerState,
                onFeedFilterSelected: { feedFilter in
                    appState.navigate(route: CompactViewRoute.feed)
                    homeViewModel.onFeedFilterSelected(selectedFeedFilter: feedFilter)
                }
            )
                .navigationDestination(for: CommonRoute.self) { route in
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
                        HomeScreen(homeViewModel: homeViewModel)
                    }
                }
        }
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
