import FeedFlowKit
import NukeUI
import SwiftData
import SwiftUI

@available(iOS 18.0, *)
struct IpadTabView: View {
  @Environment(AppState.self) private var appState
  @Environment(BrowserSelector.self) private var browserSelector
  @Environment(\.openURL) private var openURL
  @Environment(\.horizontalSizeClass) private var horizontalSizeClass
  @AppStorage("sidebarCustomizations") var tabViewCustomization: TabViewCustomization

  @Binding var selectedDrawerItem: DrawerItem?

  @State var navDrawerState: NavDrawerState = NavDrawerState(
    timeline: [],
    read: [],
    bookmarks: [],
    categories: [],
    feedSourcesWithoutCategory: [],
    feedSourcesByCategory: [:]
  )

  // TODO: is this necessary?
  @State var scrollUpTrigger: Bool = false
  @State var showSettings: Bool = false
  @State var showAddFeedSheet = false

  @State var indexHolder: HomeListIndexHolder
  let homeViewModel: HomeViewModel

  var body: some View {
    TabView(selection: $selectedDrawerItem) {
      Tab(
        feedFlowStrings.drawerTitleTimeline,
        systemImage: "newspaper",
        value: DrawerItem.Timeline()
      ) {
        IpadHomeScreen(
          toggleListScroll: $scrollUpTrigger,
          showSettings: $showSettings,
          selectedDrawerItem: $selectedDrawerItem,
          homeViewModel: homeViewModel
        )
        .environment(indexHolder)
      }
      .customizationID(DrawerItem.Timeline().customizationID)
      .customizationBehavior(.disabled, for: .sidebar, .tabBar)

      Tab(
        feedFlowStrings.drawerTitleRead,
        systemImage: "text.badge.checkmark",
        value: DrawerItem.Read()
      ) {
        IpadHomeScreen(
          toggleListScroll: $scrollUpTrigger,
          showSettings: $showSettings,
          selectedDrawerItem: $selectedDrawerItem,
          homeViewModel: homeViewModel
        )
        .environment(indexHolder)
      }
      .customizationID(DrawerItem.Read().customizationID)

      Tab(
        feedFlowStrings.drawerTitleBookmarks,
        systemImage: "bookmark.square",
        value: DrawerItem.Bookmarks()
      ) {
        IpadHomeScreen(
          toggleListScroll: $scrollUpTrigger,
          showSettings: $showSettings,
          selectedDrawerItem: $selectedDrawerItem,
          homeViewModel: homeViewModel
        )
        .environment(indexHolder)
      }
      .customizationID(DrawerItem.Bookmarks().customizationID)

      if !navDrawerState.categories.isEmpty {
        TabSection {
          ForEach(navDrawerState.categories, id: \.self) { drawerItem in
            if let categoryItem = drawerItem as? DrawerItem.DrawerCategory {
              Tab(categoryItem.category.title, systemImage: "tag", value: drawerItem) {
                IpadHomeScreen(
                  toggleListScroll: $scrollUpTrigger,
                  showSettings: $showSettings,
                  selectedDrawerItem: $selectedDrawerItem,
                  homeViewModel: homeViewModel
                )
                .environment(indexHolder)
              }
              .customizationID(categoryItem.customizationID)
            }
          }
        } header: {
          Text(feedFlowStrings.drawerTitleCategories)
        }
        .defaultVisibility(.hidden, for: .tabBar)
        .hidden(horizontalSizeClass == .compact)
      }

      if !navDrawerState.feedSourcesWithoutCategory.isEmpty {
        TabSection {
          ForEach(navDrawerState.feedSourcesWithoutCategory, id: \.self) { drawerItem in
            if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
              Tab(
                value: drawerItem,
                role: nil
              ) {
                IpadHomeScreen(
                  toggleListScroll: $scrollUpTrigger,
                  showSettings: $showSettings,
                  selectedDrawerItem: $selectedDrawerItem,
                  homeViewModel: homeViewModel
                )
                .environment(indexHolder)
              } label: {
                makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
              }
              .customizationID(drawerItem.customizationID)
            }
          }
        } header: {
          Text(feedFlowStrings.drawerTitleFeedSources)
        }
        .defaultVisibility(.hidden, for: .tabBar)
        .hidden(horizontalSizeClass == .compact)
      }

      if !navDrawerState.feedSourcesByCategory.isEmpty {

        ForEach(
          navDrawerState.feedSourcesByCategory.keys.sorted {
            $0.feedSourceCategory?.title ?? "" < $1.feedSourceCategory?.title ?? ""
          },
          id: \.self
        ) { feedSourceCategoryWrapper in

          TabSection {

            let feedSourceDrawerItems =
              navDrawerState.feedSourcesByCategory[feedSourceCategoryWrapper] ?? []

            ForEach(feedSourceDrawerItems, id: \.self) { drawerItem in
              if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
                Tab(value: drawerItem, role: nil) {
                  IpadHomeScreen(
                    toggleListScroll: $scrollUpTrigger,
                    showSettings: $showSettings,
                    selectedDrawerItem: $selectedDrawerItem,
                    homeViewModel: homeViewModel
                  )
                  .environment(indexHolder)
                } label: {
                  makeFeedSourceDrawerItem(drawerItem: drawerFeedSource)
                }
                .customizationID(drawerItem.customizationID)
                .defaultVisibility(.hidden, for: .tabBar)
                .hidden(horizontalSizeClass == .compact)
              }
            }

          } header: {
            Text(
              feedSourceCategoryWrapper.feedSourceCategory?.title
                ?? feedFlowStrings.noCategory)
          }
          .defaultVisibility(.hidden, for: .tabBar)
          .hidden(horizontalSizeClass == .compact)
        }
      }

