import Foundation

struct WebsiteBrowserNavigationState {
    private(set) var generation = 0
    private(set) var ready = false
    private var navigationID: ObjectIdentifier?

    mutating func started(_ navigation: AnyObject?) {
        generation += 1
        ready = false
        navigationID = navigation.map(ObjectIdentifier.init)
    }

    mutating func finished(_ navigation: AnyObject?) -> Bool {
        guard owns(navigation) else { return false }
        ready = true
        return true
    }

    mutating func failed(_ navigation: AnyObject?, error: Error) -> Bool {
        guard owns(navigation) else { return false }
        let error = error as NSError
        if error.domain == NSURLErrorDomain, error.code == NSURLErrorCancelled { return false }
        // WebKit reports a policy cancellation as a load error, even when the current page remains usable.
        if error.domain == "WebKitErrorDomain", error.code == 102 {
            ready = true
            return false
        }
        ready = false
        return true
    }

    func canCapture(generation: Int) -> Bool {
        ready && self.generation == generation
    }

    private func owns(_ navigation: AnyObject?) -> Bool {
        navigationID == navigation.map(ObjectIdentifier.init)
    }
}
