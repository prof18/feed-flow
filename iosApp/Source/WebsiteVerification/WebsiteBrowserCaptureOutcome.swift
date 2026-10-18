struct WebsiteBrowserCaptureOutcome {
    private(set) var requestID: String?
    private(set) var isComplete = false

    mutating func beginRequest(id: String) -> Bool {
        guard requestID != id else { return false }
        requestID = id
        isComplete = false
        return true
    }

    mutating func complete(requestID: String) -> Bool {
        guard self.requestID == requestID else { return false }
        isComplete = true
        return true
    }
}
