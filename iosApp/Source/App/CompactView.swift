//
//  CompactView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 21/10/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import Reeeed
import SwiftUI

struct CompactView: View {

  @Environment(AppState.self) private var appState
  @Environment(BrowserSelector.self) private var browserSelector
  @Environment(\.openURL) private var openURL

  @Binding var selectedDrawerItem: DrawerItem?

  @State var navDrawerState: NavDrawerState = NavDrawerState(
    timeline: [],
    read: [],
    bookmarks: [],
    categories: [],
    feedSourcesWithoutCategory: [],
    feedSourcesByCategory: [:]
  )
  @State var scrollUpTrigger: Bool = false
  @State var showAddFeedSheet = false
  @State var showEditFeedSheet = false

  @State var isToggled: Bool = false
  @State private var showFontSizeMenu: Bool = false
  @State private var fontSize = 16.0
  @State private var isSliderMoving = false
  @State private var reset = false
  @State private var isBookmarked = false

  @StateObject private var vmStoreOwner = VMStoreOwner<ReaderModeViewModel>(
    Deps.shared.getReaderModeViewModel())

  @State private var browserToOpen: BrowserToPresent?

  @State var indexHolder: HomeListIndexHolder
  let homeViewModel: HomeViewModel

  @State private var feedSourceToEdit: FeedSource?

  var body: some View {
    @Bindable var appState = appState
    NavigationStack(path: $appState.compatNavigationPath) {
      SidebarDrawer(
        selectedDrawerItem: $selectedDrawerItem,
        navDrawerState: navDrawerState,
        onFeedFilterSelected: { feedFilter in
          indexHolder.clear()
          appState.navigate(route: CompactViewRoute.feed)
          scrollUpTrigger.toggle()
          homeViewModel.onFeedFilterSelected(selectedFeedFilter: feedFilter)
        },
        onMarkAllReadClick: {
          // On compact view it's handled by the home
        },
        onDeleteOldFeedClick: {
          // On compact view it's handled by the home
        },
        onForceRefreshClick: {
          // On compact view it's handled by the home
        },
        deleteAllFeeds: {
          // On compact view it's handled by the home
        },
        onShowSettingsClick: {
          // On compact view it's handled by the home
        },
        onAddFeedClick: {
          showAddFeedSheet.toggle()
        },
        onEditFeedClick: { feedSource in
          feedSourceToEdit = feedSource
          showEditFeedSheet.toggle()
        },
        onDeleteFeedClick: { feedSource in
          homeViewModel.deleteFeedSource(feedSource: feedSource)
        }
      ).sheet(isPresented: $showAddFeedSheet) {
        AddFeedScreen(showCloseButton: true)
      }
      .sheet(isPresented: $showEditFeedSheet) {
        if let feedSource = feedSourceToEdit {
          EditFeedScreen(feedSource: feedSource)
        }
      }
      .navigationDestination(for: CompactViewRoute.self) { route in
        switch route {
        case .feed:
          HomeScreen(
            toggleListScroll: $scrollUpTrigger,
            showSettings: .constant(false),
            selectedDrawerItem: $selectedDrawerItem,
            homeViewModel: homeViewModel
          )
          .environment(indexHolder)
        }
      }
      .navigationDestination(for: CommonViewRoute.self) { route in
        switch route {
        case .readerMode(let feedItem):
          Group {
            ReeeederView(
              url: URL(string: feedItem.url)!,
              options: ReeeederViewOptions(
                theme: .init(
                  additionalCSS: """
                        #__reader_container {
                            font-size: \(fontSize)px
                        }
                    """
                ),
                onLinkClicked: { url in
                  if browserSelector.openInAppBrowser() {
                    browserToOpen = .inAppBrowser(url: url)
                  } else {
                    openURL(browserSelector.getUrlForDefaultBrowser(stringUrl: url.absoluteString))
                  }
                }
              ),
              toolbarContent: {
                Button {
                  isBookmarked.toggle()
                  vmStoreOwner.instance.updateBookmarkStatus(
                    feedItemId: FeedItemId(id: feedItem.id),
                    bookmarked: isBookmarked
                  )
                } label: {
                  if isBookmarked {
                    Image(systemName: "bookmark.slash")
                  } else {
                    Image(systemName: "bookmark")
                  }
                }

                ShareLink(
                  item: URL(string: feedItem.url)!,
                  label: {
                    Label("Share", systemImage: "square.and.arrow.up")
                  })

                Button {
                  if browserSelector.openInAppBrowser() {
                    browserToOpen = .inAppBrowser(url: URL(string: feedItem.url)!)
                  } else {
                    openURL(
                      browserSelector.getUrlForDefaultBrowser(
                        stringUrl: URL(string: feedItem.url)!.absoluteString))
                  }
                } label: {
                  Image(systemName: "globe")
                }

                fontSizeMenu
              }
            )
            .onAppear {
              isBookmarked = feedItem.isBookmarked
            }
            .id(reset)
          }

        case .search:
          SearchScreen()

        case .accounts:
          AccountsScreen()

        case .dropboxSync:
          DropboxSyncScreen()
        }
      }
      .fullScreenCover(item: $browserToOpen) { browserToOpen in
        switch browserToOpen {
        case .inAppBrowser(let url):
          SFSafariView(url: url)
            .ignoresSafeArea()
        }
      }
    }
    .navigationBarTitleDisplayMode(.inline)
    .task {
      for await state in homeViewModel.navDrawerState {
        self.navDrawerState = state
      }
    }.task {
      for await state in vmStoreOwner.instance.readerFontSizeState {
        self.fontSize = Double(truncating: state)
      }
    }
  }

  @ViewBuilder
  private var fontSizeMenu: some View {
    Button {
      showFontSizeMenu.toggle()
    } label: {
      Image(systemName: "textformat.size")
    }
    .font(.title3)
    .popover(isPresented: $showFontSizeMenu) {
      VStack(alignment: .leading) {
        Text(feedFlowStrings.readerModeFontSize)

        HStack {
          Button {
            fontSize -= 1.0
            self.reset.toggle()
            vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
          } label: {
            Image(systemName: "minus")
          }

          Slider(
            value: $fontSize,
            in: 12...40,
            onEditingChanged: { isEditing in
              if !isEditing {
                self.reset.toggle()
                vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
              }
            }
          )

          Button {
            fontSize += 1.0
            self.reset.toggle()
            vmStoreOwner.instance.updateFontSize(newFontSize: Int32(Int(fontSize)))
          } label: {
            Image(systemName: "plus")
          }
        }
      }
      .frame(width: 250, height: 100)
      .padding(.horizontal, Spacing.regular)
      .presentationCompactAdaptation((.popover))
    }
  }

}
