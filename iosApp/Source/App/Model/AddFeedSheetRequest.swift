import Foundation

/// One presentation of the Add feed sheet. Every request gets a new id, so the same link opened twice
/// still rebuilds the sheet with a fresh field.
struct AddFeedSheetRequest: Identifiable {
    let id = UUID()
    let initialUrl: String?
}
