import FeedFlowKit
import Foundation

struct EditFeedSheetItem: Identifiable {
    let feedSource: FeedSource

    var id: String {
        feedSource.id
    }
}
