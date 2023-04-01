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

struct HomeScreen: View {
    
    @EnvironmentObject var appState: AppState
    @StateObject var homeViewModel = KotlinDependencies.shared.getHomeViewModel()
    
    @State var loadingState: FeedUpdateStatus? = nil
    @State var feedState: [FeedItem] = []
    @State var errorState: UIErrorState? = nil
    @State var showLoading: Bool = true
    @State private var showSettings = false
    @State var visibleFeedItemsIds: Set<Int> = []
    
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
                    Text("FeedFlow (\(feedState.count))")
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
                    self.feedState = state
                }
            } catch {
                emitGenericError()
            }
        }
        .onChange(of: visibleFeedItemsIds) { indexSet in
            let index = indexSet.first
            
            if let index = index, index > 5 {
                homeViewModel.updateReadStatus(lastVisibleIndex: Int32(index - 5))
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
