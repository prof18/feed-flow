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
    @StateObject var settingsViewModel: SettingsViewModel = KotlinDependencies.shared.getSettingsViewModel()
    
    @State var loadingState: FeedUpdateStatus? = nil
    @State var feedState: [FeedItem] = []
    @State var showLoading: Bool = true
    @State var sheetToShow: SheetToShow?
    
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
                    //                    self.showFeeds.toggle()
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
                    Menu {
                        Button(
                            action: {
                                homeViewModel.markAllRead()
                            }) {
                                Label("Mark all read", systemImage: "checkmark")
                            }
                        
                        Button(
                            action: {
                                homeViewModel.deleteOldFeedItems()
                            }) {
                                Label("Clear old articles", systemImage: "trash")
                            }
                        
                        Button(
                            action: {
                                self.sheetToShow = .feedList
                            })  {
                                Label("Feeds", systemImage: "list.bullet.rectangle.portrait")
                            }
                        
                        Button(
                            action: {
                                self.sheetToShow = .filePicker
                            })  {
                                Label("Import feed from OPML", systemImage: "arrow.down.doc")
                            }
                        
                        Button(
                            action: {
                                if let url = getUrl() {
                                    settingsViewModel.exportFeed(opmlOutput: OPMLOutput(url: url))
                                } else {
                                    // TODO: handle error
                                }
                            })  {
                                Label("Export feed to OPML", systemImage: "arrow.up.doc")
                            }
                        
                    } label: {
                        Image(systemName: "gear")
                    }
                }
                
            }
            
        }
        .sheet(item: $sheetToShow) { item in
            switch item {
            case .filePicker:
                FilePickerController { url in
                    
                    do {
                        let data = try Data(contentsOf: url)
                        settingsViewModel.importFeed(opmlInput: OPMLInput(opmlData: data))
                        self.appState.snackbarQueue.append(
                            SnackbarData(
                                title: "Generating export",
                                subtitle: nil,
                                showBanner: true
                            )
                        )
                    } catch {
                        self.appState.snackbarQueue.append(
                            
                            SnackbarData(
                                title: "Unable to load file",
                                subtitle: nil,
                                showBanner: true
                            )
                        )
                    }
                    
                    
                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: "Importing feed",
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                }
                
                
            case .shareSheet:
                
                ShareSheet(
                    activityItems: [getUrl()! as URL],
                    applicationActivities: nil
                ) { _, _, _, _ in }
                
            case .feedList:
                FeedsScreen()
            }
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
        .task {
            do {
                let stream = asyncSequence(for: settingsViewModel.isImportDoneStateFlow)
                for try await isImportDone in stream {
                    if isImportDone as! Bool {
                        self.appState.snackbarQueue.append(
                            SnackbarData(
                                title: "Import done",
                                subtitle: nil,
                                showBanner: true
                            )
                        )
                    }
                }
            } catch {
                self.appState.snackbarQueue.append(
                    SnackbarData(
                        title: "Sorry, something went wrong :(",
                        subtitle: nil,
                        showBanner: true
                    )
                )
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: settingsViewModel.isExportDoneStateFlow)
                for try await isExportDone in stream {
                    if isExportDone as! Bool {
                        self.sheetToShow = .shareSheet
                    }
                }
            } catch {
                self.appState.snackbarQueue.append(
                    SnackbarData(
                        title: "Sorry, something went wrong :(",
                        subtitle: nil,
                        showBanner: true
                    )
                )
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
    
    private func getUrl() -> URL? {
        let cacheDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
        return cacheDirectory?.appendingPathComponent("feed-export.opml")
    }
}
