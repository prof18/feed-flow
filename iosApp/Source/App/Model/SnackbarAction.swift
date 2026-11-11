import Foundation

struct SnackbarAction {
    let label: String
    let accessibilityIdentifier: String?
    let handler: @MainActor () -> Void

    init(label: String, accessibilityIdentifier: String? = nil, handler: @escaping @MainActor () -> Void) {
        self.label = label
        self.accessibilityIdentifier = accessibilityIdentifier
        self.handler = handler
    }
}
