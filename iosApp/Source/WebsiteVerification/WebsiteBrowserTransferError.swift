import FeedFlowKit

struct WebsiteBrowserTransferError: Error {
    let reason: String

    var failure: VerificationFailure {
        switch reason {
        case "timeout": .timeout
        case "http": .accessDenied
        case "navigation", "origin", "redirect", "owner", "canceled": .browserStopped
        default: .invalidContent
        }
    }
}
