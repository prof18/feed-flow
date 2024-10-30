import FeedFlowKit
import SwiftUI

struct IpadHomeScreen: View {

  @Environment(AppState.self) private var appState
  @Environment(BrowserSelector.self) private var browserSelector
  @Environment(HomeListIndexHolder.self) private var indexHolder
  @Environment(\.scenePhase) private var scenePhase
  @Environment(\.openURL) private var openURL
  @Environment(\.dismiss) private var dismiss

  @State var loadingState: FeedUpdateStatus?
  @State var feedState: [FeedItem] = []
  @State var showLoading: Bool = true

  // TODO: Are really necessary for this view?
  //  maybe they need to be moved to the parent view
  @State var currentFeedFilter: FeedFilter = FeedFilter.Timeline()
  @Binding var toggleListScroll: Bool
  @Binding var showSettings: Bool
  @Binding var selectedDrawerItem: DrawerItem?
  @State private var sheetToShow: HomeSheetToShow?
  @State var unreadCount = 0
  @State var showFeedSyncButton: Bool = false

  let homeViewModel: HomeViewModel

  var body: some View {
    NavigationStack {
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
            // TODO: add here stuff to open
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
          unreadToolbar
          if showFeedSyncButton {
            makeFeedSynToolbarView
          }
          makeMenuToolbarView(proxy: proxy)

        }
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
  private var makeFeedSynToolbarView: some ToolbarContent {
    ToolbarItem(placement: .navigationBarTrailing) {
      Button {
        homeViewModel.enqueueBackup()
      } label: {
        Image(systemName: "arrow.uturn.up")
      }
    }
  }

  @ToolbarContentBuilder
  private var unreadToolbar: some ToolbarContent {
    ToolbarItem(placement: .navigationBarLeading) {
      // TODO: double click to scroll to top
      if !(currentFeedFilter is FeedFilter.Read) && !(currentFeedFilter is FeedFilter.Bookmarks) {
        Text("\(unreadCount)")
          .font(.body)
          .padding(.horizontal, Spacing.regular)
          .padding(.vertical, Spacing.small)
          .background(Color.secondary.opacity(0.2))
          .clipShape(Capsule())
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
}
