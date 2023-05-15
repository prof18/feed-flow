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
    @State var showLoading: Bool = true
    @State private var showSettings = false

    @State var unreadCount = 0
    
    var body: some View {
        ScrollViewReader { proxy in
            HomeScreenContent(
                loadingState: loadingState,
                feedState: feedState,
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
                    Text("FeedFlow (\(unreadCount))")
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
                let stream = asyncSequence(for: homeViewModel.loadingStateFlow)
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
                let stream = asyncSequence(for: homeViewModel.errorStateFlow)
                for try await state in stream {
                    if let message = state?.message {
                        self.appState.snackbarQueue.append(
                            
                            SnackbarData(
                                title: message,
                                subtitle: nil,
                                showBanner: true
                            )
                        )
                    }
                }
            } catch {
                emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: homeViewModel.feedStateFlow)
                for try await state in stream {
                    self.feedState = state
                }
            } catch {
                emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: homeViewModel.countStateFlow)
                for try await state in stream {
                    self.unreadCount = Int(truncating: state)
                    print("New count: \(unreadCount)")
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
        self.appState.snackbarQueue.append(
            SnackbarData(
                title: "Sorry, something went wrong :(",
                subtitle: nil,
                showBanner: true
            )
        )
    }
}
