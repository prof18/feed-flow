//
//  RegularView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import shared
import KMPNativeCoroutinesAsync

struct RegularView: View {
    @EnvironmentObject
    var appState: AppState

    @State
    var navDrawerState: NavDrawerState = NavDrawerState(timeline: [], categories: [], feedSourcesByCategory: [:])
    var drawerItems: [DrawerItem] = []

    @Binding
    var selectedDrawerItem: DrawerItem?

    let homeViewModel: HomeViewModel

    var body: some View {
        NavigationSplitView {
            SidebarDrawer(
                selectedDrawerItem: $selectedDrawerItem,
                navDrawerState: navDrawerState,
                onFeedFilterSelected: { feedFilter in
                    homeViewModel.onFeedFilterSelected(selectedFeedFilter: feedFilter)
                }
            )
        } detail: {
            NavigationStack {
                HomeScreen(homeViewModel: homeViewModel)
                    .navigationDestination(for: CommonRoute.self) { route in
                        switch route {
                        case .aboutScreen:
                            AboutScreen()

                        case .importExportScreen:
                            ImportExportScreen()
                        }
                    }
            }
        }
        .navigationSplitViewStyle(.balanced)
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
