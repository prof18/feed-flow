import FeedFlowKit
import SwiftUI

#Preview("HomeContentLoading") {
    HomeContent(
        loadingState: .constant(
            InProgressFeedUpdateStatus(
                refreshedFeedCount: Int32(10),
                totalFeedCount: Int32(42)
            )
        ),
        feedState: .constant(feedItemsForPreview),
        showLoading: .constant(true),
        unreadCount: .constant(42),
        sheetToShow: .constant(nil),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        showSettings: .constant(false),
        showFeedSyncButton: .constant(false),
        columnVisibility: .constant(.all),
        feedFontSizes: .constant(defaultFeedFontSizes()),
        swipeActions: .constant(SwipeActions(leftSwipeAction: .none, rightSwipeAction: .none)),
        feedLayout: .constant(.list),
        onRefresh: {},
        updateReadStatus: { _ in },
        onMarkAllReadClick: {},
        onDeleteOldFeedClick: {},
        onForceRefreshClick: {},
        deleteAllFeeds: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onReaderModeClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in },
        onMarkAllAboveAsRead: { _ in },
        onMarkAllBelowAsRead: { _ in },
        onBackToTimelineClick: {},
        onFeedSyncClick: {},
        openDrawer: {}
    )
    .environment(HomeListIndexHolder(fakeHomeViewModel: true))
    .environment(AppState())
    .environment(BrowserSelector())
}

#Preview("HomeContentLoaded") {
    HomeContent(
        loadingState: .constant(
            FinishedFeedUpdateStatus()
        ),
        feedState: .constant(feedItemsForPreview),
        showLoading: .constant(false),
        unreadCount: .constant(42),
        sheetToShow: .constant(nil),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        showSettings: .constant(false),
        showFeedSyncButton: .constant(false),
        columnVisibility: .constant(.all),
        feedFontSizes: .constant(defaultFeedFontSizes()),
        swipeActions: .constant(SwipeActions(leftSwipeAction: .none, rightSwipeAction: .none)),
        feedLayout: .constant(.list),
        onRefresh: {},
        updateReadStatus: { _ in },
        onMarkAllReadClick: {},
        onDeleteOldFeedClick: {},
        onForceRefreshClick: {},
        deleteAllFeeds: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onReaderModeClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in },
        onMarkAllAboveAsRead: { _ in },
        onMarkAllBelowAsRead: { _ in },
        onBackToTimelineClick: {},
        onFeedSyncClick: {},
        openDrawer: {}
    )
    .environment(HomeListIndexHolder(fakeHomeViewModel: true))
    .environment(AppState())
    .environment(BrowserSelector())
}

#Preview("HomeContentSettings") {
    HomeContent(
        loadingState: .constant(
            FinishedFeedUpdateStatus()
        ),
        feedState: .constant(feedItemsForPreview),
        showLoading: .constant(false),
        unreadCount: .constant(42),
        sheetToShow: .constant(HomeSheetToShow.noFeedSource),
        toggleListScroll: .constant(false),
        currentFeedFilter: .constant(FeedFilter.Timeline()),
        showSettings: .constant(false),
        showFeedSyncButton: .constant(false),
        columnVisibility: .constant(.all),
        feedFontSizes: .constant(defaultFeedFontSizes()),
        swipeActions: .constant(SwipeActions(leftSwipeAction: .none, rightSwipeAction: .none)),
        feedLayout: .constant(.list),
        onRefresh: {},
        updateReadStatus: { _ in },
        onMarkAllReadClick: {},
        onDeleteOldFeedClick: {},
        onForceRefreshClick: {},
        deleteAllFeeds: {},
        requestNewPage: {},
        onItemClick: { _ in },
        onReaderModeClick: { _ in },
        onBookmarkClick: { _, _ in },
        onReadStatusClick: { _, _ in },
        onMarkAllAboveAsRead: { _ in },
        onMarkAllBelowAsRead: { _ in },
        onBackToTimelineClick: {},
        onFeedSyncClick: {},
        openDrawer: {}
    )
    .environment(HomeListIndexHolder(fakeHomeViewModel: true))
    .environment(AppState())
    .environment(BrowserSelector())
}
