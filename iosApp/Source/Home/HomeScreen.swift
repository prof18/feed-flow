//
//  HomeScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/03/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import SwiftUI
import KMPNativeCoroutinesAsync
import shared
import OrderedCollections

struct HomeScreen: View {
    
    @EnvironmentObject var appState: AppState
    @Environment(\.scenePhase) var scenePhase
    
    @StateObject var homeViewModel = KotlinDependencies.shared.getHomeViewModel()
    
    @State var loadingState: FeedUpdateStatus? = nil
    @State var feedState: [FeedItem] = []
    @State var errorState: UIErrorState? = nil
    @State var showLoading: Bool = true
    @State private var showSettings = false
    @State var visibleFeedItemsIds: OrderedSet<Int> = []
    @State var unreadItemsCount: Int = 0
    
    @State private var lastReadItemIndex = 0
    
    var body: some View {
        ScrollViewReader { proxy in
            HomeScreenContent(
                loadingState: $loadingState,
                feedState: $feedState,
                errorState: $errorState,
                showLoading: $showLoading,
                visibleFeedItemsIds: $visibleFeedItemsIds,
                onReloadClick: {
                    homeViewModel.getNewFeeds()
                },
                onAddFeedClick: {
                    self.showSettings.toggle()
                }
            )
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Text("FeedFlow (\(unreadItemsCount))")
                        .font(.title2)
                        .padding(.vertical, Spacing.medium)
                        .onTapGesture(count: 2){
                            proxy.scrollTo(feedState.first?.id)
                            homeViewModel.getNewFeeds()
                        }
                        .onTapGesture {
                            withAnimation {
                                proxy.scrollTo(feedState.first?.id)
                            }
                        }
                }
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        self.showSettings.toggle()
                    } label: {
                        Image(systemName: "gear")
                    }
                }
                
            }
            
        }
        .sheet(isPresented: self.$showSettings) {
            SettingsScreen()
        }
        .task {
            do {
                let stream = asyncStream(for: homeViewModel.loadingStateNative)
                for try await state in stream {
                    let isLoading = state.isLoading() && state.totalFeedCount != 0
                    withAnimation {
                        self.showLoading = isLoading
                    }
                    self.loadingState = state
                }
            } catch {
                emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncStream(for: homeViewModel.errorStateNative)
                for try await state in stream {
                    self.errorState = state
                }
            } catch {
                emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncStream(for: homeViewModel.feedStateNative)
                for try await state in stream {
                    print(">>Updating feed: \(state.count)")
                    print(">>> last visible: \(self.lastReadItemIndex)")
                    self.feedState = state
                    
                    let computedUnread = state.count - lastReadItemIndex
                    print("")
                    if state.count > computedUnread {
                        self.unreadItemsCount = computedUnread
                    } else {
                        self.unreadItemsCount = state.count
                    }
                }
            } catch {
                emitGenericError()
            }
        }
        .onChange(of: visibleFeedItemsIds) { indexSet in
            let sortedSet = indexSet.sorted()
            let index = sortedSet.first ?? 0
            
            if index > lastReadItemIndex {
                self.lastReadItemIndex = index - 1
                self.unreadItemsCount = self.feedState.count - self.lastReadItemIndex
            }
        }
        .onChange(of: scenePhase) { newScenePhase in
            switch newScenePhase {
            case .background:
                homeViewModel.updateReadStatus(lastVisibleIndex: Int32(lastReadItemIndex))
            default:
                break
            }
        }
    }
    
    private func emitGenericError() {
        self.appState.snackbarData = SnackbarData(
            title: "Sorry, something went wrong :(",
            subtitle: nil,
            showBanner: true
        )
    }
}
