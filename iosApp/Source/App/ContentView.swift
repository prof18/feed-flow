import SwiftUI
import shared
import KMPNativeCoroutinesAsync

// TODO: clean up this file

struct ContentView: View {

    @EnvironmentObject var appState: AppState
    @EnvironmentObject var browserSelector: BrowserSelector
    @StateObject var homeViewModel = KotlinDependencies.shared.getHomeViewModel()

    var body: some View {

        ZStack {
            HomeContainer()
                .environmentObject(appState)
                .environmentObject(browserSelector)

            VStack(spacing: 0) {

                Spacer()

                Snackbar(messageQueue: $appState.snackbarQueue)
            }
        }
    }
}

// TODO: move the following stuff\

enum CompactViewRoute: Hashable {
    case feed
}

struct HomeContainer: View {

    @EnvironmentObject var appState: AppState
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @StateObject var homeViewModel = KotlinDependencies.shared.getHomeViewModel()

    @State
    private var selectedDrawerItem: DrawerItem? = DrawerItem.Timeline()

    var body: some View {
        if horizontalSizeClass == .compact {
            CompactView(selectedDrawerItem: $selectedDrawerItem, homeViewModel: homeViewModel)
        } else {
            RegularView(selectedDrawerItem: $selectedDrawerItem, homeViewModel: homeViewModel)
        }
    }
}

struct CompactView: View {

    @EnvironmentObject
    var appState: AppState

    @Binding
    var selectedDrawerItem: DrawerItem?

    let homeViewModel: HomeViewModel

    @State
    var navDrawerState: NavDrawerState = NavDrawerState(timeline: [], categories: [], feedSourcesByCategory: [:])

    var body: some View {
        NavigationStack(path: $appState.path) {
            SidebarDrawer(
                selectedDrawerItem: $selectedDrawerItem,
                navDrawerState: navDrawerState,
                onFeedFilterSelected: { feedFilter in
                    appState.navigate(route: CompactViewRoute.feed)
                    homeViewModel.onFeedFilterSelected(selectedFeedFilter: feedFilter)
                }
            )
                .navigationDestination(for: CommonRoute.self) { route in
                    switch route {
                    case .aboutScreen:
                        AboutScreen()

                    case .importExportScreen:
                        ImportExportScreen()
                    }
                }
                .navigationDestination(for: CompactViewRoute.self) { route in
                    switch route {
                    case .feed:
                        HomeScreen(homeViewModel: homeViewModel)
                    }
                }
        }
        .task {
            do {
                let stream = asyncSequence(for: homeViewModel.navDrawerStateFlow)
                for try await state in stream {
                    self.navDrawerState = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
    }
}

// struct ListDrawer: View {
//    
//    @Binding
//    var selectedDrawerItem: DrawerItem?
//    
//    var body: some View {
//        Text("ListDrawer")
//    }
// }

struct RegularView: View {
    @EnvironmentObject
    var appState: AppState

    @State
    var navDrawerState: NavDrawerState = NavDrawerState(timeline: [], categories: [], feedSourcesByCategory: [:])
    var drawerItems: [DrawerItem] = []

    @Binding
    var selectedDrawerItem: DrawerItem?

    let homeViewModel: HomeViewModel

    var body: some View {
        NavigationSplitView {
            SidebarDrawer(
                selectedDrawerItem: $selectedDrawerItem,
                navDrawerState: navDrawerState,
                onFeedFilterSelected: { feedFilter in
                    homeViewModel.onFeedFilterSelected(selectedFeedFilter: feedFilter)
                }
            )
        } detail: {
            NavigationStack {
                HomeScreen(homeViewModel: homeViewModel)
                    .navigationDestination(for: CommonRoute.self) { route in
                        switch route {
                        case .aboutScreen:
                            AboutScreen()

                        case .importExportScreen:
                            ImportExportScreen()
                        }
                    }
            }
        }
        .navigationSplitViewStyle(.balanced)
        .task {
            do {
                let stream = asyncSequence(for: homeViewModel.navDrawerStateFlow)
                for try await state in stream {
                    self.navDrawerState = state
                }
            } catch {
                self.appState.emitGenericError()
            }
        }
    }
}

struct SidebarDrawer: View {

    @Binding
    var selectedDrawerItem: DrawerItem?

    let navDrawerState: NavDrawerState
    let onFeedFilterSelected: (FeedFilter) -> Void

    var body: some View {
        List(selection: $selectedDrawerItem) {
            ForEach(navDrawerState.timeline, id: \.self) { drawerItem in
                DrawerTimelineItem()
                    .onTapGesture {
                        self.selectedDrawerItem = drawerItem
                        self.onFeedFilterSelected(FeedFilter.Timeline())
                    }
            }

            Section(
                content: {
                    ForEach(navDrawerState.categories, id: \.self) { drawerItem in
                        if let categoryItem = drawerItem as? DrawerItem.DrawerCategory {
                            DrawerCategoryItem(
                                drawerItem: categoryItem
                            ).onTapGesture {
                                self.selectedDrawerItem = categoryItem
                                self.onFeedFilterSelected(
                                    FeedFilter.Category(feedCategory: categoryItem.category)
                                )
                            }
                        }
                    }
                }, header: {
                    Text(localizer.drawer_title_categories.localized)
                }
            )

            Section(
                content: {
                    ForEach(
                        navDrawerState.feedSourcesByCategory.keys.sorted {
                            $0.feedSourceCategory?.title ?? "" < $1.feedSourceCategory?.title ?? ""
                        },
                        id: \.self
                    ) { category in
                        let categoryWrapper = category as
                        DrawerItem.DrawerFeedSource.DrawerFeedSourceFeedSourceCategoryWrapper

                        DisclosureGroup(
                            content: {
                                ForEach(
                                    navDrawerState.feedSourcesByCategory[categoryWrapper] ?? [],
                                    id: \.self
                                ) { drawerItem in
                                    if let drawerFeedSource = drawerItem as? DrawerItem.DrawerFeedSource {
                                        VStack(alignment: .leading) {
                                            Text(drawerFeedSource.feedSource.title)

                                            Text(drawerFeedSource.feedSource.url)
                                        }
                                        .onTapGesture {
                                            self.selectedDrawerItem = drawerItem
                                            self.onFeedFilterSelected(
                                                FeedFilter.Source(
                                                    feedSource: drawerFeedSource.feedSource
                                                )
                                            )
                                        }
                                    } else {
                                        EmptyView()
                                    }
                                }
                            },
                            label: {
                                Text(categoryWrapper.feedSourceCategory?.title ?? localizer.no_category.localized)
                            }
                        )
                    }
                },
                header: {
                    Text(localizer.drawer_title_feed_sources.localized)
                }
            )
        }
        .listStyle(.sidebar)
    }

}

struct DrawerTimelineItem: View {
    var body: some View {
        HStack {
            Label(
                localizer.drawer_title_timeline.localized,
                systemImage: "newspaper"
            )

            Spacer()
        }
    }
}

struct DrawerCategoryItem: View {

    let drawerItem: DrawerItem.DrawerCategory

    var body: some View {
        Label(
            drawerItem.category.title,
            systemImage: "tag"
        )
    }
}
