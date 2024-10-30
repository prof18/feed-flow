//
//  HomeScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 27/03/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI

struct IphoneHomeScreen: View {

  @Environment(AppState.self) private var appState
  @Environment(BrowserSelector.self) private var browserSelector
  @Environment(HomeListIndexHolder.self) private var indexHolder
  @Environment(\.scenePhase) private var scenePhase
  @Environment(\.openURL) private var openURL
  @Environment(\.dismiss) private var dismiss

  @State var loadingState: FeedUpdateStatus?
  @State var feedState: [FeedItem] = []
  @State var showLoading: Bool = true
  @State private var sheetToShow: HomeSheetToShow?
  @State var unreadCount = 0
  @State var currentFeedFilter: FeedFilter = FeedFilter.Timeline()
  @State var showFeedSyncButton: Bool = false
  @State private var browserToOpen: BrowserToPresent?

  @Binding var toggleListScroll: Bool
  @Binding var showSettings: Bool
  @Binding var selectedDrawerItem: DrawerItem?

  let homeViewModel: HomeViewModel

  var body: some View {
    ScrollViewReader { proxy in
      FeedListView(
        loadingState: loadingState,
        feedState: feedState,
        showLoading: showLoading,
        currentFeedFilter: currentFeedFilter,
        onReloadClick: {
          homeViewModel.getNewFeeds(isFirstLaunch: false)
        },
        onAddFeedClick: {
          self.sheetToShow = .noFeedSource
        },
        requestNewPage: {
          homeViewModel.requestNewFeedsPage()
        },
        onItemClick: { feedItemClickedInfo in
          if browserSelector.openReaderMode() {
            self.appState.navigate(
              route: CommonViewRoute.readerMode(url: URL(string: feedItemClickedInfo.url)!)
            )
          } else if browserSelector.openInAppBrowser() {
            browserToOpen = .inAppBrowser(url: URL(string: feedItemClickedInfo.url)!)
          } else {
            openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: feedItemClickedInfo.url))
          }

          // TODO: add here stuff to open
          homeViewModel.markAsRead(feedItemId: feedItemClickedInfo.id)
        },
        onBookmarkClick: { feedItemId, isBookmarked in
          homeViewModel.updateBookmarkStatus(feedItemId: feedItemId, bookmarked: isBookmarked)
        },
        onReadStatusClick: { feedItemId, isRead in
          homeViewModel.updateReadStatus(feedItemId: feedItemId, read: isRead)
        },
        onBackToTimelineClick: {
          homeViewModel.onFeedFilterSelected(selectedFeedFilter: FeedFilter.Timeline())
          selectedDrawerItem = DrawerItem.Timeline()
        },
        onCommentsClick: { commentsUrl in
          if browserSelector.openInAppBrowser() {
            browserToOpen = .inAppBrowser(url: URL(string: commentsUrl)!)
          } else {
            openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: commentsUrl))
          }
        }
      )
      .onChange(of: toggleListScroll) {
        proxy.scrollTo(feedState.first?.id)
      }
      .onChange(of: showSettings) {
        sheetToShow = .settings
      }
      .if(appState.sizeClass == .compact) { view in
        view.navigationBarBackButtonHidden(true)
      }
      .navigationBarTitleDisplayMode(.inline)
      .toolbar {
        makeToolbarHeaderView(proxy: proxy)
        if !isOnVisionOSDevice() {
          if showFeedSyncButton {
            makeFeedSynToolbarView()
          }
          makeSearchToolbarView()
          makeMenuToolbarView(proxy: proxy)
        }
      }
    }
    .fullScreenCover(item: $browserToOpen) { browserToOpen in
      switch browserToOpen {
      case .inAppBrowser(let url):
        SFSafariView(url: url)
          .ignoresSafeArea()
      }
    }
    .sheet(item: $sheetToShow) { item in
      switch item {
      case .settings:
        SettingsScreen()
      case .noFeedSource:
        NoFeedsBottomSheet(
          onAddFeedClick: {
            self.sheetToShow = .addFeed
          },
          onImportExportClick: {
            self.sheetToShow = .importExport
          }
        )
      case .addFeed:
        AddFeedScreen(showCloseButton: true)
      case .importExport:
        ImportExportScreen(showCloseButton: true)
      }
    }
    .task {
      for await state in homeViewModel.loadingState {
        let isLoading = state.isLoading() && state.totalFeedCount != 0
        withAnimation {
          self.showLoading = isLoading
        }
        self.indexHolder.isLoading = isLoading
        self.loadingState = state
      }
    }
    .task {
      for await state in homeViewModel.errorState {
        switch onEnum(of: state) {
        case .databaseError:
          self.appState.snackbarQueue.append(
            SnackbarData(
              title: feedFlowStrings.databaseError,
              subtitle: nil,
              showBanner: true
            )
          )
        case .feedErrorState(let state):
          self.appState.snackbarQueue.append(
            SnackbarData(
              title: feedFlowStrings.feedErrorMessage(state.feedName),
              subtitle: nil,
              showBanner: true
            )
          )
        case .none:
          break
        }
      }
    }
    .task {
      for await state in homeViewModel.feedState {
        self.feedState = state
      }
    }
    .task {
      for await state in homeViewModel.unreadCountFlow {
        self.unreadCount = Int(truncating: state)
      }
    }
    .task {
      for await state in homeViewModel.currentFeedFilter {
        self.currentFeedFilter = state
      }
    }
    .task {
      for await state in homeViewModel.isSyncUploadRequired {
        self.showFeedSyncButton = state as? Bool ?? false
      }
    }
    .onChange(of: scenePhase) {
      switch scenePhase {
      case .background:
        homeViewModel.enqueueBackup()
      default:
        break
      }
    }
  }

  @ToolbarContentBuilder
  private func makeToolbarHeaderView(proxy: ScrollViewProxy) -> some ToolbarContent {
    ToolbarItem(placement: .navigationBarLeading) {
      HStack {
        if appState.sizeClass == .compact {
          Button {
            self.dismiss()
          } label: {
            Image(systemName: "sidebar.left")
          }
          .accessibilityIdentifier(TestingTag.shared.DRAWER_MENU_BUTTON)
        }

        HStack {
          Text(getNavBarName(feedFilter: currentFeedFilter))
            .font(.title2)

          if !(currentFeedFilter is FeedFilter.Read) && !(currentFeedFilter is FeedFilter.Bookmarks)
          {
            Text("(\(unreadCount))")
              .font(.title2)
          }
        }
        .padding(.vertical, Spacing.medium)
        .onTapGesture(count: 1) {
          proxy.scrollTo(feedState.first?.id)
        }
        .onTapGesture(count: 2) {
          homeViewModel.markAsReadOnScroll(lastVisibleIndex: Int32(indexHolder.getLastReadIndex()))
          self.indexHolder.refresh()
          proxy.scrollTo(feedState.first?.id)
          homeViewModel.getNewFeeds(isFirstLaunch: false)
        }
        .accessibilityIdentifier(TestingTag.shared.HOME_TOOLBAR)
      }
    }
  }

  @ToolbarContentBuilder
  private func makeFeedSynToolbarView() -> some ToolbarContent {
    ToolbarItem(placement: .navigationBarTrailing) {
      Button {
        homeViewModel.enqueueBackup()
      } label: {
        Image(systemName: "arrow.uturn.up")
      }
    }
  }

  @ToolbarContentBuilder
  private func makeSearchToolbarView() -> some ToolbarContent {
    ToolbarItem(placement: .navigationBarTrailing) {
      Button {
        self.appState.navigate(
          route: CommonViewRoute.search
        )
      } label: {
        Image(systemName: "magnifyingglass")
      }
    }
  }

  @ToolbarContentBuilder
  private func makeMenuToolbarView(proxy: ScrollViewProxy) -> some ToolbarContent {
    ToolbarItem(placement: .primaryAction) {
      Menu {
        Button {
          homeViewModel.markAllRead()
        } label: {
          Label(feedFlowStrings.markAllReadButton, systemImage: "checkmark")
        }

        Button {
          homeViewModel.deleteOldFeedItems()
        } label: {
          Label(feedFlowStrings.clearOldArticlesButton, systemImage: "trash")
        }

        Button {
          proxy.scrollTo(feedState.first?.id)
          homeViewModel.forceFeedRefresh()
        } label: {
          Label(feedFlowStrings.forceFeedRefresh, systemImage: "arrow.clockwise")
        }

        Button {
          self.sheetToShow = .settings
        } label: {
          Label(feedFlowStrings.settingsButton, systemImage: "gear")
        }
        .accessibilityLabel(TestingTag.shared.SETTINGS_MENU)

        #if DEBUG
          Button {
            homeViewModel.deleteAllFeeds()
          } label: {
            Label("Delete Database", systemImage: "trash")
          }
        #endif

      } label: {
        Image(systemName: "ellipsis.circle")
      }
      .accessibilityIdentifier(TestingTag.shared.SETTING_BUTTON)
    }
  }

  func getNavBarName(feedFilter: FeedFilter) -> String {
    let deviceType = getDeviceType()

    switch feedFilter {
    case let category as FeedFilter.Category:
      switch deviceType {
      case .iphonePortrait:
        return category.feedCategory.title.truncate(maxChar: 12)
      default:
        return category.feedCategory.title.truncate(maxChar: 40)
      }

    case let source as FeedFilter.Source:
      switch deviceType {
      case .iphonePortrait:
        return source.feedSource.title.truncate(maxChar: 12)
      default:
        return source.feedSource.title.truncate(maxChar: 40)
      }

    case is FeedFilter.Read:
      return feedFlowStrings.drawerTitleRead

    case is FeedFilter.Bookmarks:
      return feedFlowStrings.drawerTitleBookmarks

    default:
      return feedFlowStrings.appName
    }
  }
}
