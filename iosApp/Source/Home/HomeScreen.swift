//
//  HomeScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import KMPNativeCoroutinesAsync
import shared
import OrderedCollections

struct HomeScreen: View {

    @EnvironmentObject var appState: AppState
    @EnvironmentObject var browserSelector: BrowserSelector
    @StateObject var indexHolder = HomeListIndexHolder()

    @Environment(\.scenePhase) var scenePhase

    @StateObject var homeViewModel = KotlinDependencies.shared.getHomeViewModel()

    @State var loadingState: FeedUpdateStatus?
    @State var feedState: [FeedItem] = []
    @State var showLoading: Bool = true
    @State var sheetToShow: HomeSheetToShow?
    @State var unreadCount = 0

    var body: some View {

        HomeContent(
            loadingState: $loadingState,
            feedState: $feedState,
            showLoading: $showLoading,
            unreadCount: $unreadCount,
            sheetToShow: $sheetToShow,
            onRefresh: {
                homeViewModel.getNewFeeds()
            },
            updateReadStatus: { index in
                homeViewModel.updateReadStatus(lastVisibleIndex: index)
            },
            onMarkAllReadClick: {
                homeViewModel.markAllRead()
            },
            onDeleteOldFeedClick: {
                homeViewModel.deleteOldFeedItems()
            },
            onForceRefreshClick: {
                homeViewModel.forceFeedRefresh()
            }
        )
        .environmentObject(indexHolder)
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
                let stream = asyncSequence(for: homeViewModel.errorState)
                for try await state in stream {
                    if let message = state?.message {
                        self.appState.snackbarQueue.append(
                            SnackbarData(
                                title: message.localized(),
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
                let stream = asyncSequence(for: homeViewModel.countState)
                for try await state in stream {
                    self.unreadCount = Int(truncating: state)
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
                title: MR.strings().generic_error_message.localized,
                subtitle: nil,
                showBanner: true
            )
        )
    }
}

struct HomeContent: View {

    @EnvironmentObject var indexHolder: HomeListIndexHolder
    @EnvironmentObject var browserSelector: BrowserSelector
    @EnvironmentObject var appState: AppState

    @Environment(\.openURL) var openURL

    @Binding var loadingState: FeedUpdateStatus?
    @Binding var feedState: [FeedItem]
    @Binding var showLoading: Bool
    @Binding var unreadCount: Int

    @Binding var sheetToShow: HomeSheetToShow?

    let onRefresh: () -> Void
    let updateReadStatus: (Int32) -> Void
    let onMarkAllReadClick: () -> Void
    let onDeleteOldFeedClick: () -> Void
    let onForceRefreshClick: () -> Void

    var body: some View {
        ScrollViewReader { proxy in
            FeedListView(
                loadingState: loadingState,
                feedState: feedState,
                showLoading: showLoading,
                onReloadClick: {
                    onRefresh()
                },
                onAddFeedClick: {
                    self.sheetToShow = .feedList
                }
            )
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Text("\(MR.strings().app_name.localized) (\(unreadCount))")
                        .font(.title2)
                        .padding(.vertical, Spacing.medium)
                        .onTapGesture(count: 2) {
                            updateReadStatus(Int32(indexHolder.getLastReadIndex()))
                            self.indexHolder.refresh()
                            proxy.scrollTo(feedState.first?.id)
                            onRefresh()
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
                                onMarkAllReadClick()
                            },
                            label: {
                                Label(
                                    MR.strings().mark_all_read_button.localized,
                                    systemImage: "checkmark"
                                )
                            }
                        )

                        Button(
                            action: {
                                onDeleteOldFeedClick()
                            },
                            label: {
                                Label(
                                    MR.strings().clear_old_articles_button.localized,
                                    systemImage: "trash"
                                )
                            }
                        )

                        Button(
                            action: {
                                onForceRefreshClick()
                            },
                            label: {
                                Label(
                                    MR.strings().force_feed_refresh.localized,
                                    systemImage: "arrow.clockwise"
                                )
                            }
                        )

                        Button(
                            action: {
                                self.sheetToShow = .feedList
                            },
                            label: {
                                Label(
                                    MR.strings().feeds_title.localized,
                                    systemImage: "list.bullet.rectangle.portrait"
                                )
                            }
                        )

                        Menu {
                            Picker(
                                selection: $browserSelector.selectedBrowser,
                                label: Text(MR.strings().browser_selection_button.localized)
                            ) {
                                ForEach(browserSelector.browsers, id: \.self) { period in
                                    Text(period.name).tag(period as Browser?)
                                }
                            }
                        }
                    label: {
                        Label(
                            MR.strings().browser_selection_button.localized,
                            systemImage: "globe"
                        )
                    }

                        NavigationLink(value: Route.importExportScreen) {
                            Label(
                                MR.strings().import_export_opml.localized,
                                systemImage: "arrow.up.arrow.down"
                            )
                        }

                        Button(
                            action: {
                                if let url = URL(string: UserFeedbackReporter.shared.getFeedbackUrl()) {
                                    self.openURL(url)
                                }
                            },
                            label: {
                                Label(
                                    MR.strings().report_issue_button.localized,
                                    systemImage: "ladybug"
                                )
                            }
                        )

                        NavigationLink(value: Route.aboutScreen) {
                            Label(
                                MR.strings().about_button.localized,
                                systemImage: "info.circle"
                            )
                        }
                    } label: {
                        Image(systemName: "gear")
                    }
                }

            }

        }
        .sheet(item: $sheetToShow) { item in
            switch item {
            case .feedList:
                FeedSourceListScreen()
            }
        }
    }
}

