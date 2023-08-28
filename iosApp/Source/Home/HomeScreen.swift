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
    @StateObject var settingsViewModel: SettingsViewModel = KotlinDependencies.shared.getSettingsViewModel()

    @State var loadingState: FeedUpdateStatus?
    @State var feedState: [FeedItem] = []
    @State var showLoading: Bool = true
    @State var sheetToShow: SheetToShow?
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
            onImportFeedClick: { input in
                settingsViewModel.importFeed(opmlInput: input)
            },
            onExportFeedClick: { output in
                settingsViewModel.exportFeed(opmlOutput: output)
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
        .task {
            do {
                let stream = asyncSequence(for: settingsViewModel.isImportDoneStateFlow)
                for try await isImportDone in stream where isImportDone as? Bool ?? false {

                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: MR.strings().feeds_import_done_message.localized,
                            subtitle: nil,
                            showBanner: true
                        )
                    )

                }
            } catch {
                emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: settingsViewModel.isExportDoneStateFlow)
                for try await isExportDone in stream where isExportDone as? Bool ?? false {

                    self.sheetToShow = .shareSheet
                }

            } catch {
                emitGenericError()
            }
        }
        .task {
            do {
                let stream = asyncSequence(for: settingsViewModel.errorState)
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

    @Binding var sheetToShow: SheetToShow?

    let onRefresh: () -> Void
    let updateReadStatus: (Int32) -> Void
    let onMarkAllReadClick: () -> Void
    let onDeleteOldFeedClick: () -> Void
    let onImportFeedClick: (OpmlInput) -> Void
    let onExportFeedClick: (OpmlOutput) -> Void
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

                        Button(
                            action: {
                                self.sheetToShow = .filePicker
                            },
                            label: {
                                Label(
                                    MR.strings().import_feed_button.localized,
                                    systemImage: "arrow.down.doc"
                                )
                            }
                        )

                        Button(
                            action: {
                                if let url = getUrlForOpmlExport() {
                                    onExportFeedClick(OpmlOutput(url: url))
                                    self.appState.snackbarQueue.append(
                                        SnackbarData(
                                            title: MR.strings().export_started_message.localized,
                                            subtitle: nil,
                                            showBanner: true
                                        )
                                    )
                                } else {
                                    self.appState.snackbarQueue.append(
                                        SnackbarData(
                                            title: MR.strings().generic_error_message.localized,
                                            subtitle: nil,
                                            showBanner: true
                                        )
                                    )
                                }
                            },
                            label: {
                                Label(
                                    MR.strings().export_feeds_button.localized,
                                    systemImage: "arrow.up.doc"
                                )
                            }
                        )

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
            case .filePicker:
                FilePickerController { url in

                    do {
                        let data = try Data(contentsOf: url)
                        onImportFeedClick(OpmlInput(opmlData: data))
                    } catch {
                        self.appState.snackbarQueue.append(

                            SnackbarData(
                                title: MR.strings().load_file_error_message.localized,
                                subtitle: nil,
                                showBanner: true
                            )
                        )
                    }

                    self.appState.snackbarQueue.append(
                        SnackbarData(
                            title: MR.strings().feeds_importing_message.localized,
                            subtitle: nil,
                            showBanner: true
                        )
                    )
                }

            case .shareSheet:

                ShareSheet(
                    activityItems: [getUrlForOpmlExport()! as URL],
                    applicationActivities: nil
                ) { _, _, _, _ in }

            case .feedList:
                FeedSourceListScreen()
            }
        }
    }
}

private func getUrlForOpmlExport() -> URL? {
    let cacheDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
    return cacheDirectory?.appendingPathComponent("feed-export.opml")
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
            onImportFeedClick: { _ in },
            onExportFeedClick: { _ in },
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
            onImportFeedClick: { _ in },
            onExportFeedClick: { _ in },
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
            sheetToShow: .constant(SheetToShow.feedList),
            onRefresh: { },
            updateReadStatus: { _ in },
            onMarkAllReadClick: { },
            onDeleteOldFeedClick: { },
            onImportFeedClick: { _ in },
            onExportFeedClick: { _ in },
            onForceRefreshClick: {}
        )
        .environmentObject(HomeListIndexHolder())
        .environmentObject(AppState())
        .environmentObject(BrowserSelector())
    }
}
