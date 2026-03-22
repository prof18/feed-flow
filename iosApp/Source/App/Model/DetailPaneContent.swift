import Foundation

enum DetailPaneContent: Equatable {
    case empty
    case readerMode
    case inAppBrowser(url: URL)
}