struct HomeContentLoading_Previews: PreviewProvider {
    static var previews: some View {
        HomeContent(
            loadingState: .constant(
                InProgressFeedUpdateStatus(
                    refreshedFeedCount: Int32(10),
                    totalFeedCount: Int32(42)
                )
            ),
            feedState: .constant(PreviewItemsKt.feedItemsForPreview),
            showLoading: .constant(true),
            unreadCount: .constant(42),
            sheetToShow: .constant(nil), onRefresh: { },
            updateReadStatus: { _ in },
            onMarkAllReadClick: { },
            onDeleteOldFeedClick: { },
            onForceRefreshClick: {}
        )
        .environmentObject(HomeListIndexHolder())
        .environmentObject(AppState())
        .environmentObject(BrowserSelector())
    }
}

struct HomeContentLoaded_Previews: PreviewProvider {
    static var previews: some View {
        HomeContent(
            loadingState: .constant(
                FinishedFeedUpdateStatus()
            ),
            feedState: .constant(PreviewItemsKt.feedItemsForPreview),
            showLoading: .constant(false),
            unreadCount: .constant(42),
            sheetToShow: .constant(nil), onRefresh: { },
            updateReadStatus: { _ in },
            onMarkAllReadClick: { },
            onDeleteOldFeedClick: { },
            onForceRefreshClick: {}
        )
        .environmentObject(HomeListIndexHolder())
        .environmentObject(AppState())
        .environmentObject(BrowserSelector())
    }
}

struct HomeContentSettings_Previews: PreviewProvider {
    static var previews: some View {
        HomeContent(
            loadingState: .constant(
                FinishedFeedUpdateStatus()
            ),
            feedState: .constant(PreviewItemsKt.feedItemsForPreview),
            showLoading: .constant(false),
            unreadCount: .constant(42),
            sheetToShow: .constant(HomeSheetToShow.feedList),
            onRefresh: { },
            updateReadStatus: { _ in },
            onMarkAllReadClick: { },
            onDeleteOldFeedClick: { },
            onForceRefreshClick: {}
        )
        .environmentObject(HomeListIndexHolder())
        .environmentObject(AppState())
        .environmentObject(BrowserSelector())
    }
}