      Tab(value: DrawerItem.Search(), role: .search) {
        // TODO: better search view
        SearchScreen()
      }
      .customizationID(DrawerItem.Search().customizationID)
    }
    .onChange(of: selectedDrawerItem) {
      switch onEnum(of: selectedDrawerItem) {
      case .timeline:
        homeViewModel.onFeedFilterSelected(selectedFeedFilter: FeedFilter.Timeline())
      case .read:
        homeViewModel.onFeedFilterSelected(selectedFeedFilter: FeedFilter.Read())
      case .bookmarks:
        homeViewModel.onFeedFilterSelected(selectedFeedFilter: FeedFilter.Bookmarks())
      case .drawerCategory(let category):
        homeViewModel.onFeedFilterSelected(
          selectedFeedFilter: FeedFilter.Category(feedCategory: category.category))
      case .drawerFeedSource(let feedSource):
        homeViewModel.onFeedFilterSelected(
          selectedFeedFilter: FeedFilter.Source(feedSource: feedSource.feedSource))
      default:
        break
      }
    }
    .tabViewStyle(.sidebarAdaptable)
    .tabViewCustomization($tabViewCustomization)
    .task {
      for await state in homeViewModel.navDrawerState {
        self.navDrawerState = state
      }
    }
  }

  @ViewBuilder
  private func makeFeedSourceDrawerItem(
    drawerItem: DrawerItem.DrawerFeedSource
  ) -> some View {
    if let imageUrl = drawerItem.feedSource.logoUrl {
      LazyImage(url: URL(string: imageUrl)) { state in
        if let image = state.image {
          let size = CGSize(width: 24, height: 24)
          Image(size: size) { gc in
            gc.draw(image, in: .init(origin: .zero, size: size))
          }
          .cornerRadius(16)
        } else {
          Image(systemName: "square.stack.3d.up")
        }

      }
    } else {
      Image(systemName: "square.stack.3d.up")
    }

    Text(drawerItem.feedSource.title)
      .lineLimit(2)
      .font(.system(size: 16))
      .padding(.bottom, 2)
      .padding(.leading, Spacing.small)
  }
}

extension DrawerItem {
  var customizationID: String {
    return "com.prof18.feedflow.Tab" + self.name()
  }
}
