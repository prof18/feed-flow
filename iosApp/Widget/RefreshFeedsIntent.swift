import AppIntents

struct RefreshFeedsIntent: AppIntent {
    static let title: LocalizedStringResource = "Refresh feeds"

    func perform() async throws -> some IntentResult {
        try await WidgetRefreshCoordinator.shared.refresh()
        return .result()
    }
}
