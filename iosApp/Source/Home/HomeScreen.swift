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
    @StateObject var indexHolder = HomeListIndexHolder()
    
    @State var loadingState: FeedUpdateStatus? = nil
    @State var feedState: [FeedItem] = []
    @State var errorState: UIErrorState? = nil
    @State var showLoading: Bool = true
    @State private var showSettings = false
        
    var body: some View {
        ScrollViewReader { proxy in
            HomeScreenContent(
                loadingState: loadingState,
                feedState: feedState,
                errorState: errorState,
                showLoading: showLoading,
                onReloadClick: {
                    homeViewModel.getNewFeeds()
                },
                onAddFeedClick: {
                    self.showSettings.toggle()
                }
            )
            .environmentObject(indexHolder)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Text("FeedFlow (\(indexHolder.unreadCount))")
                        .font(.title2)
                        .padding(.vertical, Spacing.medium)
                        .onTapGesture(count: 2){
                            homeViewModel.updateReadStatus(lastVisibleIndex: Int32(indexHolder.getLastReadIndex()))
                            self.indexHolder.refresh()
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
                    self.indexHolder.isLoading = isLoading
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
                    
                    self.indexHolder.setUnreadCount(count: self.feedState.count)
                }
            } catch {
                emitGenericError()
            }
        }
        .onChange(of: scenePhase) { newScenePhase in
            switch newScenePhase {
            case .background:
                homeViewModel.updateReadStatus(lastVisibleIndex: Int32(indexHolder.getLastReadIndex()))
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
